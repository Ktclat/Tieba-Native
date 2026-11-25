package com.huanchengfly.tieba.post.ui.page.main

import android.animation.ValueAnimator
import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.graphicsLayer
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import com.huanchengfly.tieba.post.LocalDevicePosture
import com.huanchengfly.tieba.post.LocalNotificationCountFlow
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.collectPartialAsState
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.pageViewModel
import com.huanchengfly.tieba.post.collectPreferenceAsState
import com.huanchengfly.tieba.post.dataStore
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
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun NavigationWrapper(
    currentPosition: Int,
    onSelect: (Int) -> Unit,
    navigationItems: ImmutableList<NavigationItem>,
    navigationType: MainNavigationType,
    navigationContentPosition: MainNavigationContentPosition,
    content: @Composable () -> Unit,
) {
    val latestContent by rememberUpdatedState(content)
    val page = remember { androidx.compose.runtime.movableContentOf { latestContent() } }
    when (navigationType) {
        MainNavigationType.PERMANENT_NAVIGATION_DRAWER -> androidx.compose.material3.PermanentNavigationDrawer(
            drawerContent = { NavigationDrawerContent(currentPosition, onSelect, navigationItems, navigationContentPosition) },
            content = page,
        )
        MainNavigationType.NAVIGATION_RAIL -> Row(Modifier.fillMaxSize()) {
            NavigationRail(currentPosition, onSelect, navigationItems, navigationContentPosition)
            Column(Modifier.weight(1f).fillMaxHeight()) { page() }
        }
        MainNavigationType.BOTTOM_NAVIGATION -> page()
    }
}

@OptIn(ExperimentalAnimationGraphicsApi::class)
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

    val messageCount by viewModel.uiState.collectPartialAsState(MainUiState::messageCount)

    val notificationCountFlow = LocalNotificationCountFlow.current
    LaunchedEffect(null) {
        notificationCountFlow.collect {
            viewModel.send(MainUiIntent.NewMessage.Receive(it))
        }
    }

    val context = LocalContext.current
    var lastHideExplore by rememberSaveable { mutableStateOf(false) }
    val hideExplore by context.dataStore.collectPreferenceAsState(
        key = booleanPreferencesKey("hideExplore"),
        defaultValue = false,
        initialValue = lastHideExplore,
    )
    SideEffect { lastHideExplore = hideExplore }
    val selection = rememberMainTabSelection("home")

    val coroutineScope = rememberCoroutineScope()
    val navigationItems by remember {
        derivedStateOf {
            listOfNotNull(
                NavigationItem(
                    id = "home",
                    icon = { AnimatedImageVector.animatedVectorResource(id = R.drawable.ic_animated_rounded_inventory_2) },
                    title = { stringResource(id = R.string.title_main) },
                    content = {
                        HomePage(
                            canOpenExplore = !hideExplore
                        ) {
                            selection.select("explore")
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
    val selectedIndex = navigationItems.indexOfFirst { it.id == selection.id }.coerceAtLeast(0)
    val selectedId = navigationItems[selectedIndex].id
    if (selectedId != selection.id) SideEffect { selection.select(selectedId) }

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
    val compact = navigationType == MainNavigationType.BOTTOM_NAVIGATION
    val navScrollRangePx = with(LocalDensity.current) { 132.dp.toPx() }
    val navScroll = remember(selectedId, navigationType, navScrollRangePx) { MainNavigationScrollState(navScrollRangePx) }
    val predictiveProgress = remember(selectedId) { Animatable(0f) }
    val predictTabBack by context.dataStore.collectPreferenceAsState(
        key = booleanPreferencesKey("enablePredictiveTabBack"),
        defaultValue = true,
    )
    val onSelect: (Int) -> Unit = { index ->
        navigationItems.getOrNull(index)?.let { item ->
            selection.activate(item) {
                coroutineScope.emitGlobalEvent(GlobalEvent.Refresh(item.id))
            }
        }
    }
    ProvideNavigator(navigator = navigator) {
        NavigationWrapper(
            currentPosition = selectedIndex,
            onSelect = onSelect,
            navigationItems = navigationItems,
            navigationType = navigationType,
            navigationContentPosition = navigationContentPosition
        ) {
            MyScaffold(
                backgroundColor = Color.Transparent,
                modifier = Modifier.fillMaxSize().then(if (compact) Modifier.nestedScroll(navScroll).observeNavigationTouch(navScroll) else Modifier),
                bottomBar = {
                    if (compact) {
                        MainNavigationBarSlot(navScroll) {
                            MainNavigationBar(
                                currentPosition = selectedIndex,
                                onSelect = onSelect,
                                navigationItems = navigationItems,
                            )
                        }
                    }
                }
            ) { paddingValues ->
                MainTabAccountScope(viewModel, LocalAccount.current?.uid) {
                    MainTabHost(
                        selectedId = selectedId,
                        navigationItems = navigationItems,
                        modifier = Modifier.fillMaxSize().then(
                            if (compact) Modifier.mainTabViewport(paddingValues, navScroll) else Modifier.padding(paddingValues)
                        ).graphicsLayer {
                            val preview = predictiveProgress.value
                            scaleX = 1f - preview * 0.055f
                            scaleY = 1f - preview * 0.045f
                            translationX = preview * 12.dp.toPx()
                        },
                    )
                }
            }
        }
    }
    // Only tabs intercept predictive back. Root activity can still use the
    // system's native predictive back-to-home animation.
    PredictiveBackHandler(enabled = selectedIndex != 0 && predictTabBack) { progress ->
        handleMainTabBack(progress, predictiveProgress, ValueAnimator.areAnimatorsEnabled()) {
            selection.select("home")
        }
    }
    BackHandler(enabled = selectedIndex != 0 && !predictTabBack) {
        selection.select("home")
    }
}

/** A confirmed back switches immediately. Only a cancelled gesture animates the old page back. */
internal suspend fun handleMainTabBack(
    events: Flow<BackEventCompat>,
    preview: Animatable<Float, AnimationVector1D>,
    animationsEnabled: Boolean,
    onConfirmed: () -> Unit,
) {
    try {
        events.collect { event ->
            if (animationsEnabled) preview.snapTo(event.progress.coerceIn(0f, 1f))
        }
        preview.snapTo(0f)
        onConfirmed()
    } catch (cancelled: CancellationException) {
        withContext(NonCancellable) {
            if (animationsEnabled && preview.value != 0f) {
                preview.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
            } else preview.snapTo(0f)
        }
        throw cancelled
    }
}
