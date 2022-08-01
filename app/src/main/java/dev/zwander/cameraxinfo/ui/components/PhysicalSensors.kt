package dev.zwander.cameraxinfo.ui.components

import android.hardware.camera2.CameraCharacteristics
import android.text.Html
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
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.formatResolution
import dev.zwander.cameraxinfo.getFOV
import dev.zwander.cameraxinfo.toAnnotatedString

private const val COLUMN_WEIGHT = 0.5f

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhysicalSensors(physicalSensors: Map<String, CameraCharacteristics>) {
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
                    text = stringResource(id = R.string.physical),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = stringResource(id = R.string.quality),
                    modifier = Modifier.weight(COLUMN_WEIGHT),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.width(8.dp))

                Text(
                    text = stringResource(id = R.string.angle),
                    modifier = Modifier.weight(COLUMN_WEIGHT),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }

            physicalSensors.forEach { (id, chars) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = CenterVertically
                ) {
                    Text(
                        text = id,
                        modifier = Modifier.weight(1f),
                    )

                    Text(
                        text = stringResource(
                            id = R.string.mp_format,
                            chars[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE].formatResolution()
                        ),
                        modifier = Modifier.weight(COLUMN_WEIGHT),
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = stringResource(
                            id = R.string.deg_format,
                            getFOV(
                                chars[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS].minOf { it },
                                chars[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]
                            )
                        ),
                        modifier = Modifier.weight(COLUMN_WEIGHT),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}