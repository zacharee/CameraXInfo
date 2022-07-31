package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zwander.cameraxinfo.R

enum class SupportState {
    SUPPORTED,
    UNSUPPORTED,
    OUTDATED,
    UNKNOWN,
    NOT_APPLICABLE
}

private fun Boolean?.toSupportState(): SupportState {
    return when (this) {
        true -> SupportState.SUPPORTED
        false -> SupportState.UNSUPPORTED
        else -> SupportState.UNKNOWN
    }
}

@Composable
fun SupportStateIcon(
    state: Boolean?,
    modifier: Modifier = Modifier
) {
    SupportStateIcon(
        state = state.toSupportState(),
        modifier = modifier
    )
}

@Composable
fun SupportStateIcon(
    state: SupportState,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = state,
            modifier = Modifier.heightIn(max = 24.dp)
        ) {
            when (it) {
                SupportState.SUPPORTED -> Icon(
                    painter = painterResource(id = R.drawable.check),
                    tint = Color.Green,
                    contentDescription = stringResource(id = R.string.supported)
                )
                SupportState.UNSUPPORTED -> Icon(
                    painter = painterResource(id = R.drawable.close),
                    tint = Color.Red,
                    contentDescription = stringResource(id = R.string.unsupported)
                )
                SupportState.OUTDATED -> Icon(
                    painter = painterResource(id = R.drawable.update),
                    tint = Color.Yellow,
                    contentDescription = stringResource(id = R.string.outdated)
                )
                SupportState.NOT_APPLICABLE -> Icon(
                    painter = painterResource(id = R.drawable.circle),
                    tint = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface),
                    contentDescription = stringResource(id = R.string.not_applicable),
                    modifier = Modifier.fillMaxWidth(0.15f)
                )
                SupportState.UNKNOWN -> Icon(
                    painter = painterResource(id = R.drawable.question_mark),
                    tint = Color.Yellow,
                    contentDescription = stringResource(id = R.string.unknown)
                )
            }
        }
    }
}