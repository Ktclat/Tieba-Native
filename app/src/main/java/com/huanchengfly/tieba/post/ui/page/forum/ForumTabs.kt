package com.huanchengfly.tieba.post.ui.page.forum

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R

private val sortLabels = listOf(R.string.title_sort_by_reply, R.string.title_sort_by_send)

@Composable
internal fun ForumTabs(
    selectedPage: Int,
    sortType: Int,
    onPageSelected: (Int) -> Unit,
    onSortSelected: (Int) -> Unit,
    enabled: Boolean = true,
) {
    var sortMenuOpen by remember(selectedPage, enabled) { mutableStateOf(false) }
    val sortDescription = sortLabels.getOrNull(sortType)?.let {
        stringResource(R.string.search_current_sort, stringResource(it))
    }
    PrimaryTabRow(selectedTabIndex = selectedPage) {
        Box {
            Tab(
                selected = selectedPage == 0,
                onClick = {
                    if (selectedPage == 0) sortMenuOpen = true else onPageSelected(0)
                },
                enabled = enabled,
                modifier = Modifier.semantics {
                    if (selectedPage == 0 && sortDescription != null) stateDescription = sortDescription
                },
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.tab_forum_latest))
                        if (selectedPage == 0) {
                            Icon(
                                ImageVector.vectorResource(if (sortMenuOpen) R.drawable.ms_arrow_drop_up else R.drawable.ms_arrow_drop_down),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
            )
            DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                sortLabels.forEachIndexed { value, label ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            sortMenuOpen = false
                            if (sortType != value) onSortSelected(value)
                        },
                        modifier = Modifier.semantics { selected = sortType == value },
                        trailingIcon = {
                            if (sortType == value) Icon(ImageVector.vectorResource(R.drawable.ms_check), contentDescription = null)
                        },
                    )
                }
            }
        }
        Tab(
            selected = selectedPage == 1,
            onClick = { onPageSelected(1) },
            enabled = enabled,
            unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            text = { Text(stringResource(R.string.tab_forum_good)) },
        )
    }
}
