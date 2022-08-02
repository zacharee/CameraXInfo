package dev.zwander.cameraxinfo.ui.components

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.data.Node
import dev.zwander.cameraxinfo.data.createTreeFromPaths
import dev.zwander.cameraxinfo.model.LocalDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun DataBrowser(
    lastRefreshTime: Long,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model = LocalDataModel.current
    val firestore = Firebase.firestore

    LaunchedEffect(key1 = lastRefreshTime) {
        if (model.currentPath == null) {
            withContext(Dispatchers.IO) {
                model.currentPath = firestore.collectionGroup("CameraDataNode").get().await().createTreeFromPaths()
            }
        }
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
                    model.currentPath?.parent?.apply {
                        when {
                            this() == null -> onDismissRequest()
                            else -> model.currentPath = this()
                        }
                    }
                }
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.back),
                    contentDescription = stringResource(id = R.string.back)
                )
            }

            Spacer(Modifier.size(8.dp))

            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = model.currentPath?.absolutePath ?: ""
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (model.currentPath?.content == null) {
                items(items = model.currentPath?.children ?: listOf(), key = { it.absolutePath }) {
                    it.StorageListItem {
                        model.currentPath = it
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            Text(
                                text = model.currentPath?.content ?: ""
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
private fun Node.StorageListItem(onClick: () -> Unit) {
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