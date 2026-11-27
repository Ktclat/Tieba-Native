@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.thread.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.datastore.preferences.core.stringPreferencesKey
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

internal val ThreadTabsLayoutKey = stringPreferencesKey("threadTabsLayout")
internal const val ThreadTabsGridLayout = "grid"
internal const val ThreadTabsStackLayout = "stack"

@Composable
internal fun ThreadTabsButton(
    navigator: DestinationsNavigator,
    currentThreadId: Long? = null,
    modifier: Modifier = Modifier,
    currentTab: (() -> ThreadTab)? = null,
) {
    val tabs = LocalThreadTabs.current ?: return
    val rules = LocalContentBlockRules.current
    val count by remember(tabs, rules) {
        derivedStateOf { if (rules.isEmpty) tabs.entries.size else tabs.entries.count { it.visibleWith(rules) } }
    }
    var open by rememberSaveable(tabs) { mutableStateOf(false) }
    val description = stringResource(R.string.thread_tabs_open, count)
    IconButton(
        onClick = { open = true },
        shapes = IconButtonDefaults.shapes(),
        modifier = modifier.testTag("thread-tabs-open").semantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier.heightIn(min = 23.dp).widthIn(min = 23.dp)
                .border(1.7.dp, LocalContentColor.current, RoundedCornerShape(6.dp))
                .padding(horizontal = 3.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
    if (!open) return

    val entries = tabs.entries
    val visibleEntries = remember(entries, rules) {
        if (rules.isEmpty) entries else entries.filter { it.visibleWith(rules) }
    }
    ThreadTabsWindow(
        entries = visibleEntries,
        ready = tabs.ready,
        currentThreadId = currentThreadId,
        canSave = currentTab != null,
        canCloseAll = entries.isNotEmpty(),
        onDismiss = { open = false },
        onSave = {
            currentTab?.invoke()?.let { tabs.save(it) }
            open = false
        },
        onSelect = { tab ->
            open = false
            if (tab.threadId != currentThreadId) {
                navigator.navigate(tab.direction()) {
                    if (currentThreadId != null && tabs.find(currentThreadId) != null) {
                        popUpTo(ThreadPageDestination.route) { inclusive = true }
                    }
                }
            }
        },
        onClose = tabs::close,
        onCloseAll = tabs::closeAll,
    )
}

@Composable
private fun ThreadTabsWindow(
    entries: List<ThreadTab>,
    ready: Boolean,
    currentThreadId: Long?,
    canSave: Boolean,
    canCloseAll: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onSelect: (ThreadTab) -> Unit,
    onClose: (Long) -> Unit,
    onCloseAll: () -> Unit,
) {
    val layout by LocalContext.current.dataStore.collectPreferenceAsState(
        ThreadTabsLayoutKey, defaultValue = ThreadTabsGridLayout, initialValue = null,
    )
    val stacked = layout == ThreadTabsStackLayout
    var confirmingCloseAll by rememberSaveable { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        val lightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.179f
        SideEffect {
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = lightSurface
                    isAppearanceLightNavigationBars = lightSurface
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize().testTag("thread-tabs-window"),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.safeDrawingPadding()) {
                TopAppBar(
                    title = { Text(stringResource(R.string.thread_tabs_title)) },
                    navigationIcon = {
                        IconButton(
                            onClick = onDismiss,
                            shapes = IconButtonDefaults.shapes(),
                            modifier = Modifier.testTag("thread-tabs-back"),
                        ) {
                            Icon(ImageVector.vectorResource(R.drawable.ms_arrow_back), stringResource(R.string.button_back))
                        }
                    },
                    windowInsets = WindowInsets(0, 0, 0, 0),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(
                        onClick = onSave,
                        enabled = ready && canSave,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.testTag("thread-tabs-new"),
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ms_add), null, Modifier.size(18.dp))
                        Text(stringResource(R.string.thread_tabs_new), Modifier.padding(start = 6.dp))
                    }
                    OutlinedButton(
                        onClick = { confirmingCloseAll = true },
                        enabled = ready && canCloseAll,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier.testTag("thread-tabs-close-all"),
                    ) {
                        Icon(ImageVector.vectorResource(R.drawable.ms_delete), null, Modifier.size(18.dp))
                        Text(stringResource(R.string.thread_tabs_close_all), Modifier.padding(start = 6.dp))
                    }
                }
                when {
                    !ready || layout == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                    entries.isEmpty() -> Column(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically),
                    ) {
                        Icon(
                            ImageVector.vectorResource(R.drawable.ms_bookmark), null, Modifier.size(32.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            stringResource(if (canCloseAll) R.string.thread_tabs_filtered else R.string.thread_tabs_empty),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            stringResource(if (canCloseAll) R.string.thread_tabs_filtered_hint else R.string.thread_tabs_empty_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    else -> LazyVerticalGrid(
                        columns = if (stacked) GridCells.Fixed(1) else GridCells.Adaptive(148.dp),
                        modifier = Modifier.fillMaxSize().testTag(if (stacked) "thread-tabs-stack" else "thread-tabs-grid"),
                        contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(if (stacked) (-10).dp else 10.dp),
                    ) {
                        items(entries, key = { it.threadId }) { tab ->
                            ThreadTabCard(
                                tab = tab,
                                active = tab.threadId == currentThreadId,
                                stacked = stacked,
                                onClick = { onSelect(tab) },
                                onClose = { onClose(tab.threadId) },
                            )
                        }
                    }
                }
            }
        }
        if (confirmingCloseAll) {
            AlertDialog(
                onDismissRequest = { confirmingCloseAll = false },
                title = { Text(stringResource(R.string.thread_tabs_confirm_close_all)) },
                dismissButton = {
                    TextButton(onClick = { confirmingCloseAll = false }) { Text(stringResource(R.string.button_cancel)) }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmingCloseAll = false
                            onCloseAll()
                        },
                        modifier = Modifier.testTag("thread-tabs-confirm-close-all"),
                    ) { Text(stringResource(R.string.thread_tabs_close_all)) }
                },
            )
        }
    }
}

@Composable
private fun ThreadTabCard(tab: ThreadTab, active: Boolean, stacked: Boolean, onClick: () -> Unit, onClose: () -> Unit) {
    val title = tab.title.ifBlank { stringResource(R.string.thread_tab_id, tab.threadId.toString()) }
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag("thread-tab-${tab.threadId}").semantics { selected = active },
        shape = MaterialTheme.shapes.large,
        color = if (active) colors.secondaryContainer else colors.surfaceContainer,
        border = BorderStroke(1.dp, if (active) colors.primary else colors.outlineVariant),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 14.dp, end = 6.dp, bottom = if (stacked) 20.dp else 14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (tab.forumName.isBlank()) "" else stringResource(R.string.title_forum, tab.forumName),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) colors.onSecondaryContainer else colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = onClose,
                    shapes = IconButtonDefaults.shapes(),
                    modifier = Modifier.testTag("thread-tab-close-${tab.threadId}"),
                ) {
                    Icon(ImageVector.vectorResource(R.drawable.ms_close), stringResource(R.string.thread_tab_close, title), Modifier.size(18.dp))
                }
            }
            Text(
                title,
                modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                minLines = if (stacked) 1 else 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (tab.floor > 0) {
                Text(
                    stringResource(R.string.favorite_read_floor, tab.floor),
                    modifier = Modifier.padding(top = 6.dp, end = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (active) colors.onSecondaryContainer else colors.onSurfaceVariant,
                )
            }
        }
    }
}
