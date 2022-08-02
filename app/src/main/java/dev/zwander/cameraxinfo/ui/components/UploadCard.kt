package dev.zwander.cameraxinfo.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.android.internal.R.attr.visible
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import dev.zwander.cameraxinfo.BuildConfig
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.latestUploadTime
import dev.zwander.cameraxinfo.model.LocalDataModel
import dev.zwander.cameraxinfo.util.UploadResult
import dev.zwander.cameraxinfo.util.uploadToCloud
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadCard(lastRefreshTime: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val model = LocalDataModel.current
    val scope = rememberCoroutineScope()

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
        AlertDialog(
            onDismissRequest = { uploadStatus = null },
            confirmButton = { if (uploadStatus?.e != null) Text(text = stringResource(id = R.string.ok)) },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            title = { Text(text = stringResource(id = R.string.uploading)) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (uploadStatus == UploadResult.Uploading) {
                        CircularProgressIndicator()
                    }

                    uploadStatus?.e?.let { e ->
                        Text(
                            text = stringResource(id = R.string.error, e.message.toString())
                        )
                    }
                }
            }
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
                        lastRefreshTime = lastRefreshTime,
                        onDismissRequest = { browsing = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}