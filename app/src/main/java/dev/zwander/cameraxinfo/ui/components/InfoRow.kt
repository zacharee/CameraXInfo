package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zwander.cameraxinfo.util.ArrangementExt

@Composable
fun InfoRow(
    title: String,
    supportedQualities: List<String>?,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
        )

        FlowRow(
            horizontalArrangement = ArrangementExt.SpaceEvenly(8.dp),
            modifier = Modifier.fillMaxWidth()
                .animateContentSize(),
        ) {
            supportedQualities?.forEach {
                Text(
                    text = it,
                )
            }
        }
    }
}