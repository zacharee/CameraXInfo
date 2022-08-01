package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.launchUrl
import dev.zwander.cameraxinfo.model.LocalDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun DataBrowser(
    lastRefreshTime: Long,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val model = LocalDataModel.current

    val storage = Firebase.storage

    LaunchedEffect(key1 = lastRefreshTime) {
        if (model.currentReference == null) {
            model.currentReference = storage.getReference("/CameraData")
        }
    }

    LaunchedEffect(key1 = model.currentReference, key2 = lastRefreshTime) {
        val (prefixes, items) = withContext(Dispatchers.IO) {
            model.currentReference?.listAll()?.await()?.run { prefixes to items }
        } ?: (listOf<StorageReference>() to listOf())

        model.currentPrefixListing.clear()
        model.currentItemListing.clear()

        model.currentPrefixListing.addAll(prefixes)
        model.currentItemListing.addAll(items)
    }

    Column(
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = {
                    model.currentReference?.parent.apply {
                        when {
                            model.currentItemReference != null -> {
                                model.currentItemReference = null
                                model.currentItemText = null
                            }
                            this == null || model.currentReference?.name == "CameraData" -> onDismissRequest()
                            else -> model.currentReference = this
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = stringResource(id = R.string.back)
                )
            }

            AnimatedVisibility(visible = model.currentItemReference != null) {
                IconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            context.launchUrl(
                                model.currentItemReference!!.downloadUrl
                                    .await()
                                    .toString()
                            )
                        }
                    }
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.download),
                        contentDescription = stringResource(id = R.string.download)
                    )
                }
            }

            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.currentItemReference?.path ?: model.currentReference?.path ?: ""
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (model.currentItemReference == null) {
                items(items = model.currentPrefixListing.toList(), key = { it.path }) {
                    it.StorageListItem {
                        model.currentReference = it
                    }
                }

                items(items = model.currentItemListing.toList(), key = { it.path }) {
                    it.StorageListItem {
                        scope.launch(Dispatchers.IO) {
                            model.currentItemReference = it
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        LaunchedEffect(null) {
                            if (model.currentItemText == null) {
                                model.currentItemText = withContext(Dispatchers.IO) {
                                    model.currentItemReference?.stream?.await()?.stream?.bufferedReader()?.use { reader -> reader.readText() } ?: ""
                                }
                            }
                        }

                        SelectionContainer {
                            Text(
                                text = model.currentItemText ?: ""
                            )
                        }
                    }
                }
            }
        }
    }
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StorageReference.StorageListItem(onClick: () -> Unit) {
    Card(
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name
            )
        }
    }
}