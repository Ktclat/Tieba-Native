package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    prefixIcon: (@Composable () -> Unit)? = null,
    appendIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    invertColor: Boolean = false,
) {
    val scheme = Material3Theme.colorScheme
    if (onClick != null) {
        AssistChip(
            onClick = onClick,
            modifier = modifier,
            label = { Text(text) },
            leadingIcon = prefixIcon,
            trailingIcon = appendIcon,
        )
        return
    }

    Badge(
        modifier = modifier,
        containerColor = if (invertColor) scheme.secondaryContainer else scheme.surfaceContainerHigh,
        contentColor = if (invertColor) scheme.onSecondaryContainer else scheme.onSurfaceVariant,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            prefixIcon?.let { Box(Modifier.size(16.dp)) { it() } }
            Text(text, style = Material3Theme.typography.labelMedium)
            appendIcon?.let { Box(Modifier.size(16.dp)) { it() } }
        }
    }
}
