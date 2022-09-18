package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout

@Composable
fun CenteredWithInfo(
    centeredComponent: @Composable () -> Unit,
    expandedInfoComponent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    var infoExpanded by remember {
        mutableStateOf(false)
    }
    val infoButtonRotation by animateFloatAsState(targetValue = if (infoExpanded) 180f else 0f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConstraintLayout(modifier = modifier) {
            val (centered, infoButton) = createRefs()

            Box(
                modifier = Modifier.constrainAs(centered) {
                    top.linkTo(parent.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            ) {
                centeredComponent()
            }

            IconButton(
                onClick = { infoExpanded = !infoExpanded },
                modifier = Modifier.constrainAs(infoButton) {
                    top.linkTo(centered.top)
                    bottom.linkTo(centered.bottom)
                    start.linkTo(centered.end)
                }
            ) {
                Icon(
                    painter = painterResource(id = dev.zwander.cameraxinfo.R.drawable.baseline_info_24),
                    contentDescription = stringResource(id = dev.zwander.cameraxinfo.R.string.info),
                    modifier = Modifier.rotate(infoButtonRotation)
                )
            }
        }

        AnimatedVisibility(
            visible = infoExpanded,
        ) {
            Column(
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                expandedInfoComponent()
            }
        }
    }
}