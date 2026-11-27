@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.thread.tabs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.ui.page.destinations.MainPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadTabSearchPageDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

internal fun DestinationsNavigator.leaveClosedThread() {
    if (!navigateUp()) navigate(MainPageDestination) {
        popUpTo(ThreadPageDestination.route) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
internal fun ThreadTabsButton(
    navigator: DestinationsNavigator,
    currentThreadId: Long? = null,
    modifier: Modifier = Modifier,
) {
    val tabs = LocalThreadTabs.current ?: return
    val rules = LocalContentBlockRules.current
    val count by remember(tabs, rules) {
        derivedStateOf { if (rules.isEmpty) tabs.entries.size else tabs.entries.count { it.visibleWith(rules) } }
    }
    var open by rememberSaveable { mutableStateOf(false) }
    val description = stringResource(R.string.thread_tabs_open, count)
    IconButton(
        onClick = { open = true }, shapes = IconButtonDefaults.shapes(),
        modifier = modifier.testTag("thread-tabs-open").semantics { contentDescription = description },
    ) {
        Box(
            Modifier.heightIn(min = 23.dp).widthIn(min = 23.dp)
                .border(1.7.dp, LocalContentColor.current, RoundedCornerShape(6.dp))
                .padding(horizontal = 3.dp), contentAlignment = Alignment.Center,
        ) {
            Text(if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clearAndSetSemantics {})
        }
    }
    if (!open) return
    val entries = if (rules.isEmpty) tabs.entries else tabs.entries.filter { it.visibleWith(rules) }
    fun select(tab: ThreadTab) {
        open = false
        if (tab.threadId != currentThreadId) navigator.navigate(tab.direction()) {
            if (currentThreadId != null) popUpTo(ThreadPageDestination.route) { inclusive = true }
        }
    }
    ModalBottomSheet(
        onDismissRequest = { open = false },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.thread_tabs_title), Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge)
            FilledTonalButton(
                onClick = { open = false; navigator.navigate(ThreadTabSearchPageDestination) { launchSingleTop = true } },
                shapes = ButtonDefaults.shapes(), modifier = Modifier.testTag("thread-tabs-new"),
            ) {
                Icon(ImageVector.vectorResource(R.drawable.ms_add), contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.thread_tabs_new), Modifier.padding(start = 6.dp))
            }
        }
        if (entries.isEmpty()) {
            Text(stringResource(R.string.thread_tabs_empty), Modifier.padding(start = 24.dp, end = 24.dp, bottom = 36.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                items(entries, key = { it.threadId }) { tab ->
                    val active = tab.threadId == currentThreadId
                    val title = tab.title.ifBlank { stringResource(R.string.thread_tab_id, tab.threadId.toString()) }
                    Surface(
                        color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(
                                Modifier.weight(1f).testTag("thread-tab-${tab.threadId}")
                                    .clickable { select(tab) }.semantics { selected = active }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Text(title, style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                                val details = listOfNotNull(
                                    tab.forumName.takeIf(String::isNotBlank)?.let { stringResource(R.string.title_forum, it) },
                                    tab.floor.takeIf { it > 0 }?.let { stringResource(R.string.favorite_read_floor, it) },
                                )
                                if (details.isNotEmpty()) Text(details.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(
                                onClick = {
                                    tabs.close(tab.threadId)
                                    if (active) {
                                        val next = tabs.entries.firstOrNull { it.visibleWith(rules) }
                                        if (next != null) select(next) else { open = false; navigator.leaveClosedThread() }
                                    }
                                },
                                shapes = IconButtonDefaults.shapes(),
                                modifier = Modifier.padding(end = 4.dp).testTag("thread-tab-close-${tab.threadId}"),
                            ) {
                                Icon(ImageVector.vectorResource(R.drawable.ms_close),
                                    stringResource(R.string.thread_tab_close, title), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
