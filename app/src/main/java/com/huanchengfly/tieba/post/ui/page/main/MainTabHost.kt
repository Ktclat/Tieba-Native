package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlin.reflect.KClass
import kotlin.math.roundToInt
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.huanchengfly.tieba.post.arch.LocalMainTabActive
import com.huanchengfly.tieba.post.arch.createHiltViewModelFactory
import com.huanchengfly.tieba.post.ui.widgets.compose.ProvideShouldLoad
import kotlinx.collections.immutable.ImmutableList

@Stable
internal class MainTabSelection(initialId: String) {
    var id by mutableStateOf(initialId)
        private set

    fun activate(item: NavigationItem, onReselected: () -> Unit) {
        select(item.id, onReselected)
        item.onClick?.invoke()
    }

    fun select(tabId: String, onReselected: () -> Unit = {}) {
        if (id == tabId) onReselected() else id = tabId
    }
}

@Composable
internal fun rememberMainTabSelection(initialId: String): MainTabSelection = rememberSaveable(
    saver = Saver(save = { it.id }, restore = { MainTabSelection(it) }),
) { MainTabSelection(initialId) }

/** During back, reveal Home next to the current tab without activating its observers or resetting either page. */
@Composable
internal fun MainTabHost(
    selectedId: String,
    navigationItems: ImmutableList<NavigationItem>,
    modifier: Modifier = Modifier,
    backProgress: () -> Float = { 0f },
) {
    val homeId = navigationItems.firstOrNull()?.id
    val previewHome by remember(homeId, selectedId, backProgress) {
        derivedStateOf { selectedId != homeId && backProgress() != 0f }
    }
    Layout(modifier = modifier.clipToBounds(), content = {
        navigationItems.forEach { item ->
            key(item.id) {
                val active = item.id == selectedId
                var visited by remember { mutableStateOf(false) }
                if (active || visited || (item.id == homeId && previewHome)) {
                    if (active) SideEffect { visited = true }
                    val focusManager = LocalFocusManager.current
                    var hasFocus by remember { mutableStateOf(false) }
                    if (!active && hasFocus) SideEffect {
                        if (hasFocus) focusManager.clearFocus(force = true)
                    }
                    val visibility = if (active) Modifier else Modifier.clearAndSetSemantics { hideFromAccessibility() }
                    Box(
                        Modifier.layoutId(item.id).then(visibility)
                            .onFocusChanged { hasFocus = it.hasFocus }
                            .focusProperties { onEnter = { if (!active) cancelFocusChange() } }
                            .focusGroup(),
                        propagateMinConstraints = true,
                    ) {
                        MainTabLifecycle(active) {
                            CompositionLocalProvider(LocalMainTabActive provides active) {
                                ProvideShouldLoad(active) { item.content() }
                            }
                        }
                    }
                }
            }
        }
    }) { children, constraints ->
        val selected = children.firstOrNull { it.layoutId == selectedId }?.measure(constraints)
        val home = if (previewHome) children.firstOrNull { it.layoutId == homeId }?.measure(constraints) else null
        layout(selected?.width ?: constraints.minWidth, selected?.height ?: constraints.minHeight) {
            if (home == null || selected == null) {
                selected?.place(0, 0)
            } else {
                // Both pages share one physical edge. Gesture progress invalidates placement only.
                val offset = (backProgress().coerceIn(-1f, 1f) * selected.width).roundToInt()
                home.placeWithLayer(if (offset < 0) offset + selected.width else offset - home.width, 0)
                selected.placeWithLayer(offset, 0)
            }
        }
    }
}

/** Pauses lifecycle observers and back callbacks while this retained tab is off screen. */
@Composable
private fun MainTabLifecycle(active: Boolean, content: @Composable () -> Unit) {
    val parent = LocalLifecycleOwner.current
    val child = remember(parent) {
        object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry(this)
        }
    }
    val update by rememberUpdatedState {
        val parentState = parent.lifecycle.currentState
        child.lifecycle.currentState = if (active) parentState else minOf(parentState, Lifecycle.State.CREATED)
    }
    DisposableEffect(parent, child) {
        val observer = LifecycleEventObserver { _, _ -> update() }
        parent.lifecycle.addObserver(observer)
        onDispose {
            parent.lifecycle.removeObserver(observer)
            if (child.lifecycle.currentState.isAtLeast(Lifecycle.State.CREATED)) {
                child.lifecycle.currentState = Lifecycle.State.DESTROYED
            }
        }
    }
    SideEffect { update() }
    CompositionLocalProvider(LocalLifecycleOwner provides child, content = content)
}

/** Keep the tab models across configuration changes and release them when the signed-in account changes. */
@Composable
internal fun MainTabAccountScope(viewModel: MainViewModel, accountId: String?, content: @Composable () -> Unit) {
    val parent = checkNotNull(LocalViewModelStoreOwner.current)
    val factory = createHiltViewModelFactory(parent)
    val owner = remember(viewModel, parent, accountId) {
        val defaults = parent as HasDefaultViewModelProviderFactory
        object : ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
            override val viewModelStore = viewModel.tabStoreFor(accountId)
            override val defaultViewModelCreationExtras = defaults.defaultViewModelCreationExtras
            override val defaultViewModelProviderFactory = object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                    // The parent navigation entry saves handles; give each account its own restored keys.
                    val scopedExtras = MutableCreationExtras(extras)
                    extras[ViewModelProvider.VIEW_MODEL_KEY]?.let { key ->
                        scopedExtras[ViewModelProvider.VIEW_MODEL_KEY] = "main-tab/${accountId ?: "guest"}/$key"
                    }
                    return (factory ?: defaults.defaultViewModelProviderFactory).create(modelClass, scopedExtras)
                }
            }
        }
    }
    key(accountId) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
    }
}
