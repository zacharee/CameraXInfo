package dev.zwander.cameraxinfo.util

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.icu.text.SimpleDateFormat
import android.os.Build
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import dev.zwander.cameraxinfo.extensionModeToString
import dev.zwander.cameraxinfo.getFOV
import dev.zwander.cameraxinfo.lensFacingToString
import dev.zwander.cameraxinfo.model.DataModel
import dev.zwander.cameraxinfo.ui.components.defaultExtensionState
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

sealed class UploadResult(open val e: Exception?) {
    object Uploading : UploadResult(null)
    object Success : UploadResult(null)

    object DuplicateData : ErrorResult(null)
    object SafetyNetFailure : ErrorResult(null)

    data class SignInFailure(override val e: Exception?) : ErrorResult(e)
    data class UploadFailure(override val e: Exception?) : ErrorResult(e)

    sealed class ErrorResult(e: Exception?) : UploadResult(e)
}

suspend fun DataModel.uploadToCloud(context: Context): UploadResult {
    try {
        if (!context.verifySafetyNet()) {
            return UploadResult.SafetyNetFailure
        }
    } catch (e: Exception) {
        return UploadResult.UploadFailure(e)
    }

    try {
        BackendlessUtils.ensureLogin()
    } catch (e: Exception) {
        return UploadResult.SignInFailure(e)
    }

    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())

        val path = "/CameraData/${Build.BRAND.uppercase()}/${Build.MODEL.uppercase()}/${Build.VERSION.SDK_INT}/"
        val file = "${sdf.format(Date())}.json"

        val newInfo = buildInfo(context)

        try {
            BackendlessUtils.uploadFile(path, file, newInfo)
        } catch (e: Exception) {
            return UploadResult.UploadFailure(e)
        }
    } catch (e: Exception) {
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
                    }
                )
                put(
                    "video_qualities",
                    JSONArray().apply {
                        supportedQualities[info2.cameraId]?.forEach { q ->
                            put(q)
                        }
                    }
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
                    }
                )
            }
        )
    }

    return infoObject.toString(4)
}