package com.huanchengfly.tieba.post.ui.page.login

import android.app.Application
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LoginFlowControllerTest {
    private val callback = "https://tieba.baidu.com/index/tbwise/mine"
    private val old = "BDUSS=offline-old; STOKEN=offline-old-secondary"
    private val fresh = "BDUSS=offline-new"

    @Test fun aFreshSessionCompletesOnceEvenWhenSeveralBrowserCallbacksArrive() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val response = CompletableDeferred<Unit>()
        val sessions = mutableListOf<LoginSession>()
        val login = LoginFlowController(this, old) { session ->
            sessions += session
            entered.complete(Unit)
            response.await()
        }
        login.onReturn(LOGIN_URL, fresh)
        login.onReturn("https://tieba.baidu.com/p/123", fresh)
        assertTrue(sessions.isEmpty())
        assertEquals(LoginStage.Browsing, login.stage)

        login.onReturn(callback, fresh)
        yield()
        assertTrue(entered.isCompleted)
        assertEquals(LoginStage.Verifying, login.stage)
        login.onReturn(callback, fresh)
        login.onReturn(callback, fresh)
        response.complete(Unit)
        yield()
        assertEquals(LoginStage.Complete, login.stage)
        login.onReturn(callback, fresh)
        yield()
        assertEquals("offline-new", sessions.single().bduss)
        assertEquals("", sessions.single().sToken)
    }

    @Test fun cachedBrowserSessionRequiresExplicitConsentBeforeItCanBeSelected() = runBlocking {
        val sessions = mutableListOf<String>()
        val login = LoginFlowController(this, null) { sessions += it.bduss }
        val alternateReturn = "https://tiebac.baidu.com/index/tbwise/mine/"
        val cookies = mapOf(LOGIN_COOKIE_ORIGIN to "BDUSS=offline-root", alternateReturn to old)
        login.restartWith(existingLoginCookies(cookies::get))
        login.onReturn(alternateReturn, old)
        login.onReturn(alternateReturn, old)
        yield()
        assertTrue(sessions.isEmpty())
        assertEquals(LoginStage.ExistingSession, login.stage)

        login.confirmExisting()
        yield()
        assertEquals(listOf("offline-old"), sessions)
        assertEquals(LoginStage.Complete, login.stage)
    }

    @Test fun anOfficialReturnCanFinishAfterTheBrowserReportedAnError() = runBlocking {
        val sessions = mutableListOf<String>()
        val login = LoginFlowController(this, null) { sessions += it.bduss }
        login.pageFailed()
        login.onReturn(callback, null)
        assertEquals(LoginStage.PageFailed, login.stage)
        login.onReturn(callback, fresh)
        yield()
        assertEquals(LoginStage.Complete, login.stage)
        assertEquals(listOf("offline-new"), sessions)
    }

    @Test fun missingSessionAndPageErrorsLeaveTheUserSignedOutAndAllowAFreshAttempt() = runBlocking {
        val sessions = mutableListOf<String>()
        val login = LoginFlowController(this, null) { sessions += it.bduss }
        login.pageFailed()
        assertEquals(LoginStage.PageFailed, login.stage)
        login.restart(null)
        assertEquals(LoginStage.Browsing, login.stage)
        login.onReturn(callback, "BAIDUID=offline-device")
        assertEquals(LoginStage.MissingSession, login.stage)
        yield()
        assertTrue(sessions.isEmpty())

        login.restart(null)
        login.onReturn(callback, fresh)
        yield()
        assertEquals(listOf("offline-new"), sessions)
        assertEquals(LoginStage.Complete, login.stage)
    }

    @Test fun aVerificationFailureWaitsForExplicitRetry() = runBlocking {
        var attempts = 0
        val login = LoginFlowController(this, null) {
            if (++attempts == 1) throw IOException("offline test response")
        }
        login.onReturn(callback, fresh)
        yield()
        assertEquals(LoginStage.VerificationFailed, login.stage)
        login.onReturn(callback, fresh)
        yield()
        assertEquals(1, attempts)
        login.retryVerification()
        yield()
        assertEquals(2, attempts)
        assertEquals(LoginStage.Complete, login.stage)
    }

    @Test fun leavingDuringVerificationCannotCommitALateSuccess() = runBlocking {
        val response = CompletableDeferred<Unit>()
        val committed = mutableListOf<String>()
        val login = LoginFlowController(this, null) { session ->
            response.await()
            committed += session.bduss
        }
        login.onReturn(callback, fresh)
        yield()
        assertEquals(LoginStage.Verifying, login.stage)
        login.cancel()
        response.complete(Unit)
        yield()
        assertTrue(committed.isEmpty())
        assertNotEquals(LoginStage.Complete, login.stage)
    }
}
