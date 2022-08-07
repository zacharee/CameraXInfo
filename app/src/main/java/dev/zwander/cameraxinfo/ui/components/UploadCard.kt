package dev.zwander.cameraxinfo.ui.components

import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.zwander.cameraxinfo.BuildConfig
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.data.createZipFile
import dev.zwander.cameraxinfo.latestDownloadTime
import dev.zwander.cameraxinfo.latestUploadTime
import dev.zwander.cameraxinfo.model.LocalDataModel
import dev.zwander.cameraxinfo.util.UploadResult
import dev.zwander.cameraxinfo.util.awaitCatchingError
import dev.zwander.cameraxinfo.util.signInIfNeeded
import dev.zwander.cameraxinfo.util.uploadToCloud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun UploadCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = LocalDataModel.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember {
        mutableStateOf(false)
    }

    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip")
        )
    ) { result ->
        result?.let { uri ->
            scope.launch(Dispatchers.IO) {
                isDownloading = true
                context.contentResolver.openOutputStream(uri).use { writer ->
                    val group = Firebase.firestore.collectionGroup("CameraDataNode")
                    val handle = group.addSnapshotListener { _, _ ->  }
                    group.get().awaitCatchingError()
                        .createZipFile(context)
                        .apply {
                            try {
                                inputStream().use { reader ->
                                    reader.copyTo(writer)
                                }
                            } catch (e: Exception) {
                                Log.e("CameraXInfo", "Error copying file", e)
                            }
                        }
                    handle.remove()
                }
                isDownloading = false
            }
        }
    }

    var uploadStatus by rememberSaveable {
        mutableStateOf<UploadResult?>(null)
    }
    var browsing by rememberSaveable {
        mutableStateOf(false)
    }

    LaunchedEffect(key1 = uploadStatus) {
        if (uploadStatus == UploadResult.Success) {
            Toast.makeText(context, R.string.success, Toast.LENGTH_SHORT).show()
            uploadStatus = null
        }
    }

    if (uploadStatus != null) {
        Dialog(
            onDismissRequest = { uploadStatus = null },
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            ),
        ) {
            Surface(
                modifier = Modifier
                    .wrapContentHeight()
                    .fillMaxWidth(0.75f),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.uploading),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.size(16.dp))

                    Crossfade(
                        targetState = uploadStatus,
                    ) {
                        when (it) {
                            UploadResult.Uploading -> {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            else -> {
                                Text(
                                    text = when {
                                        uploadStatus?.e != null -> stringResource(id = R.string.error, uploadStatus?.e?.message.toString())
                                        uploadStatus == UploadResult.DuplicateData -> stringResource(id = R.string.duplicate_data)
                                        uploadStatus == UploadResult.SafetyNetFailure -> stringResource(id = R.string.safetynet_failed)
                                        else -> ""
                                    }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = uploadStatus?.e != null || uploadStatus is UploadResult.ErrorResult,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))

                            TextButton(
                                onClick = { uploadStatus = null },
                            ) {
                                Text(
                                    text = stringResource(id = R.string.ok),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    PaddedColumnCard(
        modifier = modifier.animateContentSize()
    ) {
        Text(
            text = stringResource(id = R.string.upload),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Divider(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(0.33f)
        )

        Text(
            text = stringResource(id = R.string.upload_desc),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(16.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            mainAxisSize = SizeMode.Expand,
            mainAxisAlignment = MainAxisAlignment.SpaceEvenly,
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
        ) {
            Button(
                onClick = {
                    val currentTime = System.currentTimeMillis()

                    if (!BuildConfig.DEBUG && (context.latestUploadTime - currentTime).absoluteValue < 30_000) {
                        Toast.makeText(context, R.string.rate_limited, Toast.LENGTH_SHORT).show()
                    } else {
                        context.latestUploadTime = currentTime
                        uploadStatus = UploadResult.Uploading
                        scope.launch(Dispatchers.IO) {
                            uploadStatus = model.uploadToCloud(context)
                        }
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.upload))
            }

            Box(
                contentAlignment = Alignment.Center
            ) {
                val animatedAlpha by animateFloatAsState(targetValue = if (isDownloading) 0f else 1f)

                Button(
                    onClick = {
                        val currentTime = System.currentTimeMillis()

                        if (!BuildConfig.DEBUG && (context.latestDownloadTime - currentTime).absoluteValue < 30_000) {
                            Toast.makeText(context, R.string.rate_limited, Toast.LENGTH_SHORT).show()
                        } else {
                            context.latestDownloadTime = currentTime

                            scope.launch(Dispatchers.IO) {
                                val signInResult = signInIfNeeded()

                                if (signInResult != null) {
                                    Toast.makeText(context, context.resources.getString(R.string.error, signInResult.message), Toast.LENGTH_SHORT).show()
                                } else {
                                    saver.launch("CameraXData_${System.currentTimeMillis()}.zip")
                                }
                            }
                        }
                    },
                    enabled = !isDownloading,
                    modifier = Modifier.alpha(animatedAlpha)
                ) {
                    Text(text = stringResource(id = R.string.download))
                }

                androidx.compose.animation.AnimatedVisibility(visible = isDownloading) {
                    CircularProgressIndicator()
                }
            }

            Button(
                onClick = {
                    browsing = !browsing
                }
            ) {
                Text(text = stringResource(id = R.string.browse))
            }
        }

        AnimatedVisibility(visible = browsing) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.size(16.dp))

                Card(
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(),
                ) {
                    DataBrowser(
                        onDismissRequest = { browsing = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .animateContentSize()
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}