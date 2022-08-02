package dev.zwander.cameraxinfo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.icu.text.DecimalFormat
import android.net.Uri
import android.util.Size
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.atan

fun Int?.lensFacingToString(context: Context): String {
    return context.resources.getString(
        when (this) {
            CameraCharacteristics.LENS_FACING_FRONT -> R.string.front_facing
            CameraCharacteristics.LENS_FACING_BACK -> R.string.rear_facing
            CameraCharacteristics.LENS_FACING_EXTERNAL -> R.string.external
            else -> R.string.unknown
        }
    )
}

fun Int.extensionModeToString(context: Context): String {
    return context.resources.getString(
        when (this) {
            ExtensionMode.AUTO -> R.string.auto
            ExtensionMode.BOKEH -> R.string.bokeh
            ExtensionMode.HDR -> R.string.hdr
            ExtensionMode.NIGHT -> R.string.night
            ExtensionMode.FACE_RETOUCH -> R.string.face_retouch
            ExtensionMode.NONE -> R.string.none
            else -> R.string.unknown
        }
    )
}

@SuppressLint("UnsafeOptInUsageError")
fun Camera2CameraInfo.formatResolution(): String {
    return getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE).formatResolution()
}

fun Size?.formatResolution(): String {
    val pxTotal = this?.run { width * height } ?: 0

    return DecimalFormat("##.#").format(pxTotal / 1_000_000.0)
}

fun getFOV(focal: Float, frameSize: SizeF): String {
    return DecimalFormat("##.#").format(
        2f * atan(frameSize.run { height * 16.0 / 9.0 } / (focal * 2f)) * 180 / PI
    )
}

fun Context.launchUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)

        startActivity(intent)
    } catch (_: Exception) {}
}

suspend fun ArCoreApk.awaitAvailability(context: Context): ArCoreApk.Availability {
    val status = checkAvailability(context)

    return if (status.isTransient) {
        delay(200)
        awaitAvailability(context)
    } else {
        status
    }
}

var Context.latestUploadTime: Long
    get() = PreferenceManager.getDefaultSharedPreferences(this).getLong("upload_time", 0L)
    set(value) {
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putLong("upload_time", value)
        }
    }

var Context.latestDownloadTime: Long
    get() = PreferenceManager.getDefaultSharedPreferences(this).getLong("download_time", 0L)
    set(value) {
        PreferenceManager.getDefaultSharedPreferences(this).edit {
            putLong("download_time", value)
        }
    }