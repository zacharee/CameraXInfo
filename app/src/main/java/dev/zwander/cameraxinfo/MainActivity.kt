package dev.zwander.cameraxinfo

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.zwander.cameraxinfo.model.DataModel
import dev.zwander.cameraxinfo.model.LocalDataModel
import dev.zwander.cameraxinfo.ui.components.*
import dev.zwander.cameraxinfo.ui.theme.CameraXInfoTheme

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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        if (checkCallingOrSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsRequester.launch(android.Manifest.permission.CAMERA)
        } else {
            setContent {
                MainContent()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class,
    ExperimentalMaterial3Api::class
)
@SuppressLint("UnsafeOptInUsageError", "InlinedApi")
@Preview
@Composable
fun MainContent() {
    CompositionLocalProvider(
        LocalDataModel provides DataModel()
    ) {
        val context = LocalContext.current
        val model = LocalDataModel.current

        var lastRefresh by remember {
            mutableLongStateOf(System.currentTimeMillis())
        }
        var isRefreshing by remember {
            mutableStateOf(false)
        }

        LaunchedEffect(key1 = lastRefresh) {
            isRefreshing = true

            model.populate(context)

            isRefreshing = false
        }

        CameraXInfoTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { lastRefresh = System.currentTimeMillis() }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = WindowInsets.systemBars.add(
                            WindowInsets.ime,
                        ).add(
                            WindowInsets(8.dp)
                        ).asPaddingValues(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "InfoCard") {
                            AnimateInBox(
                                modifier = Modifier.animateItem()
                            ) {
                                InfoCard(lastRefresh)
                            }
                        }

                        item(key = "UploadCard") {
                            AnimateInBox(
                                modifier = Modifier.animateItem()
                            ) {
                                UploadCard()
                            }
                        }

                        item(key = "ARCore") {
                            AnimateInBox(
                                modifier = Modifier.animateItem()
                            ) {
                                ARCoreCard()
                            }
                        }

                        model.cameraInfos.forEach { (_, info2) ->
                            item(key = info2.cameraId) {
                                AnimateInBox(
                                    modifier = Modifier.animateItem()
                                ) {
                                    CameraCard(
                                        which2 = info2,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (model.pathLoadError != null) {
                CustomDialog(
                    onDismissRequest = { model.pathLoadError = null },
                    title = stringResource(id = R.string.error_no_format),
                    content = {
                        Text(text = stringResource(id = R.string.error_browsing, model.pathLoadError?.localizedMessage ?: ""))
                    }
                )
            }
        }
    }
}
