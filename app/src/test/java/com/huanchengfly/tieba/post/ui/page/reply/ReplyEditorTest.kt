package com.huanchengfly.tieba.post.ui.page.reply

import android.app.Application
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavOptionsBuilder
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.models.database.Account
import com.huanchengfly.tieba.post.repository.ReplyRequest
import com.huanchengfly.tieba.post.repository.ReplySubmission
import com.huanchengfly.tieba.post.repository.ReplySubmissionState
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.ui.page.destinations.ReplyPageDestination
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import androidx.core.view.ancestors
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.google.android.material.textfield.TextInputLayout
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.dataStore
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.edittext.widget.UndoableEditText
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h760dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ReplyEditorTest {
    @get:Rule(order = 0) val app = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun lightEditorKeepsTheRecipientAndNativeTextHistory() = checkEditor(dark = false)

    @Test fun darkEditorKeepsTheRecipientAndNativeTextHistory() = checkEditor(dark = true)

    @Test fun guestCanLoginAndKeepTheExactReplyDestination() {
        ThemeUtils.init(App.ThemeDelegate)
        val routes = mutableListOf<String>()
        val navigator = object : DestinationsNavigator by EmptyDestinationsNavigator {
            override fun navigate(direction: Direction, onlyIfResumed: Boolean, builder: NavOptionsBuilder.() -> Unit) {
                routes += direction.route
            }
        }
        val args = ReplyArgs(1L, "旅行", 100L, 12L, 201L, 20L, "林间月", tbs = "guest-token")
        navigator.openReply(args, accountId = null)
        val expected = ReplyPageDestination(1L, "旅行", 100L, 12L, 201L, 20L, "林间月")
        assertEquals(listOf(expected.route, LoginPageDestination.route), routes)
        lateinit var host: View
        val model = ReplyViewModel()
        compose.setContent {
            val view = LocalView.current
            SideEffect { host = view }
            TiebaLiteTheme {
                ReplyPage(navigator, 1L, "旅行", 100L, postId = 12L, subPostId = 201L, viewModel = model)
            }
        }
        val login = compose.onNodeWithText(App.INSTANCE.getString(R.string.login_to_reply))
        login.assertIsEnabled()
        compose.captureReviewImage(host, "reply-guest-login.png")
        login.performClick()
        assertEquals(LoginPageDestination.route, routes.last())
    }

    private fun checkEditor(dark: Boolean) {
        ThemeUtils.init(App.ThemeDelegate)
        LitePal.initialize(App.INSTANCE)
        runBlocking { App.INSTANCE.dataStore.edit { it[booleanPreferencesKey("postOrReplyWarning")] = false } }
        val originalTheme = ThemeUtil.themeState.value
        ThemeUtil.themeState.value = if (dark) ThemeUtil.THEME_GREY_DARK else ThemeUtil.THEME_DEFAULT
        val account = Account(uid = "19", tbs = "account-token")
        val requests = mutableListOf<ReplyRequest>()
        val model = ReplyViewModel(ReplySubmission { request ->
            requests += request
            if (requests.size == 1) throw java.io.IOException("测试断网")
            ReplySubmissionState.Sent(GlobalEvent.ReplySuccess(100L, 12L, 12L, 201L, 202L), "")
        })
        val target = ReplyDrafts.target(account.uid, 100L, 12L, 201L)
        runBlocking { ReplyDrafts.clear(target) }
        var closed = false
        val navigator = object : DestinationsNavigator by EmptyDestinationsNavigator {
            override fun navigateUp(): Boolean { closed = true; return true }
        }
        lateinit var view: View
        var expectedText = 0
        try {
            compose.setContent {
                val host = LocalContext.current
                val themed = remember(host) {
                    ContextThemeWrapper(host, R.style.Theme_NewAppTheme)
                }
                val currentView = LocalView.current
                SideEffect { view = currentView }
                CompositionLocalProvider(LocalContext provides themed, LocalAccount provides account) {
                    TiebaLiteTheme {
                        val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
                        SideEffect { expectedText = onSurface }
                        ProvideNavigator(navigator) {
                            ReplyPage(
                                navigator = navigator, viewModel = model,
                                forumId = 1L, forumName = "旅行", threadId = 100L, postId = 12L,
                                subPostId = 201L, replyUserId = 20L, replyUserName = "林间月", tbs = "thread-token", tbsAccountId = account.uid,
                            )
                        }
                    }
                }
            }
            val send = compose.onNodeWithContentDescription(App.INSTANCE.getString(R.string.send_reply))
            send.assertIsNotEnabled()
            compose.runOnIdle {
                val editor = view.rootView.findViewById<UndoableEditText>(R.id.edit_text)
                val field = editor.ancestors.filterIsInstance<TextInputLayout>().first()
                assertEquals(App.INSTANCE.getString(R.string.hint_reply, "林间月"), field.hint.toString())
                assertEquals("Native editor follows the selected app theme", expectedText, editor.currentTextColor)
                assertTrue(editor.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE != 0)
                editor.append("我也走过北岸这段。")
                editor.append("\n雨后记得穿防滑鞋。")
                assertTrue(editor.undo())
                assertEquals("我也走过北岸这段。", editor.text.toString())
                assertTrue(editor.redo())
                assertEquals("我也走过北岸这段。\n雨后记得穿防滑鞋。", editor.text.toString())
            }
            send.assertIsEnabled()
            val emoji = compose.onNodeWithContentDescription(App.INSTANCE.getString(R.string.insert_emotions))
            for (control in listOf(emoji, send)) {
                val bounds = control.fetchSemanticsNode().touchBoundsInRoot
                assertTrue("Expected a 48dp composer control at mdpi", bounds.width >= 48f && bounds.height >= 48f)
            }
            compose.captureReviewImage(view, if (dark) "reply-editor-dark.png" else "reply-editor-light.png")
            if (!dark) {
                send.performClick()
                val retry = compose.onNodeWithContentDescription(App.INSTANCE.getString(R.string.reply_retry))
                retry.assertIsEnabled()
                val text = "我也走过北岸这段。\n雨后记得穿防滑鞋。"
                assertEquals(text, runBlocking { ReplyDrafts.read(target) })
                assertEquals(null, runBlocking { ReplyDrafts.read(ReplyDrafts.target("20", 100L, 12L, 201L)) })
                assertEquals(null, runBlocking { ReplyDrafts.read(ReplyDrafts.target("19", 100L, 12L, 209L)) })
                compose.captureReviewImage(view, "reply-editor-retry.png")
                retry.performClick()
                compose.waitUntil(5_000) { closed }
                assertEquals(listOf(text, text), requests.map { it.content })
                assertTrue(requests.all { it.postId == 12L && it.subPostId == 201L && it.replyUserId == 20L })
                assertEquals("thread-token", requests.last().tbs)
                assertEquals(null, runBlocking { ReplyDrafts.read(target) })
            }
        } finally {
            ThemeUtil.themeState.value = originalTheme
        }
    }
}
