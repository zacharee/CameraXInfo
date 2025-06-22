package dev.zwander.cameraxinfo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.icu.text.DecimalFormat
import android.icu.text.DecimalFormatSymbols
import android.net.Uri
import android.util.Size
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.ar.core.ArCoreApk
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan
import androidx.core.net.toUri

fun Int?.lensFacingToString(context: Context, ui: Boolean = true): String {
    return context.resources.getString(
        when (this) {
            CameraCharacteristics.LENS_FACING_FRONT -> if (ui) R.string.front_facing else R.string.front_facing_json
            CameraCharacteristics.LENS_FACING_BACK -> if (ui) R.string.rear_facing else R.string.rear_facing_json
            CameraCharacteristics.LENS_FACING_EXTERNAL -> if (ui) R.string.external else R.string.external_json
            else -> if (ui) R.string.unknown else R.string.unknown_json
        }
    )
}

fun Int.extensionModeToString(context: Context, ui: Boolean = true): String {
    return context.resources.getString(
        when (this) {
            ExtensionMode.AUTO -> if (ui) R.string.auto else R.string.auto_json
            ExtensionMode.BOKEH -> if (ui) R.string.bokeh else R.string.bokeh_json
            ExtensionMode.HDR -> if (ui) R.string.hdr else R.string.hdr_json
            ExtensionMode.NIGHT -> if (ui) R.string.night else R.string.night_json
            ExtensionMode.FACE_RETOUCH -> if (ui) R.string.face_retouch else R.string.face_retouch_json
            ExtensionMode.NONE -> if (ui) R.string.none else R.string.none_json
            else -> if (ui) R.string.unknown else R.string.unknown_json
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

fun getFOV(focal: Float, frameSize: SizeF, alwaysShowPeriod: Boolean = false): String {
    return DecimalFormat(
        "##.#",
        DecimalFormatSymbols.getInstance().apply {
            if (alwaysShowPeriod) {
                decimalSeparator = '.'
            }
        }
    ).format(
        2f * atan(frameSize.run { height * 16.0 / 9.0 } / (focal * 2f)) * 180 / PI
    )
}

fun Context.launchUrl(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = url.toUri()

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