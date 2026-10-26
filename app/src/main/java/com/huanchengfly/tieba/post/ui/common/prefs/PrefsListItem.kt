package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ProvideMaterial3Content
import com.huanchengfly.tieba.post.ui.common.theme.compose.ProvideUnifiedTextStyle

/** Native M3 list rows; old preference keys and call signatures remain unchanged. */
@Composable
@ExperimentalMaterialApi
fun PrefsListItem(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    darkenOnDisable: Boolean = true,
    icon: @Composable (() -> Unit)? = null,
    secondaryText: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    minimalHeight: Boolean = false,
    text: @Composable () -> Unit,
) {
    val opacity = if (enabled || !darkenOnDisable) 1f else 0.38f
    val secondary = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = opacity)
    ListItem(
        modifier = modifier.heightIn(min = if (minimalHeight) 48.dp else 64.dp),
        headlineContent = {
            ProvideMaterial3Content {
                ProvideUnifiedTextStyle(MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)) {
                    text()
                }
            }
        },
        supportingContent = secondaryText?.let { slot ->
            { ProvideMaterial3Content { slot() } }
        },
        leadingContent = icon?.let { slot ->
            { ProvideMaterial3Content { slot() } }
        },
        trailingContent = trailing?.let { slot ->
            { ProvideMaterial3Content { slot() } }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = textColor.copy(alpha = opacity),
            supportingColor = secondary,
            leadingIconColor = MaterialTheme.colorScheme.primary.copy(alpha = opacity),
            trailingIconColor = secondary,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    )
}
