package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withAnnotation
import androidx.compose.ui.text.withStyle
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.components.spans.EmoticonSpanV2
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.utils.EmoticonManager.getEmoticonDrawable
import com.huanchengfly.tieba.post.utils.EmoticonManager.getEmoticonIdByName
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.regex.Pattern
import kotlin.math.roundToInt

object StringUtil {
    @JvmStatic
    fun getEmoticonContent(
        tv: TextView,
        source: CharSequence?,
        emoticonType: Int = EmoticonUtil.EMOTICON_ALL_TYPE
    ): SpannableString {
        return try {
            if (source == null) {
                return SpannableString("")
            }
            val spannableString: SpannableString = if (source is SpannableString) {
                source
            } else {
                SpannableString(source)
            }
            val regexEmoticon = EmoticonUtil.getRegex(emoticonType)
            val patternEmoticon = Pattern.compile(regexEmoticon)
            val matcherEmoticon = patternEmoticon.matcher(spannableString)
            while (matcherEmoticon.find()) {
                val key = matcherEmoticon.group()
                val start = matcherEmoticon.start()
                val group1 = matcherEmoticon.group(1) ?: ""
                val emoticonDrawable = getEmoticonDrawable(tv.context, getEmoticonIdByName(group1))
                if (emoticonDrawable != null) {
                    val paint = tv.paint
                    val size = (-paint.ascent() + paint.descent()).roundToInt()
                    val span = EmoticonSpanV2(emoticonDrawable, size)
                    spannableString.setSpan(
                        span,
                        start,
                        start + key.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            spannableString
        } catch (e: Exception) {
            e.printStackTrace()
            val spannableString: SpannableString = if (source is SpannableString) {
                source
            } else {
                SpannableString(source)
            }
            spannableString
        }
    }

    @JvmStatic
    fun getUsernameString(context: Context, username: String, nickname: String?): CharSequence {
        val showBoth = context.appPreferences.showBothUsernameAndNickname
        if (TextUtils.isEmpty(nickname)) {
            return if (TextUtils.isEmpty(username)) "" else username
        } else if (showBoth && !TextUtils.isEmpty(username) && !TextUtils.equals(
                username,
                nickname
            )
        ) {
            val builder = SpannableStringBuilder(nickname)
            builder.append(
                "($username)",
                ForegroundColorSpan(ThemeUtils.getColorByAttr(context, R.attr.color_text_disabled)),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            return builder
        }
        return nickname ?: ""
    }

    @Stable
    fun getUsernameAnnotatedString(
        context: Context,
        username: String,
        nickname: String?,
        color: Color = Color.Unspecified
    ): AnnotatedString {
        val showBoth = App.isInitialized && context.appPreferences.showBothUsernameAndNickname
        return buildAnnotatedString {
            if (showBoth && !nickname.isNullOrBlank() && username != nickname && username.isNotBlank()) {
                append(nickname)
                withStyle(SpanStyle(color = color)) {
                    append("(${username})")
                }
            } else {
                append(nickname ?: username)
            }
        }
    }

    @OptIn(ExperimentalTextApi::class)
    @Stable
    fun buildAnnotatedStringWithUser(
        userId: String,
        username: String,
        nickname: String?,
        content: String,
        context: Context = App.INSTANCE,
    ): AnnotatedString {
        return buildAnnotatedString {
            withAnnotation(tag = "user", annotation = userId) {
                withStyle(
                    SpanStyle(
                        color = Color(ThemeUtils.getColorByAttr(context, R.attr.colorNewPrimary))
                    )
                ) {
                    append("@")
                    append(
                        getUsernameAnnotatedString(
                            context,
                            username,
                            nickname,
                        )
                    )
                }
            }
            append(": ")
            append(content)
        }
    }

    @JvmStatic
    @Stable
    fun getAvatarUrl(portrait: String?): String = portraitUrl(portrait, "portrait")

    @JvmStatic
    fun getBigAvatarUrl(portrait: String?): String = portraitUrl(portrait, "portraith")

    private fun portraitUrl(portrait: String?, size: String): String {
        val value = portrait?.trim().orEmpty()
        if (value.isEmpty()) return ""
        val url = when {
            value.startsWith("//") -> "https:$value"
            value.startsWith("/sys/") -> "https://himg.bdimg.com$value"
            value.startsWith("sys/") -> "https://himg.bdimg.com/$value"
            value.contains("://") -> value
            else -> "https://himg.bdimg.com/sys/$size/item/$value"
        }.toHttpUrlOrNull() ?: return ""

        // Normalize old CDN URLs already saved in accounts, history, or returned by the API.
        return if (url.host in portraitHosts && url.encodedPath.startsWith("/sys/portrait")) {
            url.newBuilder().scheme("https").host("himg.bdimg.com").build().toString()
        } else {
            url.toString()
        }
    }

    private val portraitHosts = setOf("tb.himg.baidu.com", "himg.baidu.com", "himg.bdimg.com")

    fun String.getShortNumString(): String {
        val long = toLongOrNull() ?: return ""
        return long.getShortNumString()
    }

    fun Int.getShortNumString(): String {
        return toLong().getShortNumString()
    }

    fun Long.getShortNumString(): String {
        val long = this
        return if (long > 9999) {
            val longW = long * 10 / 10000L / 10F
            if (longW > 999) {
                val longKW = longW.toLong() * 10 / 1000L / 10F
                "${longKW}KW"
            } else {
                "${longW}W"
            }
        } else {
            "$this"
        }
    }
}