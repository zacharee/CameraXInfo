package dev.zwander.cameraxinfo.util

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.os.Parcelable
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.zwander.cameraxinfo.BuildConfig
import dev.zwander.cameraxinfo.extensionModeToString
import dev.zwander.cameraxinfo.getFOV
import dev.zwander.cameraxinfo.lensFacingToString
import dev.zwander.cameraxinfo.model.DataModel
import dev.zwander.cameraxinfo.ui.components.defaultExtensionState
import kotlinx.parcelize.Parcelize
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

sealed class UploadResult(open val e: Exception?) : Parcelable {
    @Parcelize
    object Uploading : UploadResult(null)
    @Parcelize
    object Success : UploadResult(null)

    @Parcelize
    object DuplicateData : ErrorResult(null)
    @Parcelize
    object SafetyNetFailure : ErrorResult(null)

    @Parcelize
    class SignInFailure(@Transient override val e: Exception?) : ErrorResult(e)
    @Parcelize
    class UploadFailure(@Transient override val e: Exception?) : ErrorResult(e)

    sealed class ErrorResult(e: Exception?) : UploadResult(e)
}

suspend fun signInIfNeeded(): Exception? {
    val auth = FirebaseAuth.getInstance()

    return if (auth.currentUser == null) {
        val task = auth.signInAnonymously()
        task.awaitCatchingError()

        if (task.isSuccessful) {
            null
        } else {
            task.exception
        }
    } else {
        null
    }
}

suspend fun DataModel.uploadToCloud(context: Context): UploadResult {
    try {
        if (!context.verifySafetyNet()) {
            return UploadResult.SafetyNetFailure
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return UploadResult.UploadFailure(e)
    }

    val signInResult = try {
        signInIfNeeded()
    } catch (e: Exception) {
        e
    }

    if (signInResult != null) {
        return UploadResult.SignInFailure(signInResult)
    }

    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

        val querySnapshotListener = EventListener<QuerySnapshot> { _, _ -> }
        val documentSnapshotListener = EventListener<DocumentSnapshot> { _, _ -> }

        val firestore = Firebase.firestore
        val collection = firestore
            .collection("CameraData")
            .document(Build.BRAND.uppercase())
            .collection(Build.MODEL.uppercase())
            .document(Build.VERSION.SDK_INT.toString())
            .collection("CameraDataNode")

        val c = collection.addSnapshotListener(querySnapshotListener)
        val existingDocs = collection.get().awaitCatchingError().map { it.data.values.last().toString() }
        c.remove()

        val newInfo = buildInfo(context)

        if (!BuildConfig.DEBUG && existingDocs.contains(newInfo)) {
            return UploadResult.DuplicateData
        }

        val doc = collection.document(sdf.format(Date()))
        val d = doc.addSnapshotListener(documentSnapshotListener)
        val task = doc.set("data" to newInfo)
        task.awaitCatchingError()
        d.remove()

        if (!task.isSuccessful) {
            task.exception?.printStackTrace()
            return UploadResult.UploadFailure(task.exception)
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return UploadResult.UploadFailure(e)
    }

    return UploadResult.Success
}

@SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
fun JSONObject.insertCameraInfo(info: CameraInfo, context: Context) {
    insertCameraInfo(Camera2CameraInfo.extractCameraCharacteristics(info), context)
}

fun JSONObject.insertCameraInfo(info: CameraCharacteristics, context: Context) {
    put(
        "lens_facing",
        info.get(CameraCharacteristics.LENS_FACING).lensFacingToString(context, false)
    )
    put(
        "fov",
        getFOV(
            info.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOf { it } ?: 0f,
            info.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(0f, 0f),
            true
        )
    )
    put(
        "resolution",
        info.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)
    )
}

@SuppressLint("UnsafeOptInUsageError")
fun DataModel.buildInfo(context: Context): String {
    val infoObject = JSONObject()

    infoObject.put("device_brand", Build.BRAND)
    infoObject.put("device_model", Build.MODEL)
    infoObject.put("device_sdk", Build.VERSION.SDK_INT)
    infoObject.put("device_release", Build.VERSION.RELEASE)
    infoObject.put("device_security", Build.VERSION.SECURITY_PATCH)
    infoObject.put("build_fingerprint", Build.FINGERPRINT)

    infoObject.put(
        "arcore",
        JSONObject().apply {
            put("arcore_support", arCoreStatus)
            put("depth_support", depthStatus)
        }
    )

    cameraInfos.forEach { (info, info2) ->
        infoObject.put(
            info2.cameraId,
            JSONObject().apply {
                insertCameraInfo(info, context)
                put(
                    "physical_sensors",
                    JSONArray().apply {
                        physicalSensors[info2.cameraId]?.forEach { (_, u) ->
                            put(JSONObject().apply { insertCameraInfo(u, context) })
                        }
                    },
                )
                put(
                    "video_qualities",
                    JSONArray().apply {
                        supportedSdrQualities[info2.cameraId]?.forEach { q ->
                            put(q)
                        }
                    },
                )
                put(
                    "hlg_video_qualities",
                    JSONArray().apply {
                        supportedHlgQualities[info2.cameraId]?.forEach { q ->
                            put(q)
                        }
                    },
                )
                put(
                    "hdr_10_video_qualities",
                    JSONArray().apply {
                        supportedHdr10Qualities[info2.cameraId]?.forEach { q ->
                            put(q)
                        }
                    },
                )
                put(
                    "hdr_10_plus_video_qualities",
                    JSONArray().apply {
                        supportedHdr10PlusQualities[info2.cameraId]?.forEach { q ->
                            put(q)
                        }
                    },
                )
                put(
                    "dolby_vision_10_bit_video_qualities",
                    JSONArray().apply {
                        supportedDolbyVision10BitQualities[info2.cameraId]?.forEach { q ->
                            put(q)
                        }
                    },
                )
                put(
                    "dolby_vision_8_bit_video_qualities",
                    JSONArray().apply {
                        supportedDolbyVision8BitQualities[info2.cameraId]?.forEach { q ->
                            put(q)
                        }
                    },
                )
                put(
                    "image_capture_capabilities",
                    JSONArray().apply {
                        imageCaptureCapabilities[info2.cameraId]?.forEach { q ->
                            put(q)
                        }
                    },
                )
                put(
                    "extensions",
                    JSONObject().apply {
                        (extensions[info2.cameraId] ?: defaultExtensionState).forEach { (t, u) ->
                            put(
                                t.extensionModeToString(context, false),
                                JSONObject().apply {
                                    put("camera2", u.camera2Availability)
                                    put("camerax", u.cameraXAvailability)
                                }
                            )
                        }
                    },
                )
            }
        )
    }

    return infoObject.toString(4)
}