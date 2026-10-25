package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
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
import kotlin.math.roundToInt

/**
 * Floating, gesture-aware bottom navigation.
 *
 * The outlined selection becomes a filled surface while dragged ("line to plane").
 * A horizontal drag beginning on the selected segment may select another tab.
 * The parent Scaffold reserves the bar's measured height so it cannot overlap content.
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

    val active = currentPosition.coerceIn(0, navigationItems.lastIndex)
    val accent = MaterialTheme.colors.secondary
    val capsule = RoundedCornerShape(24.dp)
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val indicatorFill by animateColorAsState(
        targetValue = accent.copy(alpha = if (isDragging) 0.28f else 0.12f),
        label = "navigation-indicator-fill",
    )
    LaunchedEffect(active, navigationItems.size) {
        if (!isDragging) dragDistance = 0f
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp),
        color = themeColors.bottomBar,
        shape = RoundedCornerShape(32.dp),
        elevation = 8.dp,
        border = BorderStroke(1.dp, themeColors.divider.copy(alpha = 0.65f)),
    ) {
        BoxWithConstraints(Modifier.padding(5.dp)) {
            val itemWidth = maxWidth / navigationItems.size
            val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
            val indicatorBase by animateFloatAsState(
                targetValue = active * itemWidthPx,
                animationSpec = tween(durationMillis = 220),
                label = "navigation-selected-position",
            )

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset((indicatorBase + dragDistance).roundToInt(), 0)
                    }
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(capsule)
                    .background(indicatorFill)
                    .border(1.5.dp, accent, capsule),
            )

            Row(Modifier.fillMaxSize()) {
                navigationItems.forEachIndexed { index, item ->
                    val isSelected = index == active
                    val dragModifier = if (isSelected) {
                        Modifier.pointerInput(active, itemWidthPx, navigationItems.size) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragDistance = 0f
                                    isDragging = true
                                },
                                onDragEnd = {
                                    val destination = resolveNavigationDragTarget(
                                        active,
                                        dragDistance,
                                        itemWidthPx,
                                        navigationItems.size,
                                    )
                                    isDragging = false
                                    dragDistance = 0f
                                    if (destination != active) {
                                        onChangePosition(destination)
                                        navigationItems[destination].onClick?.invoke()
                                    }
                                },
                                onDragCancel = {
                                    isDragging = false
                                    dragDistance = 0f
                                },
                                onHorizontalDrag = { change, delta ->
                                    change.consume()
                                    dragDistance = (dragDistance + delta).coerceIn(
                                        -active * itemWidthPx,
                                        (navigationItems.lastIndex - active) * itemWidthPx,
                                    )
                                },
                            )
                        }
                    } else Modifier

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .then(dragModifier)
                            .clickable {
                                if (isSelected) onReselected(index)
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
                                        atEnd = isSelected,
                                    ),
                                    contentDescription = item.title(isSelected),
                                    tint = if (isSelected) accent else themeColors.unselected,
                                    modifier = Modifier.size(23.dp),
                                )
                                if (item.badge) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 6.dp, y = (-4).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colors.error),
                                    )
                                }
                            }
                            Text(
                                text = item.title(isSelected),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) accent else themeColors.unselected,
                            )
                        }
                    }
                }
            }
        }
    }
}
