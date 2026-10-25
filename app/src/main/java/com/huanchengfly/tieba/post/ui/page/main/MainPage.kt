package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collect
import com.huanchengfly.tieba.post.LocalDevicePosture
import com.huanchengfly.tieba.post.LocalNotificationCountFlow
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.rememberPreferenceAsState
import com.huanchengfly.tieba.post.ui.common.theme.compose.ExtendedTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowHeightSizeClass
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowWidthSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.main.explore.ExplorePage
import com.huanchengfly.tieba.post.ui.page.main.home.HomePage
import com.huanchengfly.tieba.post.ui.page.main.notifications.NotificationsPage
import com.huanchengfly.tieba.post.ui.page.main.user.UserPage
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.huanchengfly.tieba.post.ui.utils.MainNavigationContentPosition
import com.huanchengfly.tieba.post.ui.utils.MainNavigationType
import com.huanchengfly.tieba.post.ui.widgets.compose.LazyLoadHorizontalPager
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.appPreferences
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch

@Composable
private fun NavigationWrapper(
    currentPosition: Int,
    onChangePosition: (position: Int) -> Unit,
    onReselected: (position: Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationType: MainNavigationType,
    navigationContentPosition: MainNavigationContentPosition,
    content: @Composable () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(visible = navigationType == MainNavigationType.PERMANENT_NAVIGATION_DRAWER) {
            NavigationDrawerContent(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        AnimatedVisibility(visible = navigationType == MainNavigationType.NAVIGATION_RAIL) {
            NavigationRail(
                currentPosition = currentPosition,
                onChangePosition = onChangePosition,
                onReselected = onReselected,
                navigationItems = navigationItems,
                navigationContentPosition = navigationContentPosition
            )
        }
        Column(modifier = Modifier
            .weight(1f)
            .fillMaxHeight()) {
            content()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationGraphicsApi::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun MainPage(
    navigator: DestinationsNavigator,
    viewModel: MainViewModel = pageViewModel<MainUiIntent, MainViewModel>(emptyList()),
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val windowHeightSizeClass by rememberUpdatedState(newValue = windowSizeClass.heightSizeClass)
    val windowWidthSizeClass by rememberUpdatedState(newValue = windowSizeClass.widthSizeClass)
    val foldingDevicePosture by LocalDevicePosture.current

    val messageCount by viewModel.uiState.collectPartialAsState(
        prop1 = MainUiState::messageCount,
        initial = 0
    )

    val notificationCountFlow = LocalNotificationCountFlow.current
    LaunchedEffect(null) {
        notificationCountFlow.collect {
            viewModel.send(MainUiIntent.NewMessage.Receive(it))
        }
    }

    val hideExplore by rememberPreferenceAsState(
        key = booleanPreferencesKey("hideExplore"),
        defaultValue = LocalContext.current.appPreferences.hideExplore
    )
    val pageCount by remember {
        derivedStateOf {
            if (hideExplore) 3 else 4
        }
    }
    val pagerState = rememberPagerState { pageCount }
    LaunchedEffect(hideExplore) {
        if (pagerState.currentPage == 3 && hideExplore) {
            pagerState.scrollToPage(2)
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val themeColors = ExtendedTheme.colors
    val navigationItems by remember {
        derivedStateOf {
            listOfNotNull(
                NavigationItem(
                    id = "home",
                    icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_inventory_2) },
                    title = { stringResource(id = R.string.title_main) },
                    content = {
                        HomePage(
                            canOpenExplore = !LocalContext.current.appPreferences.hideExplore
                        ) {
                            coroutineScope.launch {
                                pagerState.scrollToPage(1)
                            }
                        }
                    }
                ),
                if (hideExplore) null
                else NavigationItem(
                    id = "explore",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_toy_fans)
                    },
                    title = { stringResource(id = R.string.title_explore) },
                    content = {
                        ExplorePage()
                    }
                ),
                NavigationItem(
                    id = "notification",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_notifications)
                    },
                    title = { stringResource(id = R.string.title_notifications) },
                    badge = messageCount > 0,
                    badgeText = "$messageCount",
                    onClick = {
                        viewModel.send(MainUiIntent.NewMessage.Clear)
                    },
                    content = {
                        NotificationsPage()
                    }
                ),
                NavigationItem(
                    id = "user",
                    icon = {
                        AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_person)
                    },
                    title = { stringResource(id = R.string.title_user) },
                    content = {
                        UserPage()
                    }
                ),
            ).toImmutableList()
        }
    }
    val navigationType by remember {
        derivedStateOf {
            when (windowWidthSizeClass) {
                WindowWidthSizeClass.Compact -> {
                    MainNavigationType.BOTTOM_NAVIGATION
                }

                WindowWidthSizeClass.Medium -> {
                    MainNavigationType.NAVIGATION_RAIL
                }

                WindowWidthSizeClass.Expanded -> {
                    if (foldingDevicePosture is DevicePosture.BookPosture) {
                        MainNavigationType.NAVIGATION_RAIL
                    } else {
                        MainNavigationType.PERMANENT_NAVIGATION_DRAWER
                    }
                }

                else -> {
                    MainNavigationType.BOTTOM_NAVIGATION
                }
            }
        }
    }

    /**
     * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
     * ergonomics and reachability depending upon the height of the device.
     */
    val navigationContentPosition by remember {
        derivedStateOf {
            when (windowHeightSizeClass) {
                WindowHeightSizeClass.Compact -> {
                    MainNavigationContentPosition.TOP
                }

                WindowHeightSizeClass.Medium,
                WindowHeightSizeClass.Expanded -> {
                    MainNavigationContentPosition.CENTER
                }

                else -> {
                    MainNavigationContentPosition.TOP
                }
            }
        }
    }
    // Observes child scroll, consuming none of its distance.
    // The Scaffold bottom bar owns layout height, never overlays page content.
    val navHideFraction = remember { mutableFloatStateOf(0f) }
    val navScrollRangePx = with(LocalDensity.current) { 132.dp.toPx() }
    val navScrollObserver = remember(navigationType, navScrollRangePx) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (navigationType == MainNavigationType.BOTTOM_NAVIGATION &&
                    (source == NestedScrollSource.Drag || source == NestedScrollSource.Fling)
                ) {
                    navHideFraction.floatValue = updateNavigationHideFraction(
                        current = navHideFraction.floatValue,
                        consumedDyPx = consumed.y,
                        distancePx = navScrollRangePx,
                    )
                }
                return Offset.Zero
            }
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        navHideFraction.floatValue = 0f
    }

    // A very small page-scale overshoot accompanies the jelly indicator.
    // graphicsLayer changes only presentation; list measurements stay stable.
    val pageScale = remember { Animatable(1f) }
    val predictiveProgress = remember { Animatable(0f) }
    val predictTabBack by rememberPreferenceAsState(
        key = booleanPreferencesKey("enablePredictiveTabBack"),
        defaultValue = true,
    )
    var lastAnimatedTab by remember { mutableIntStateOf(pagerState.currentPage) }
    LaunchedEffect(pagerState.currentPage) {
        if (lastAnimatedTab != pagerState.currentPage) {
            lastAnimatedTab = pagerState.currentPage
            pageScale.snapTo(0.974f)
            pageScale.animateTo(
                1f,
                spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessMedium)
            )
        }
    }

    val onReselected: (Int) -> Unit = {
        coroutineScope.emitGlobalEvent(
            GlobalEvent.Refresh(navigationItems[it].id)
        )
    }
    ProvideNavigator(navigator = navigator) {
        NavigationWrapper(
            currentPosition = pagerState.currentPage,
            onChangePosition = { coroutineScope.launch { pagerState.scrollToPage(it) } },
            onReselected = onReselected,
            navigationItems = navigationItems,
            navigationType = navigationType,
            navigationContentPosition = navigationContentPosition
        ) {
            MyScaffold(
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize().nestedScroll(navScrollObserver),
                bottomBar = {
                    if (navigationType == MainNavigationType.BOTTOM_NAVIGATION) {
                        val visibleFraction = 1f - navHideFraction.floatValue
                        // Bottom bar owns the reserved height instead of covering
                        // page content. Android's gesture inset stays clear at 0%.
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(74.dp * visibleFraction)
                                    .clipToBounds()
                            ) {
                                FloatingBottomNavigation(
                                    currentPosition = pagerState.currentPage,
                                    onChangePosition = {
                                        coroutineScope.launch { pagerState.scrollToPage(it) }
                                    },
                                    onReselected = onReselected,
                                    navigationItems = navigationItems,
                                    themeColors = themeColors,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(horizontal = 20.dp, vertical = 5.dp)
                                        .graphicsLayer { alpha = visibleFraction }
                                )
                            }
                            Spacer(Modifier.navigationBarsPadding())
                        }
                    }
                }
            ) { paddingValues ->
                LazyLoadHorizontalPager(
                    contentPadding = paddingValues,
                    state = pagerState,
                    key = { navigationItems[it].id },
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        // Layer-only updates avoid recomposition of the full tab content.
                        val preview = predictiveProgress.value
                        scaleX = pageScale.value * (1f - preview * 0.055f)
                        scaleY = pageScale.value * (1f - preview * 0.045f)
                        translationX = preview * 12.dp.toPx()
                    },
                    verticalAlignment = Alignment.Top,
                    userScrollEnabled = false
                ) {
                    navigationItems[it].content()
                }
            }
        }
    }
    // Only tabs intercept predictive back. Root activity can still use the
    // system's native predictive back-to-home animation.
    PredictiveBackHandler(enabled = pagerState.currentPage != 0 && predictTabBack) { progress ->
        try {
            progress.collect { event ->
                predictiveProgress.snapTo(event.progress.coerceIn(0f, 1f))
            }
            pagerState.animateScrollToPage(0)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            // A cancelled swipe restores the original tab without navigating.
            withContext(NonCancellable) {
                predictiveProgress.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
            }
        }
    }
    BackHandler(enabled = pagerState.currentPage != 0 && !predictTabBack) {
        coroutineScope.launch { pagerState.scrollToPage(0) }
    }
}

