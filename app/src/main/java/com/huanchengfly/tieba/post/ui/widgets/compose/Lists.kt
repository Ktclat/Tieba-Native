package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun ListMenuItem(
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    iconColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: (() -> Unit)? = null,
    customContent: @Composable (RowScope.() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(text) },
        leadingContent = { Icon(imageVector = icon, contentDescription = null, tint = iconColor) },
        trailingContent = customContent?.let { { Row(verticalAlignment = Alignment.CenterVertically, content = it) } },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick == null) modifier else modifier.clickable(onClick = onClick),
    )
}
