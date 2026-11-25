package com.huanchengfly.tieba.post.ui.common.prefs

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Simple divider used to separate individual preferences
 */
@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    thickness: Dp = 1.dp,
    indent: Dp = 0.dp
) {
    HorizontalDivider(
        modifier = modifier.padding(horizontal = indent),
        thickness = thickness,
        color = color,
    )
}
