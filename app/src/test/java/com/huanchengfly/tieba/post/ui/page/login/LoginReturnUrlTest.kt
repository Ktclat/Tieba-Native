package com.huanchengfly.tieba.post.ui.page.login

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LoginReturnUrlTest {
    @Test fun recognizesTheOfficialHomepageAndMobileAccountReturnPaths() {
        for (url in listOf(
            "https://tieba.baidu.com/", "https://tieba.baidu.com/index/tbwise/",
            "https://tieba.baidu.com:443/index/tbwise/mine?source=login#top",
            "https://tiebac.baidu.com/index/tbwise/mine/",
        )) assertTrue(url, isLoginReturnUrl(url))
    }

    @Test fun browsingAPostOrBeingOnTheSignInPageDoesNotCompleteLogin() {
        for (url in listOf(
            null, "about:blank", LOGIN_URL, "https://tieba.baidu.com/p/123",
            "https://tieba.baidu.com/f?kw=摄影", "https://tieba.baidu.com/index/tbwise/messages",
            "http://tieba.baidu.com/", "https://tieba.baidu.com:8080/", "https://example.org/",
        )) {
            assertFalse(url, isLoginReturnUrl(url))
            assertNull(cookieForLoginReturn(url) { error("Unrelated pages must not read the browser session") })
        }
    }

    @Test fun aReturnPathCanSupplementOnlyItsOwnSessionFromTheRoot() {
        val returned = "https://tiebac.baidu.com/index/tbwise/mine"
        val cookies = mutableMapOf(returned to "BDUSS=offline-mobile; BAIDUID=offline-browser")
        cookies[LOGIN_COOKIE_ORIGIN] = "BDUSS=offline-other; STOKEN=unrelated"
        val isolated = LoginSession.from(cookieForLoginReturn(returned, cookies::get))!!
        assertEquals("offline-mobile", isolated.bduss)
        assertEquals("", isolated.sToken)

        cookies[LOGIN_COOKIE_ORIGIN] = "BDUSS=offline-mobile; STOKEN=offline-secondary"
        val completed = LoginSession.from(cookieForLoginReturn(returned, cookies::get))!!
        assertEquals("offline-mobile", completed.bduss)
        assertEquals("offline-secondary", completed.sToken)
        assertTrue(completed.cookie.contains("BAIDUID=offline-browser"))
    }
}
