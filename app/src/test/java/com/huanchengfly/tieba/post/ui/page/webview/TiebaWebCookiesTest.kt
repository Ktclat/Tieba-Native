package com.huanchengfly.tieba.post.ui.page.webview

import com.huanchengfly.tieba.post.models.database.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TiebaWebCookiesTest {
    private val origin = "https://tieba.baidu.com/"

    @Test fun onlyExactHttpsTiebaOriginsCanReceiveSessionCookies() {
        for (host in listOf("tieba.baidu.com", "tiebac.baidu.com", "wapp.baidu.com")) {
            assertEquals("https://$host/", tiebaWebCookieOrigin("https://$host:443/f?kw=test"))
        }
        for (url in listOf(
            "http://tieba.baidu.com/", "https://tieba.baidu.com:444/",
            "https://tieba.baidu.com.example.org/", "https://evil-tieba.baidu.com/",
            "https://tieba.baidu.com@example.org/", "https://someone@tieba.baidu.com/",
        )) assertNull(tiebaWebCookieOrigin(url))
        for (passport in listOf("passport.baidu.com", "pass.baidu.com", "wappass.baidu.com")) {
            assertTrue(isInternalHost(passport))
            assertNull(tiebaWebCookieOrigin("https://$passport/"))
        }
    }

    private fun writes(account: Account?, existing: String?): List<String> = buildList {
        syncTiebaWebCookies(origin, account, existing) { target, cookie ->
            assertEquals(origin, target)
            add(cookie)
        }
    }

    @Test fun selectingAnAccountReplacesStaleParentDomainAndHostCredentials() {
        val account = Account(uid = "202", bduss = "selected", sToken = "current",
            cookie = "BDUSS=expired; BDUSS_BFESS=old")
        val updates = writes(account, "BDUSS=old; BDUSS=selected; STOKEN=old; BDUSS_BFESS=old; unrelated=keep")
        val newValues = updates.filterNot { "Max-Age=0" in it }
        assertEquals(listOf("BDUSS=selected", "STOKEN=current"), newValues.map { it.substringBefore(';') })
        assertTrue(newValues.all { "Domain=" !in it && "Secure" in it && "HttpOnly" in it })
        assertTrue(updates.any { it.startsWith("BDUSS=;") && "Domain=baidu.com;" in it })
        assertTrue(updates.any { it.startsWith("BDUSS_BFESS=;") })
        assertFalse(updates.any { "unrelated" in it })
        assertTrue(writes(account, "BDUSS=selected; STOKEN=current").isEmpty())
    }

    @Test fun guestOrMissingOptionalTokenClearsThePreviousLogin() {
        val guest = writes(null, "BDUSS=old; STOKEN=old; STOKEN_BFESS=old")
        assertTrue(guest.isNotEmpty() && guest.all { "Max-Age=0;" in it })
        val account = Account(uid = "202", bduss = "selected")
        assertTrue(writes(account, "BDUSS=selected; STOKEN=old").all { it.startsWith("STOKEN=;") })
    }
}
