package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha

@Composable
fun AnimateInBox(
    modifier: Modifier = Modifier,
    key: Any? = null,
    content: @Composable BoxScope.() -> Unit
) {
    var attached by rememberSaveable {
        mutableStateOf(false)
    }
    val alpha by animateFloatAsState(targetValue = if (attached) 1f else 0f)

    LaunchedEffect(key1 = key) {
        attached = true
    }

    Box(
        modifier = modifier.alpha(alpha)
    ) {
        content()
    }
}