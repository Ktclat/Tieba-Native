package com.huanchengfly.tieba.post.ui.page.thread

import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PixelLocalReplyRulesTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivityV2>()

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val storeRules get() = LocalBlockRuleStore.rules(context)

    private fun save(text: String) = runBlocking { LocalBlockRuleStore.save(context, BlockRuleTarget.COMMENT, text) }

    private fun displayed(text: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(text), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText(text, useUnmergedTree = true).assertIsDisplayed()
    }

    private fun showFloors(vararg rows: PostItemData) {
        val floors = persistentListOf(*rows)
        val activity = compose.activity
        val updates = storeRules
        compose.runOnUiThread {
            activity.setContent {
                val rules by updates.collectAsState(LocalBlockRules.Empty)
                CompositionLocalProvider(
                    LocalContentBlockRules provides rules,
                    LocalWindowSizeClass provides calculateWindowSizeClass(activity),
                ) {
                    TiebaLiteTheme {
                        ProvideNavigator(EmptyDestinationsNavigator) {
                            MyScaffold(topBar = { ThreadTopBar(SimpleForum(name = "徒步").wrapImmutable(), {}, {}) }) { padding ->
                                MyLazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
                                    items(rules.displayFloors(floors, hide = false)) { row ->
                                        PostCard(row.post, row.contentRenders, row.subPosts, blocked = row.blocked, onMenuCopyClick = {})
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test fun updatingRulesBlocksAndRestoresAFloorAndOneNestedReply() {
        val previous = runBlocking { storeRules.first().text(BlockRuleTarget.COMMENT) }
        val floorText = "周末路线招募推广。"
        val advertising = DiscussionSample.floor.copy(
            id = 11, floor = 11, content = listOf(PbContent(text = floorText)), sub_post_list = null, sub_post_number = 0,
        )
        val firstReply = DiscussionSample.children[0].content.last().text
        val nextReply = DiscussionSample.children[1].content.last().text
        val floorTip = context.getString(R.string.tip_blocked_post, advertising.floor)
        val replyTip = context.getString(R.string.tip_blocked_sub_post)
        try {
            save("")
            showFloors(PostItemData(advertising.wrapImmutable()), PostItemData(DiscussionSample.floor.wrapImmutable()))
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

    @Test fun blockOnlyTheChosenLiteralInsideAComment() {
        val previous = runBlocking { storeRules.first().text(BlockRuleTarget.COMMENT) }
        val phrase = "红点.+"
        val sentence = "请认准 $phrase 标记。"
        val other = "请认准 红点ABC 标记。"
        fun row(id: Long, floor: Int, body: String) = PostItemData(
            DiscussionSample.floor.copy(
                id = id, floor = floor, content = listOf(PbContent(text = body)),
                sub_post_list = null, sub_post_number = 0,
            ).wrapImmutable(),
        )
        try {
            save("早期规则")
            showFloors(row(21, 21, sentence), row(22, 22, other))
            displayed(sentence)
            compose.onNodeWithText(sentence, useUnmergedTree = true).performTouchInput { longClick() }
            val menu = compose.onNode(hasText("选词屏蔽") and hasClickAction())
            menu.assertIsDisplayed()
            compose.capturePhoneReview("selected-phrase-menu")
            menu.performClick()
            val field = compose.onNodeWithTag("comment-phrase-selection")
            field.assertIsDisplayed()
            val confirm = compose.onNode(hasText("加入屏蔽") and hasClickAction())
            confirm.assertIsNotEnabled()
            val body = field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
            assertEquals(sentence, body)
            field.performTouchInput { longClick(percentOffset(.25f, .30f)) }
            confirm.assertIsEnabled()
            val start = body.indexOf(phrase)
            field.performSemanticsAction(SemanticsActions.SetSelection) { setRange ->
                check(setRange(start, start + phrase.length, false))
            }
            confirm.assertIsEnabled()
            displayed(context.getString(R.string.comment_phrase_preview, phrase))
            compose.capturePhoneReview("selected-phrase-confirm")
            confirm.performClick()
            displayed(context.getString(R.string.tip_blocked_post, 21))
            displayed(other)
            compose.onNodeWithText(sentence, useUnmergedTree = true).assertDoesNotExist()
            val saved = runBlocking { storeRules.first() }
            assertEquals(2, saved.count(BlockRuleTarget.COMMENT))
            assertFalse(saved.blocksComment(other))
            compose.capturePhoneReview("selected-phrase-blocked")
        } finally {
            save(previous)
        }
    }
}
