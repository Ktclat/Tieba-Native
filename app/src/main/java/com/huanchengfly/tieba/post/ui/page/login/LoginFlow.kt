package com.huanchengfly.tieba.post.ui.page.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.net.URI

internal const val LOGIN_COOKIE_ORIGIN = "https://tieba.baidu.com/"
private const val RETURN_PARAMETER = "https%3A%2F%2Ftieba.baidu.com%2Findex%2Ftbwise%2Fmine"
const val LOGIN_URL = "https://wappass.baidu.com/passport?login&tpl=tb&u=$RETURN_PARAMETER"
internal const val ALTERNATIVE_LOGIN_URL = "https://passport.baidu.com/v2/?login&tpl=tb&u=$RETURN_PARAMETER"

private val returnHosts = setOf("tieba.baidu.com", "tiebac.baidu.com")
private val returnPaths = setOf("", "/index/tbwise", "/index/tbwise/mine")

internal fun isLoginReturnUrl(url: String?): Boolean {
    val uri = url?.let { runCatching { URI(it) }.getOrNull() } ?: return false
    return uri.scheme.equals("https", true) && uri.rawUserInfo == null && (uri.port == -1 || uri.port == 443) &&
        uri.host?.lowercase() in returnHosts && uri.rawPath.orEmpty().trimEnd('/') in returnPaths
}

internal fun existingLoginCookies(read: (String) -> String?): List<String> =
    returnHosts.flatMap { host ->
        returnPaths.flatMap { path -> listOf(path.ifEmpty { "/" }, "$path/").distinct().mapNotNull { read("https://$host$it") } }
    }.distinct()

internal fun cookieForLoginReturn(url: String?, read: (String) -> String?): String? {
    if (!isLoginReturnUrl(url)) return null
    val uri = URI(url!!)
    val host = uri.host.lowercase()
    val path = uri.rawPath.orEmpty().ifEmpty { "/" }
    val locations = listOf("https://$host$path", "https://$host/", LOGIN_COOKIE_ORIGIN).distinct()
    val sessions = locations.mapNotNull { LoginSession.from(read(it)) }
    val current = sessions.firstOrNull() ?: return null
    if (current.sToken.isNotEmpty()) return current.cookie
    val sameAccount = sessions.firstOrNull { it.bduss == current.bduss && it.sToken.isNotEmpty() } ?: return current.cookie
    return current.withUpdatedToken(sameAccount).cookie
}

// A normal class deliberately does not print the web session in toString().
internal class LoginSession private constructor(val bduss: String, val sToken: String, val cookie: String) {
    fun withUpdatedToken(newer: LoginSession): LoginSession {
        if (newer.bduss != bduss || newer.sToken.isBlank() || newer.sToken == sToken) return this
        val parts = cookie.split(';').filterNot { it.substringBefore('=').trim().equals("STOKEN", true) }
        return LoginSession(bduss, newer.sToken, parts.joinToString(";") + "; STOKEN=" + newer.sToken)
    }

    companion object {
        fun from(header: String?): LoginSession? {
            if (header.isNullOrBlank()) return null
            val cookies = mutableMapOf<String, String>()
            for (part in header.split(';')) {
                val separator = part.indexOf('=')
                if (separator > 0) {
                    // Android returns more specific paths before the root cookie of the same name.
                    cookies.putIfAbsent(part.substring(0, separator).trim().uppercase(), part.substring(separator + 1).trim())
                }
            }
            val bduss = cookies["BDUSS"]?.takeIf { it.isNotBlank() } ?: return null
            return LoginSession(bduss, cookies["STOKEN"].orEmpty(), header)
        }
    }
}

internal enum class LoginStage {
    Browsing, PageFailed, MissingSession, ExistingSession, Verifying, VerificationFailed, Complete
}

/** All calls originate on the UI thread. Only an officially verified account completes this flow. */
internal class LoginFlowController(
    private val scope: CoroutineScope,
    initialCookie: String?,
    private val verify: suspend (LoginSession) -> Unit,
) {
    var stage by mutableStateOf(LoginStage.Browsing)
        private set

    private var initial = setOfNotNull(LoginSession.from(initialCookie)?.bduss)
    private var returned: LoginSession? = null
    private var job: Job? = null
    private var closed = false

    fun onReturn(url: String?, cookie: String?) {
        if (closed || !isLoginReturnUrl(url) || stage == LoginStage.Complete) return
        val session = LoginSession.from(cookie)
        if (stage == LoginStage.Verifying || stage == LoginStage.VerificationFailed) {
            // Passport may finish writing STOKEN after the first request. Only an explicit retry uses it.
            if (session != null) returned = returned?.withUpdatedToken(session)
            return
        }
        if (session == null && stage == LoginStage.PageFailed) return
        returned = session
        when {
            session == null -> stage = LoginStage.MissingSession
            session.bduss in initial -> stage = LoginStage.ExistingSession
            else -> verifyReturned()
        }
    }

    fun confirmExisting() {
        if (stage == LoginStage.ExistingSession) verifyReturned()
    }

    fun retryVerification() {
        if (stage == LoginStage.VerificationFailed) verifyReturned()
    }

    fun pageFailed() {
        if (!closed && stage == LoginStage.Browsing) stage = LoginStage.PageFailed
    }

    fun restart(currentCookie: String?) = restartWith(listOfNotNull(currentCookie))

    fun restartWith(previousCookies: List<String>) {
        job?.cancel()
        returned = null
        initial = previousCookies.mapNotNull { LoginSession.from(it)?.bduss }.toSet()
        stage = LoginStage.Browsing
        closed = false
    }

    fun cancel() {
        closed = true
        job?.cancel()
        returned = null
        initial = emptySet()
    }

    private fun verifyReturned() {
        val session = returned ?: return
        if (closed) return
        stage = LoginStage.Verifying
        job = scope.launch {
            try {
                verify(session)
                if (!closed) {
                    stage = LoginStage.Complete
                    returned = null
                    initial = emptySet()
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (_: Exception) {
                if (!closed) stage = LoginStage.VerificationFailed
            }
        }
    }
}

internal suspend fun clearLoginCookies(
    read: (String) -> String?,
    expire: suspend (url: String, cookie: String) -> Unit,
) {
    val pages = returnHosts.flatMap { host -> returnPaths.map { path -> URI("https://$host$path") } } +
        listOf(URI(LOGIN_URL), URI(ALTERNATIVE_LOGIN_URL))
    val authorizedNames = setOf("BDUSS", "STOKEN", "BDUSS_BFESS", "STOKEN_BFESS")
    for ((host, urls) in pages.groupBy { it.host }) {
        // CookieManager exposes names and values only. Every matching Cookie Path is a directory
        // prefix or the complete path, and a trailing slash identifies a different stored cookie.
        val paths = mutableSetOf("/")
        for (url in urls) {
            var prefix = ""
            for (segment in url.path.split('/').filter { it.isNotEmpty() }) {
                prefix += "/$segment"
                paths.add(prefix)
                paths.add("$prefix/")
            }
        }
        for (path in paths) {
            val url = "https://$host$path"
            val names = read(url).orEmpty().split(';').map { it.substringBefore('=').trim() }.distinct()
            for (name in names.filter { it.uppercase() in authorizedNames }) {
                val deletion = "$name=; Path=$path; Max-Age=0; Secure; HttpOnly"
                expire(url, deletion)
                expire(url, "$deletion; Domain=$host")
                expire(url, "$deletion; Domain=baidu.com")
            }
        }
    }
}
