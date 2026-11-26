@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R

private const val COLLAPSED_COUNT = 6

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun <T> SearchHistorySection(
    entries: List<T>,
    term: (T) -> String,
    clearMessage: String,
    onSearch: (T) -> Unit,
    onDelete: (T) -> Unit,
    onClear: () -> Unit,
) {
    val hasEntries = entries.isNotEmpty()
    var expanded by rememberSaveable(hasEntries) { mutableStateOf(false) }
    var managing by rememberSaveable(hasEntries) { mutableStateOf(false) }
    var confirmClear by rememberSaveable(hasEntries) { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.title_search_history),
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (hasEntries) {
                    TextButton(onClick = { managing = !managing }, shapes = ButtonDefaults.shapes()) {
                        Text(stringResource(if (managing) R.string.button_finish else R.string.search_history_manage))
                    }
                }
            }
        }
        if (!hasEntries) {
            item {
                Text(
                    stringResource(R.string.search_history_empty),
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (hasEntries && managing) {
            item {
                TextButton(onClick = { confirmClear = true }, modifier = Modifier.padding(horizontal = 8.dp), shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(R.string.button_clear_all), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        items(if (expanded || managing) entries else entries.take(COLLAPSED_COUNT)) { entry ->
            val query = term(entry)
            val deleteLabel = stringResource(R.string.search_history_delete_term, query)
            ListItem(
                modifier = Modifier.combinedClickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.search_history_search_again),
                    onClick = { onSearch(entry) },
                    onLongClickLabel = deleteLabel,
                    onLongClick = { onDelete(entry) },
                ),
                headlineContent = { Text(query, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                leadingContent = { Icon(Icons.Rounded.History, contentDescription = null) },
                trailingContent = if (managing) {
                    { IconButton(onClick = { onDelete(entry) }, shapes = IconButtonDefaults.shapes()) { Icon(Icons.Rounded.Close, deleteLabel) } }
                } else null,
            )
        }
        if (!managing && entries.size > COLLAPSED_COUNT) {
            item {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth(), shapes = ButtonDefaults.shapes()) {
                    Text(stringResource(if (expanded) R.string.button_expand_less_history else R.string.button_expand_more_history))
                }
            }
        }
    }

    if (hasEntries && confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.search_history_confirm_clear)) },
            text = { Text(clearMessage) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClear()
                }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.button_clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }, shapes = ButtonDefaults.shapes()) { Text(stringResource(R.string.button_cancel)) }
            },
        )
    }
}
