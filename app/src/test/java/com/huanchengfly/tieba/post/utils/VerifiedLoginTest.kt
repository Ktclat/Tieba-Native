package com.huanchengfly.tieba.post.utils

import com.google.gson.Gson
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VerifiedLoginTest {
    private fun verify(json: String) =
        Gson().fromJson(json, LoginBean::class.java).verifiedAccount("fixture", "", "BDUSS=fixture")

    @Test
    fun confirmedIdentityCanSignInBeforeOptionalProfileAndDeviceDetailsArrive() {
        val account = verify("""{"error_code":"0","user":{"id":234,"name":"吧友"}}""")
        assertEquals("234", account.uid)
        assertEquals("吧友", account.name)
        assertEquals("", account.tbs)
        assertEquals("", account.sToken)
    }

    @Test
    fun rejectedOrIncompleteResponsesDoNotCreateAccounts() {
        listOf(
            """{"error_code":"1","user":{"id":"12"}}""",
            """{"user":{"id":"12"}}""",
            """{"error_code":"0","user":{"id":"12","is_login":false}}""",
            """{"error_code":"0","user":{"id":"12","is_login":"0"}}""",
            """{"error_code":"0","user":{"id":"0"}}""",
            """{"error_code":"0","user":{"id":"guest"}}""",
            """{"error_code":"0"}""",
        ).forEach {
            assertThrows(TiebaNotLoggedInException::class.java) { verify(it) }
        }
    }
}
