package com.huanchengfly.tieba.post.ui.page.user

import android.content.ClipboardManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.PostInfoList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.capturePhoneReview
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.calculateWindowSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.user.post.PostListItemData
import com.huanchengfly.tieba.post.ui.page.user.post.UserPostItem
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.TiebaUtil
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelUserProfileTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivityV2>()

    @Test fun aReadersProfileKeepsTheDisplayIdSecondaryAndCopiesIt() {
        val activity = compose.activity
        val profile = User(
            id = 71409, name = "雾岛听风", nameShow = "雾岛听风", tieba_uid = "5986190117",
            concern_num = 12, fans_num = 78, total_agree_num = 10000, thread_num = 83, my_like_num = 5,
            sex = 2, tb_age = "0.9", ip_address = "四川", intro = "周末在山里散步，也记录沿途的风景。",
        )
        val headline = "雨后的山脊步道重新开放了"
        val post = PostListItemData(PostInfoList(
            thread_id = 9184, post_id = 918401, is_thread = 1, title = headline,
            rich_abstract = listOf(PbContent(text = headline)), user_id = profile.id,
            user_name = profile.name, name_show = profile.nameShow, forum_name = "徒步", reply_num = 18,
        ).wrapImmutable())
        val user = profile.wrapImmutable()

        compose.runOnUiThread {
            activity.setContent {
                CompositionLocalProvider(LocalWindowSizeClass provides calculateWindowSizeClass(activity)) {
                    TiebaLiteTheme {
                        ProvideNavigator(EmptyDestinationsNavigator) {
                            MyScaffold(topBar = { UserProfileToolbar(user, isSelf = false, showTitle = false, onBack = {}) }) { padding ->
                                Column(Modifier.fillMaxSize().padding(padding)) {
                                    UserProfileDetail(user, Modifier.padding(16.dp).testTag("profile-header"),
                                        onCopyIdClick = { TiebaUtil.copyText(activity, it) })
                                    val pages = persistentListOf(
                                        UserProfilePageData("threads", { stringResource(R.string.title_profile_threads_tab, "83") }, { _, _ -> }),
                                        UserProfilePageData("forums", { stringResource(R.string.title_profile_concern_forums_tab, "5") }, { _, _ -> }),
                                    )
                                    UserProfileTabRow(user, pages, rememberPagerState { pages.size })
                                    UserPostItem(post, onAgree = {})
                                }
                            }
                        }
                    }
                }
            }
        }

        val id = activity.getString(R.string.text_profile_user_id, profile.tieba_uid)
        val withinHeader = hasAnyAncestor(hasTestTag("profile-header"))
        fun textLayout(text: String): TextLayoutResult {
            val result = mutableListOf<TextLayoutResult>()
            compose.onNode(hasText(text) and withinHeader, useUnmergedTree = true)
                .assertIsDisplayed().performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(result) }
            return result.single()
        }
        val nickname = textLayout(profile.nameShow)
        val idLayout = textLayout(id)
        assertTrue(nickname.layoutInput.style.fontSize.value > idLayout.layoutInput.style.fontSize.value)
        assertFalse(idLayout.hasVisualOverflow)
        compose.onNodeWithText(profile.intro, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.text_profile_ip_location, profile.ip_address), useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithText(activity.getString(R.string.text_profile_tb_age, profile.tb_age), useUnmergedTree = true).assertIsDisplayed()
        compose.onAllNodesWithText(headline, useUnmergedTree = true).assertCountEquals(1)
        compose.capturePhoneReview("user-profile-hierarchy")
        compose.onNode(hasText(id) and hasClickAction()).performClick()
        compose.runOnIdle {
            val clipboard = activity.getSystemService(ClipboardManager::class.java)
            assertEquals(profile.tieba_uid, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }
}
