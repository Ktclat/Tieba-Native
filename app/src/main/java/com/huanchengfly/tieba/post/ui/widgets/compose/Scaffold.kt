package com.huanchengfly.tieba.post.ui.widgets.compose

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.DismissValue
import androidx.compose.material.DrawerDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FabPosition
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material3.Scaffold as Material3Scaffold
import androidx.compose.material3.FabPosition as Material3FabPosition
import androidx.compose.material3.MaterialTheme as Material3Theme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.ScaffoldState
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.contentColorFor
import androidx.compose.material.rememberDismissState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun SwipeToDismissSnackbarHost(hostState: SnackbarHostState) {
    val dismissState = rememberDismissState(
        confirmStateChange = { value ->
            if (value != DismissValue.Default) {
                hostState.currentSnackbarData?.dismiss()
                true
            } else {
                false
            }
        }
    )
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue != DismissValue.Default) {
            dismissState.reset()
        }
    }
    SwipeToDismiss(state = dismissState, background = {}) {
        SnackbarHost(hostState = hostState)
    }
}

val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> { error("no scaffold here!") }

@Composable
fun MyScaffold(
    modifier: Modifier = Modifier,
    scaffoldState: ScaffoldState = rememberScaffoldState(),
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable (SnackbarHostState) -> Unit = { SwipeToDismissSnackbarHost(it) },
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    isFloatingActionButtonDocked: Boolean = false,
    drawerContent: @Composable (ColumnScope.() -> Unit)? = null,
    drawerGesturesEnabled: Boolean = true,
    drawerShape: Shape = MaterialTheme.shapes.large,
    drawerElevation: Dp = DrawerDefaults.Elevation,
    drawerBackgroundColor: Color = MaterialTheme.colors.surface,
    drawerContentColor: Color = contentColorFor(drawerBackgroundColor),
    drawerScrimColor: Color = DrawerDefaults.scrimColor,
    backgroundColor: Color = MaterialTheme.colors.background,
    contentColor: Color = contentColorFor(backgroundColor),
    content: @Composable (PaddingValues) -> Unit
) {
    CompositionLocalProvider(LocalSnackbarHostState provides scaffoldState.snackbarHostState) {
        if (drawerContent != null) {
            // Preserve the legacy drawer contract for any remaining View screens.
            Scaffold(
                modifier,
                scaffoldState,
                topBar,
                bottomBar,
                snackbarHost,
                floatingActionButton,
                floatingActionButtonPosition,
                isFloatingActionButtonDocked,
                drawerContent,
                drawerGesturesEnabled,
                drawerShape,
                drawerElevation,
                drawerBackgroundColor,
                drawerContentColor,
                drawerScrimColor,
                backgroundColor,
                contentColor,
                content,
            )
        } else {
            Material3Scaffold(
                modifier = modifier,
                topBar = topBar,
                bottomBar = bottomBar,
                snackbarHost = { snackbarHost(scaffoldState.snackbarHostState) },
                floatingActionButton = floatingActionButton,
                floatingActionButtonPosition = if (floatingActionButtonPosition == FabPosition.Center)
                    Material3FabPosition.Center else Material3FabPosition.End,
                containerColor = if (backgroundColor == Color.Transparent)
                    Material3Theme.colorScheme.background else backgroundColor,
                contentColor = if (contentColor == Color.Unspecified)
                    Material3Theme.colorScheme.onBackground else contentColor,
                // Existing top/bottom bars already handle system insets themselves.
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                content = content,
            )
        }
    }
}