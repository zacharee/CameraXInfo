package dev.zwander.cameraxinfo

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.zwander.cameraxinfo.ui.components.ARCoreCard
import dev.zwander.cameraxinfo.ui.components.CameraCard
import dev.zwander.cameraxinfo.ui.theme.CameraXInfoTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.withContext

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

@OptIn(ExperimentalFoundationApi::class)
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
        val p = withContext(Dispatchers.IO) {
            ProcessCameraProvider.getInstance(context).await()
        }
        extensionsManager = withContext(Dispatchers.IO) {
            ExtensionsManager.getInstanceAsync(context, p).await()
        }
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
                item(key = "ARCore") {
                    ARCoreCard(modifier = Modifier.animateItemPlacement())
                }

                infos.forEach { (info, info2) ->
                    item(key = info2.cameraId) {
                        CameraCard(
                            which = info,
                            which2 = info2,
                            extensionsManager = extensionsManager,
                            modifier = Modifier.animateItemPlacement()
                        )
                    }
                }
            }
        }
    }
}
