package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Surface as Material3Surface
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.invertChipBackground
import com.huanchengfly.tieba.post.ui.common.theme.compose.invertChipContent

@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    prefixIcon: (@Composable () -> Unit)? = null,
    appendIcon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    invertColor: Boolean = false,
    shape: Shape = RoundedCornerShape(16.dp),
) {
    val color =
        if (invertColor) ExtendedTheme.colors.invertChipContent else ExtendedTheme.colors.onChip
    val backgroundColor =
        if (invertColor) ExtendedTheme.colors.invertChipBackground else ExtendedTheme.colors.chip

    val animatedColor by animateColorAsState(targetValue = color, label = "ChipColor")
    val animatedBackgroundColor by animateColorAsState(
        targetValue = backgroundColor,
        label = "ChipBackgroundColor"
    )

    Material3Surface(
        modifier = modifier
            .clip(shape)
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        shape = shape,
        color = animatedBackgroundColor,
        contentColor = animatedColor,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            prefixIcon?.let {
                Box(Modifier.size(17.dp)) { it() }
            }
            Text(
                text = text,
                style = Material3Theme.typography.labelMedium,
                color = animatedColor,
            )
            appendIcon?.let {
                Box(Modifier.size(17.dp)) { it() }
            }
        }
    }
}
