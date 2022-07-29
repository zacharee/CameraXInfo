package dev.zwander.cameraxinfo.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.SizeF
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.formatResolution
import dev.zwander.cameraxinfo.getFOV
import dev.zwander.cameraxinfo.lensFacingToString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
@Composable
fun CameraCard(which: CameraInfo, which2: Camera2CameraInfo, extensionsManager: ExtensionsManager?) {
    val context = LocalContext.current
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    val supportedQualities = remember {
        QualitySelector.getSupportedQualities(which).map {
            context.resources.getString(
                when (it) {
                    Quality.SD -> (R.string.sd)
                    Quality.HD -> (R.string.hd)
                    Quality.FHD -> (R.string.fhd)
                    Quality.UHD -> (R.string.uhd)
                    else -> (R.string.unknown)
                }
            )
        }.asReversed()
    }
    val physicalSensors = remember(which2.cameraId) {
        mutableStateListOf<Pair<String, CameraCharacteristics>>()
    }
    val extensions = remember(which2.cameraId) {
        mutableStateListOf<Pair<Int, Boolean?>>()
    }

    LaunchedEffect(key1 = which2.cameraId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val physicals = withContext(Dispatchers.IO) {
                val logicalChars = cameraManager.getCameraCharacteristics(which2.cameraId)

                logicalChars.physicalCameraIds.map {
                    it to cameraManager.getCameraCharacteristics(it)
                }
            }

            physicalSensors.addAll(physicals)
        }

        val extensionAvailability = arrayOf(
            ExtensionMode.AUTO,
            ExtensionMode.BOKEH,
            ExtensionMode.HDR,
            ExtensionMode.NIGHT,
            ExtensionMode.FACE_RETOUCH
        ).map {
            it to extensionsManager?.isExtensionAvailable(which.cameraSelector, it)
        }
        extensions.addAll(extensionAvailability)
    }

    PaddedColumnCard {
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
            text = if (physicalSensors.isNotEmpty()) {
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

        if (physicalSensors.isNotEmpty()) {
            Spacer(modifier = Modifier.size(4.dp))

            PhysicalSensors(physicalSensors = physicalSensors)
        }

        if (supportedQualities.isNotEmpty()) {
            Spacer(modifier = Modifier.size(4.dp))

            VideoQualities(supportedQualities = supportedQualities)
        }

        Spacer(Modifier.size(16.dp))

        ExtensionsCard(extensionAvailability = extensions)
    }
}