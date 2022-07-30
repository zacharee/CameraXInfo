package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.zwander.cameraxinfo.R

@Composable
fun SupportStateIcon(
    state: Boolean?,
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
                true -> Icon(
                    painter = painterResource(id = R.drawable.check),
                    tint = Color.Green,
                    contentDescription = stringResource(id = R.string.supported)
                )
                false -> Icon(
                    painter = painterResource(id = R.drawable.close),
                    tint = Color.Red,
                    contentDescription = stringResource(id = R.string.unsupported)
                )
                else -> Icon(
                    painter = painterResource(id = R.drawable.question_mark),
                    tint = Color.Yellow,
                    contentDescription = stringResource(id = R.string.unknown)
                )
            }
        }
    }
}