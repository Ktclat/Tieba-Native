package com.huanchengfly.tieba.post.ui.page.main

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.invisibleToUser
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStoreOwner
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

    fun select(tabId: String, onReselected: () -> Unit = {}) {
        if (id == tabId) onReselected() else id = tabId
    }
}

@Composable
internal fun rememberMainTabSelection(initialId: String): MainTabSelection = rememberSaveable(
    saver = Saver(save = { it.id }, restore = { MainTabSelection(it) }),
) { MainTabSelection(initialId) }

/** Unvisited tabs have no composition. Only the selected child is measured, placed and drawn. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun MainTabHost(
    selectedId: String,
    navigationItems: ImmutableList<NavigationItem>,
    modifier: Modifier = Modifier,
) {
    Layout(modifier = modifier, content = {
        navigationItems.forEach { item ->
            key(item.id) {
                val active = item.id == selectedId
                var visited by remember { mutableStateOf(false) }
                if (active || visited) {
                    if (active) SideEffect { visited = true }
                    val visibility = if (active) Modifier else Modifier.clearAndSetSemantics { invisibleToUser() }
                    Box(Modifier.layoutId(item.id).then(visibility), propagateMinConstraints = true) {
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
        layout(selected?.width ?: constraints.minWidth, selected?.height ?: constraints.minHeight) {
            selected?.placeRelative(0, 0)
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
            override val defaultViewModelProviderFactory = factory ?: defaults.defaultViewModelProviderFactory
            override val defaultViewModelCreationExtras = defaults.defaultViewModelCreationExtras
        }
    }
    key(accountId) {
        CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
    }
}
