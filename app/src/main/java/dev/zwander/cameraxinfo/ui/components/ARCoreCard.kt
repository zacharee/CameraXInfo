package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import com.google.accompanist.flowlayout.MainAxisAlignment
import com.google.accompanist.flowlayout.SizeMode
import com.google.ar.core.ArCoreApk
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.model.LocalDataModel

@Composable
fun ARCoreCard(modifier: Modifier = Modifier) {
    val model = LocalDataModel.current

    PaddedColumnCard(
        modifier = modifier.animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.animateContentSize(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(id = R.string.ar_core),
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )

            AnimatedVisibility(visible = model.arCoreStatus == null) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Divider(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 16.dp)
                .fillMaxWidth(0.33f)
        )

        Text(
            text = stringResource(id = R.string.ar_core),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        FlowRow(
            mainAxisSize = SizeMode.Expand,
            mainAxisAlignment = MainAxisAlignment.SpaceEvenly,
            mainAxisSpacing = 8.dp
        ) {
            val (arCoreText, arCoreColor) = when {
                model.arCoreStatus?.isSupported == true -> R.string.supported to Color.Green
                model.arCoreStatus == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> R.string.unsupported to Color.Red
                else -> R.string.unknown to Color.Yellow
            }

            val acColor by animateColorAsState(targetValue = arCoreColor)

            Text(
                text = stringResource(id = arCoreText),
                color = acColor,
                modifier = Modifier.animateContentSize()
            )

            val (arCoreInstallText, arCoreInstallColor) = when (model.arCoreStatus) {
                ArCoreApk.Availability.SUPPORTED_INSTALLED -> R.string.installed to Color.Green
                ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> R.string.outdated to Color.Yellow
                ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> R.string.not_installed to Color.Red
                else -> R.string.unknown to Color.Yellow
            }

            val aciColor by animateColorAsState(targetValue = arCoreInstallColor)

            Text(
                text = stringResource(id = arCoreInstallText),
                color = aciColor,
                modifier = Modifier.animateContentSize()
            )
        }

        Spacer(Modifier.size(4.dp))

        Text(
            text = stringResource(id = R.string.depth_api),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        val (depthText, depthColor) = when (model.depthStatus) {
            true -> R.string.supported to Color.Green
            false -> R.string.unsupported to Color.Red
            else -> R.string.unknown to Color.Yellow
        }

        val dColor by animateColorAsState(targetValue = depthColor)

        Text(
            text = stringResource(id = depthText),
            color = dColor,
            modifier = Modifier.animateContentSize()
        )
    }
}
