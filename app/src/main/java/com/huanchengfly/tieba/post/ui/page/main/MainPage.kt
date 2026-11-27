package com.huanchengfly.tieba.post.ui.page.main

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.huanchengfly.tieba.post.LocalDevicePosture
import com.huanchengfly.tieba.post.LocalDestination
import com.huanchengfly.tieba.post.LocalNotificationCountFlow
import com.huanchengfly.tieba.post.LocalVisibleMainTabs
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
import com.huanchengfly.tieba.post.ui.page.destinations.MainPageDestination
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

internal fun mainNavigationType(width: WindowWidthSizeClass, posture: DevicePosture): MainNavigationType =
    when (width) {
        WindowWidthSizeClass.Medium -> MainNavigationType.NAVIGATION_RAIL
        WindowWidthSizeClass.Expanded -> if (posture is DevicePosture.BookPosture) {
            MainNavigationType.NAVIGATION_RAIL
        } else {
            MainNavigationType.PERMANENT_NAVIGATION_DRAWER
        }
        else -> MainNavigationType.BOTTOM_NAVIGATION
    }

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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@RootNavGraph(start = true)
@Destination
@Composable
fun MainPage(
    navigator: DestinationsNavigator,
    viewModel: MainViewModel = pageViewModel<MainUiIntent, MainViewModel>(emptyList()),
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val foldingDevicePosture by LocalDevicePosture.current

    val messageCount by viewModel.uiState.collectPartialAsState(MainUiState::messageCount)

    val notificationCountFlow = LocalNotificationCountFlow.current
    LaunchedEffect(null) {
        notificationCountFlow.collect {
            viewModel.send(MainUiIntent.NewMessage.Receive(it))
        }
    }

    val context = LocalContext.current
    val tabs = LocalVisibleMainTabs.current
    val selection = rememberMainTabSelection(tabs.first())

    val coroutineScope = rememberCoroutineScope()
    val navigationItems by remember(tabs, selection, viewModel) {
        derivedStateOf {
            listOf(
                NavigationItem(
                    id = "home",
                    icon = R.drawable.ms_home,
                    selectedIcon = R.drawable.ms_home_filled,
                    title = { stringResource(id = R.string.title_main) },
                    content = {
                        HomePage(
                            canOpenExplore = "explore" in tabs
                        ) {
                            selection.select("explore")
                        }
                    }
                ),
                NavigationItem(
                    id = "explore",
                    icon = R.drawable.ms_explore,
                    selectedIcon = R.drawable.ms_explore_filled,
                    title = { stringResource(id = R.string.title_explore) },
                    content = {
                        ExplorePage()
                    }
                ),
                NavigationItem(
                    id = "notification",
                    icon = R.drawable.ms_notifications,
                    selectedIcon = R.drawable.ms_notifications_filled,
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
                    icon = R.drawable.ms_person,
                    selectedIcon = R.drawable.ms_person_filled,
                    title = { stringResource(id = R.string.title_user) },
                    content = {
                        UserPage()
                    }
                ),
            ).filter { it.id in tabs }.toImmutableList()
        }
    }
    val selectedIndex = navigationItems.indexOfFirst { it.id == selection.id }.coerceAtLeast(0)
    val selectedId = navigationItems[selectedIndex].id
    if (selectedId != selection.id) SideEffect { selection.select(selectedId) }

    val navigationType = mainNavigationType(windowSizeClass.widthSizeClass, foldingDevicePosture)
    val navigationContentPosition = when (windowSizeClass.heightSizeClass) {
        WindowHeightSizeClass.Medium, WindowHeightSizeClass.Expanded -> MainNavigationContentPosition.CENTER
        else -> MainNavigationContentPosition.TOP
    }
    val compact = navigationType == MainNavigationType.BOTTOM_NAVIGATION
    val navScrollRangePx = with(LocalDensity.current) { 132.dp.toPx() }
    val navScroll = remember(selectedId, navigationType, navScrollRangePx) { MainNavigationScrollState(navScrollRangePx) }
    BackHandler(enabled = selectedIndex != 0 && LocalDestination.current == MainPageDestination) {
        selection.select(navigationItems.first().id)
    }
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
                        val floating by context.dataStore.collectPreferenceAsState(FloatingNavigationBarKey, false)
                        MainNavigationBarSlot(navScroll) {
                            MainNavigationBar(
                                currentPosition = selectedIndex,
                                onSelect = onSelect,
                                navigationItems = navigationItems,
                                floating = floating,
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
                        ),
                    )
                }
            }
        }
    }
}
