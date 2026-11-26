@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.huanchengfly.tieba.post.ui.page.login

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.withResumed
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.widgets.compose.BackNavigationIcon
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.ui.widgets.compose.Toolbar
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.ClientUtils
import com.huanchengfly.tieba.post.toastShort
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private fun currentLoginCookies() = existingLoginCookies(CookieManager.getInstance()::getCookie)

internal class LoginPageViewModel : ViewModel() {
    val flow = LoginFlowController(viewModelScope, null) { session ->
        AccountUtil.completeLogin(App.INSTANCE, session.bduss, session.sToken, session.cookie)
        val baiduId = AccountUtil.parseCookie(session.cookie).entries.firstOrNull { it.key.equals("BAIDUID", true) }?.value
        if (!baiduId.isNullOrBlank() && ClientUtils.baiduId.isNullOrBlank()) {
            // Device metadata is optional after the account has been verified and selected.
            runCatching { ClientUtils.saveBaiduId(App.INSTANCE, baiduId) }
        }
    }
    val network = LoginNetwork { isLoginDefinitelyOffline(App.INSTANCE) }
    private var initialized = false

    fun prepareBrowser() {
        if (!initialized) restartBrowser(currentLoginCookies())
    }

    fun restartBrowser(previous: List<String>) {
        flow.restartWith(previous)
        initialized = true
    }

    override fun onCleared() {
        flow.cancel()
    }
}

private class LoginBrowser(var restored: Bundle? = null) {
    var view: WebView? = null
    var host by mutableStateOf("")
    var progress by mutableIntStateOf(0)
    var canGoBack by mutableStateOf(false)
    var attempt by mutableIntStateOf(0)
        private set

    fun startNewPage() {
        restored = null
        view = null
        host = ""
        progress = 0
        canGoBack = false
        attempt++
    }

    companion object {
        val saver = Saver<LoginBrowser, Bundle>(
            save = { browser -> browser.view?.let { web -> Bundle().also { web.saveState(it) } } ?: browser.restored ?: Bundle() },
            restore = { LoginBrowser(it) },
        )
    }
}

@Destination
@Composable
fun LoginPage(navigator: DestinationsNavigator) {
    val model = viewModel<LoginPageViewModel>()
    val flow = model.flow
    val browser = rememberSaveable(saver = LoginBrowser.saver) { LoginBrowser() }
    val network = remember(model) { model.network.apply { enter() } }
    var alternate by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val entry = if (alternate) ALTERNATIVE_LOGIN_URL else LOGIN_URL
    val stage = if (network.pending != null && flow.stage != LoginStage.Complete) LoginStage.PageFailed else flow.stage

    fun returnIfReady(url: String?, finished: Boolean): Boolean {
        if (!isLoginReturnUrl(url)) return false
        if (network.pending != null) return true
        if (flow.stage !in setOf(LoginStage.Verifying, LoginStage.Complete, LoginStage.VerificationFailed) &&
            !network.allow(LoginAttempt.CheckSession)) return true
        val cookie = cookieForLoginReturn(url, CookieManager.getInstance()::getCookie)
        if (cookie != null || finished) flow.onReturn(url, cookie)
        return cookie != null
    }

    fun request(attempt: LoginAttempt) {
        if (!network.allow(attempt)) return
        when (attempt) {
            LoginAttempt.Resume -> {
                if (flow.stage == LoginStage.PageFailed) {
                    model.restartBrowser(currentLoginCookies())
                    browser.startNewPage()
                } else {
                    // A restored view may already be on the official callback. A fresh view checks on navigation.
                    browser.view?.let { returnIfReady(it.url, false) }
                }
            }
            LoginAttempt.OpenPage -> {
                model.restartBrowser(currentLoginCookies())
                browser.startNewPage()
            }
            LoginAttempt.CheckSession -> {
                // After rotation the factory restores the browser first and its normal callbacks check this URL.
                browser.view?.let { returnIfReady(it.url, true) }
            }
            LoginAttempt.Verify -> {
                returnIfReady(browser.view?.url, false)
                flow.retryVerification()
            }
            LoginAttempt.Confirm -> flow.confirmExisting()
            LoginAttempt.ChangeAccount -> {
                if (!switching) {
                    switching = true
                    val previous = currentLoginCookies()
                    scope.launch {
                        try {
                            clearWebSignInSession()
                            model.restartBrowser(previous)
                            network.allow(LoginAttempt.Resume)
                            browser.startNewPage()
                        } finally { switching = false }
                    }
                }
            }
        }
    }

    fun close() {
        flow.cancel()
        navigator.navigateUp()
    }

    DisposableEffect(owner, browser, flow) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> browser.view?.onPause()
                Lifecycle.Event.ON_RESUME -> browser.view?.let { web ->
                    web.onResume()
                    if (isLoginReturnUrl(web.url)) returnIfReady(web.url, false)
                    else if (flow.stage == LoginStage.Browsing) network.enter()
                }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(stage, owner) {
        if (stage == LoginStage.Complete) owner.lifecycle.withResumed { navigator.navigateUp() }
    }
    // Let the NavHost preview the preceding screen when there is no web history to consume.
    BackHandler(enabled = stage == LoginStage.Verifying || stage == LoginStage.Browsing && browser.canGoBack) {
        if (stage == LoginStage.Browsing && browser.canGoBack) browser.view?.goBack() else close()
    }
    MyScaffold(topBar = {
        Toolbar(
            title = {
                Column {
                    Text(stringResource(R.string.baidu_sign_in), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (browser.host.isNotBlank()) Text(
                        browser.host, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            },
            navigationIcon = { BackNavigationIcon(onBackPressed = ::close) },
            actions = {
                if (stage !in setOf(LoginStage.Verifying, LoginStage.Complete)) Box {
                    IconButton(onClick = { menuOpen = true }, enabled = !switching, shapes = IconButtonDefaults.shapes()) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.btn_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(if (alternate) R.string.login_mobile_page else R.string.login_alternative_page)) },
                            onClick = { menuOpen = false; alternate = !alternate; request(LoginAttempt.OpenPage) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.title_refresh)) },
                            onClick = { menuOpen = false; request(LoginAttempt.OpenPage) })
                    }
                }
            },
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            // An already mounted login page may receive the real redirect after a temporary network loss.
            if (network.pending == null || browser.view != null) {
                key(browser.attempt) {
                    OfficialLoginBrowser(browser, entry, stage == LoginStage.Browsing, model::prepareBrowser, flow, ::returnIfReady)
                }
            }
            if (stage == LoginStage.Browsing && browser.progress in 1..99) {
                LinearWavyProgressIndicator(progress = { browser.progress / 100f }, modifier = Modifier.fillMaxWidth())
            }
            if (stage != LoginStage.Browsing) {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        val title = when (stage) {
                            LoginStage.PageFailed -> R.string.login_page_unavailable
                            LoginStage.MissingSession -> R.string.login_session_pending
                            LoginStage.ExistingSession -> R.string.login_existing_session
                            LoginStage.VerificationFailed -> R.string.login_sync_failed
                            else -> R.string.login_verifying
                        }
                        Text(stringResource(title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                        if (stage == LoginStage.PageFailed && network.pending != null) {
                            Text(
                                stringResource(R.string.login_network_offline_hint),
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                        when (stage) {
                            LoginStage.Verifying, LoginStage.Complete -> LoadingIndicator()
                            LoginStage.PageFailed -> Button(
                                onClick = { request(network.pending ?: LoginAttempt.OpenPage) },
                                shapes = ButtonDefaults.shapes(),
                            ) { Text(stringResource(R.string.button_retry)) }
                            LoginStage.VerificationFailed -> {
                                Button(
                                    onClick = { request(LoginAttempt.Verify) },
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text(stringResource(R.string.button_retry)) }
                                TextButton(
                                    onClick = { request(LoginAttempt.OpenPage) },
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text(stringResource(R.string.login_restart)) }
                            }
                            LoginStage.MissingSession -> {
                                Button(
                                    onClick = { request(LoginAttempt.OpenPage) },
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text(stringResource(R.string.login_restart)) }
                                TextButton(
                                    onClick = { request(LoginAttempt.CheckSession) },
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text(stringResource(R.string.login_check_again)) }
                            }
                            LoginStage.ExistingSession -> {
                                Button(
                                    onClick = { request(LoginAttempt.Confirm) },
                                    enabled = !switching,
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text(stringResource(R.string.login_use_current)) }
                                TextButton(
                                    enabled = !switching,
                                    onClick = { request(LoginAttempt.ChangeAccount) },
                                    shapes = ButtonDefaults.shapes(),
                                ) { Text(stringResource(R.string.login_other_account)) }
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun OfficialLoginBrowser(
    browser: LoginBrowser,
    entry: String,
    visible: Boolean,
    beforeCreate: () -> Unit,
    flow: LoginFlowController,
    onReturn: (String?, Boolean) -> Boolean,
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
            view.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        },
        factory = { context ->
            beforeCreate()
            WebView(context).apply {
                browser.view = this
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = false
                    allowContentAccess = false
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    builtInZoomControls = true
                    displayZoomControls = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        if (browser.view === view) browser.progress = newProgress
                    }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        if (browser.view !== view) return
                        browser.host = url?.let { Uri.parse(it).host }.orEmpty()
                        browser.progress = 0
                        onReturn(url, false)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        if (browser.view !== view) return
                        browser.canGoBack = view.canGoBack()
                        if (view.url == url) onReturn(url, true)
                    }

                    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                        if (browser.view !== view) return
                        browser.canGoBack = view.canGoBack()
                        onReturn(url, false)
                    }

                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (request.url.scheme.equals("https", true)) return false
                        if (request.isForMainFrame && request.hasGesture()) openLoginApp(context, request.url)
                        return true
                    }

                    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                        if (browser.view === view && request.isForMainFrame && !onReturn(request.url.toString(), false)) flow.pageFailed()
                    }

                    override fun onReceivedHttpError(view: WebView, request: WebResourceRequest, response: WebResourceResponse) {
                        if (browser.view === view && request.isForMainFrame && response.statusCode >= 400 &&
                            !onReturn(request.url.toString(), false)) flow.pageFailed()
                    }
                }
                val restored = browser.restored
                browser.restored = null
                if (restored == null || restoreState(restored) == null) loadUrl(entry)
            }
        },
        onRelease = { view ->
            if (browser.view === view) browser.view = null
            view.stopLoading()
            view.webViewClient = WebViewClient()
            view.webChromeClient = null
            view.destroy()
        },
    )
}

private fun openLoginApp(context: Context, url: Uri) {
    val intent = when (url.scheme?.lowercase()) {
        "sms", "smsto" -> Intent(Intent.ACTION_SENDTO, url)
        "baiduboxapp" -> Intent(Intent.ACTION_VIEW, url).setPackage("com.baidu.searchbox")
        "weixin" -> Intent(Intent.ACTION_VIEW, url).setPackage("com.tencent.mm")
        "mqqapi", "mqq" -> Intent(Intent.ACTION_VIEW, url).setPackage("com.tencent.mobileqq")
        else -> null
    }
    if (intent == null || runCatching { context.startActivity(intent.addCategory(Intent.CATEGORY_BROWSABLE)) }.isFailure) {
        context.toastShort(R.string.login_method_unavailable)
    }
}

// Run only when the user selects “切换百度账号”. Keep other websites and saved native accounts intact.
private suspend fun clearWebSignInSession() {
    val jar = CookieManager.getInstance()
    clearLoginCookies(jar::getCookie) { url, cookie ->
        suspendCancellableCoroutine { continuation ->
            jar.setCookie(url, cookie) {
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }
}
