package dev.zwander.cameraxinfo.model

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.compose.runtime.*
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.zwander.cameraxinfo.awaitAvailability
import dev.zwander.cameraxinfo.data.CameraInfoHolder
import dev.zwander.cameraxinfo.data.ExtensionAvailability
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.data.Node
import dev.zwander.cameraxinfo.data.createTreeFromPaths
import dev.zwander.cameraxinfo.util.awaitCatchingError
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.tasks.await

val LocalDataModel = compositionLocalOf<DataModel> { error("No DataModel set") }

class DataModel {
    val supportedQualities = mutableStateMapOf<String, List<String>>()
    val physicalSensors = mutableStateMapOf<String, Map<String, CameraCharacteristics>>()
    val extensions = mutableStateMapOf<String, Map<Int, ExtensionAvailability>>()
    val cameraInfos = mutableStateListOf<CameraInfoHolder>()

    var arCoreStatus by mutableStateOf<ArCoreApk.Availability?>(null)
    var depthStatus by mutableStateOf<Boolean?>(null)

    var currentPath by mutableStateOf<Node?>(null)

    suspend fun populatePath() = coroutineScope {
        val firestore = Firebase.firestore

        currentPath = firestore.collectionGroup("CameraDataNode").get().awaitCatchingError().createTreeFromPaths()
    }

    @SuppressLint("UnsafeOptInUsageError", "InlinedApi")
    suspend fun populate(context: Context) = coroutineScope {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (currentPath != null) {
            withContext(Dispatchers.IO) {
                populatePath()
            }
        }

        extensions.clear()
        arCoreStatus = null
        depthStatus = null

        val (p, e) = withContext(Dispatchers.IO) {
            val provider = ProcessCameraProvider.getInstance(context).await()
            provider to ExtensionsManager.getInstanceAsync(context, provider)
        }

        val newList = p.availableCameraInfos.map {
            CameraInfoHolder(
                cameraInfo = it,
                camera2Info = Camera2CameraInfo.from(it)
            ).also { (info, info2) ->
                @Suppress("DeferredResultUnused")
                async {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val physicals = withContext(Dispatchers.IO) {
                            val logicalChars =
                                cameraManager.getCameraCharacteristics(info2.cameraId)

                            logicalChars.physicalCameraIds.map { id ->
                                id to cameraManager.getCameraCharacteristics(id)
                            }
                        }

                        physicalSensors[info2.cameraId] = physicals.toMap()
                    }
                }

                @Suppress("DeferredResultUnused")
                async {
                    val camera2Extensions =
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            withContext(Dispatchers.IO) {
                                cameraManager.getCameraExtensionCharacteristics(info2.cameraId).supportedExtensions
                            }
                        } else {
                            listOf()
                        }

                    val extensionAvailability = arrayOf(
                        ExtensionMode.AUTO to CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
                        ExtensionMode.BOKEH to CameraExtensionCharacteristics.EXTENSION_BOKEH,
                        ExtensionMode.HDR to CameraExtensionCharacteristics.EXTENSION_HDR,
                        ExtensionMode.NIGHT to CameraExtensionCharacteristics.EXTENSION_NIGHT,
                        ExtensionMode.FACE_RETOUCH to CameraExtensionCharacteristics.EXTENSION_BEAUTY
                    ).map { (cameraXExtension, camera2Extension) ->
                        cameraXExtension to ExtensionAvailability(
                            extension = cameraXExtension,
                            camera2Availability = camera2Extensions.contains(camera2Extension),
                            cameraXAvailability = e.await().isExtensionAvailable(
                                info.cameraSelector,
                                cameraXExtension
                            )
                        )
                    }

                    extensions[info2.cameraId] = extensionAvailability.toMap()
                }

                @Suppress("DeferredResultUnused")
                async {
                    supportedQualities[info2.cameraId] =
                        QualitySelector.getSupportedQualities(info).map { quality ->
                            context.resources.getString(
                                when (quality) {
                                    Quality.SD -> (R.string.sd)
                                    Quality.HD -> (R.string.hd)
                                    Quality.FHD -> (R.string.fhd)
                                    Quality.UHD -> (R.string.uhd)
                                    else -> (R.string.unknown)
                                }
                            )
                        }.asReversed()
                }
            }
        }.sortedBy { (_, info2) ->
            info2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)?.times(-1)
        }

        cameraInfos.clear()
        cameraInfos.addAll(newList)

        val status = withContext(Dispatchers.IO) {
            ArCoreApk.getInstance().run {
                try {
                    this::class.java.declaredFields.find { it.type == ArCoreApk.Availability::class.java }
                        ?.apply { isAccessible = true }
                        ?.set(this, null)
                } catch (e: Exception) {
                    Log.e("CameraXInfo", "Error unsetting ARCore status", e)
                }

                try {
                    awaitAvailability(context)
                } catch (e: Exception) {
                    Log.e("CameraXInfo", "Error awaiting ARCore availability", e)
                    null
                }
            }
        }

        depthStatus = if (status == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
            withContext(Dispatchers.IO) {
                try {
                    val session = Session(context)
                    session.isDepthModeSupported(Config.DepthMode.AUTOMATIC).also {
                        session.close()
                    }
                } catch (e: Exception) {
                    Log.e("CameraXInfo", "Error checking depth mode status", e)
                    false
                }
            }
        } else {
            null
        }

        arCoreStatus = try {
            Session(context).close()
            status
        } catch (e: Exception) {
            Log.e("CameraXInfo", "Error opening session", e)
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        }
    }
}