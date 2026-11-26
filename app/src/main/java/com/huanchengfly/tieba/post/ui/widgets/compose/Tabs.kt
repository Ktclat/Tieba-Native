package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TabClickMenu(
    selected: Boolean,
    onClick: () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = remember(menuState) { MenuScope(menuState) }
    Box {
        Tab(
            selected = selected,
            onClick = {
                if (!selected) {
                    onClick()
                } else {
                    menuState.toggle()
                }
            },
            modifier = modifier,
            enabled = enabled,
            interactionSource = interactionSource,
            selectedContentColor = selectedContentColor,
            unselectedContentColor = unselectedContentColor,
            content = content
        )
        DropdownMenu(expanded = menuState.expanded, onDismissRequest = menuState::dismiss) {
            scope.menuContent()
        }
    }
}

@Composable
fun TabClickMenu(
    selected: Boolean,
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    selectedContentColor: Color = MaterialTheme.colorScheme.primary,
    unselectedContentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    TabClickMenu(
        selected = selected,
        onClick = onClick,
        menuContent = menuContent,
        modifier = modifier,
        enabled = enabled,
        menuState = menuState,
        interactionSource = interactionSource,
        selectedContentColor = selectedContentColor,
        unselectedContentColor = unselectedContentColor,
    ) {
        val rotate by animateFloatAsState(
            targetValue = if (menuState.expanded) 180f else 0f,
            label = "ArrowIndicatorRotate"
        )
        val alpha by animateFloatAsState(
            targetValue = if (selected) 1f else 0f,
            label = "ArrowIndicatorAlpha"
        )

        val tabTextStyle =
            MaterialTheme.typography.labelLarge

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .height(48.dp)
                .padding(start = 16.dp)
        ) {
            ProvideTextStyle(tabTextStyle) {
                text()
            }
            Icon(
                imageVector = Icons.Rounded.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(rotate)
                    .alpha(alpha)
            )
        }
    }
}