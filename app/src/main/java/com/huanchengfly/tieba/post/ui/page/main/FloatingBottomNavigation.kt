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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedColors
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

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
    val accent = themeColors.primary
    val indicatorShape = RoundedCornerShape(22.dp)

    // Both edges are fractional tab indices, so changing density does not
    // invalidate their positions. Slower trailing edge gives a jelly response.
    val leadingEdge = remember { Animatable(selectedIndex.toFloat()) }
    val trailingEdge = remember { Animatable(selectedIndex + 1f) }
    var draggedPx by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(selectedIndex, navigationItems.size) {
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
        modifier = modifier.widthIn(max = 380.dp).fillMaxWidth().height(64.dp),
        color = themeColors.bottomBar,
        shape = RoundedCornerShape(32.dp),
        border = BorderStroke(0.75.dp, themeColors.textSecondary.copy(alpha = 0.16f)),
        elevation = 3.dp,
    ) {
        BoxWithConstraints(Modifier.padding(5.dp).clip(RoundedCornerShape(26.dp))) {
            val tabWidth = maxWidth / navigationItems.size
            val widthPx = with(LocalDensity.current) { tabWidth.toPx() }
            val inset = with(LocalDensity.current) { 2.dp.toPx() }

            val start = min(leadingEdge.value, trailingEdge.value) * widthPx + draggedPx + inset
            val end = max(leadingEdge.value, trailingEdge.value) * widthPx + draggedPx - inset
            val pillWidthDp = with(LocalDensity.current) {
                (end - start).coerceAtLeast(widthPx * 0.45f).toDp()
            }
            Box(
                Modifier
                    .offset { IntOffset(start.roundToInt(), 0) }
                    .width(pillWidthDp)
                    .fillMaxHeight()
                    .clip(indicatorShape)
                    .background(fill)
                    .border(1.dp, stroke, indicatorShape),
            )

            Row(Modifier.fillMaxSize()) {
                navigationItems.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val iconScale by animateFloatAsState(
                        targetValue = if (selected) 1.08f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                        label = "navigation-icon-scale",
                    )
                    val dragGesture = if (selected) {
                        Modifier.pointerInput(selectedIndex, widthPx, navigationItems.size) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    draggedPx = 0f
                                    dragging = true
                                },
                                onDragCancel = {
                                    dragging = false
                                    draggedPx = 0f
                                },
                                onDragEnd = {
                                    val target = resolveNavigationDragTarget(
                                        selectedIndex, draggedPx, widthPx, navigationItems.size
                                    )
                                    dragging = false
                                    draggedPx = 0f
                                    if (target != selectedIndex) {
                                        onChangePosition(target)
                                        navigationItems[target].onClick?.invoke()
                                    }
                                },
                                onHorizontalDrag = { change, delta ->
                                    change.consume()
                                    draggedPx = (draggedPx + delta).coerceIn(
                                        -selectedIndex * widthPx,
                                        (navigationItems.lastIndex - selectedIndex) * widthPx
                                    )
                                }
                            )
                        }
                    } else Modifier

                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .then(dragGesture)
                            .clickable {
                                if (selected) onReselected(index)
                                else onChangePosition(index)
                                item.onClick?.invoke()
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
                                    contentDescription = item.title(selected),
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
