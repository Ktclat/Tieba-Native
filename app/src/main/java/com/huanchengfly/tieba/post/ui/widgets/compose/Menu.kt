package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.ui.common.theme.compose.ProvideMaterial3Content
import kotlinx.coroutines.flow.filterIsInstance
import kotlin.math.roundToInt

class MenuScope(private val menuState: MenuState, private val onDismiss: (() -> Unit)? = null) {
    fun dismiss() {
        menuState.expanded = false
        onDismiss?.invoke()
    }
}

@Composable
private fun TrackMenuPresses(source: MutableInteractionSource, state: MenuState) {
    LaunchedEffect(source, state) {
        source.interactions.filterIsInstance<PressInteraction.Press>().collect {
            state.offset = it.pressPosition
        }
    }
}

/** M3 popup container shared by toolbar menus and post long-press actions. */
@Composable
private fun TonalMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val popupShape = shape as? CornerBasedShape ?: RoundedCornerShape(20.dp)
    MaterialTheme(shapes = MaterialTheme.shapes.copy(extraSmall = popupShape)) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = modifier) {
            ProvideMaterial3Content { content() }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ClickMenu(
    menuContent: @Composable MenuScope.() -> Unit,
    modifier: Modifier = Modifier,
    menuState: MenuState = rememberMenuState(),
    menuShape: Shape = RoundedCornerShape(20.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    triggerShape: Shape? = null,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val menuScope = MenuScope(menuState, onDismiss)
    TrackMenuPresses(interactionSource, menuState)
    Box {
        val clip = if (triggerShape != null) Modifier.clip(triggerShape) else Modifier
        Box(
            modifier = clip.combinedClickable(
                interactionSource = interactionSource,
                indication = indication,
                onClick = { menuState.show() },
            ),
        ) { content() }
        Box(Modifier.offset { menuState.offset.toMenuOffset() }) {
            TonalMenu(menuState.expanded, { menuScope.dismiss() }, menuShape, modifier) {
                menuScope.menuContent()
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LongClickMenu(
    menuContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    menuState: MenuState = rememberMenuState(),
    onClick: (() -> Unit)? = null,
    shape: Shape = RoundedCornerShape(0.dp),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    indication: Indication? = LocalIndication.current,
    content: @Composable () -> Unit,
) {
    TrackMenuPresses(interactionSource, menuState)
    Box(
        modifier = modifier.clip(shape).combinedClickable(
            interactionSource = interactionSource,
            indication = indication,
            enabled = enabled,
            onLongClick = { menuState.show() },
            onClick = { onClick?.invoke() },
        ),
    ) {
        content()
        Box(Modifier.offset { menuState.offset.toMenuOffset() }) {
            TonalMenu(menuState.expanded, { menuState.dismiss() }, RoundedCornerShape(20.dp)) {
                menuContent()
            }
        }
    }
}

private fun Offset.toMenuOffset(): IntOffset = if (x.isFinite() && y.isFinite()) {
    IntOffset(x.roundToInt(), y.roundToInt())
} else IntOffset.Zero

@Composable
fun rememberMenuState(): MenuState = rememberSaveable(saver = MenuState.Saver) { MenuState() }

@Stable
class MenuState internal constructor() {
    var expanded by mutableStateOf(false)
    var offset by mutableStateOf(Offset.Zero)
    fun toggle() { expanded = !expanded }
    fun show() { expanded = true }
    fun dismiss() { expanded = false }
    companion object {
        val Saver: Saver<MenuState, *> = listSaver(
            save = { listOf<Any>(it.expanded, it.offset.x, it.offset.y) },
            restore = {
                MenuState().apply {
                    expanded = it[0] as Boolean
                    offset = Offset(it[1] as Float, it[2] as Float)
                }
            },
        )
    }
}
