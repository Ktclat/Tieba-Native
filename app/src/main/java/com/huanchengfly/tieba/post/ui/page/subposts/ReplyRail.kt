package com.huanchengfly.tieba.post.ui.page.subposts

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

internal fun Modifier.replyRail(color: Color): Modifier = drawBehind {
    val width = 2.dp.toPx()
    val left = if (layoutDirection == LayoutDirection.Ltr) 0f else size.width - width
    drawRoundRect(
        color,
        topLeft = Offset(left, 0f),
        size = Size(width, size.height),
        cornerRadius = CornerRadius(width / 2f),
    )
}
