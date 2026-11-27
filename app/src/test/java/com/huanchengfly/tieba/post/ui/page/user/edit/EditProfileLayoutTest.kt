package com.huanchengfly.tieba.post.ui.page.user.edit

import android.app.Application
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w411dp-h923dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class EditProfileLayoutTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun fullWidthFieldsHaveSeparateRowsAndPreserveEditing() {
        lateinit var host: View
        var openedSex = false
        compose.setContent {
            val view = LocalView.current
            SideEffect { host = view }
            var nick by remember { mutableStateOf("北窗的雨") }
            var intro by remember { mutableStateOf("常去贴吧聊摄影和旅行。") }
            TiebaLiteTheme {
                Surface(Modifier.fillMaxSize()) {
                    EditProfileCard(
                        portrait = "", name = "山间听风", nickName = nick, sex = 1, intro = intro,
                        loading = false, onNickNameChange = { nick = it }, onIntroChange = { intro = it },
                        onModifySex = { openedSex = true },
                    )
                }
            }
        }
        val fields = compose.onAllNodes(hasSetTextAction())
        val nick = fields[0].fetchSemanticsNode().boundsInRoot
        val intro = fields[1].fetchSemanticsNode().boundsInRoot
        val sex = compose.onNodeWithText("性别").fetchSemanticsNode().boundsInRoot
        assertTrue(nick.bottom <= sex.top && sex.bottom <= intro.top)
        assertTrue(nick.width > host.width * .8f && intro.width > host.width * .8f)
        compose.onNodeWithText("性别").performClick()
        compose.runOnIdle { assertTrue(openedSex) }
        fields[0].performTextReplacement("北窗")
        compose.onNodeWithText("北窗").assertIsDisplayed()
        fields[1].performTextReplacement("旅行途中也记录步道和天气。")
        compose.onNodeWithText("旅行途中也记录步道和天气。").assertIsDisplayed()
        compose.captureReviewImage(host, "profile-edit-standard-phone.png")
    }
}
