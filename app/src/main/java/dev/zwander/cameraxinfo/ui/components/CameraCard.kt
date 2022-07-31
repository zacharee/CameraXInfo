package dev.zwander.cameraxinfo.ui.components

import android.annotation.SuppressLint
import android.hardware.camera2.CameraCharacteristics
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.data.ExtensionAvailability
import dev.zwander.cameraxinfo.formatResolution
import dev.zwander.cameraxinfo.getFOV
import dev.zwander.cameraxinfo.lensFacingToString
import dev.zwander.cameraxinfo.model.LocalDataModel

private val defaultExtensionState = mapOf(
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

    PaddedColumnCard(
        modifier = modifier
    ) {
        Text(
            text = stringResource(id = R.string.logical_camera_format, which2.cameraId),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
        )

        Divider(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(0.33f)
        )

        Text(
            text = if (model.physicalSensors[which2.cameraId]?.isNotEmpty() == true) {
                which2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                    .lensFacingToString()
            } else {
                stringResource(
                    id = R.string.camera_direction_format,
                    which2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)
                        .lensFacingToString(),
                    which2.formatResolution(),
                    getFOV(
                        which2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                            ?.minOf { it } ?: 0f,
                        which2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)
                            ?: SizeF(0f, 0f)
                    )
                )
            },
            modifier = Modifier.padding(bottom = 8.dp)
        )

        val physicalSensors = model.physicalSensors[which2.cameraId]

        AnimatedVisibility(visible = physicalSensors?.isNotEmpty() == true) {
            Column {
                Spacer(modifier = Modifier.size(4.dp))

                PhysicalSensors(physicalSensors = physicalSensors ?: mapOf())
            }
        }

        val supportedQualities = model.supportedQualities[which2.cameraId]

        AnimatedVisibility(visible = supportedQualities?.isNotEmpty() == true) {
            Column {
                Spacer(modifier = Modifier.size(4.dp))

                VideoQualities(supportedQualities = supportedQualities ?: listOf())
            }
        }

        Spacer(Modifier.size(16.dp))

        ExtensionsCard(extensionAvailability = model.extensions[which2.cameraId] ?: defaultExtensionState)
    }
}