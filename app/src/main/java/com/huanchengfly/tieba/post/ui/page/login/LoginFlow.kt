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
    val header = current.cookie.split(';').filterNot { it.substringBefore('=').trim().equals("STOKEN", true) }
    return header.joinToString(";") + "; STOKEN=" + sameAccount.sToken
}

// A normal class deliberately does not print the web session in toString().
internal class LoginSession private constructor(val bduss: String, val sToken: String, val cookie: String) {
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
        if (closed || !isLoginReturnUrl(url) || stage == LoginStage.Verifying || stage == LoginStage.Complete ||
            stage == LoginStage.VerificationFailed) return
        val session = LoginSession.from(cookie)
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
