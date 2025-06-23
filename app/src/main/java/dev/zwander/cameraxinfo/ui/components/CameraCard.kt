package dev.zwander.cameraxinfo.ui.components

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.data.ExtensionAvailability
import dev.zwander.cameraxinfo.formatResolution
import dev.zwander.cameraxinfo.getFOV
import dev.zwander.cameraxinfo.lensFacingToString
import dev.zwander.cameraxinfo.model.LocalDataModel

val defaultExtensionState = mapOf(
    ExtensionMode.AUTO to ExtensionAvailability(ExtensionMode.AUTO),
    ExtensionMode.BOKEH to ExtensionAvailability(ExtensionMode.BOKEH),
    ExtensionMode.HDR to ExtensionAvailability(ExtensionMode.HDR),
    ExtensionMode.NIGHT to ExtensionAvailability(ExtensionMode.NIGHT),
    ExtensionMode.FACE_RETOUCH to ExtensionAvailability(ExtensionMode.FACE_RETOUCH)
)

@SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
@Composable
fun CameraCard(which2: Camera2CameraInfo, modifier: Modifier = Modifier) {
    val model = LocalDataModel.current
    val physicalSensors by model.physicalSensors.collectAsState()
    val isLogical = physicalSensors[which2.cameraId]?.isNotEmpty() == true

    val extensions by model.extensions.collectAsState()

    var expanded by rememberSaveable(which2.cameraId) {
        mutableStateOf(true)
    }

    Card(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 16.dp,
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (expanded) 16.dp else 0.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CenteredWithInfo(
                centeredComponent = {
                    Text(
                        text = stringResource(id = R.string.logical_camera_format, which2.cameraId),
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp,
                    )
                },
                expandedInfoComponent = {
                    Text(
                        text = stringResource(id = R.string.logical_sensor_desc),
                        textAlign = TextAlign.Center
                    )
                }
            )

            HorizontalDivider(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(0.33f)
            )

            Text(
                text = if (isLogical) {
                    stringResource(
                        id = R.string.camera_direction_and_logical_format,
                        which2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                            .lensFacingToString(LocalContext.current),
                        stringResource(id = R.string.logical_sensor)
                    )
                } else {
                    stringResource(
                        id = R.string.camera_direction_format,
                        which2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                            .lensFacingToString(LocalContext.current),
                        which2.formatResolution(),
                        getFOV(
                            which2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                                ?.minOf { it } ?: 0f,
                            which2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                                ?: SizeF(0f, 0f)
                        )
                    )
                },
                modifier = Modifier
                    .animateContentSize()
            )

            Card(
                onClick = { expanded = !expanded },
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(min = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val rotation by animateFloatAsState(if (!expanded) 180f else 0f)

                    Icon(
                        painter = painterResource(R.drawable.outline_keyboard_arrow_up_24),
                        contentDescription = stringResource(if (expanded) R.string.collapse else R.string.expand),
                        modifier = Modifier.rotate(rotation),
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    mapOf(
                        R.string.video_quality_sdr to model.supportedSdrQualities.collectAsState().value[which2.cameraId],
                        R.string.video_quality_hlg to model.supportedHlgQualities.collectAsState().value[which2.cameraId],
                        R.string.video_quality_hdr_10 to model.supportedHdr10Qualities.collectAsState().value[which2.cameraId],
                        R.string.video_quality_hdr_10_plus to model.supportedHdr10PlusQualities.collectAsState().value[which2.cameraId],
                        R.string.video_quality_dolby_vision_10_bit to model.supportedDolbyVision10BitQualities.collectAsState().value[which2.cameraId],
                        R.string.video_quality_dolby_vision_8_bit to model.supportedDolbyVision8BitQualities.collectAsState().value[which2.cameraId],
                    ).forEach { (dynamicRange, supportedQualities) ->
                        AnimatedVisibility(visible = supportedQualities?.isNotEmpty() == true) {
                            Column {
                                Spacer(modifier = Modifier.size(4.dp))

                                InfoRow(
                                    title = stringResource(id = R.string.video_qualities_format, stringResource(dynamicRange)),
                                    supportedQualities = supportedQualities,
                                )
                            }
                        }
                    }

                    val imageCaptureCapabilities = model.imageCaptureCapabilities.collectAsState().value[which2.cameraId]

                    AnimatedVisibility(visible = !imageCaptureCapabilities.isNullOrEmpty()) {
                        Column {
                            Spacer(modifier = Modifier.size(4.dp))

                            InfoRow(
                                title = stringResource(R.string.image_capture_capabilities),
                                supportedQualities = imageCaptureCapabilities,
                            )
                        }
                    }

                    val physicalSensors = physicalSensors[which2.cameraId]

                    AnimatedVisibility(visible = physicalSensors?.isNotEmpty() == true) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.size(16.dp))

                            CenteredWithInfo(
                                centeredComponent = {
                                    Text(
                                        text = stringResource(id = R.string.physical_sensors),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp
                                    )
                                },
                                expandedInfoComponent = {
                                    Text(
                                        text = stringResource(id = R.string.physical_sensors_desc),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            )

                            PhysicalSensors(physicalSensors = physicalSensors ?: mapOf())
                        }
                    }

                    Spacer(Modifier.size(16.dp))

                    CenteredWithInfo(
                        centeredComponent = {
                            Text(
                                text = stringResource(id = R.string.extension_support),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        },
                        expandedInfoComponent = {
                            Text(
                                text = stringResource(id = R.string.extension_support_desc),
                                textAlign = TextAlign.Center
                            )
                        }
                    )

                    ExtensionsCard(extensionAvailability = extensions[which2.cameraId] ?: defaultExtensionState)
                }
            }
        }
    }
}