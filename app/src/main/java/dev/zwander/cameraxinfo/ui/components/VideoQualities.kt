package dev.zwander.cameraxinfo.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import dev.zwander.cameraxinfo.R

@Composable
fun VideoQualities(supportedQualities: List<String>) {
    Text(
        text = stringResource(id = R.string.video_qualities),
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    )

    FlowRow(
        mainAxisSize = SizeMode.Expand,
        mainAxisAlignment = MainAxisAlignment.SpaceEvenly,
        mainAxisSpacing = 8.dp
    ) {
        supportedQualities.forEach {
            Text(
                text = it
            )
        }
    }
}