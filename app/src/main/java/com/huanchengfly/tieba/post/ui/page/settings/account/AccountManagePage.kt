package com.huanchengfly.tieba.post.ui.page.settings.account

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.prefs.PrefsScreen
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.DropDownPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.EditTextPref
import com.huanchengfly.tieba.post.ui.common.prefs.widgets.TextPref
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.settings.LeadingIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.SupportingMessage
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.AccountUtil.AllAccounts
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.appPreferences
import com.huanchengfly.tieba.post.utils.launchUrl
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@OptIn(ExperimentalComposeUiApi::class)
@Destination
@Composable
fun AccountManagePage(
    navigator: DestinationsNavigator,
) {
    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TitleCentredToolbar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_account_manage),
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    BackNavigationIcon(onBackPressed = { navigator.navigateUp() })
                }
            )
        },
    ) { paddingValues ->
        val account = LocalAccount.current
        val context = LocalContext.current
        PrefsScreen(
            dataStore = LocalContext.current.dataStore,
            dividerThickness = 0.dp,
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
        ) {
            prefsItem {
                if (account != null) {
                    DropDownPref(
                        key = "switch_account",
                        title = stringResource(id = R.string.title_switch_account),
                        summary = stringResource(
                            id = R.string.summary_now_account,
                            account.nameShow ?: account.name
                        ),
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(R.drawable.ms_account_circle),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        onValueChange = { AccountUtil.switchAccount(context, it.toInt()) },
                        enabled = true,
                        defaultValue = account.id.toString(),
                        entries = AllAccounts.current.associate {
                            it.id.toString() to (it.nameShow ?: it.name)
                        }
                    )
                } else {
                    TextPref(
                        title = stringResource(id = R.string.title_switch_account),
                        summary = null,
                        leadingIcon = {
                            LeadingIcon {
                                AvatarIcon(
                                    icon = ImageVector.vectorResource(R.drawable.ms_account_circle),
                                    size = Sizes.Small,
                                    contentDescription = null,
                                )
                            }
                        },
                        darkenOnDisable = true,
                        enabled = false,
                    )
                }
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_new_account),
                    onClick = { navigator.navigate(LoginPageDestination) },
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_add_circle),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
            prefsItem {
                SupportingMessage(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text(buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(stringResource(R.string.tip_start))
                        }
                        append(stringResource(R.string.tip_account_error))
                    })
                }
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_exit_account),
                    onClick = { AccountUtil.exit(context) },
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(id = R.drawable.ms_cancel),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                )
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_modify_username),
                    onClick = {
                        launchUrl(
                            context,
                            navigator,
                            "https://wappass.baidu.com/static/manage-chunk/change-username.html#/showUsername"
                        )
                    },
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_supervised_user_circle),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = account != null
                )
            }
            prefsItem {
                TextPref(
                    title = stringResource(id = R.string.title_copy_bduss),
                    summary = stringResource(id = R.string.summary_copy_bduss),
                    onClick = { TiebaUtil.copyText(context, account?.bduss, isSensitive = true) },
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_content_copy),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = account != null
                )
            }
            prefsItem {
                val littleTail = remember { context.appPreferences.littleTail }
                EditTextPref(
                    key = "little_tail",
                    title = stringResource(id = R.string.title_my_tail),
                    summary = if (littleTail.isNullOrEmpty())
                        stringResource(id = R.string.tip_no_little_tail)
                    else
                        littleTail,
                    leadingIcon = {
                        LeadingIcon {
                            AvatarIcon(
                                icon = ImageVector.vectorResource(R.drawable.ms_edit),
                                size = Sizes.Small,
                                contentDescription = null,
                            )
                        }
                    },
                    enabled = true,
                    dialogTitle = stringResource(id = R.string.title_dialog_modify_little_tail),
                )
            }
        }
    }
}