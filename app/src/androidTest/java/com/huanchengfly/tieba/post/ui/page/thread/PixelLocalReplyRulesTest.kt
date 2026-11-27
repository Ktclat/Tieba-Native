package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.huanchengfly.tieba.post.MainActivityV2
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.capturePhoneReview
import com.huanchengfly.tieba.post.ui.common.LocalContentBlockRules
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.calculateWindowSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.BlockRuleTarget
import com.huanchengfly.tieba.post.utils.LocalBlockRuleStore
import com.huanchengfly.tieba.post.utils.LocalBlockRules
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelLocalReplyRulesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivityV2>()

    @Test fun updatingRulesBlocksAndRestoresAFloorAndOneNestedReply() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storeRules = LocalBlockRuleStore.rules(context)
        val previous = runBlocking { storeRules.first().text(BlockRuleTarget.COMMENT) }
        val floorText = "周末路线招募推广。"
        val advertising = DiscussionSample.floor.copy(
            id = 11, floor = 11, content = listOf(PbContent(text = floorText)), sub_post_list = null, sub_post_number = 0,
        )
        val floors = persistentListOf(PostItemData(advertising.wrapImmutable()), PostItemData(DiscussionSample.floor.wrapImmutable()))
        val firstReply = DiscussionSample.children[0].content.last().text
        val nextReply = DiscussionSample.children[1].content.last().text
        val floorTip = context.getString(R.string.tip_blocked_post, advertising.floor)
        val replyTip = context.getString(R.string.tip_blocked_sub_post)
        fun save(text: String) = runBlocking { LocalBlockRuleStore.save(context, BlockRuleTarget.COMMENT, text) }
        fun displayed(text: String) {
            compose.waitUntil(10_000) {
                compose.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
        }

        try {
            save("")
            val activity = compose.activity
            compose.runOnUiThread {
                activity.setContent {
                    val rules by storeRules.collectAsState(LocalBlockRules.Empty)
                    CompositionLocalProvider(
                        LocalContentBlockRules provides rules,
                        LocalWindowSizeClass provides calculateWindowSizeClass(activity),
                    ) {
                        TiebaLiteTheme {
                            ProvideNavigator(EmptyDestinationsNavigator) {
                                MyScaffold(topBar = { ThreadTopBar(SimpleForum(name = "徒步").wrapImmutable(), {}, {}) }) { padding ->
                                    MyLazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                                        items(rules.displayFloors(floors, hide = false)) { row ->
                                            PostCard(row.post, row.contentRenders, row.subPosts, blocked = row.blocked)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            displayed(floorText)
            displayed(firstReply)
            save("招募推广\n^北岸线的爬升")
            displayed(floorTip)
            displayed(replyTip)
            displayed(nextReply)
            compose.onNodeWithText(floorText, useUnmergedTree = true).assertDoesNotExist()
            compose.onNodeWithText(firstReply, useUnmergedTree = true).assertDoesNotExist()
            compose.capturePhoneReview("local-block-replies")
            save("")
            displayed(floorText)
            displayed(firstReply)
            compose.onNodeWithText(replyTip, useUnmergedTree = true).assertDoesNotExist()
            compose.capturePhoneReview("local-block-replies-restored")
        } finally {
            save(previous)
        }
    }
}
