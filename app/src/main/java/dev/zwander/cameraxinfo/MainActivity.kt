package dev.zwander.cameraxinfo

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.backendless.Backendless
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.safetynet.SafetyNetAppCheckProviderFactory
import dev.zwander.cameraxinfo.model.DataModel
import dev.zwander.cameraxinfo.model.LocalDataModel
import dev.zwander.cameraxinfo.ui.components.*
import dev.zwander.cameraxinfo.ui.theme.CameraXInfoTheme
import dev.zwander.cameraxinfo.util.BackendlessUtils

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

        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            SafetyNetAppCheckProviderFactory.getInstance()
        )

        BackendlessUtils.setup()

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

        var lastRefresh by remember {
            mutableStateOf(System.currentTimeMillis())
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
                color = MaterialTheme.colorScheme.background
            ) {
                SwipeRefresh(
                    state = rememberSwipeRefreshState(isRefreshing = isRefreshing),
                    onRefresh = { lastRefresh = System.currentTimeMillis() }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item(key = "InfoCard") {
                            AnimateInBox(
                                modifier = Modifier.animateItemPlacement()
                            ) {
                                InfoCard(lastRefresh)
                            }
                        }

                        item(key = "UploadCard") {
                            AnimateInBox(
                                modifier = Modifier.animateItemPlacement()
                            ) {
                                UploadCard()
                            }
                        }

                        item(key = "ARCore") {
                            AnimateInBox(
                                modifier = Modifier.animateItemPlacement()
                            ) {
                                ARCoreCard()
                            }
                        }

                        model.cameraInfos.forEach { (_, info2) ->
                            item(key = info2.cameraId) {
                                AnimateInBox(
                                    modifier = Modifier.animateItemPlacement()
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
        }
    }
}
