package dev.zwander.cameraxinfo.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import dev.zwander.cameraxinfo.launchUrl

@Composable
fun LinkIconButton(
    link: String,
    icon: Painter,
    contentDescription: String
) {
    val context = LocalContext.current

    IconButton(onClick = { context.launchUrl(link) }) {
        Icon(
            painter = icon,
            contentDescription = contentDescription
        )
    }
}