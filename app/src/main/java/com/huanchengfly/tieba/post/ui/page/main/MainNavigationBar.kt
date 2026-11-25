package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

/** Measure the whole bar at its natural height even when only part of it is visible. */
@Composable
internal fun MainNavigationBarSlot(
    visibleFraction: Float,
    navigationInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable () -> Unit,
) {
    val fraction = visibleFraction.coerceIn(0f, 1f)
    Layout(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(navigationInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .clipToBounds(),
        content = { if (fraction > 0f) Box { content() } },
    ) { children, constraints ->
        val bar = children.firstOrNull()?.measure(constraints.copy(minHeight = 0))
        val height = ((bar?.height ?: 0) * fraction).roundToInt()
        layout(constraints.maxWidth, height) { bar?.placeRelative(0, 0) }
    }
}

@Composable
internal fun MainNavigationBar(
    currentPosition: Int,
    onSelect: (Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    modifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val select by rememberUpdatedState(onSelect)
    ShortNavigationBar(modifier = modifier.fillMaxWidth(), windowInsets = WindowInsets(0, 0, 0, 0)) {
        navigationItems.forEachIndexed { index, item ->
            key(item.id) {
                val selected = index == currentPosition
                val drag = if (selected) {
                    Modifier.pointerInput(index, navigationItems.size, rtl) {
                        var distance = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { distance = 0f },
                            onHorizontalDrag = { change, delta ->
                                change.consume()
                                distance += if (rtl) -delta else delta
                            },
                            onDragEnd = {
                                val target = resolveNavigationDragTarget(index, distance, size.width.toFloat(), navigationItems.size)
                                if (target != index) select(target)
                            },
                        )
                    }
                } else Modifier
                ShortNavigationBarItem(
                    selected = selected,
                    onClick = { select(index) },
                    modifier = drag.destinationBadgeDescription(item),
                    label = { Text(item.title(selected), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    icon = { DestinationIcon(item, selected) },
                )
            }
        }
    }
}
