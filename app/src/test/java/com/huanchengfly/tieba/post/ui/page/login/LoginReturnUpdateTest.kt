package com.huanchengfly.tieba.post.ui.page.login

import android.app.Application
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LoginReturnUpdateTest {
    private val callback = "https://tieba.baidu.com/index/tbwise/mine"

    @Test fun lateTokenDuringOrAfterVerificationIsUsedOnlyByAnExplicitRetry() = runBlocking {
        for (duringRequest in listOf(true, false)) {
            val first = CompletableDeferred<Unit>()
            val attempts = mutableListOf<LoginSession>()
            val flow = LoginFlowController(this, null) { session ->
                attempts.add(session)
                if (attempts.size == 1) first.await()
            }
            flow.onReturn(callback, "BDUSS=candidate; BAIDUID=device")
            yield()
            if (duringRequest) flow.onReturn(callback, "BDUSS=candidate; STOKEN=issued")
            first.completeExceptionally(IOException("verification failed"))
            yield()
            if (!duringRequest) flow.onReturn(callback, "BDUSS=candidate; STOKEN=issued")
            flow.onReturn(callback, "BDUSS=candidate")
            flow.onReturn(callback, "BDUSS=other; STOKEN=unrelated")
            flow.onReturn(callback, "BDUSS=candidate; STOKEN=issued")
            yield()
            assertEquals(LoginStage.VerificationFailed, flow.stage)
            assertEquals(listOf(""), attempts.map { it.sToken })
            flow.retryVerification()
            flow.retryVerification()
            yield()
            assertEquals(LoginStage.Complete, flow.stage)
            assertEquals(listOf("", "issued"), attempts.map { it.sToken })
            assertEquals("BDUSS=candidate; BAIDUID=device; STOKEN=issued", attempts.last().cookie)
        }
    }

    @Test fun switchingExpiresOnlyVisibleSessionNamesAtMatchingHostDomainAndPaths() = runBlocking {
        val cookies = mutableListOf<Cookie>()
        fun store(url: String, header: String) {
            cookies.add(checkNotNull(Cookie.parse(url.toHttpUrl(), header)))
        }
        fun read(url: String): String = cookies.filter { it.matches(url.toHttpUrl()) }
            .sortedByDescending { it.path.length }
            .joinToString("; ") { "${it.name}=${it.value}" }
        store(callback, "BDUSS=old-path; Domain=baidu.com; Path=/index/tbwise")
        store(callback, "BDUSS=old-root; Path=/")
        store(callback, "sToKeN=old-case; Domain=tieba.baidu.com; Path=/index/tbwise/")
        store("$callback/", "BDUSS_BFESS=old-slash; Path=/index/tbwise/mine/")
        store(ALTERNATIVE_LOGIN_URL, "STOKEN=old-passport; Path=/v2")
        store(LOGIN_URL, "BDUSS=old-passport; Domain=wappass.baidu.com; Path=/passport")
        store(callback, "BAIDUID=keep; Path=/")
        store(callback, "BDUSS=outside-login; Path=/unrelated")
        store("https://example.org/", "BDUSS=other-website; Path=/")
        assertTrue(read(callback).startsWith("sToKeN=old-case; BDUSS=old-path"))

        clearLoginCookies(::read) { url, header ->
            val deletion = checkNotNull(Cookie.parse(url.toHttpUrl(), header))
            assertTrue(deletion.expiresAt <= System.currentTimeMillis())
            cookies.removeAll { it.name == deletion.name && it.hostOnly == deletion.hostOnly &&
                it.domain == deletion.domain && it.path == deletion.path }
        }

        assertEquals("BAIDUID=keep", read("$callback/"))
        assertFalse(read(LOGIN_URL).contains("old-passport"))
        assertFalse(read(ALTERNATIVE_LOGIN_URL).contains("old-passport"))
        assertEquals(setOf("keep", "outside-login", "other-website"), cookies.map { it.value }.toSet())
        store(callback, "BDUSS=new-account; Domain=baidu.com; Path=/")
        assertEquals("new-account", LoginSession.from(cookieForLoginReturn(callback, ::read))?.bduss)
    }
}
