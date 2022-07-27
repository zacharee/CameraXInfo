package dev.zwander.cameraxinfo

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.hardware.camera2.CameraCharacteristics
import android.icu.text.DecimalFormat
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.util.Size
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlin.math.PI
import kotlin.math.atan

// https://stackoverflow.com/a/68935732/5496177
fun Spanned.toAnnotatedString(): AnnotatedString = buildAnnotatedString {
    val spanned = this@toAnnotatedString
    append(spanned.toString())
    getSpans(0, spanned.length, Any::class.java).forEach { span ->
        val start = getSpanStart(span)
        val end = getSpanEnd(span)
        when (span) {
            is StyleSpan -> when (span.style) {
                Typeface.BOLD -> addStyle(SpanStyle(fontWeight = FontWeight.Bold), start, end)
                Typeface.ITALIC -> addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, end)
                Typeface.BOLD_ITALIC -> addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic), start, end)
            }
            is UnderlineSpan -> addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
            is ForegroundColorSpan -> addStyle(SpanStyle(color = Color(span.foregroundColor)), start, end)
        }
    }
}

@Composable
fun Int?.lensFacingToString(): String {
    return stringResource(
        id = when (this) {
            CameraCharacteristics.LENS_FACING_FRONT -> R.string.front_facing
            CameraCharacteristics.LENS_FACING_BACK -> R.string.rear_facing
            CameraCharacteristics.LENS_FACING_EXTERNAL -> R.string.external
            else -> R.string.unknown
        }
    )
}

@Composable
fun Int.extensionModeToString(): String {
    return stringResource(
        id = when (this) {
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