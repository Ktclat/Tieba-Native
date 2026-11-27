package com.huanchengfly.tieba.post

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.github.panpf.sketch.compose.AsyncImage
import com.github.panpf.sketch.fetch.newFileUri
import com.google.accompanist.systemuicontroller.SystemUiController
import com.huanchengfly.tieba.post.api.retrofit.exception.getErrorMessage
import com.huanchengfly.tieba.post.arch.BaseComposeActivity
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.arch.onGlobalEvent
import com.huanchengfly.tieba.post.components.ClipBoardForumLink
import com.huanchengfly.tieba.post.components.ClipBoardLink
import com.huanchengfly.tieba.post.components.ClipBoardLinkDetector
import com.huanchengfly.tieba.post.components.ClipBoardThreadLink
import com.huanchengfly.tieba.post.services.NotifyJobService
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.utils.LocalBlockRuleStore
import com.huanchengfly.tieba.post.ui.common.theme.compose.LocalAppThemeName
import androidx.compose.runtime.key
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.huanchengfly.tieba.post.ui.page.thread.tabs.LocalThreadTabs
import com.huanchengfly.tieba.post.ui.page.thread.tabs.rememberThreadTabs
import com.huanchengfly.tieba.post.ui.page.NavGraphs
import com.huanchengfly.tieba.post.ui.page.destinations.ForumPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ThreadPageDestination
import com.huanchengfly.tieba.post.ui.utils.DevicePosture
import com.huanchengfly.tieba.post.ui.utils.isBookPosture
import com.huanchengfly.tieba.post.ui.utils.isSeparating
import com.huanchengfly.tieba.post.ui.widgets.compose.AlertDialog
import com.huanchengfly.tieba.post.ui.widgets.compose.Avatar
import com.huanchengfly.tieba.post.ui.widgets.compose.AvatarIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.Dialog
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogNegativeButton
import com.huanchengfly.tieba.post.ui.widgets.compose.DialogPositiveButton
import com.huanchengfly.tieba.post.ui.widgets.compose.Sizes
import com.huanchengfly.tieba.post.ui.widgets.compose.rememberDialogState
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.utils.PermissionUtils
import com.huanchengfly.tieba.post.utils.PickMediasRequest
import com.huanchengfly.tieba.post.utils.QuickPreviewUtil
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.huanchengfly.tieba.post.utils.compose.LaunchActivityForResult
import com.huanchengfly.tieba.post.utils.compose.LaunchActivityRequest
import com.huanchengfly.tieba.post.utils.isIgnoringBatteryOptimizations
import com.huanchengfly.tieba.post.utils.newIntentFilter
import com.huanchengfly.tieba.post.utils.registerPickMediasLauncher
import com.huanchengfly.tieba.post.utils.requestIgnoreBatteryOptimizations
import com.huanchengfly.tieba.post.utils.requestPermission
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.animations.defaults.RootNavGraphDefaultAnimations
import com.ramcosta.composedestinations.rememberNavHostEngine
import com.ramcosta.composedestinations.spec.DestinationSpec
import com.ramcosta.composedestinations.spec.Direction
import com.ramcosta.composedestinations.utils.currentDestinationAsState
import com.ramcosta.composedestinations.utils.currentDestinationFlow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

val LocalNotificationCountFlow =
    staticCompositionLocalOf<Flow<Int>> { throw IllegalStateException("not allowed here!") }
val LocalDevicePosture =
    staticCompositionLocalOf<State<DevicePosture>> { throw IllegalStateException("not allowed here!") }
val LocalNavController =
    staticCompositionLocalOf<NavHostController> { throw IllegalStateException("not allowed here!") }
val LocalDestination = compositionLocalOf<DestinationSpec<*>?> { null }

@AndroidEntryPoint
class MainActivityV2 : BaseComposeActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private val newMessageReceiver: BroadcastReceiver = NewMessageReceiver()
    private var messageReceiverRegistered = false

    private val notificationCountFlow: MutableSharedFlow<Int> =
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val pickMediasLauncher =
        registerPickMediasLauncher {
            emitGlobalEvent(GlobalEvent.SelectedImages(it.id, it.uris))
        }

    private val mLaunchActivityForResultLauncher = registerForActivityResult(
        LaunchActivityForResult()
    ) {
        emitGlobalEvent(GlobalEvent.ActivityResult(it.requesterId, it.resultCode, it.intent))
    }

    private val devicePostureFlow: StateFlow<DevicePosture> by lazy {
        WindowInfoTracker.getOrCreate(this)
            .windowLayoutInfo(this)
            .flowWithLifecycle(lifecycle)
            .map { layoutInfo ->
                val foldingFeature =
                    layoutInfo.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .firstOrNull()
                when {
                    isBookPosture(foldingFeature) ->
                        DevicePosture.BookPosture(foldingFeature.bounds)

                    isSeparating(foldingFeature) ->
                        DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

                    else -> DevicePosture.NormalPosture
                }
            }
            .stateIn(
                scope = lifecycleScope,
                started = SharingStarted.Eagerly,
                initialValue = DevicePosture.NormalPosture
            )
    }

    private var direction: Direction? = null
    private var waitingNavCollectorToNavigate = AtomicBoolean(false)
    private var myNavController: NavHostController? = null
        set(value) {
            field = value
            if (value != null && waitingNavCollectorToNavigate.get() && direction != null) {
                launch {
                    value.currentDestinationFlow
                        .take(1)
                        .collect {
                            if (waitingNavCollectorToNavigate.get() && direction != null) {
                                value.navigate(direction!!.route)
                                waitingNavCollectorToNavigate.set(false)
                                direction = null
                            }
                        }
                }
            }
        }

    private fun navigate(direction: Direction) {
        if (myNavController == null) {
            waitingNavCollectorToNavigate.set(true)
            this.direction = direction
        } else {
            myNavController?.navigate(direction.route)
        }
    }

    private fun checkIntent(intent: Intent): Boolean {
        return if (intent.data?.scheme == "com.baidu.tieba" && intent.data?.host == "unidispatch") {
            val uri = intent.data!!
            when (uri.path.orEmpty().lowercase()) {
                "/frs" -> {
                    val forumName = uri.getQueryParameter("kw") ?: return true
                    navigate(ForumPageDestination(forumName))
                }

                "/pb" -> {
                    val threadId = uri.getQueryParameter("tid")?.toLongOrNull() ?: return true
                    navigate(ThreadPageDestination(threadId))
                }
            }
            true
        } else {
            false
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (!checkIntent(intent)) myNavController?.handleDeepLink(intent)
    }

    private fun fetchAccount() {
        lifecycleScope.launch(Dispatchers.Main) {
            if (AccountUtil.isLoggedIn()) {
                AccountUtil.fetchAccountFlow()
                    .flowOn(Dispatchers.IO)
                    .catch { e ->
                        toastShort(e.getErrorMessage())
                        e.printStackTrace()
                    }
                    .collect()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && AccountUtil.isLoggedIn()) {
            requestPermission {
                permissions = listOf(PermissionUtils.POST_NOTIFICATIONS)
                description = getString(R.string.desc_permission_post_notifications)
            }
        }
    }

    private fun initAutoSign() {
        lifecycleScope.launch(Dispatchers.IO) {
            runCatching { TiebaUtil.initAutoSign(applicationContext) }
        }
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            newMessageReceiver,
            newIntentFilter(NotifyJobService.ACTION_NEW_MESSAGE).apply { addAction(AccountUtil.ACTION_SWITCH_ACCOUNT) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        messageReceiverRegistered = true
        updateNotificationJob()
        handler.postDelayed({
            requestNotificationPermission()
        }, 100)
    }

    override fun onStop() {
        if (messageReceiverRegistered) {
            unregisterReceiver(newMessageReceiver)
            messageReceiverRegistered = false
        }
        super.onStop()
    }

    private fun updateNotificationJob() {
        NotifyJobService.updateSchedule(this, AccountUtil.currentAccount?.uid)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        launch {
            ClientUtils.setActiveTimestamp()
        }
        intent?.let { checkIntent(it) }
    }

    override fun onCreateContent(systemUiController: SystemUiController) {
        super.onCreateContent(systemUiController)
        fetchAccount()
        initAutoSign()
    }

    private fun openClipBoardLink(link: ClipBoardLink) {
        when (link) {
            is ClipBoardThreadLink -> {
                myNavController?.navigate(Uri.parse("tblite://thread/${link.threadId}"))
            }

            is ClipBoardForumLink -> {
                navigate(ForumPageDestination(link.forumName))
            }

            else -> {
//                launchUrl(this, link.url)
            }
        }
    }

    @Composable
    private fun ClipBoardDetectDialog() {
        val latest by ClipBoardLinkDetector.previewInfoStateFlow.collectAsState()
        val rules = LocalContentBlockRules.current
        val previewInfo = latest?.takeUnless { preview ->
            rules.blocksForum(preview.forumName) || rules.blocksUser(preview.authorName, preview.authorNickname)
        } ?: return

        val dialogState = rememberDialogState()

        LaunchedEffect(previewInfo) {
            dialogState.show()
        }

        Dialog(
            dialogState = dialogState,
            title = {
                Text(text = stringResource(id = R.string.title_dialog_clip_board_tieba_url))
            },
            buttons = {
                DialogPositiveButton(text = stringResource(id = R.string.button_open)) {
                    openClipBoardLink(previewInfo.clipBoardLink)
                }
                DialogNegativeButton(text = stringResource(id = R.string.btn_close))
            },
        ) {
            previewInfo.let {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = androidx.compose.material3.MaterialTheme.shapes.medium
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        it.icon?.let { icon ->
                            if (icon.type == QuickPreviewUtil.Icon.TYPE_DRAWABLE_RES) {
                                AvatarIcon(
                                    resId = icon.res,
                                    size = Sizes.Medium,
                                    contentDescription = null
                                )
                            } else {
                                Avatar(
                                    data = icon.url,
                                    size = Sizes.Medium,
                                    contentDescription = null
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            it.title?.let { title ->
                                Text(text = title, style = MaterialTheme.typography.titleMedium)
                            }
                            it.subtitle?.let { subtitle ->
                                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    override fun Content() {
        val storedRules by remember { LocalBlockRuleStore.rules(applicationContext) }.collectAsState(initial = null)
        val rules = storedRules
        if (rules == null) {
            // Draw an opaque first frame while the local rules are read. Links and previews
            // must wait for those rules before displaying any content.
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) { LoadingIndicator() }
            }
        } else {
            CompositionLocalProvider(LocalContentBlockRules provides rules) { MainContent() }
        }
    }

    @Composable
    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    private fun MainContent() {
        val okSignAlertDialogState = rememberDialogState()
        ClipBoardDetectDialog()
        AlertDialog(
            dialogState = okSignAlertDialogState,
            title = { Text(text = stringResource(id = R.string.title_dialog_oksign_battery_optimization)) },
            content = { Text(text = stringResource(id = R.string.message_dialog_oksign_battery_optimization)) },
            buttons = {
                DialogPositiveButton(
                    text = stringResource(id = R.string.button_go_to_ignore_battery_optimization),
                    onClick = {
                        requestIgnoreBatteryOptimizations()
                    }
                )
                DialogNegativeButton(
                    text = stringResource(id = R.string.button_cancel)
                )
                DialogNegativeButton(
                    text = stringResource(id = R.string.button_dont_remind_again),
                    onClick = {
                        appPreferences.ignoreBatteryOptimizationsDialog = true
                    }
                )
            },
        )
        LaunchedEffect(Unit) {
            if (appPreferences.autoSign && !isIgnoringBatteryOptimizations() && !appPreferences.ignoreBatteryOptimizationsDialog) {
                okSignAlertDialogState.show()
            }
        }
        onGlobalEvent<GlobalEvent.StartSelectImages> {
            pickMediasLauncher.launch(
                PickMediasRequest(it.id, it.maxCount, it.mediaType)
            )
        }
        onGlobalEvent<GlobalEvent.StartActivityForResult> {
            mLaunchActivityForResultLauncher.launch(
                LaunchActivityRequest(
                    it.requesterId,
                    it.intent
                )
            )
        }
        TiebaLiteLocalProvider {
            TranslucentThemeBackground(modifier = Modifier.fillMaxSize()) {
                val navController = rememberNavController()
                val currentDestination by navController.currentDestinationAsState()

                CompositionLocalProvider(
                    LocalThreadTabs provides key(LocalAccount.current?.uid) { rememberThreadTabs() },
                    LocalNavController provides navController,
                    LocalDestination provides currentDestination,
                ) {
                    val motion = MaterialTheme.motionScheme.fastEffectsSpec<IntOffset>()
                    val transitions = remember(motion) {
                        RootNavGraphDefaultAnimations(
                            enterTransition = { slideIntoContainer(SlideDirection.Start, motion) },
                            exitTransition = { slideOutOfContainer(SlideDirection.Start, motion) },
                            popEnterTransition = { fadeIn(tween(140, delayMillis = 70)) },
                            popExitTransition = { fadeOut(tween(70)) },
                        )
                    }
                    DestinationsNavHost(
                        navController = navController,
                        navGraph = NavGraphs.root,
                        engine = rememberNavHostEngine(rootDefaultAnimations = transitions),
                    )
                }

                SideEffect {
                    myNavController = navController
                }
            }
        }
    }

    @Composable
    private fun TranslucentThemeBackground(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = modifier
        ) {
            if (ThemeUtil.isTranslucentTheme(LocalAppThemeName.current)) {
                val backgroundPath by dataStore.collectPreferenceAsState(
                    key = stringPreferencesKey("translucent_theme_background_path"),
                    defaultValue = ""
                )
                val backgroundUri by remember { derivedStateOf { newFileUri(backgroundPath) } }
                AsyncImage(
                    imageUri = backgroundUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            content()
        }
    }

    @Composable
    fun TiebaLiteLocalProvider(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalNotificationCountFlow provides notificationCountFlow,
            LocalDevicePosture provides devicePostureFlow.collectAsState(),
        ) {
            content()
        }
    }

    private inner class NewMessageReceiver : BroadcastReceiver() {
        @SuppressLint("RestrictedApi")
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AccountUtil.ACTION_SWITCH_ACCOUNT) {
                notificationCountFlow.tryEmit(0)
                updateNotificationJob()
                return
            }
            if (intent.action == NotifyJobService.ACTION_NEW_MESSAGE) {
                val channel = intent.getStringExtra("channel")
                val count = intent.getIntExtra("count", 0)
                if (channel != null && channel == NotifyJobService.CHANNEL_TOTAL) {
                    lifecycleScope.launch {
                        notificationCountFlow.emit(count)
                    }
                }
            }
        }
    }
}
