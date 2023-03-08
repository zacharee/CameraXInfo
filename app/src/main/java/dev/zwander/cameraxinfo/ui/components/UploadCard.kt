package dev.zwander.cameraxinfo.ui.components

import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.documentfile.provider.DocumentFile
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
import dev.zwander.cameraxinfo.util.ResultSaver
import dev.zwander.cameraxinfo.util.UploadErrorSaver
import dev.zwander.cameraxinfo.util.UploadResult
import dev.zwander.cameraxinfo.util.awaitCatchingError
import dev.zwander.cameraxinfo.util.signInIfNeeded
import dev.zwander.cameraxinfo.util.uploadToCloud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Composable
fun UploadCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = LocalDataModel.current
    val scope = rememberCoroutineScope()

    var isDownloading by remember {
        mutableStateOf(false)
    }

    var uploadErrorToShow by rememberSaveable(saver = UploadErrorSaver) {
        mutableStateOf(null)
    }

    val saver = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip") ?: "*/*"
        )
    ) { result ->
        result?.let { uri ->
            scope.launch(Dispatchers.IO) {
                isDownloading = true
                context.contentResolver.openOutputStream(uri)?.use { writer ->
                    val group = Firebase.firestore.collectionGroup("CameraDataNode")
                    val handle = group.addSnapshotListener { _, _ ->  }
                    try {
                        group.get().awaitCatchingError()
                            .createZipFile(context)
                            .inputStream().use { reader ->
                                reader.copyTo(writer)
                            }
                    } catch (e: Exception) {
                        launch(Dispatchers.Main) {
                            uploadErrorToShow = e to uri
                        }
                    }
                    handle.remove()
                }
                isDownloading = false
            }
        }
    }

    var uploadStatus by rememberSaveable(saver = ResultSaver) {
        mutableStateOf(null)
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

    if (uploadErrorToShow != null) {
        CustomDialog(
            onDismissRequest = { uploadErrorToShow = null },
            title = stringResource(id = R.string.error_no_format),
            content = {
                Text(text = stringResource(id = R.string.error_downloading, uploadErrorToShow?.first?.localizedMessage ?: ""))
            },
            onConfirm = {
                try {
                    DocumentFile.fromSingleUri(context, uploadErrorToShow!!.second!!)?.delete()
                } catch (ignored: Exception) {}
            }
        )
    }

    if (uploadStatus != null) {
        CustomDialog(
            onDismissRequest = { uploadStatus = null },
            title = stringResource(id = R.string.uploading),
            content = {
                Crossfade(
                    targetState = uploadStatus, label = "Upload",
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
            },
            showConfirm = uploadStatus?.e != null || uploadStatus is UploadResult.ErrorResult,
            properties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        )
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