package com.huanchengfly.tieba.post.ui.page.settings.block

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.ui.common.prefs.GroupHeader
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.destinations.BlockListPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.BlockRuleEditorPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.BlockRuleTarget
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination
@Composable
fun BlockSettingsPage(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    val rules = LocalContentBlockRules.current
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_block_settings),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        PrefsScreen(
            dataStore = context.dataStore,
            dividerThickness = 0.dp,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            prefsGroup(header = { GroupHeader(stringResource(R.string.block_rules_section)) }) {
                BlockRuleTarget.entries.forEach { target ->
                    prefsItem {
                        TextPref(
                            title = stringResource(target.titleRes),
                            summary = stringResource(R.string.block_rules_count, rules.count(target)),
                            leadingIcon = {
                                LeadingIcon {
                                    AvatarIcon(
                                        icon = ImageVector.vectorResource(target.iconRes),
                                        size = Sizes.Small,
                                        contentDescription = null,
                                    )
                                }
                            },
                            onClick = { navigator.navigate(BlockRuleEditorPageDestination(target)) },
                        )
                    }
                }
            }
            prefsGroup(header = { GroupHeader(stringResource(R.string.block_management_section)) }) {
                prefsItem {
                    TextPref(
                        title = stringResource(id = R.string.title_block_list),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(R.drawable.ms_block),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        onClick = { navigator.navigate(BlockListPageDestination) }
                    )
                }
                prefsItem {
                    SwitchPref(
                        key = "hideBlockedContent",
                        title = stringResource(id = R.string.settings_hide_blocked_content),
                        defaultChecked = false
                    ) {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_hide_source),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    }
                }
                prefsItem {
                    SwitchPref(
                        key = "blockVideo",
                        title = stringResource(id = R.string.settings_block_video),
                        summary = stringResource(id = R.string.settings_block_video_summary),
                        defaultChecked = false,
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(R.drawable.ms_videocam_off),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}
