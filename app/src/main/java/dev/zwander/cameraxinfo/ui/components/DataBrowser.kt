package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.data.Node
import dev.zwander.cameraxinfo.model.LocalDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class SortMode {
    NAME,
    COUNT
}

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DataBrowser(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    val model = LocalDataModel.current
    val context = LocalContext.current
    val listState = rememberLazyListState()

    var sortMode by rememberSaveable {
        mutableStateOf(SortMode.NAME)
    }

    LaunchedEffect(key1 = sortMode) {
        listState.scrollToItem(0)
    }

    LaunchedEffect(key1 = null) {
        if (model.currentPath == null) {
            withContext(Dispatchers.IO) {
                model.populatePath(context)
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

            AnimatedVisibility(visible = model.currentPath?.run { this.content == null && this.children.none { it.content != null } } == true) {
                IconButton(
                    onClick = {
                        sortMode = if (sortMode == SortMode.NAME) SortMode.COUNT else SortMode.NAME
                    }
                ) {
                    Crossfade(targetState = sortMode) {
                        when (it) {
                            SortMode.NAME -> Icon(
                                painter = painterResource(id = R.drawable.alphabetical),
                                contentDescription = stringResource(id = R.string.sort_by_name)
                            )
                            SortMode.COUNT -> Icon(
                                painter = painterResource(id = R.drawable.count),
                                contentDescription = stringResource(id = R.string.sort_by_count)
                            )
                        }
                    }
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
                    text = model.currentPath?.absolutePath ?: stringResource(id = R.string.loading)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            state = listState
        ) {
            if (model.currentPath == null) {
                item(key = "LoadingIndicator") {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (model.currentPath?.content == null) {
                items(items = model.currentPath?.children?.run {
                    when (sortMode) {
                        SortMode.NAME -> sortedBy { it.name }
                        SortMode.COUNT -> sortedBy { -it.children.size }
                    }
                } ?: listOf(), key = { it.absolutePath }) {
                    it.StorageListItem(Modifier.animateItemPlacement()) {
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
private fun Node.StorageListItem(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier
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

            Spacer(Modifier.weight(1f))

            if (children.size > 0 && content == null) {
                Text(
                    text = "(${children.size})"
                )
            }
        }
    }
}