package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
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
    val contentColor = MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)

    val hsl = FloatArray(3).apply { ColorUtils.colorToHSL(MaterialTheme.colorScheme.error.toArgb(), this) }

    val newSaturation = kotlin.math.min(hsl[1] + 0.5f, 1.0f)

    val redShifted = Color.hsl(hsl[0], newSaturation, hsl[2])
    val yellowShifted = Color.hsl(60f, newSaturation, hsl[2])
    val greenShifted = Color.hsl(130f, newSaturation, hsl[2])

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Crossfade(
            targetState = state,
            modifier = Modifier.heightIn(max = 24.dp),
            label = "SupportState"
        ) {
            when (it) {
                SupportState.SUPPORTED -> {
                    TooltippedIcon(
                        painter = painterResource(id = R.drawable.check),
                        tint = greenShifted,
                        contentDescription = stringResource(id = R.string.supported),
                    )
                }
                SupportState.UNSUPPORTED -> {
                    TooltippedIcon(
                        painter = painterResource(id = R.drawable.close),
                        tint = redShifted,
                        contentDescription = stringResource(id = R.string.unsupported),
                    )
                }
                SupportState.OUTDATED -> {
                    TooltippedIcon(
                        painter = painterResource(id = R.drawable.update),
                        tint = yellowShifted,
                        contentDescription = stringResource(id = R.string.outdated),
                    )
                }
                SupportState.NOT_APPLICABLE -> {
                    TooltippedIcon(
                        painter = painterResource(id = R.drawable.circle),
                        tint = contentColor,
                        contentDescription = stringResource(id = R.string.not_applicable),
                        modifier = Modifier
                            .fillMaxWidth(0.15f)
                    )
                }
                SupportState.UNKNOWN -> {
                    TooltippedIcon(
                        painter = painterResource(id = R.drawable.question_mark),
                        tint = yellowShifted,
                        contentDescription = stringResource(id = R.string.unknown),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TooltippedIcon(
    painter: Painter,
    tint: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    message: String = contentDescription,
) {
    PlainTooltipBox(
        tooltip = {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.wrapContentWidth(unbounded = true)
            ) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                )
            }
        },
        modifier = modifier
    ) {
        Icon(
            painter = painter,
            tint = tint,
            contentDescription = contentDescription,
            modifier = Modifier.tooltipAnchor()
        )
    }
}
