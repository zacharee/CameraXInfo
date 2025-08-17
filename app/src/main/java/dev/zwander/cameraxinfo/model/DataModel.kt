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
import androidx.camera.core.CameraUnavailableException
import androidx.camera.core.DynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.Recorder
import androidx.compose.runtime.*
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import dev.zwander.cameraxinfo.awaitAvailability
import dev.zwander.cameraxinfo.data.CameraInfoHolder
import dev.zwander.cameraxinfo.data.ExtensionAvailability
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.data.Node
import dev.zwander.cameraxinfo.data.createTreeFromPaths
import dev.zwander.cameraxinfo.util.awaitCatchingError
import dev.zwander.cameraxinfo.util.signInIfNeeded
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.guava.await
import kotlin.math.absoluteValue

val LocalDataModel = compositionLocalOf<DataModel> { error("No DataModel set") }

class DataModel {
    val supportedSdrQualities = MutableStateFlow<Map<String, List<String>>>(mapOf())
    val supportedHlgQualities = MutableStateFlow<Map<String, List<String>>>(mapOf())
    val supportedHdr10Qualities = MutableStateFlow<Map<String, List<String>>>(mapOf())
    val supportedHdr10PlusQualities = MutableStateFlow<Map<String, List<String>>>(mapOf())
    val supportedDolbyVision10BitQualities = MutableStateFlow<Map<String, List<String>>>(mapOf())
    val supportedDolbyVision8BitQualities = MutableStateFlow<Map<String, List<String>>>(mapOf())
    val physicalSensors = MutableStateFlow<Map<String, Map<String, CameraCharacteristics>>>(mapOf())
    val extensions = MutableStateFlow<Map<String, Map<Int, ExtensionAvailability>>>(mapOf())
    val cameraInfos = MutableStateFlow<List<CameraInfoHolder>>(listOf())
    val imageCaptureCapabilities = MutableStateFlow<Map<String, List<String>>>(mapOf())

    val arCoreStatus = MutableStateFlow<ArCoreApk.Availability?>(null)
    val depthStatus = MutableStateFlow<Boolean?>(null)

    val currentPath = MutableStateFlow<Node?>(null)
    val pathLoadError = MutableStateFlow<Exception?>(null)

    private var previousPathPopulateTime = 0L

    suspend fun populatePath(context: Context) = coroutineScope {
        val firestore = Firebase.firestore

        currentPath.value = null

        val signInResult = signInIfNeeded()

        if (signInResult != null) {
            currentPath.value = Node(
                name = context.resources.getString(R.string.error, signInResult.message)
            )
            return@coroutineScope
        }

        val group = firestore.collectionGroup("CameraDataNode")
        val g = group.addSnapshotListener { _, _ -> }

        currentPath.value = try {
            group.get().awaitCatchingError().createTreeFromPaths()
        } catch (e: Exception) {
            pathLoadError.value = e
            null
        }
        g.remove()
    }

    @SuppressLint("UnsafeOptInUsageError", "InlinedApi")
    suspend fun populate(context: Context) = coroutineScope {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val currentTime = System.currentTimeMillis()

        if (currentPath.value != null && (currentTime - previousPathPopulateTime).absoluteValue > 30_000) {
            previousPathPopulateTime = currentTime
            withContext(Dispatchers.IO) {
                populatePath(context)
            }
        }

        extensions.value = mapOf()
        arCoreStatus.value = null
        depthStatus.value = null

        launch(Dispatchers.IO) {
            val p = try {
                ProcessCameraProvider.getInstance(context).await()
            } catch (e: CameraUnavailableException) {
                Bugsnag.leaveBreadcrumb(
                    "Error getting cameras",
                    mapOf("error" to e),
                    BreadcrumbType.ERROR,
                )
                return@launch
            }
            val e = ExtensionsManager.getInstanceAsync(context, p).await()

            val newList = p.availableCameraInfos.map {
                CameraInfoHolder(
                    cameraInfo = it,
                    camera2Info = Camera2CameraInfo.from(it),
                ).also { (info, info2) ->
                    launch(Dispatchers.IO) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            val newPhysicalSensors = mutableMapOf<String, Map<String, CameraCharacteristics>>()

                            try {
                                val logicalChars =
                                    cameraManager.getCameraCharacteristics(info2.cameraId)
                                val physicals = logicalChars.physicalCameraIds.map { id ->
                                    id to cameraManager.getCameraCharacteristics(id)
                                }

                                newPhysicalSensors[info2.cameraId] = physicals.toMap()
                            } catch (_: IllegalArgumentException) {
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, R.string.unable_to_retrieve_physical_cameras, Toast.LENGTH_SHORT).show()
                                }
                            }

                            physicalSensors.update { s ->
                                s + newPhysicalSensors
                            }
                        }
                    }

                    launch(Dispatchers.IO) {
                        val newExtensionsMap = mutableMapOf<String, Map<Int, ExtensionAvailability>>()

                        val camera2Extensions = try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                cameraManager.getCameraExtensionCharacteristics(info2.cameraId).supportedExtensions
                            } else {
                                listOf()
                            }
                        } catch (e: NullPointerException) {
                            Bugsnag.leaveBreadcrumb(
                                "Error getting camera extensions",
                                mapOf("error" to e),
                                BreadcrumbType.ERROR,
                            )
                            listOf()
                        }

                        val extensionAvailability = arrayOf(
                            ExtensionMode.AUTO to CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
                            ExtensionMode.BOKEH to CameraExtensionCharacteristics.EXTENSION_BOKEH,
                            ExtensionMode.HDR to CameraExtensionCharacteristics.EXTENSION_HDR,
                            ExtensionMode.NIGHT to CameraExtensionCharacteristics.EXTENSION_NIGHT,
                            ExtensionMode.FACE_RETOUCH to CameraExtensionCharacteristics.EXTENSION_FACE_RETOUCH
                        ).map { (cameraXExtension, camera2Extension) ->
                            val cameraXExtensionAvailable = try {
                                e.isExtensionAvailable(
                                    info.cameraSelector,
                                    cameraXExtension,
                                )
                            } catch (_: IllegalStateException) {
                                // There's a bug in the Pixel Android 14 DP2 CameraX vendor lib where
                                // it uses the wrong extension constants when initializing extensions,
                                // causing a crash when checking for extension availability.
                                null
                            }

                            cameraXExtension to ExtensionAvailability(
                                extension = cameraXExtension,
                                camera2Availability = camera2Extensions.contains(camera2Extension),
                                cameraXAvailability = cameraXExtensionAvailable,
                                strengthAvailability = when (cameraXExtensionAvailable) {
                                    true -> {
                                        e.getCameraExtensionsInfo(
                                            p.getCameraInfo(e.getExtensionEnabledCameraSelector(info.cameraSelector, cameraXExtension)),
                                        ).isExtensionStrengthAvailable
                                    }
                                    false -> {
                                        false
                                    }
                                    else -> {
                                        null
                                    }
                                },
                            )
                        }

                        newExtensionsMap[info2.cameraId] = extensionAvailability.toMap()

                        extensions.update { e ->
                            e + newExtensionsMap
                        }
                    }

                    launch(Dispatchers.IO) {
                        val videoCapabilities = Recorder.getVideoCapabilities(info)

                        supportedSdrQualities.update { q ->
                            q.toMutableMap().apply {
                                this[info2.cameraId] =
                                    videoCapabilities.getSupportedQualities(DynamicRange.SDR).mapQualities(context)
                            }
                        }
                        supportedHlgQualities.update { q ->
                            q.toMutableMap().apply {
                                this[info2.cameraId] =
                                    videoCapabilities.getSupportedQualities(DynamicRange.HLG_10_BIT).mapQualities(context)
                            }
                        }
                        supportedHdr10Qualities.update { q ->
                            q.toMutableMap().apply {
                                this[info2.cameraId] =
                                    videoCapabilities.getSupportedQualities(DynamicRange.HDR10_10_BIT).mapQualities(context)
                            }
                        }
                        supportedHdr10PlusQualities.update { q ->
                            q.toMutableMap().apply {
                                this[info2.cameraId] =
                                    videoCapabilities.getSupportedQualities(DynamicRange.HDR10_PLUS_10_BIT).mapQualities(context)
                            }
                        }
                        supportedDolbyVision10BitQualities.update { q ->
                            q.toMutableMap().apply {
                                this[info2.cameraId] =
                                    videoCapabilities.getSupportedQualities(DynamicRange.DOLBY_VISION_10_BIT).mapQualities(context)
                            }
                        }
                        supportedDolbyVision8BitQualities.update { q ->
                            q.toMutableMap().apply {
                                this[info2.cameraId] =
                                    videoCapabilities.getSupportedQualities(DynamicRange.DOLBY_VISION_8_BIT).mapQualities(context)
                            }
                        }
                    }

                    launch(Dispatchers.IO) {
                        val newCapabilitiesMap = mutableMapOf<String, List<String>>()

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

                        newCapabilitiesMap[info2.cameraId] = capabilitiesList

                        this@DataModel.imageCaptureCapabilities.update { c ->
                            c + newCapabilitiesMap
                        }
                    }
                }
            }.sortedWith { (_, info1), (_, info2) ->
                val firstFacing = info1.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)?.times(-1) ?: -1
                val secondFacing = info2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)?.times(-1) ?: -1

                if (firstFacing != secondFacing) {
                    firstFacing.compareTo(secondFacing)
                } else {
                    info1.cameraId.compareTo(info2.cameraId)
                }
            }

            cameraInfos.value = newList
        }

        launch(Dispatchers.IO) {
            val status = ArCoreApk.getInstance().run {
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

            depthStatus.value = if (status == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                try {
                    val session = Session(context)
                    session.isDepthModeSupported(Config.DepthMode.AUTOMATIC).also {
                        session.close()
                    }
                } catch (e: Throwable) {
                    Log.e("CameraXInfo", "Error checking depth mode status", e)
                    false
                }
            } else {
                null
            }

            arCoreStatus.value = try {
                Session(context).close()
                status
            } catch (e: Throwable) {
                Log.e("CameraXInfo", "Error opening session", e)
                ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE
            }
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