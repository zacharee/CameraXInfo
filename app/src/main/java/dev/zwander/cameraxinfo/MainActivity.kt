package dev.zwander.cameraxinfo

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.icu.text.DecimalFormat
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.util.Size
import android.util.SizeF
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.core.CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import dev.zwander.cameraxinfo.ui.theme.CameraXInfoTheme
import kotlinx.coroutines.guava.await
import kotlin.math.PI
import kotlin.math.atan

class MainActivity : ComponentActivity() {
    private val permissionsRequester = registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        if (!result) {
            finish()
        } else {
            setContent {
                MainContent()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkCallingOrSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequester.launch(android.Manifest.permission.CAMERA)
        } else {
            setContent {
                MainContent()
            }
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Preview
@Composable
fun MainContent() {
    val context = LocalContext.current
    var provider by remember {
        mutableStateOf<ProcessCameraProvider?>(null)
    }

    var extensionsManager by remember {
        mutableStateOf<ExtensionsManager?>(null)
    }

    LaunchedEffect(key1 = null) {
        val p = ProcessCameraProvider.getInstance(context).await()
        extensionsManager = ExtensionsManager.getInstanceAsync(context, p).await()
        provider = p
    }

    CameraXInfoTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val infos = provider?.availableCameraInfos?.map { it to Camera2CameraInfo.from(it) }?.sortedBy {
                it.second.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)?.times(-1)
            } ?: listOf()

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                infos.forEach { (info, info2) ->
                    item(key = info2.cameraId) {
                        CameraCard(which = info, which2 = info2, extensionsManager = extensionsManager)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
@Composable
fun CameraCard(which: CameraInfo, which2: Camera2CameraInfo, extensionsManager: ExtensionsManager?) {
    val context = LocalContext.current
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    val supportedQualities = remember {
        QualitySelector.getSupportedQualities(which).map {
            context.resources.getString(when (it) {
                Quality.SD -> (R.string.sd)
                Quality.HD -> (R.string.hd)
                Quality.FHD -> (R.string.fhd)
                Quality.UHD -> (R.string.uhd)
                else -> (R.string.unknown)
            })
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
            val logicalChars = cameraManager.getCameraCharacteristics(which2.cameraId)

            val physicals = logicalChars.physicalCameraIds.map {
                it to cameraManager.getCameraCharacteristics(it)
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

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
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
                text = if (physicalSensors.isNotEmpty()) {
                    which2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING).lensFacingToString()
                } else {
                    stringResource(
                        id = R.string.camera_direction_format,
                        which2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING).lensFacingToString(),
                        which2.formatResolution(),
                        getFOV(
                            which2.getCameraCharacteristic(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.minOf { it } ?: 0f,
                            which2.getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE) ?: SizeF(0f, 0f)
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
}

@Composable
fun PhysicalSensors(physicalSensors: List<Pair<String, CameraCharacteristics>>) {
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

@Composable
fun VideoQualities(supportedQualities: List<String>) {
    Text(
        text = stringResource(id = R.string.video_qualities),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    FlowRow(
        mainAxisSize = SizeMode.Expand,
        mainAxisAlignment = MainAxisAlignment.SpaceEvenly,
        mainAxisSpacing = 8.dp
    ) {
        supportedQualities.forEach {
            Text(
                text = it
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionsCard(extensionAvailability: List<Pair<Int, Boolean?>>) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface))
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
                CameraFeature(featureName = extension.extensionModeToString(), supported = available ?: false)
            }
        }
    }
}

@Composable
private fun Int?.lensFacingToString(): String {
    return stringResource(
        id = when (this) {
            CameraCharacteristics.LENS_FACING_FRONT -> R.string.front_facing
            CameraCharacteristics.LENS_FACING_BACK -> R.string.rear_facing
            CameraCharacteristics.LENS_FACING_EXTERNAL -> R.string.external
            else -> R.string.unknown
        }
    )
}

@Composable
private fun Int.extensionModeToString(): String {
    return stringResource(
        id = when (this) {
            ExtensionMode.AUTO -> R.string.auto
            ExtensionMode.BOKEH -> R.string.bokeh
            ExtensionMode.HDR -> R.string.hdr
            ExtensionMode.NIGHT -> R.string.night
            ExtensionMode.FACE_RETOUCH -> R.string.face_retouch
            ExtensionMode.NONE -> R.string.none
            else -> R.string.unknown
        }
    )
}

@SuppressLint("UnsafeOptInUsageError")
private fun Camera2CameraInfo.formatResolution(): String {
    return getCameraCharacteristic(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE).formatResolution()
}

private fun Size?.formatResolution(): String {
    val pxTotal = this?.run { width * height } ?: 0

    return DecimalFormat("##.#").format(pxTotal / 1_000_000.0)
}

private fun getFOV(focal: Float, frameSize: SizeF): String {
    return DecimalFormat("##.#").format(
        2f * atan(frameSize.run { height * 16.0 / 9.0 } / (focal * 2f)) * 180 / PI
    )
}

@Composable
fun CameraFeature(featureName: String, supported: Boolean) {
    CameraItem(
        featureName = featureName,
        text = stringResource(id = if (supported) R.string.supported else R.string.unsupported),
        color = if (supported) Color.Green else Color.Red
    )
}

@Composable
fun CameraItem(featureName: String, text: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = featureName,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.weight(1f))

        Text(
            text = text,
            color = color
        )
    }
}