package com.huanchengfly.tieba.post.api

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.api.retrofit.converter.gson.GsonConverterFactory
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaApiException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.api.retrofit.interceptors.CommonParamInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.DropInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interceptors.FailureResponseInterceptor
import com.huanchengfly.tieba.post.api.retrofit.interfaces.OfficialLoginApi
import com.huanchengfly.tieba.post.api.retrofit.interfaces.forOfficialLogin
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.utils.AccountUtil
import com.huanchengfly.tieba.post.utils.verifiedAccount
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.HttpException
import retrofit2.Retrofit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class OfficialLoginTransportTest {
    private fun loginApi(
        selectedBduss: String?,
        status: Int = 200,
        responseBody: String = """{"user":{"id":"202"},"error_code":0}""",
        inspect: (Request) -> Unit = {},
    ): OfficialLoginApi {
        val commonClient = OkHttpClient.Builder()
            .addInterceptor(CommonParamInterceptor("BDUSS" to { selectedBduss }))
            .addInterceptor(DropInterceptor)
            .addInterceptor(FailureResponseInterceptor)
            .addInterceptor { chain ->
                val request = chain.request()
                inspect(request)
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(status).message("fixture").header("Location", "http://example.invalid/")
                    .body(responseBody.toResponseBody("application/json".toMediaType())).build()
            }.build()
        val service = Retrofit.Builder().baseUrl("http://example.invalid/").client(commonClient)
            .addConverterFactory(GsonConverterFactory.create()).build().forOfficialLogin()
        assertFalse((service.callFactory() as OkHttpClient).followRedirects)
        return service.create(OfficialLoginApi::class.java)
    }

    @Test fun firstLoginAndAddingAnotherAccountSubmitOnlyTheCandidateToTheHttpsEndpoint() = runBlocking {
        for (previous in listOf(null, "old-fixture")) {
            val api = loginApi(previous) { request ->
                assertEquals("https://tiebac.baidu.com/c/s/login", request.url.toString())
                assertEquals("POST", request.method)
                assertEquals("ka=open", request.header(Header.COOKIE))
                val form = request.body as FormBody
                val fields = (0 until form.size).associate { form.name(it) to form.value(it) }
                assertFalse("user_id" in fields)
                assertFalse("BDUSS" in fields)
                assertEquals("candidate-fixture|", fields["bdusstoken"])
                assertEquals("fresh-fixture", fields["stoken"])
            }
            assertEquals("202", api.login("candidate-fixture|", "fresh-fixture").user?.id)
        }
    }

    @Test fun redirectsRemainErrorsAndMissingSTokenRemainsAnExplicitEmptyField() = runBlocking {
        val api = loginApi(null, 307) { request ->
            val form = request.body as FormBody
            val tokenIndex = (0 until form.size).single { form.name(it) == "stoken" }
            assertEquals("", form.value(tokenIndex))
        }
        val redirected = runCatching { api.login("fixture|", "") }.exceptionOrNull()
        assertEquals(307, (redirected as HttpException).code())
    }

    @Test fun rejectedOrSignedOutResponsesCannotCreateOrSelectTheCandidateAccount() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        LitePal.initialize(context)
        LitePal.deleteAll(Account::class.java)
        val old = AccountUtil.persistLogin(context, Account(uid = "101", bduss = "previous-fixture"))
        for ((body, error) in listOf(
            """{"error_code":"4","error_msg":"verification required","user":{"id":"202"}}""" to TiebaApiException::class.java,
            """{"error_code":"0","user":{"id":"202","is_login":"0"}}""" to TiebaNotLoggedInException::class.java,
        )) {
            val rejected = runCatching {
                val login = loginApi(old.bduss, responseBody = body).login("candidate-fixture|", "")
                AccountUtil.persistLogin(context, login.verifiedAccount("candidate-fixture", "", "BDUSS=candidate-fixture"))
            }.exceptionOrNull()
            assertTrue(error.isInstance(rejected))
            AccountUtil.init(context)
            assertEquals(old.id, AccountUtil.currentAccount?.id)
            assertEquals(listOf("101"), AccountUtil.allAccounts.map { it.uid })
        }
    }
}
