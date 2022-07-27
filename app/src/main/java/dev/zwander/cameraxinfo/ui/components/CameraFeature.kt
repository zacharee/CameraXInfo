package dev.zwander.cameraxinfo.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import dev.zwander.cameraxinfo.R

@Composable
fun CameraFeature(featureName: String, supported: Boolean) {
    CameraItem(
        featureName = featureName,
        text = stringResource(id = if (supported) R.string.supported else R.string.unsupported),
        color = if (supported) Color.Green else Color.Red
    )
}