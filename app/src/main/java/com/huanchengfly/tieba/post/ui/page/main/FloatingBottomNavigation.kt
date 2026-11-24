package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

private val CapsuleHeight = 64.dp
private val CapsuleVerticalMargin = 5.dp

@Composable
internal fun FloatingNavigationBarSlot(
    visibleFraction: Float,
    navigationInsets: WindowInsets = WindowInsets.navigationBars,
    content: @Composable () -> Unit,
) {
    val fraction = visibleFraction.coerceIn(0f, 1f)
    val fullHeight = CapsuleHeight + CapsuleVerticalMargin * 2
    val visibleHeight = fullHeight * fraction
    Box(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(navigationInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .height(visibleHeight)
            .clipToBounds(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (with(LocalDensity.current) { visibleHeight.roundToPx() > CapsuleVerticalMargin.roundToPx() }) {
            // Scaffold moves the shrinking viewport down. Keep the capsule at its
            // full height and clip the part that has crossed the bottom safe area.
            Box(
                Modifier.fillMaxWidth()
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .height(fullHeight)
                    .padding(horizontal = 20.dp, vertical = CapsuleVerticalMargin),
                contentAlignment = Alignment.Center,
            ) {
                content()
            }
        }
    }
}

/**
 * Compact, theme-aware floating navigation. The selection is one elastic pill:
 * its two ends chase the destination at different speeds, stretching like jelly
 * before springing back. Dragging the active item directly moves the outline.
 */
@OptIn(ExperimentalAnimationGraphicsApi::class)
@Composable
fun FloatingBottomNavigation(
    currentPosition: Int,
    onChangePosition: (Int) -> Unit,
    onReselected: (Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    modifier: Modifier = Modifier,
    themeColors: ExtendedColors = ExtendedTheme.colors,
) {
    if (navigationItems.isEmpty()) return
    val selectedIndex = currentPosition.coerceIn(0, navigationItems.lastIndex)
    val accent = themeColors.accent
    val indicatorShape = RoundedCornerShape(22.dp)

    // Both edges are fractional tab indices, so changing density does not
    // invalidate their positions. Slower trailing edge gives a jelly response.
    val leadingEdge = remember { Animatable(selectedIndex.toFloat()) }
    val trailingEdge = remember { Animatable(selectedIndex + 1f) }
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    var dragging by remember { mutableStateOf(false) }
    val selectTab by rememberUpdatedState(newValue = { index: Int ->
        if (index == selectedIndex) onReselected(index) else onChangePosition(index)
        navigationItems[index].onClick?.invoke()
    })
    val releasedPosition = if (dragging) null else dragPosition
    LaunchedEffect(selectedIndex, navigationItems.size, dragging, releasedPosition) {
        if (dragging) return@LaunchedEffect
        if (releasedPosition != null) {
            // Continue the selection spring from the finger's release point.
            leadingEdge.snapTo(releasedPosition)
            trailingEdge.snapTo(releasedPosition + 1f)
            dragPosition = null
            return@LaunchedEffect
        }
        val forward = selectedIndex > leadingEdge.value
        coroutineScope {
            launch {
                leadingEdge.animateTo(
                    selectedIndex.toFloat(),
                    spring(dampingRatio = 0.62f, stiffness = if (forward) 180f else 335f),
                )
            }
            launch {
                trailingEdge.animateTo(
                    selectedIndex + 1f,
                    spring(dampingRatio = 0.56f, stiffness = if (forward) 335f else 180f),
                )
            }
        }
    }
    val fill by animateColorAsState(
        targetValue = accent.copy(alpha = if (dragging) 0.19f else 0.11f),
        label = "navigation-jelly-fill",
    )
    val stroke by animateColorAsState(
        targetValue = accent.copy(alpha = if (dragging) 0.65f else 0.35f),
        label = "navigation-jelly-outline",
    )
    Surface(
        modifier = modifier.widthIn(max = 380.dp).fillMaxWidth().height(CapsuleHeight),
        color = themeColors.bottomBar,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(0.75.dp, themeColors.textSecondary.copy(alpha = 0.16f)),
        elevation = 0.dp,
    ) {
        BoxWithConstraints(Modifier.padding(5.dp).clip(RoundedCornerShape(26.dp))) {
            val tabWidth = maxWidth / navigationItems.size
            val widthPx = with(LocalDensity.current) { tabWidth.toPx() }
            val inset = with(LocalDensity.current) { 2.dp.toPx() }
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            val direction = if (rtl) -1f else 1f

            // Read animated edges in the graphics layer rather than composition.
            // This avoids recomposing the complete navigation row on every frame.
            Box(
                Modifier
                    .width((tabWidth - 4.dp).coerceAtLeast(0.dp))
                    .fillMaxHeight()
                    .graphicsLayer {
                        val leading = dragPosition ?: min(leadingEdge.value, trailingEdge.value)
                        val trailing = dragPosition?.plus(1f) ?: max(leadingEdge.value, trailingEdge.value)
                        val targetWidth = ((trailing - leading) * widthPx - 2f * inset)
                            .coerceAtLeast(widthPx * 0.45f)
                        translationX = direction * (leading * widthPx + inset)
                        scaleX = if (size.width > 0f) targetWidth / size.width else 1f
                        transformOrigin = TransformOrigin(if (rtl) 1f else 0f, 0.5f)
                    }
                    .clip(indicatorShape)
                    .background(fill)
                    .border(1.dp, stroke, indicatorShape),
            )

            Row(Modifier.fillMaxSize().selectableGroup()) {
                navigationItems.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val interaction = remember(item.id) { MutableInteractionSource() }
                    val isPressed by interaction.collectIsPressedAsState()
                    val pressedColor by animateColorAsState(
                        targetValue = if (isPressed) accent.copy(alpha = 0.09f)
                        else Color.Transparent,
                        label = "nav-pressed",
                    )
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "navigation-icon-scale",
                    )
                    val dragGesture = if (selected && widthPx > 0f) {
                        Modifier.pointerInput(selectedIndex, widthPx, navigationItems.size, rtl) {
                            var logicalDragPx = 0f
                            try {
                                detectHorizontalDragGestures(
                                    onDragStart = {
                                        logicalDragPx = 0f
                                        dragPosition = selectedIndex.toFloat()
                                        dragging = true
                                    },
                                    onDragCancel = { dragging = false },
                                    onDragEnd = {
                                        val target = resolveNavigationDragTarget(
                                            selectedIndex, logicalDragPx, widthPx, navigationItems.size
                                        )
                                        dragging = false
                                        if (target != selectedIndex) selectTab(target)
                                    },
                                    onHorizontalDrag = { change, delta ->
                                        change.consume()
                                        logicalDragPx = (logicalDragPx + direction * delta).coerceIn(
                                            -selectedIndex * widthPx,
                                            (navigationItems.lastIndex - selectedIndex) * widthPx,
                                        )
                                        dragPosition = selectedIndex + logicalDragPx / widthPx
                                    }
                                )
                            } finally {
                                dragging = false
                            }
                        }
                    } else Modifier

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            // The previous clickable emitted a rectangular indication.
                            // Both the hit surface and pressed overlay are now rounded.
                            .padding(horizontal = 1.dp, vertical = 1.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(pressedColor)
                            .then(dragGesture)
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                interactionSource = interaction,
                                indication = null,
                            ) {
                                selectTab(index)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Icon(
                                    painter = rememberAnimatedVectorPainter(
                                        animatedImageVector = item.icon(),
                                        atEnd = selected
                                    ),
                                    tint = if (selected) accent else themeColors.textSecondary,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                        .graphicsLayer {
                                            scaleX = iconScale
                                            scaleY = iconScale
                                        },
                                )
                                if (item.badge) {
                                    Box(
                                        Modifier.align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-4).dp)
                                            .size(8.dp).clip(CircleShape)
                                            .background(accent)
                                    )
                                }
                            }
                            Text(
                                text = item.title(selected),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (selected) themeColors.text else themeColors.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}
