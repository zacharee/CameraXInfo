package dev.zwander.cameraxinfo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.zwander.cameraxinfo.R

@Composable
fun CameraFeature(featureName: String, supported: Boolean?) {
    val (statusText, statusColor) = when (supported) {
        true -> R.string.supported to Color.Green
        false -> R.string.unsupported to Color.Red
        else -> R.string.unknown to Color.Yellow
    }

    CameraItem(
        featureName = featureName,
        text = stringResource(id = statusText),
        color = statusColor
    )
}