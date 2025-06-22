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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.data.ExtensionAvailability
import dev.zwander.cameraxinfo.extensionModeToString

private const val COLUMN_WEIGHT = 0.5f

@Composable
fun ExtensionsCard(extensionAvailability: Map<Int, ExtensionAvailability>) {
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

                TooltippedIcon(
                    painter = painterResource(R.drawable.numeric_2_circle_outline),
                    contentDescription = stringResource(R.string.camera2),
                    modifier = Modifier.weight(COLUMN_WEIGHT),
                )

                Spacer(Modifier.width(8.dp))

                TooltippedIcon(
                    painter = painterResource(R.drawable.alpha_x_circle_outline),
                    contentDescription = stringResource(R.string.camerax),
                    modifier = Modifier.weight(COLUMN_WEIGHT),
                )

                Spacer(Modifier.width(8.dp))

                TooltippedIcon(
                    painter = painterResource(R.drawable.arm_flex),
                    contentDescription = stringResource(R.string.camerax_strength),
                    modifier = Modifier.weight(COLUMN_WEIGHT),
                )
            }

            extensionAvailability.forEach { (extension, availability) ->
                val (_, camera2Availability, cameraXAvailability, extensionStrengthAvailability) = availability

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = CenterVertically
                ) {
                    Text(
                        text = extension.extensionModeToString(LocalContext.current),
                        modifier = Modifier.weight(1f)
                    )

                    SupportStateIcon(
                        state = camera2Availability,
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )

                    Spacer(Modifier.width(8.dp))

                    SupportStateIcon(
                        state = cameraXAvailability,
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )

                    Spacer(Modifier.width(8.dp))

                    SupportStateIcon(
                        state = extensionStrengthAvailability,
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )
                }
            }
        }
    }
}
