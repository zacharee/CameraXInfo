package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.extensionModeToString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsCard(extensionAvailability: Map<Int, Boolean?>) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
        ),
        modifier = Modifier.animateContentSize().fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.extensions),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            extensionAvailability.forEach { (extension, available) ->
                CameraFeature(
                    featureName = extension.extensionModeToString(),
                    supported = available ?: false
                )
            }
        }
    }
}