package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.extensionModeToString

private const val COLUMN_WEIGHT = 0.5f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsCard(extensionAvailability: Map<Int, Pair<Boolean?, Boolean?>>) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
        ),
        modifier = Modifier
            .animateContentSize()
            .fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(id = R.string.extension),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = stringResource(id = R.string.camera2),
                    modifier = Modifier.weight(COLUMN_WEIGHT),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.camerax),
                    modifier = Modifier.weight(COLUMN_WEIGHT),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            extensionAvailability.forEach { (extension, availability) ->
                val (camera2Availability, cameraXAvailability) = availability

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    verticalAlignment = CenterVertically
                ) {
                    Text(
                        text = extension.extensionModeToString(),
                        modifier = Modifier.weight(1f)
                    )

                    SupportStateIcon(
                        state = camera2Availability,
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )

                    SupportStateIcon(
                        state = cameraXAvailability,
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )
                }
            }
        }
    }
}
