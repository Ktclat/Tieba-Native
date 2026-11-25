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

    private fun checkEditor(dark: Boolean) {
        ThemeUtils.init(App.ThemeDelegate)
        LitePal.initialize(App.INSTANCE)
        runBlocking { App.INSTANCE.dataStore.edit { it[booleanPreferencesKey("postOrReplyWarning")] = false } }
        val originalTheme = ThemeUtil.themeState.value
        ThemeUtil.themeState.value = if (dark) ThemeUtil.THEME_GREY_DARK else ThemeUtil.THEME_DEFAULT
        val model = ReplyViewModel()
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
                CompositionLocalProvider(LocalContext provides themed) {
                    TiebaLiteTheme {
                        val onSurface = MaterialTheme.colorScheme.onSurface.toArgb()
                        SideEffect { expectedText = onSurface }
                        ProvideNavigator(EmptyDestinationsNavigator) {
                            ReplyPage(
                                navigator = EmptyDestinationsNavigator, viewModel = model,
                                forumId = 1L, forumName = "旅行", threadId = 100L, postId = 12L,
                                subPostId = 201L, replyUserId = 20L, replyUserName = "林间月", tbs = "",
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
                val bounds = control.fetchSemanticsNode().boundsInRoot
                assertTrue("Expected a 48dp composer control at mdpi", bounds.width >= 48f && bounds.height >= 48f)
            }
            compose.captureReviewImage(view, if (dark) "reply-editor-dark.png" else "reply-editor-light.png")
        } finally {
            ThemeUtil.themeState.value = originalTheme
        }
    }
}
