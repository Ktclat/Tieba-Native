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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
    }.also { it.restartWith(currentLoginCookies()) }
}

private class LoginBrowser(var restored: Bundle? = null) {
    var view: WebView? = null
    var host by mutableStateOf("")
    var progress by mutableIntStateOf(0)
    var canGoBack by mutableStateOf(false)

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
    var alternate by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var switching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val owner = LocalLifecycleOwner.current
    val entry = if (alternate) ALTERNATIVE_LOGIN_URL else LOGIN_URL
    val stage = flow.stage

    fun returnIfReady(url: String?, finished: Boolean): Boolean {
        if (!isLoginReturnUrl(url)) return false
        val cookie = cookieForLoginReturn(url, CookieManager.getInstance()::getCookie)
        if (cookie != null || finished) flow.onReturn(url, cookie)
        return cookie != null
    }

    fun reopen(switchProvider: Boolean = false) {
        val previousCookies = currentLoginCookies()
        if (switchProvider) alternate = !alternate
        flow.restartWith(previousCookies)
        browser.view?.loadUrl(if (alternate) ALTERNATIVE_LOGIN_URL else LOGIN_URL)
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
                    returnIfReady(web.url, false)
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
    BackHandler {
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
                    IconButton(onClick = { menuOpen = true }, enabled = !switching) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.btn_more))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(if (alternate) R.string.login_mobile_page else R.string.login_alternative_page)) },
                            onClick = { menuOpen = false; reopen(switchProvider = true) })
                        DropdownMenuItem(text = { Text(stringResource(R.string.title_refresh)) },
                            onClick = { menuOpen = false; reopen() })
                    }
                }
            },
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            OfficialLoginBrowser(browser, entry, flow, ::returnIfReady)
            if (stage == LoginStage.Browsing && browser.progress in 1..99) {
                LinearProgressIndicator(progress = { browser.progress / 100f }, modifier = Modifier.fillMaxWidth())
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
                        when (stage) {
                            LoginStage.Verifying, LoginStage.Complete -> CircularProgressIndicator()
                            LoginStage.PageFailed -> Button(onClick = { reopen() }) { Text(stringResource(R.string.button_retry)) }
                            LoginStage.VerificationFailed -> {
                                Button(onClick = flow::retryVerification) { Text(stringResource(R.string.button_retry)) }
                                TextButton(onClick = { reopen() }) { Text(stringResource(R.string.login_restart)) }
                            }
                            LoginStage.MissingSession -> {
                                Button(onClick = { reopen() }) { Text(stringResource(R.string.login_restart)) }
                                TextButton(onClick = { returnIfReady(browser.view?.url, true) }) { Text(stringResource(R.string.login_check_again)) }
                            }
                            LoginStage.ExistingSession -> {
                                Button(onClick = flow::confirmExisting, enabled = !switching) { Text(stringResource(R.string.login_use_current)) }
                                TextButton(enabled = !switching, onClick = {
                                    switching = true
                                    val previous = currentLoginCookies()
                                    scope.launch {
                                        try {
                                            clearWebSignInSession()
                                            flow.restartWith(previous)
                                            browser.view?.loadUrl(entry)
                                        } finally { switching = false }
                                    }
                                }) { Text(stringResource(R.string.login_other_account)) }
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
private fun OfficialLoginBrowser(browser: LoginBrowser, entry: String, flow: LoginFlowController, onReturn: (String?, Boolean) -> Boolean) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        update = { view ->
            val visible = flow.stage == LoginStage.Browsing
            view.visibility = if (visible) View.VISIBLE else View.INVISIBLE
            view.importantForAccessibility = if (visible) View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
                else View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        },
        factory = { context ->
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
                    override fun onProgressChanged(view: WebView, newProgress: Int) { browser.progress = newProgress }
                }
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                        browser.host = url?.let { Uri.parse(it).host }.orEmpty()
                        browser.progress = 0
                        if (browser.view === view) onReturn(url, false)
                    }

                    override fun onPageFinished(view: WebView, url: String?) {
                        browser.canGoBack = view.canGoBack()
                        if (browser.view === view && view.url == url) onReturn(url, true)
                    }

                    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                        browser.canGoBack = view.canGoBack()
                        if (browser.view === view) onReturn(url, false)
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
            browser.view = null
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
    for (name in listOf("BDUSS", "STOKEN", "BDUSS_BFESS", "STOKEN_BFESS")) {
        for (host in listOf("tieba.baidu.com", "tiebac.baidu.com", "wappass.baidu.com", "passport.baidu.com", "baidu.com")) {
            val domain = if (host == "baidu.com") "; Domain=.baidu.com" else ""
            suspendCancellableCoroutine { continuation ->
                jar.setCookie("https://$host/", "$name=; Path=/; Max-Age=0; Secure$domain") {
                    if (continuation.isActive) continuation.resume(Unit)
                }
            }
        }
    }
}
