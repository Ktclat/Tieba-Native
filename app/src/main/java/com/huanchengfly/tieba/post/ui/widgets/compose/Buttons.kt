package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.ContentAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.ProvideMaterial3Content

@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    elevation: ButtonElevation? = ButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
    shape: Shape = RoundedCornerShape(20.dp),
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        backgroundColor = ExtendedTheme.colors.accent,
        contentColor = ExtendedTheme.colors.onAccent,
    ),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    val containerColor by colors.backgroundColor(enabled)
    val contentColor by colors.contentColor(enabled)
    val disabledBackground by colors.backgroundColor(false)
    val disabledContent by colors.contentColor(false)
    val resolvedElevation = elevation?.elevation(enabled, interactionSource)?.value ?: 0.dp
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 48.dp),
        enabled = enabled,
        shape = shape,
        border = border,
        interactionSource = interactionSource,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = disabledBackground,
            disabledContentColor = disabledContent,
        ),
        elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(
            defaultElevation = resolvedElevation,
            pressedElevation = resolvedElevation,
            focusedElevation = resolvedElevation,
            hoveredElevation = resolvedElevation,
            disabledElevation = resolvedElevation,
        ),
        contentPadding = contentPadding,
    ) {
        // A migrated M3 Button must also supply its on-container color to old M2 Text.
        ProvideMaterial3Content { content() }
    }
}

@Composable
fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    shape: Shape = RoundedCornerShape(20.dp),
    border: BorderStroke? = null,
    color: Color = ExtendedTheme.colors.text,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = shape,
        border = border,
        colors = ButtonDefaults.buttonColors(
            backgroundColor = color.copy(alpha = 0.1f),
            contentColor = color,
            disabledBackgroundColor = color.copy(alpha = ContentAlpha.disabled * 0.1f),
            disabledContentColor = color.copy(alpha = ContentAlpha.disabled),
        ),
        contentPadding = contentPadding,
        content = content,
    )
}
