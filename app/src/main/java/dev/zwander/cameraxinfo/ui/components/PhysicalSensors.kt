package dev.zwander.cameraxinfo.ui.components

import android.hardware.camera2.CameraCharacteristics
import android.text.Html
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.formatResolution
import dev.zwander.cameraxinfo.getFOV
import dev.zwander.cameraxinfo.toAnnotatedString

@Composable
fun PhysicalSensors(physicalSensors: Map<String, CameraCharacteristics>) {
    Text(
        text = stringResource(id = R.string.physical_cameras),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    FlowRow(
        mainAxisSize = SizeMode.Expand,
        mainAxisAlignment = MainAxisAlignment.SpaceEvenly,
        mainAxisSpacing = 8.dp
    ) {
        physicalSensors.forEach { (id, chars) ->
            Text(
                text = Html.fromHtml(
                    stringResource(
                        id = R.string.physical_camera_format,
                        id,
                        chars[CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE].formatResolution(),
                        getFOV(
                            chars[CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS].minOf { it },
                            chars[CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE]
                        )
                    ),
                    0
                ).toAnnotatedString(),
            )
        }
    }
}