package com.huanchengfly.tieba.post.ui.page.main.user

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.text.style.TextOverflow
import com.huanchengfly.tieba.post.ui.widgets.compose.PullToRefreshLayout
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.widgets.compose.loadingPlaceholder
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.AboutPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.AppThemePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.HistoryPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.SettingsPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadStorePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.WebViewPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.ConfirmDialog
import androidx.compose.material3.VerticalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.ListMenuItem
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.CuidUtils
import com.huanchengfly.tieba.post.utils.StringUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.appPreferences

@Composable
private fun StatCard(account: Account?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp).loadingPlaceholder(visible = account == null),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatCardItem(account?.concernNum?.toIntOrNull() ?: 0, stringResource(R.string.text_stat_follow))
            VerticalDivider(Modifier.height(16.dp))
            StatCardItem(account?.fansNum?.toIntOrNull() ?: 0, stringResource(R.string.text_stat_fans))
            VerticalDivider(Modifier.height(16.dp))
            StatCardItem(account?.postNum?.toIntOrNull() ?: 0, stringResource(R.string.title_stat_posts_num))
        }
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    userName: String = "",
    userIntro: String = "",
    avatar: String? = null,
    isPlaceholder: Boolean = false
) {
    ListItem(
        modifier = modifier,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        headlineContent = {
            if (isPlaceholder) Spacer(Modifier.size(112.dp, 20.dp).loadingPlaceholder(true))
            else Text(userName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            if (isPlaceholder) Spacer(Modifier.size(164.dp, 16.dp).loadingPlaceholder(true))
            else Text(userIntro, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        leadingContent = {
            if (isPlaceholder) Spacer(Modifier.size(Sizes.Medium).loadingPlaceholder(true, shape = CircleShape))
            else Avatar(data = avatar, size = Sizes.Medium, contentDescription = null)
        },
    )
}

@Composable
private fun RowScope.StatCardItem(
    statNum: Int,
    statText: String
) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "$statNum",
            style = MaterialTheme.typography.titleLarge,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = statText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LoginTipCard(onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onLogin,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = { Text(stringResource(R.string.tip_login), style = MaterialTheme.typography.titleMedium) },
            leadingContent = { Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(32.dp)) },
            trailingContent = { Icon(ImageVector.vectorResource(R.drawable.ic_round_chevron_right), contentDescription = null) },
        )
    }
}

@Composable
fun UserPage(
    viewModel: UserViewModel = pageViewModel<UserUiIntent, UserViewModel>(
        listOf(
            UserUiIntent.Refresh
        )
    )
) {
    val context = LocalContext.current
    val navigator = LocalNavigator.current
    val isLoading by viewModel.uiState.collectPartialAsState(UserUiState::isLoading)
    val account by viewModel.uiState.collectPartialAsState(UserUiState::account)

    val switchToNightDialogState = rememberDialogState()
    ConfirmDialog(
        dialogState = switchToNightDialogState,
        onConfirm = {},
        onCancel = {
            context.appPreferences.followSystemNight = false
            ThemeUtil.switchNightMode()
        },
        confirmText = stringResource(id = R.string.btn_keep_following),
        cancelText = stringResource(id = R.string.btn_close_following)
    ) {
        Text(text = stringResource(id = R.string.message_dialog_follow_system_night))
    }

    MyScaffold(
        backgroundColor = Color.Transparent,
        modifier = Modifier
            .statusBarsPadding()
            .fillMaxSize()
    ) { contentPaddings ->
        PullToRefreshLayout(
            refreshing = isLoading,
            onRefresh = { viewModel.send(UserUiIntent.Refresh) },
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPaddings),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState())
                    .fillMaxSize()
            ) {
                if (account != null) {
                    val selected = checkNotNull(account)
                    Card(
                        onClick = { navigator.navigate(UserProfilePageDestination(selected.uid.toLong())) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        InfoCard(
                            userName = selected.nameShow ?: selected.name,
                            userIntro = selected.intro ?: stringResource(R.string.tip_no_intro),
                            avatar = StringUtil.getAvatarUrl(selected.portrait),
                        )
                    }
                    StatCard(selected)
                } else if (isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        InfoCard(isPlaceholder = true)
                    }
                    StatCard(null)
                } else {
                    LoginTipCard(
                        onLogin = { navigator.navigate(LoginPageDestination) },
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (account != null) {
                    ListMenuItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_favorite),
                        text = stringResource(id = R.string.title_my_collect),
                        onClick = {
                            navigator.navigate(ThreadStorePageDestination)
                        }
                    )
                }
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_outline_watch_later_24),
                    text = stringResource(id = R.string.title_history),
                    onClick = {
                        navigator.navigate(HistoryPageDestination)
                    }
                )
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_brush_24),
                    text = stringResource(id = R.string.title_theme),
                    onClick = {
                        navigator.navigate(AppThemePageDestination)
                    }
                ) {
                    Text(
                        text = stringResource(id = R.string.my_info_night),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = ThemeUtil.isNightMode(ThemeUtil.themeState.value),
                        onCheckedChange = {
                            if (context.appPreferences.followSystemNight) {
                                switchToNightDialogState.show()
                            } else {
                                ThemeUtil.switchNightMode()
                            }
                        }
                    )
                }
                if (account != null) {
                    ListMenuItem(
                        icon = ImageVector.vectorResource(id = R.drawable.ic_help_outline_black_24),
                        text = stringResource(id = R.string.my_info_service_center),
                        onClick = {
                            navigator.navigate(
                                WebViewPageDestination(
                                    initialUrl = "https://tieba.baidu.com/mo/q/hybrid-main-service/uegServiceCenter?cuid=${CuidUtils.getNewCuid()}&cuid_galaxy2=${CuidUtils.getNewCuid()}&cuid_gid=&timestamp=${System.currentTimeMillis()}&_client_version=12.52.1.0&nohead=1"
                                )
                            )
                        },
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp)
                )
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_settings_24),
                    text = stringResource(id = R.string.my_info_settings),
                    onClick = { navigator.navigate(SettingsPageDestination) },
                )
                ListMenuItem(
                    icon = ImageVector.vectorResource(id = R.drawable.ic_info_black_24),
                    text = stringResource(id = R.string.my_info_about),
                    onClick = { navigator.navigate(AboutPageDestination) },
                )
            }
        }
    }
}
