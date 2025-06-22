package dev.zwander.cameraxinfo.model

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
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
import dev.zwander.cameraxinfo.util.signInIfNeeded
import kotlinx.coroutines.*
import kotlinx.coroutines.guava.await
import kotlin.math.absoluteValue

val LocalDataModel = compositionLocalOf<DataModel> { error("No DataModel set") }

class DataModel {
    val supportedSdrQualities = mutableStateMapOf<String, List<String>>()
    val supportedHlgQualities = mutableStateMapOf<String, List<String>>()
    val supportedHdr10Qualities = mutableStateMapOf<String, List<String>>()
    val supportedHdr10PlusQualities = mutableStateMapOf<String, List<String>>()
    val supportedDolbyVision10BitQualities = mutableStateMapOf<String, List<String>>()
    val supportedDolbyVision8BitQualities = mutableStateMapOf<String, List<String>>()
    val physicalSensors = mutableStateMapOf<String, Map<String, CameraCharacteristics>>()
    val extensions = mutableStateMapOf<String, Map<Int, ExtensionAvailability>>()
    val cameraInfos = mutableStateListOf<CameraInfoHolder>()
    val imageCaptureCapabilities = mutableStateMapOf<String, List<String>>()

    var arCoreStatus by mutableStateOf<ArCoreApk.Availability?>(null)
    var depthStatus by mutableStateOf<Boolean?>(null)

    var currentPath by mutableStateOf<Node?>(null)
    var pathLoadError by mutableStateOf<Exception?>(null)

    private var previousPathPopulateTime = 0L

    suspend fun populatePath(context: Context) = coroutineScope {
        val firestore = Firebase.firestore

        currentPath = null

        val signInResult = signInIfNeeded()

        if (signInResult != null) {
            currentPath = Node(
                name = context.resources.getString(R.string.error, signInResult.message)
            )
            return@coroutineScope
        }

        val group = firestore.collectionGroup("CameraDataNode")
        val g = group.addSnapshotListener { _, _ -> }

        currentPath = try {
            group.get().awaitCatchingError().createTreeFromPaths()
        } catch (e: Exception) {
            pathLoadError = e
            null
        }
        g.remove()
    }

    @SuppressLint("UnsafeOptInUsageError", "InlinedApi")
    suspend fun populate(context: Context) = coroutineScope {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val currentTime = System.currentTimeMillis()

        if (currentPath != null && (currentTime - previousPathPopulateTime).absoluteValue > 30_000) {
            previousPathPopulateTime = currentTime
            withContext(Dispatchers.IO) {
                populatePath(context)
            }
        }

        extensions.clear()
        arCoreStatus = null
        depthStatus = null

        val (p, e) = withContext(Dispatchers.IO) {
            val provider = ProcessCameraProvider.getInstance(context).await()
            provider to ExtensionsManager.getInstanceAsync(context, provider).await()
        }

        val newList = p.availableCameraInfos.map {
            CameraInfoHolder(
                cameraInfo = it,
                camera2Info = Camera2CameraInfo.from(it)
            ).also { (info, info2) ->
                launch(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        try {
                            val physicals = withContext(Dispatchers.IO) {
                                val logicalChars =
                                    cameraManager.getCameraCharacteristics(info2.cameraId)

                                logicalChars.physicalCameraIds.map { id ->
                                    id to cameraManager.getCameraCharacteristics(id)
                                }
                            }

                            physicalSensors[info2.cameraId] = physicals.toMap()
                        } catch (_: IllegalArgumentException) {
                            launch(Dispatchers.Main) {
                                Toast.makeText(context, R.string.unable_to_retrieve_physical_cameras, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch(Dispatchers.IO) {
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
                        ExtensionMode.FACE_RETOUCH to CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH
                    ).map { (cameraXExtension, camera2Extension) ->
                        cameraXExtension to ExtensionAvailability(
                            extension = cameraXExtension,
                            camera2Availability = camera2Extensions.contains(camera2Extension),
                            cameraXAvailability = try {
                                e.isExtensionAvailable(
                                    info.cameraSelector,
                                    cameraXExtension
                                )
                            } catch (_: IllegalStateException) {
                                // There's a bug in the Pixel Android 14 DP2 CameraX vendor lib where
                                // it uses the wrong extension constants when initializing extensions,
                                // causing a crash when checking for extension availability.
                                null
                            }
                        )
                    }

                    extensions[info2.cameraId] = extensionAvailability.toMap()
                }

                launch(Dispatchers.IO) {
                    val videoCapabilities = Recorder.getVideoCapabilities(info)
                    supportedSdrQualities[info2.cameraId] =
                        videoCapabilities.getSupportedQualities(DynamicRange.SDR).mapQualities(context)
                    supportedHlgQualities[info2.cameraId] =
                        videoCapabilities.getSupportedQualities(DynamicRange.HLG_10_BIT).mapQualities(context)
                    supportedHdr10Qualities[info2.cameraId] =
                        videoCapabilities.getSupportedQualities(DynamicRange.HDR10_10_BIT).mapQualities(context)
                    supportedHdr10PlusQualities[info2.cameraId] =
                        videoCapabilities.getSupportedQualities(DynamicRange.HDR10_PLUS_10_BIT).mapQualities(context)
                    supportedDolbyVision10BitQualities[info2.cameraId] =
                        videoCapabilities.getSupportedQualities(DynamicRange.DOLBY_VISION_10_BIT).mapQualities(context)
                    supportedDolbyVision8BitQualities[info2.cameraId] =
                        videoCapabilities.getSupportedQualities(DynamicRange.DOLBY_VISION_8_BIT).mapQualities(context)
                }

                launch(Dispatchers.IO) {
                    val imageCaptureCapabilities = ImageCapture.getImageCaptureCapabilities(info)
                    val captureProcessProgress = imageCaptureCapabilities.isCaptureProcessProgressSupported
                    val postView = imageCaptureCapabilities.isPostviewSupported
                    val outputFormats = imageCaptureCapabilities.supportedOutputFormats

                    val capabilitiesList = mutableListOf<String>()

                    if (captureProcessProgress) {
                        capabilitiesList.add(context.resources.getString(R.string.capture_process_progress))
                    }

                    if (postView) {
                        capabilitiesList.add(context.resources.getString(R.string.postview))
                    }

                    capabilitiesList.addAll(outputFormats.map { format ->
                        val value = when (format) {
                            ImageCapture.OUTPUT_FORMAT_JPEG -> R.string.jpeg
                            ImageCapture.OUTPUT_FORMAT_RAW -> R.string.raw
                            ImageCapture.OUTPUT_FORMAT_RAW_JPEG -> R.string.raw_jpeg
                            ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR -> R.string.ultra_hdr
                            else -> R.string.unknown
                        }

                        context.resources.getString(
                            R.string.output_format,
                            context.resources.getString(value),
                        )
                    })

                    this@DataModel.imageCaptureCapabilities[info2.cameraId] = capabilitiesList
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
                } catch (e: Throwable) {
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
        } catch (e: Throwable) {
            Log.e("CameraXInfo", "Error opening session", e)
            ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
        }
    }
}

private fun List<Quality>.mapQualities(context: Context): List<String> {
    return map { quality ->
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