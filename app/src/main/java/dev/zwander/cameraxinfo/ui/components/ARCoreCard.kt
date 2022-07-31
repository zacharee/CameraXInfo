package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.ar.core.ArCoreApk
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.model.LocalDataModel

private const val COLUMN_WEIGHT = 0.5f

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ARCoreCard(modifier: Modifier = Modifier) {
    val model = LocalDataModel.current

    PaddedColumnCard(
        modifier = modifier.animateContentSize()
    ) {
        Text(
            text = stringResource(id = R.string.ar_core),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        Divider(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 16.dp)
                .fillMaxWidth(0.33f)
        )

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.contentColorFor(MaterialTheme.colorScheme.surface)
            ),
            modifier = Modifier
                .animateContentSize()
                .fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = stringResource(id = R.string.status),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = stringResource(id = R.string.ar_core),
                        modifier = Modifier.weight(COLUMN_WEIGHT),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.width(8.dp))

                    Text(
                        text = stringResource(id = R.string.depth_api),
                        modifier = Modifier.weight(COLUMN_WEIGHT),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.supported),
                        modifier = Modifier.weight(1f)
                    )

                    SupportStateIcon(
                        state = when {
                            model.arCoreStatus?.isSupported == true -> true
                            model.arCoreStatus == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> false
                            else -> null
                        },
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )

                    Spacer(Modifier.width(8.dp))

                    SupportStateIcon(
                        state = model.depthStatus,
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.installed),
                        modifier = Modifier.weight(1f)
                    )

                    SupportStateIcon(
                        state = when (model.arCoreStatus) {
                            ArCoreApk.Availability.SUPPORTED_INSTALLED -> SupportState.SUPPORTED
                            ArCoreApk.Availability.SUPPORTED_APK_TOO_OLD -> SupportState.OUTDATED
                            ArCoreApk.Availability.SUPPORTED_NOT_INSTALLED -> SupportState.UNSUPPORTED
                            else -> SupportState.UNKNOWN
                        },
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )

                    Spacer(Modifier.width(8.dp))

                    SupportStateIcon(
                        state = SupportState.NOT_APPLICABLE,
                        modifier = Modifier.weight(COLUMN_WEIGHT)
                    )
                }
            }
        }
    }
}
