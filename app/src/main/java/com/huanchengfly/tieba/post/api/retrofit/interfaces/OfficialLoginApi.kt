package com.huanchengfly.tieba.post.api.retrofit.interfaces

import com.huanchengfly.tieba.post.api.Header
import com.huanchengfly.tieba.post.api.models.LoginBean
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST

internal fun Retrofit.forOfficialLogin(): Retrofit {
    val transport = (callFactory() as OkHttpClient).newBuilder().followRedirects(false).build()
    return newBuilder().baseUrl("https://tiebac.baidu.com/").client(transport).build()
}

internal interface OfficialLoginApi {
    @Headers(
        "${Header.COOKIE}: ka=open",
        "${Header.DROP_HEADERS}: ${Header.CHARSET},${Header.CLIENT_TYPE}",
        "${Header.DROP_PARAMS}: BDUSS",
    )
    @POST("/c/s/login")
    @FormUrlEncoded
    suspend fun login(
        @Field("bdusstoken") bdussToken: String,
        @Field("stoken") sToken: String,
        @Field("user_id") userId: String? = null,
        @Field("channel_id") channelId: String = "",
        @Field("channel_uid") channelUid: String = "",
        @Field("_client_version") clientVersion: String = "11.10.8.6",
        @retrofit2.http.Header(Header.USER_AGENT) userAgent: String = "bdtb for Android $clientVersion",
        @Field("authsid") authsid: String = "null",
    ): LoginBean
}
