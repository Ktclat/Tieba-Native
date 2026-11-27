package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalDisplayPreferences

@Composable
fun BlockTip(
    text: @Composable () -> Unit = { Text(text = stringResource(id = R.string.tip_blocked_content)) },
) {
    SupportingMessage(content = text)
}

@Composable
fun BlockableContent(
    blocked: Boolean,
    modifier: Modifier = Modifier,
    blockedTip: @Composable () -> Unit = { BlockTip() },
    hideBlockedContent: Boolean = LocalDisplayPreferences.current.hideBlockedContent,
    content: @Composable () -> Unit,
) {
    if (!blocked) {
        content()
    } else if (!hideBlockedContent) {
        Column(modifier = modifier) {
            blockedTip()
        }
    }
}
@Composable
fun BlockedDestination(
    onBack: () -> Unit,
    message: String = stringResource(R.string.tip_blocked_content),
) {
    MyScaffold(
        topBar = {
            TitleCentredToolbar(title = {}, navigationIcon = { BackNavigationIcon(onBack) })
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            BlockTip { Text(message) }
        }
    }
}
