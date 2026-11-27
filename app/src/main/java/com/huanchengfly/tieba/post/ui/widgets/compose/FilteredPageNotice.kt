package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R

@Composable
internal fun FilteredPageNotice(onLoadMore: (() -> Unit)? = null) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.local_rules_filtered_page),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (onLoadMore != null) {
            TextButton(onClick = onLoadMore) { Text(stringResource(R.string.local_rules_continue_loading)) }
        }
    }
}
