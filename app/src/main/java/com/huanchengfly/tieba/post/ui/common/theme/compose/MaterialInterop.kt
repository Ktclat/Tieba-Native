package com.huanchengfly.tieba.post.ui.common.theme.compose

import androidx.compose.material.LocalContentAlpha as LegacyContentAlpha
import androidx.compose.material.LocalContentColor as LegacyContentColor
import androidx.compose.material.LocalTextStyle as LegacyTextStyle
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/** A migration boundary, not a second theme: both libraries receive the same effective color. */
@Composable
fun ProvideUnifiedContentColor(
    color: Color,
    alpha: Float = color.alpha,
    content: @Composable () -> Unit,
) {
    val opacity = if (alpha.isFinite()) alpha.coerceIn(0f, 1f) else 1f
    CompositionLocalProvider(
        LegacyContentColor provides color.copy(alpha = 1f),
        LegacyContentAlpha provides opacity,
        LocalContentColor provides color.copy(alpha = opacity),
        content = content,
    )
}

/** M3 Text does not read the old Material LocalTextStyle; explicitly bridge both slot contracts. */
@Composable
fun ProvideUnifiedTextStyle(style: TextStyle, content: @Composable () -> Unit) {
    val merged = LocalTextStyle.current.merge(style)
    CompositionLocalProvider(
        LegacyTextStyle provides merged,
        LocalTextStyle provides merged,
        content = content,
    )
}

/** Use at M3 slots that may still receive legacy Text/Icon callers during migration. */
@Composable
fun ProvideMaterial3Content(content: @Composable () -> Unit) {
    val color = LocalContentColor.current
    val style = LocalTextStyle.current
    ProvideUnifiedContentColor(color) {
        ProvideUnifiedTextStyle(style, content)
    }
}
