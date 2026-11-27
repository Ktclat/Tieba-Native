package com.huanchengfly.tieba.post.ui.page.settings.custom

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.PredictiveBackKey
import com.huanchengfly.tieba.post.activities.AppFontSizeActivity
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.goToActivity
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.ListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.MultiSelectListPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.SwitchPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.main.FloatingNavigationBarKey
import com.huanchengfly.tieba.post.ui.page.main.MainNavigationPreferences
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.page.thread.tabs.ThreadTabsGridLayout
import com.huanchengfly.tieba.post.ui.page.thread.tabs.ThreadTabsLayoutKey
import com.huanchengfly.tieba.post.ui.page.thread.tabs.ThreadTabsStackLayout
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AppIconUtil
import com.huanchengfly.tieba.post.utils.LauncherIcons
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalComposeUiApi::class)
@Destination
@Composable
fun CustomSettingsPage(
    navigator: DestinationsNavigator
) {
    val context = LocalContext.current
    MyScaffold(
        backgroundColor = Color.Transparent,
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_settings_custom),
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
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_custom_font_size),
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_font_download),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    onClick = {
                        context.goToActivity<AppFontSizeActivity>()
                    }
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                prefsItem {
                    SwitchPref(
                        key = PredictiveBackKey.name,
                        title = stringResource(R.string.title_predictive_back),
                        summary = stringResource(R.string.summary_predictive_back),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(resId = R.drawable.ms_arrow_back, size = Sizes.Small, contentDescription = null)
                            }
                        },
                    )
                }
            }
            prefsItem {
                val darkThemeValues = stringArrayResource(id = R.array.dark_theme_values)
                val darkThemeNames = stringArrayResource(id = R.array.dark_theme_names)
                ListPref(
                    key = "dark_theme",
                    title = stringResource(id = R.string.settings_night_mode),
                    defaultValue = ThemeUtil.THEME_AMOLED_DARK,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_brightness_2),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    entries = darkThemeValues.associateWith { value ->
                        darkThemeNames[darkThemeValues.indexOf(
                            value
                        )]
                    },
                    useSelectedAsSummary = true,
                )
            }
            prefsItem {
                ListPref(
                    key = "app_icon",
                    title = stringResource(id = R.string.settings_app_icon),
                    defaultValue = LauncherIcons.DEFAULT_ICON,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_apps),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    entries = mapOf(
                        LauncherIcons.NEW_ICON to "新图标",
                        LauncherIcons.NEW_ICON_INVERT to "新图标（反色）",
                        LauncherIcons.OLD_ICON to "旧图标",
                    ),
                    icons = mapOf(
                        LauncherIcons.NEW_ICON to {
                            Image(
                                painter = rememberDrawablePainter(
                                    drawable = LocalContext.current.getDrawable(
                                        R.drawable.ic_launcher_new_round
                                    )
                                ),
                                contentDescription = "新图标",
                                modifier = Modifier.size(Sizes.Medium)
                            )
                        },
                        LauncherIcons.NEW_ICON_INVERT to {
                            Image(
                                painter = rememberDrawablePainter(
                                    drawable = LocalContext.current.getDrawable(
                                        R.drawable.ic_launcher_new_invert_round
                                    )
                                ),
                                contentDescription = "新图标（反色）",
                                modifier = Modifier.size(Sizes.Medium)
                            )
                        },
                        LauncherIcons.OLD_ICON to {
                            Image(
                                painter = rememberDrawablePainter(
                                    drawable = LocalContext.current.getDrawable(
                                        R.drawable.ic_launcher_round
                                    )
                                ),
                                contentDescription = "旧图标",
                                modifier = Modifier.size(Sizes.Medium)
                            )
                        },
                    ),
                    onValueChange = { AppIconUtil.setIcon(icon = it) },
                    useSelectedAsSummary = true,
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                prefsItem {
                    val supportThemedIcons = remember {
                        persistentListOf(
                            LauncherIcons.NEW_ICON,
//                            LauncherIcons.NEW_ICON_INVERT,
                        )
                    }
                    val currentLauncherIcon by rememberPreferenceAsState(
                        key = stringPreferencesKey("app_icon"),
                        defaultValue = LauncherIcons.NEW_ICON
                    )
                    val isCurrentSupportThemedIcon by remember {
                        derivedStateOf {
                            supportThemedIcons.contains(currentLauncherIcon)
                        }
                    }
                    SwitchPref(
                        key = "useThemedIcon",
                        title = stringResource(id = R.string.title_settings_use_themed_icon),
                        defaultChecked = false,
                        enabled = isCurrentSupportThemedIcon,
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(R.drawable.ms_color_lens),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        onCheckedChange = {
                            AppIconUtil.setIcon(isThemed = it)
                        },
                        summary = stringResource(id = R.string.tip_settings_use_themed_icon_summary_not_supported).takeUnless { isCurrentSupportThemedIcon },
                    )
                }
            }
            prefsItem {
                SwitchPref(
                    key = "follow_system_night",
                    title = stringResource(id = R.string.title_settings_follow_system_night),
                    defaultChecked = true,
                ) {
                    LeadingIcon {
                        AvatarIcon(
                            icon = ImageVector.vectorResource(R.drawable.ms_brightness_auto),
                            size = Sizes.Small,
                            contentDescription = null,
                        )
                    }
                }
            }
            prefsItem {
                SwitchPref(
                    key = "status_bar_darker",
                    title = stringResource(id = R.string.title_settings_status_bar_darker),
                    summary = stringResource(id = R.string.summary_settings_status_bar_darker),
                    defaultChecked = true,
                ) {
                    LeadingIcon {
                        AvatarIcon(
                            icon = ImageVector.vectorResource(id = R.drawable.ms_science),
                            size = Sizes.Small,
                            contentDescription = null,
                        )
                    }
                }
            }
            prefsItem {
                SwitchPref(
                    key = "custom_toolbar_primary_color",
                    title = stringResource(id = R.string.tip_toolbar_primary_color),
                    defaultChecked = false,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_format_color_fill),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    summary = stringResource(id = R.string.tip_toolbar_primary_color_summary),
                )
            }
            prefsItem {
                ListPref(
                    key = ThreadTabsLayoutKey.name,
                    title = stringResource(R.string.thread_tabs_layout_title),
                    defaultValue = ThreadTabsGridLayout,
                    entries = mapOf(
                        ThreadTabsGridLayout to stringResource(R.string.thread_tabs_layout_grid),
                        ThreadTabsStackLayout to stringResource(R.string.thread_tabs_layout_stack),
                    ),
                    useSelectedAsSummary = true,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(resId = R.drawable.ms_apps, size = Sizes.Small, contentDescription = null)
                        }
                    },
                )
            }
            prefsItem {
                SwitchPref(
                    key = "listSingle",
                    title = stringResource(id = R.string.settings_forum_single),
                    defaultChecked = false,
                ) {
                    LeadingIcon {
                        AvatarIcon(
                            icon = ImageVector.vectorResource(R.drawable.ms_view_agenda),
                            size = Sizes.Small,
                            contentDescription = null,
                        )
                    }
                }
            }
            prefsItem {
                SwitchPref(
                    key = FloatingNavigationBarKey.name,
                    title = stringResource(R.string.title_floating_navigation_bar),
                    summary = stringResource(R.string.summary_floating_navigation_bar),
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(resId = R.drawable.ms_bottom_navigation, size = Sizes.Small, contentDescription = null)
                        }
                    },
                )
            }
            prefsItem {
                val labels = MainNavigationPreferences.options.mapValues { stringResource(it.value) }
                val userLabel = stringResource(R.string.title_user)
                MultiSelectListPref(
                    key = MainNavigationPreferences.key.name,
                    title = stringResource(R.string.title_visible_main_tabs),
                    defaultValue = MainNavigationPreferences.options.keys,
                    entries = labels,
                    selectedSummary = { enabled ->
                        MainNavigationPreferences.visibleIds(enabled).joinToString("、") { labels[it] ?: userLabel }
                    },
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(resId = R.drawable.ms_bottom_navigation, size = Sizes.Small, contentDescription = null)
                        }
                    },
                )
            }
            prefsItem {
                SwitchPref(
                    key = "liftUpBottomBar",
                    title = stringResource(id = R.string.title_lift_up_bottom_bar),
                    defaultChecked = true,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_upcoming),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    summary = stringResource(id = R.string.summary_lift_up_bottom_bar),
                )
            }
        }
    }
}
