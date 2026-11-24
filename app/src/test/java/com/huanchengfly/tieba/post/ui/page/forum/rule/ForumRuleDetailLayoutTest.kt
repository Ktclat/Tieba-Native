package com.huanchengfly.tieba.post.ui.page.forum.rule

import android.view.View
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.TextContentRender
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = App::class, qualifiers = "zh-rCN-w360dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ForumRuleDetailLayoutTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstRuleTitleStartsBelowTheToolbar() {
        val state = ForumRuleDetailUiState(
            isLoading = false,
            title = "摄影吧交流规则",
            preface = "欢迎交流拍摄经验。发布作品前，请先阅读本吧规则。",
            data = persistentListOf(
                ForumRuleItemData("尊重原创", persistentListOf(TextContentRender("转载照片请注明作者，并取得授权。"))),
                ForumRuleItemData("友善讨论", persistentListOf(TextContentRender("分享建议时，请围绕作品本身。"))),
            ),
        )
        lateinit var view: View
        compose.setContent {
            val renderingView = LocalView.current
            SideEffect { view = renderingView }
            TiebaLiteTheme {
                ProvideNavigator(EmptyDestinationsNavigator) {
                    ForumRuleDetailContent(state, onBack = {})
                }
            }
        }
        val toolbarBottom = compose.onNodeWithContentDescription(
            RuntimeEnvironment.getApplication().getString(R.string.button_back)
        ).fetchSemanticsNode().boundsInRoot.bottom
        val firstTitle = compose.onNodeWithText(state.title).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("Rule title (top ${firstTitle.top}) overlaps toolbar action (bottom $toolbarBottom)",
            firstTitle.top >= toolbarBottom)
        compose.captureReviewImage(view, "forum-rules-toolbar.png")
    }
}
