package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.zwander.cameraxinfo.R

@Composable
fun VideoQualities(
    dynamicRange: String,
    supportedQualities: List<String>?,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.video_qualities_format, dynamicRange),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        FlowRow(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
                .animateContentSize()
        ) {
            supportedQualities?.forEach {
                Text(
                    text = it
                )
            }
        }
    }
}