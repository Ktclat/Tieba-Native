package com.huanchengfly.tieba.post.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

internal val LocalCommentLinkColor = staticCompositionLocalOf { Color.Unspecified }

@Composable
internal fun ProvideCommentLinkColor(background: Color, content: @Composable () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val linkColor = remember(colors, background) {
        val surface = background.copy(alpha = 1f)
        val candidates = listOf(colors.primary, colors.secondary, colors.onSurface, Color.Black, Color.White)
        candidates.map { it.compositeOver(surface) }.first {
            ColorUtils.calculateContrast(it.toArgb(), surface.toArgb()) >= 4.5
        }
    }
    CompositionLocalProvider(LocalCommentLinkColor provides linkColor, content = content)
}
