package dev.zwander.cameraxinfo

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraExtensionCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.extensions.ExtensionMode
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Config
import com.google.ar.core.Session
import dev.zwander.cameraxinfo.data.CameraInfoHolder
import dev.zwander.cameraxinfo.data.ExtensionAvailability
import dev.zwander.cameraxinfo.model.DataModel
import dev.zwander.cameraxinfo.model.LocalDataModel
import dev.zwander.cameraxinfo.ui.components.ARCoreCard
import dev.zwander.cameraxinfo.ui.components.CameraCard
import dev.zwander.cameraxinfo.ui.components.InfoCard
import dev.zwander.cameraxinfo.ui.theme.CameraXInfoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val permissionsRequester =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
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

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("UnsafeOptInUsageError", "InlinedApi")
@Preview
@Composable
fun MainContent() {
    CompositionLocalProvider(
        LocalDataModel provides DataModel()
    ) {
        val context = LocalContext.current
        val model = LocalDataModel.current
        val cameraManager = remember {
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        }

        LaunchedEffect(key1 = null) {
            val (p, e) = withContext(Dispatchers.IO) {
                val provider = ProcessCameraProvider.getInstance(context).await()
                provider to ExtensionsManager.getInstanceAsync(context, provider).await()
            }

            model.cameraInfos.clear()
            model.cameraInfos.addAll(
                p.availableCameraInfos.map {
                    CameraInfoHolder(
                        cameraInfo = it,
                        camera2Info = Camera2CameraInfo.from(it)
                    ).also { (info, info2) ->
                        @Suppress("DeferredResultUnused")
                        async {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val physicals = withContext(Dispatchers.IO) {
                                    val logicalChars =
                                        cameraManager.getCameraCharacteristics(info2.cameraId)

                                    logicalChars.physicalCameraIds.map { id ->
                                        id to cameraManager.getCameraCharacteristics(id)
                                    }
                                }

                                model.physicalSensors[info2.cameraId] = physicals.toMap()
                            }
                        }

                        @Suppress("DeferredResultUnused")
                        async {
                            val camera2Extensions =
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    withContext(Dispatchers.IO) {
                                        cameraManager.getCameraExtensionCharacteristics(info2.cameraId).supportedExtensions
                                    }
                                } else {
                                    listOf()
                                }

                            val extensionAvailability = arrayOf(
                                ExtensionMode.AUTO to CameraExtensionCharacteristics.EXTENSION_AUTOMATIC,
                                ExtensionMode.BOKEH to CameraExtensionCharacteristics.EXTENSION_BOKEH,
                                ExtensionMode.HDR to CameraExtensionCharacteristics.EXTENSION_HDR,
                                ExtensionMode.NIGHT to CameraExtensionCharacteristics.EXTENSION_NIGHT,
                                ExtensionMode.FACE_RETOUCH to CameraExtensionCharacteristics.EXTENSION_BEAUTY
                            ).map { (cameraXExtension, camera2Extension) ->
                                cameraXExtension to ExtensionAvailability(
                                    extension = cameraXExtension,
                                    camera2Availability = camera2Extensions.contains(camera2Extension),
                                    cameraXAvailability = e.isExtensionAvailable(
                                        info.cameraSelector,
                                        cameraXExtension
                                    )
                                )
                            }

                            model.extensions[info2.cameraId] = extensionAvailability.toMap()
                        }

                        @Suppress("DeferredResultUnused")
                        async {
                            model.supportedQualities[info2.cameraId] =
                                QualitySelector.getSupportedQualities(info).map { quality ->
                                    context.resources.getString(
                                        when (quality) {
                                            Quality.SD -> (R.string.sd)
                                            Quality.HD -> (R.string.hd)
                                            Quality.FHD -> (R.string.fhd)
                                            Quality.UHD -> (R.string.uhd)
                                            else -> (R.string.unknown)
                                        }
                                    )
                                }.asReversed()
                        }
                    }
                }.sortedBy { (_, info2) ->
                    info2.getCameraCharacteristic(CameraCharacteristics.LENS_FACING)?.times(-1)
                }
            )
        }

        LaunchedEffect(key1 = null) {
            val status = withContext(Dispatchers.IO) {
                ArCoreApk.getInstance().awaitAvailability(context)
            }

            model.depthStatus = if (status == ArCoreApk.Availability.SUPPORTED_INSTALLED) {
                withContext(Dispatchers.IO) {
                    val session = Session(context)
                    session.isDepthModeSupported(Config.DepthMode.AUTOMATIC).also {
                        session.close()
                    }
                }
            } else {
                null
            }

            model.arCoreStatus = status
        }

        CameraXInfoTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "InfoCard") {
                        InfoCard(modifier = Modifier.animateItemPlacement())
                    }

                    item(key = "ARCore") {
                        ARCoreCard(modifier = Modifier.animateItemPlacement())
                    }

                    model.cameraInfos.forEach { (_, info2) ->
                        item(key = info2.cameraId) {
                            CameraCard(
                                which2 = info2,
                                modifier = Modifier.animateItemPlacement()
                            )
                        }
                    }
                }
            }
        }
    }
}
