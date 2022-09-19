package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.zwander.cameraxinfo.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CustomDialog(
    onDismissRequest: () -> Unit,
    title: String,
    content: @Composable () -> Unit,
    onConfirm: (() -> Unit)? = null,
    showConfirm: Boolean = true,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false)
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties,
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
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.size(16.dp))

                content()

                AnimatedVisibility(
                    visible = showConfirm,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = {
                                onConfirm?.invoke()
                                onDismissRequest()
                            },
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