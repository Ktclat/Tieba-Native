@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.main.notifications

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.widgets.compose.StateIllustration
import com.huanchengfly.tieba.post.ui.widgets.compose.TipScreen
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.page.LocalNavigator
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.SearchPageDestination
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsListPage
import com.huanchengfly.tieba.post.ui.page.main.notifications.list.NotificationsType
import com.huanchengfly.tieba.post.ui.widgets.compose.ActionItem
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import androidx.compose.material3.PrimaryTabRow
import com.huanchengfly.tieba.post.ui.widgets.compose.TitleCentredToolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.ui.widgets.compose.accountNavIconIfCompact
import com.ramcosta.composedestinations.annotation.DeepLink
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

@Destination(deepLinks = [DeepLink(uriPattern = "tblite://notifications/{initialTab}")])
@Composable
fun NotificationsPage(navigator: DestinationsNavigator, initialTab: Int = 0) {
    ProvideNavigator(navigator) {
        NotificationInbox(initialTab, standalone = true)
    }
}

@Composable
fun NotificationsPage(initialTab: Int = 0) {
    NotificationInbox(initialTab, standalone = false)
}

@Composable
private fun NotificationInbox(initialTab: Int, standalone: Boolean) {
    val navigator = LocalNavigator.current
    val accountId = LocalAccount.current?.uid
    val types = NotificationsType.entries
    val pager = rememberPagerState(initialPage = initialTab.coerceIn(types.indices)) { types.size }
    val tabs: @Composable ColumnScope.() -> Unit = {
        if (accountId != null) {
            PrimaryTabRow(selectedTabIndex = pager.currentPage, divider = {}, containerColor = Color.Transparent) {
                types.forEachIndexed { index, type ->
                    Tab(
                        selected = pager.currentPage == index,
                        onClick = { pager.requestScrollToPage(index) },
                        text = { Text(stringResource(if (type == NotificationsType.ReplyMe) R.string.title_reply_me else R.string.title_at_me)) },
                    )
                }
            }
        }
    }
    MyScaffold(
        modifier = Modifier.fillMaxSize(),
        backgroundColor = Color.Transparent,
        topBar = {
            if (standalone) {
                TitleCentredToolbar(
                    title = stringResource(R.string.title_notifications),
                    navigationIcon = { BackNavigationIcon { navigator.navigateUp() } },
                    content = tabs,
                )
            } else {
                Toolbar(
                    title = stringResource(R.string.title_notifications),
                    navigationIcon = accountNavIconIfCompact(),
                    actions = {
                        ActionItem(icon = Icons.Rounded.Search, contentDescription = stringResource(R.string.title_search)) {
                            navigator.navigate(SearchPageDestination)
                        }
                    },
                    content = tabs,
                )
            }
        },
    ) { padding ->
        if (accountId == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                TipScreen(
                    title = { Text(stringResource(R.string.notification_guest_title)) },
                    image = { StateIllustration(Icons.Rounded.Notifications) },
                    message = { Text(stringResource(R.string.notification_guest_summary)) },
                    actions = {
                        Button(onClick = { navigator.navigate(LoginPageDestination) }, shapes = ButtonDefaults.shapes()) {
                            Text(stringResource(R.string.button_login))
                        }
                    },
                )
            }
        } else {
            LazyLoadHorizontalPager(
                state = pager,
                contentPadding = padding,
                verticalAlignment = Alignment.Top,
                key = { types[it].name },
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                NotificationsListPage(types[page], accountId)
            }
        }
    }
}
