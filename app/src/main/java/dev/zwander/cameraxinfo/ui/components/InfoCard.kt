package dev.zwander.cameraxinfo.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.zwander.cameraxinfo.BuildConfig
import dev.zwander.cameraxinfo.R
import dev.zwander.cameraxinfo.launchUrl
import tk.zwander.patreonsupportersretrieval.data.SupporterInfo
import tk.zwander.patreonsupportersretrieval.util.DataParser

@Suppress("OPT_IN_IS_NOT_ENABLED")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoCard(refreshTime: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var showingSupportersList by rememberSaveable {
        mutableStateOf(false)
    }

    val supporters = rememberSaveable(
        saver = listSaver(
            save = {
                it
            },
            restore = {
                it.toMutableStateList()
            }
        )
    ) {
        mutableStateListOf<SupporterInfo>()
    }

    LaunchedEffect(key1 = showingSupportersList, key2 = refreshTime) {
        val newList = DataParser.getInstance(context).parseSupporters()

        if (newList.firstOrNull() != supporters.firstOrNull()) {
            supporters.clear()
            supporters.addAll(newList)
        }
    }

    PaddedColumnCard(modifier = modifier) {
        Text(
            text = stringResource(id = R.string.app_name),
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp
        )

        HorizontalDivider(
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(0.33f)
        )

        Text(
            text = BuildConfig.VERSION_NAME
        )

        Spacer(Modifier.size(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            item(key = "Website") {
                LinkIconButton(
                    link = "https://zwander.dev",
                    icon = painterResource(id = R.drawable.earth),
                    contentDescription = stringResource(id = R.string.website)
                )
            }

            item(key = "Twitter") {
                LinkIconButton(
                    link = "https://twitter.com/Wander1236",
                    icon = painterResource(id = R.drawable.twitter),
                    contentDescription = stringResource(id = R.string.twitter)
                )
            }

            item(key = "GitHub") {
                LinkIconButton(
                    link = "https://github.com/zacharee/CameraXInfo",
                    icon = painterResource(id = R.drawable.github),
                    contentDescription = stringResource(id = R.string.github)
                )
            }

            item(key = "Patreon") {
                LinkIconButton(
                    link = "https://www.patreon.com/zacharywander",
                    icon = painterResource(id = R.drawable.patreon),
                    contentDescription = stringResource(id = R.string.patreon)
                )
            }

            item(key = "Donate") {
                LinkIconButton(
                    link = "https://www.paypal.com/donate/?hosted_button_id=EWAPDSENZ7U44",
                    icon = painterResource(id = R.drawable.outline_attach_money_24),
                    contentDescription = stringResource(id = R.string.donate)
                )
            }

            item(key = "Supporters") {
                IconButton(onClick = { showingSupportersList = !showingSupportersList }) {
                    Icon(
                        painter = painterResource(id = R.drawable.heart),
                        contentDescription = stringResource(id = R.string.supporters)
                    )
                }
            }
        }

        AnimatedVisibility(visible = showingSupportersList) {
            Column(
                modifier = Modifier.animateContentSize(),
            ) {
                Spacer(modifier = Modifier.size(12.dp))

                Crossfade(
                    targetState = supporters.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                ) { hasSupporters ->
                    if (hasSupporters) {
                        LazyColumn(
                            modifier = Modifier
                                .heightIn(max = 300.dp)
                                .fillMaxWidth()
                        ) {
                            items(count = supporters.size, key = { supporters[it].hashCode() }) {
                                val supporter = supporters[it]

                                OutlinedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        context.launchUrl(supporter.link)
                                    },
                                    colors = CardDefaults.outlinedCardColors(
                                        containerColor = Color.Transparent
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .heightIn(min = 48.dp)
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = supporter.name
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}