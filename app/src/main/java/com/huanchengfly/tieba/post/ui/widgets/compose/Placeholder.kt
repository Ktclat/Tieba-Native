package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R

@Composable
fun EmptyPlaceholder(
    modifier: Modifier = Modifier,
    emptyText: String = stringResource(id = R.string.tip_empty),
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = emptyText, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        actions()
    }
}

@Composable
fun Modifier.loadingPlaceholder(
    visible: Boolean,
    color: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    shape: Shape = MaterialTheme.shapes.small,
): Modifier = if (visible) {
    clearAndSetSemantics { }.background(color, shape).drawWithContent { }
} else this
