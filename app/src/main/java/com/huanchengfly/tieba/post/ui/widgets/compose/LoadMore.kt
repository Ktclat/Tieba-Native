package com.huanchengfly.tieba.post.ui.widgets.compose

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.huanchengfly.tieba.post.R

private val LoadThreshold = 70.dp
private const val RepeatPullIntervalMs = 500L

@Composable
fun LoadMoreLayout(
    isLoading: Boolean,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
    enableLoadMore: Boolean = true,
    loadEnd: Boolean = false,
    indicator: @Composable (Boolean, Boolean, Boolean) -> Unit = { loading, end, willLoad ->
        DefaultIndicator(isLoading = loading, loadEnd = end, willLoad = willLoad)
    },
    lazyListState: LazyListState? = null,
    isEmpty: Boolean = lazyListState?.layoutInfo?.totalItemsCount == 0,
    preloadCount: Int = 1,
    content: @Composable () -> Unit,
) {
    val enabled = LocalShouldLoad.current && enableLoadMore
    val threshold = with(LocalDensity.current) { LoadThreshold.toPx() }
    val endInView by remember(lazyListState, preloadCount, isEmpty) {
        derivedStateOf {
            val layout = lazyListState?.layoutInfo
            val last = layout?.visibleItemsInfo?.lastOrNull()?.index
            if (!isEmpty && preloadCount > 0 && last != null && last >= layout.totalItemsCount - preloadCount) {
                layout.totalItemsCount
            } else -1
        }
    }
    var lastRequestedEnd by remember(lazyListState) { mutableIntStateOf(-1) }
    var lastRequestTime by remember(lazyListState) { mutableLongStateOf(-RepeatPullIntervalMs) }
    val requestPage by rememberUpdatedState {
        lastRequestedEnd = endInView
        lastRequestTime = SystemClock.elapsedRealtime()
        onLoadMore()
    }

    // A failed request can be retried by pulling or revisiting the end. Recomposition alone
    // must not repeatedly request the same page while the list is sitting at the bottom.
    LaunchedEffect(endInView, enabled, isLoading, loadEnd) {
        if (endInView == -1) {
            lastRequestedEnd = -1
        } else if (enabled && !loadEnd && !isLoading && endInView != lastRequestedEnd) {
            requestPage()
        }
    }

    val onRelease by rememberUpdatedState {
        if (enabled && !isLoading && !loadEnd && SystemClock.elapsedRealtime() - lastRequestTime >= RepeatPullIntervalMs) {
            requestPage()
        }
    }
    val canPull = enabled && !isLoading
    val gesture = remember(threshold, canPull) { LoadMoreGesture(threshold) { onRelease() } }
    Box(
        modifier = modifier.fillMaxSize().then(if (canPull) Modifier.nestedScroll(gesture) else Modifier)
    ) {
        content()
        AnimatedVisibility(
            visible = enabled && (isLoading || gesture.pulled > 0f),
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
        ) {
            Box(Modifier.padding(8.dp)) { indicator(isLoading, loadEnd, gesture.pulled >= threshold) }
        }
    }
}

/** Consumes only upward overscroll left by a child, and lets a downward drag retract the footer. */
private class LoadMoreGesture(
    private val threshold: Float,
    private val onLoad: () -> Unit,
) : NestedScrollConnection {
    var pulled by mutableFloatStateOf(0f)
        private set

    private fun consume(delta: Float): Offset {
        val newPull = (pulled - delta).coerceIn(0f, threshold * 2f)
        val consumed = pulled - newPull
        pulled = newPull
        return Offset(0f, consumed)
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        return if (source == NestedScrollSource.UserInput && available.y > 0 && pulled > 0f) {
            consume(available.y)
        } else Offset.Zero
    }

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        return if (source == NestedScrollSource.UserInput && available.y < 0) {
            consume(available.y)
        } else Offset.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (pulled == 0f) return Velocity.Zero
        if (pulled >= threshold) onLoad()
        pulled = 0f
        return Velocity(0f, available.y.coerceAtMost(0f))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DefaultIndicator(
    isLoading: Boolean,
    loadEnd: Boolean,
    willLoad: Boolean,
    loadingText: String = stringResource(id = R.string.text_loading),
    loadEndText: String = stringResource(id = R.string.no_more),
    pullToLoadText: String = stringResource(id = R.string.pull_to_load),
    releaseToLoadText: String = stringResource(id = R.string.release_to_load),
) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (isLoading) LoadingIndicator(modifier = Modifier.size(24.dp))
            Text(
                text = when {
                    isLoading -> loadingText
                    loadEnd -> loadEndText
                    willLoad -> releaseToLoadText
                    else -> pullToLoadText
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}
