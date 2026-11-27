package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlin.math.roundToInt
import kotlinx.collections.immutable.ImmutableList

internal val FloatingNavigationBarKey = booleanPreferencesKey("floatingNavigationBar")

@Stable
internal class MainNavigationScrollState(private val scrollRangePx: Float) : NestedScrollConnection {
    var hiddenFraction by mutableFloatStateOf(0f)
        private set
    private var moving by mutableStateOf(false)
    private var pointerDown = false
    private var flinging = false
    // Scaffold measures the bar before its body. This is the bar alone, without system insets.
    var measuredBarHeightPx = 0
    val visibleBarHeightPx: Int get() = (measuredBarHeightPx * (1f - hiddenFraction)).roundToInt()
    val bufferedHeightPx: Int get() =
        if (moving && hiddenFraction > 0f && hiddenFraction < 1f) visibleBarHeightPx else 0

    fun pointerPressed(pressed: Boolean) {
        pointerDown = pressed
        if (!pressed && !flinging) moving = false
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        val next = updateNavigationHideFraction(hiddenFraction, consumed.y, scrollRangePx)
        if (next != hiddenFraction) {
            moving = (pointerDown || flinging) && available.y == 0f
            hiddenFraction = next
        } else if (available.y != 0f) {
            // A list at its edge must be able to reach its final row even before the finger lifts.
            moving = false
        }
        return Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        flinging = true
        return Velocity.Zero
    }

    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
        flinging = false
        if (!pointerDown) moving = false
        return Velocity.Zero
    }
}

/** Observe physical contact without consuming taps, drags, or flings from the active page. */
internal fun Modifier.observeNavigationTouch(state: MainNavigationScrollState) = pointerInput(state) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        state.pointerPressed(true)
        try {
            do {
                val pressed = awaitPointerEvent(PointerEventPass.Initial).changes.any { it.pressed }
            } while (pressed)
        } finally {
            state.pointerPressed(false)
        }
    }
}

/** During a drag, measure the page once up to the system inset and clip it at the moving bar. */
internal fun Modifier.mainTabViewport(padding: PaddingValues, state: MainNavigationScrollState): Modifier =
    padding(padding).clipToBounds().layout { measurable, constraints ->
        val extra = if (constraints.hasBoundedHeight) state.bufferedHeightPx else 0
        val page = measurable.measure(constraints.copy(
            minHeight = constraints.minHeight + extra,
            maxHeight = constraints.maxHeight + extra,
        ))
        layout(page.width, constraints.constrainHeight(page.height)) { page.placeRelative(0, 0) }
    }

@Composable
internal fun MainNavigationBarSlot(
    state: MainNavigationScrollState,
    navigationInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable () -> Unit,
) {
    val show by remember(state) { derivedStateOf { state.hiddenFraction < 1f } }
    Layout(
        modifier = Modifier.fillMaxWidth()
            .windowInsetsPadding(navigationInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .clipToBounds(),
        content = { if (show) Box { content() } },
    ) { children, constraints ->
        val bar = children.firstOrNull()?.measure(constraints.copy(minHeight = 0))
        if (bar != null) state.measuredBarHeightPx = bar.height
        layout(constraints.maxWidth, state.visibleBarHeightPx) { bar?.placeRelative(0, 0) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MainNavigationBar(
    currentPosition: Int,
    onSelect: (Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    modifier: Modifier = Modifier,
    floating: Boolean = false,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val select by rememberUpdatedState(onSelect)
    val destinations: @Composable () -> Unit = {
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
    val noInsets = WindowInsets(0, 0, 0, 0) // MainNavigationBarSlot owns the system insets.
    if (floating) {
        val margin = FloatingToolbarDefaults.ScreenOffset
        Box(
            Modifier.fillMaxWidth().padding(start = margin, end = margin, top = 8.dp, bottom = margin),
            contentAlignment = Alignment.Center,
        ) {
            HorizontalFloatingToolbar(
                expanded = true,
                modifier = modifier.width((80 * navigationItems.size).dp),
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                ShortNavigationBar(
                    modifier = Modifier.wrapContentWidth(),
                    containerColor = Color.Transparent,
                    windowInsets = noInsets,
                    content = destinations,
                )
            }
        }
    } else {
        ShortNavigationBar(
            modifier = modifier.wrapContentWidth(),
            windowInsets = noInsets,
            content = destinations,
        )
    }
}
