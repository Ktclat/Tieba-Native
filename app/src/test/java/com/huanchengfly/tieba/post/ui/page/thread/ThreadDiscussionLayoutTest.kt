package com.huanchengfly.tieba.post.ui.page.thread

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.BitmapFactory
import android.os.Looper
import android.view.View
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavOptionsBuilder
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.models.protos.Agree
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPost
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.contentRenders
import com.huanchengfly.tieba.post.api.models.protos.subPosts
import com.huanchengfly.tieba.post.arch.BaseComposeActivity.Companion.LocalWindowSizeClass
import com.huanchengfly.tieba.post.arch.wrapImmutable
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.PicContentRender
import com.huanchengfly.tieba.post.ui.common.PbContentRender
import androidx.compose.material3.MaterialTheme
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.huanchengfly.tieba.post.ui.common.windowsizeclass.WindowSizeClass
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.UserProfilePageDestination
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostsHeading
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItem as DetailReply
import com.huanchengfly.tieba.post.ui.page.subposts.SubPostItemData as DetailData
import com.huanchengfly.tieba.post.ui.widgets.compose.MyLazyColumn
import com.huanchengfly.tieba.post.ui.widgets.compose.MyScaffold
import com.huanchengfly.tieba.post.utils.ThemeUtil
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import java.io.File
import java.time.Duration
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h840dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ThreadDiscussionLayoutTest {
    @get:Rule(order = 0) val context = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    private val lin = User(id = 20, name = "林间月", nameShow = "林间月")
    private val qing = User(id = 30, name = "青禾", nameShow = "青禾")
    private fun reply(id: Long, sender: User, recipient: User?, body: String) = SubPostList(
        id = id, author = sender, time = 1695374000 + id.toInt(), agree = Agree(diffAgreeNum = id - 200),
        content = buildList {
            if (recipient != null) {
                add(PbContent(text = "回复 "))
                add(PbContent(type = 4, uid = recipient.id, text = "@${recipient.name}"))
                add(PbContent(text = "："))
            }
            add(PbContent(text = body))
        },
    )
    private val children = listOf(
        reply(201, lin, qing, "北岸线的爬升集中在哪一段？"),
        reply(202, qing, lin, "石桥之后，缓坡一直到观景台。"),
        reply(203, User(id = 40, name = "听风", nameShow = "听风"), null, "").copy(
            content = listOf(PbContent(text = "我和 "), PbContent(type = 4, uid = 30, text = "@青禾"), PbContent(text = " 都走过这条路。")),
        ),
        reply(204, User(id = 50, name = "春山", nameShow = "春山"), null, "回复 岸上风 :雨天过桥小心。"),
    )
    private val topicTitle = "北岸步道周末路线"
    private val topic = Post(
        id = 1, floor = 1, time = 1695370100,
        author = User(id = 10, name = "溪口", nameShow = "溪口"),
        content = listOf(PbContent(text = "北岸步道的路线图，供周末出行参考。")),
    )
    private val floor = Post(
        id = 12, floor = 12, time = 1695373200,
        author = User(id = 12, name = "远舟", nameShow = "远舟", ip_address = "海南"),
        content = listOf(PbContent(text = "放一张实测海拔剖面。")), agree = Agree(diffAgreeNum = 404),
        sub_post_number = 4, sub_post_list = SubPost(sub_post_list = children),
    )
    private var opened by mutableStateOf<Long?>(null)
    private var filter by mutableStateOf(false)
    private var menu by mutableStateOf(false)
    private var desc by mutableStateOf(false)
    private var copied: String? = null
    private val liked = mutableListOf<Long>()
    private val accounts = mutableListOf<Long>()
    private val routes = mutableListOf<String>()
    private lateinit var view: View
    private lateinit var scroll: (Int, Int) -> Unit

    private fun picture(name: String): PbContentRender {
        val file = File(App.INSTANCE.cacheDir, "$name.png")
        javaClass.getResourceAsStream("/discussion/$name.png")!!.use { src -> file.outputStream().use { out -> src.copyTo(out) } }
        val render = PicContentRender(android.net.Uri.fromFile(file).toString(), "", false, 0, 720, 284, name)
        return object : PbContentRender {
            @Composable override fun Render() { Box(Modifier.testTag(name)) { render.Render() } }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    private fun show(
        width: Int,
        font: Float,
        topicPost: Post = topic,
        threadTitle: String? = topicTitle,
        immersiveMode: Boolean = false,
    ) {
        val topMedia = picture("trail-map")
        val replyMedia = picture("trail-profile")
        val navigator = object : DestinationsNavigator by EmptyDestinationsNavigator {
            override fun navigate(direction: Direction, onlyIfResumed: Boolean, builder: NavOptionsBuilder.() -> Unit) {
                routes += direction.route
            }
        }
        compose.setContent {
            val host = LocalView.current
            SideEffect { view = host }
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalWindowSizeClass provides WindowSizeClass.calculateFromSize(DpSize(width.dp,840.dp))) {
                TiebaLiteTheme {
                    CompositionLocalProvider(LocalDensity provides Density(density, font)) {
                        ProvideNavigator(navigator) {
                            MyScaffold(bottomBar = {
                                if (opened == null) ThreadBottomBar(User().wrapImmutable(), {}, {}, { menu = true }, agreeNum = 18)
                            }) { padding ->
                                if (menu) {
                                    ThreadMenu(
                                        isSeeLz = filter, isCollected = false, isImmersiveMode = false, isDesc = desc,
                                        canDelete = { false },
                                        onSeeLzClick = { filter = !filter; menu = false },
                                        onDescClick = { desc = !desc; menu = false },
                                        onCollectClick = {}, onImmersiveModeClick = {}, onJumpPageClick = {},
                                        onShareClick = {}, onCopyLinkClick = {}, onReportClick = {}, onDeleteClick = {},
                                        modifier = Modifier.padding(padding),
                                    )
                                } else key(opened) {
                                    val state = rememberLazyListState()
                                    val scope = rememberCoroutineScope()
                                    SideEffect { scroll = { i, offset -> scope.launch { state.scrollToItem(i, offset) } } }
                                    MyLazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)
                                        .padding(padding).testTag("discussion"), state = state) {
                                        if (opened == null) {
                                            item {
                                                TopicSurface {
                                                    PostCard(
                                                        postHolder = topicPost.wrapImmutable(),
                                                        contentRenders = (topicPost.contentRenders + topMedia).toImmutableList(),
                                                        threadAuthorId = topicPost.author?.id ?: 0L, showSubPosts = false,
                                                        discussionRole = DiscussionRole.Topic, threadTitle = threadTitle,
                                                        immersiveMode = immersiveMode, onUserClick = { accounts += it.id },
                                                    )
                                                }
                                            }
                                            stickyHeader {
                                                Box(Modifier.testTag("reply-controls")) {
                                                    ThreadRepliesHeader(269, filter) { filter = it }
                                                }
                                            }
                                            if (!filter) item {
                                                PostCard(
                                                    floor.wrapImmutable(), (floor.contentRenders + replyMedia).toImmutableList(), floor.subPosts,
                                                    threadAuthorId = topicPost.author?.id ?: 0L, onAgree = { liked += floor.id },
                                                    immersiveMode = immersiveMode,
                                                    onSubPostAgree = { liked += it.id }, onUserClick = { accounts += it.id },
                                                    onOpenSubPosts = { opened = it }, onMenuCopyClick = { copied = it },
                                                )
                                            }
                                        } else {
                                            item {
                                                PostCard(
                                                    floor.wrapImmutable(), (floor.contentRenders + replyMedia).toImmutableList(),
                                                    showSubPosts = false, discussionRole = DiscussionRole.ParentFloor,
                                                )
                                            }
                                            stickyHeader { SubPostsHeading(children.size) }
                                            items(children, key = { it.id }) { child ->
                                                DetailReply(DetailData(child), onAgree = { liked += it.id }, onUserClick = { accounts += it.id },
                                                    onMenuCopyClick = { copied = it })
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun node(text: String) = compose.onNodeWithText(text, useUnmergedTree = true)
    private fun resource(id: Int) = App.INSTANCE.getString(id)

    @Test fun readsFiltersAndOpensTheConversation() = review(360, 1f, "360")

    @Test @Config(qualifiers = "zh-rCN-w320dp-h840dp-mdpi")
    fun readsTheConversationInDarkThemeAtLargeFont() {
        val previous = ThemeUtil.themeState.value
        try {
            ThemeUtil.themeState.value = ThemeUtil.THEME_GREY_DARK
            review(320, 1.3f, "320-dark-large")
        } finally {
            ThemeUtil.themeState.value = previous
        }
    }

    @Test fun titlelessAnonymousTopicKeepsItsBodyOnceWithoutExposingAProfile() {
        ThemeUtils.init(App.ThemeDelegate)
        val body = topic.content.single().text
        val anonymous = User(id = 28, is_mask = 1, name = "内部账号", nameShow = "匿名吧友", portrait = "profile-28")
        val withoutAlias = anonymous.copy(nameShow = "")
        assertEquals(resource(R.string.comment_anonymous_user), withoutAlias.commentDisplayName(App.INSTANCE))
        assertNull(anonymous.commentAvatarPortrait)
        assertNull(anonymous.commentMentionPortrait)
        val publicAuthor = withoutAlias.copy(is_mask = 0)
        assertEquals("内部账号", publicAuthor.commentDisplayName(App.INSTANCE))
        assertEquals("profile-28", publicAuthor.commentAvatarPortrait)
        assertEquals("profile-28", publicAuthor.commentMentionPortrait)
        show(360, 1f, topicPost = topic.copy(author = anonymous, title = body, is_ntitle = 1), threadTitle = body)
        compose.onAllNodesWithText(body, useUnmergedTree = true).assertCountEquals(1)
        node(body).assert(!SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        node("匿名吧友").assertIsDisplayed().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .performTouchInput { click() }
        compose.runOnIdle { assertTrue(accounts.isEmpty()) }
        node("内部账号").assertDoesNotExist()
        node(resource(R.string.tip_lz)).assertDoesNotExist()
    }

    @Test fun immersiveTopicKeepsTheTitleAndTextWithoutItsAuthorOrNestedConversation() {
        ThemeUtils.init(App.ThemeDelegate)
        show(360, 1f, topicPost = topic.copy(title = topicTitle), threadTitle = null, immersiveMode = true)
        node(topicTitle).assertIsDisplayed().assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        node(topic.content.single().text).assertIsDisplayed()
        node("溪口").assertDoesNotExist()
        node(resource(R.string.tip_lz)).assertDoesNotExist()
        node("远舟").assertDoesNotExist()
        node(floor.content.single().text).assertIsDisplayed()
        node(children.first().content.last().text).assertDoesNotExist()
    }

    private fun review(width: Int, font: Float, suffix: String) {
        ThemeUtils.init(App.ThemeDelegate)
        show(width, font)
        awaitImage("trail-map")
        val heading = SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading)
        val title = node(topicTitle).assertIsDisplayed().assert(heading).fetchSemanticsNode().boundsInRoot
        val byline = node(topic.author!!.nameShow).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val body = node(topic.content.single().text).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("The title, author and body must remain distinct", title.bottom <= byline.top && byline.bottom <= body.top)
        compose.onAllNodesWithText(resource(R.string.tip_lz), useUnmergedTree = true).assertCountEquals(1)
        node(App.INSTANCE.getString(R.string.title_thread_header, "269")).assertIsDisplayed().assert(heading)
        compose.onNode(hasClickAction() and hasText(resource(R.string.text_all))).assertIsSelected()
        compose.captureReviewImage(view, "discussion-topic-$suffix.png")
        assertTopicSurfaceAndTransition("discussion-topic-$suffix.png")
        val like = hasClickAction() and hasContentDescription(resource(R.string.title_agree))
        compose.onNode(like and hasText("404")).performTouchInput { click() }
        node(resource(R.string.title_see_lz)).performTouchInput { click() }
        compose.runOnIdle { assertTrue(filter) }
        compose.onNode(hasClickAction() and hasText(resource(R.string.title_see_lz))).assertIsSelected()
        node(floor.content.single().text).assertDoesNotExist()
        node(resource(R.string.text_all)).performTouchInput { click() }
        compose.onNodeWithContentDescription(resource(R.string.btn_more)).performClick()
        node(resource(R.string.title_sort)).performTouchInput { click() }
        compose.runOnIdle { assertTrue(desc); assertFalse(menu) }

        compose.runOnIdle { scroll(1, 0) }
        awaitImage("trail-profile")
        val mainBody = node(floor.content.single().text).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val nestedBody = node(children[0].content.last().text).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        assertTrue("A reply preview must be indented under its parent", nestedBody.left > mainBody.left)
        node(App.INSTANCE.getString(R.string.tip_post_floor, floor.floor)).assertIsDisplayed()
        compose.captureReviewImage(view, "discussion-preview-$suffix.png")
        val firstRecipient = App.INSTANCE.getString(R.string.hint_reply, "@青禾")
        val secondRecipient = App.INSTANCE.getString(R.string.hint_reply, "@林间月")
        compose.onAllNodesWithText(firstRecipient, useUnmergedTree = true).assertCountEquals(1)
        compose.onNode(like and hasText("1")).performTouchInput { click() }
        assertDiscussionLinkContrast(view, node(firstRecipient), "@青禾")
        node(firstRecipient).performTouchInput { click() }
        node(secondRecipient).performTouchInput { click() }
        node("林间月").performTouchInput { click() }
        compose.runOnIdle {
            assertEquals(listOf(UserProfilePageDestination(30).route, UserProfilePageDestination(20).route), routes)
            assertEquals(listOf(20L), accounts)
            assertNull(opened)
        }
        node(children[0].content.last().text).performTouchInput { click() }
        compose.runOnIdle { assertEquals(201L, opened) }
        awaitImage("trail-profile")
        node(floor.author!!.nameShow).assertIsDisplayed()
        node(App.INSTANCE.getString(R.string.tip_post_floor, floor.floor)).assertIsDisplayed()
        val replyHeading = node(App.INSTANCE.getString(R.string.title_sub_posts_header, children.size))
            .assertIsDisplayed().assert(heading).fetchSemanticsNode().boundsInRoot
        val parentMedia = compose.onNodeWithTag("trail-profile", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("The detail screen must identify the parent before its replies", parentMedia.bottom <= replyHeading.top)
        compose.captureReviewImage(view, "discussion-expanded-$suffix.png")
        compose.runOnIdle { scroll(2, 0) }
        compose.onNode(like and hasText("2")).performTouchInput { click() }
        node(secondRecipient).assertIsDisplayed().performTouchInput { click() }
        node(children[1].content.last().text).performTouchInput { longClick() }
        node("复制").performClick()
        compose.runOnIdle {
            assertEquals(listOf(12L, 201L, 202L), liked)
            assertEquals("回复 @林间月：石桥之后，缓坡一直到观景台。", copied)
            assertEquals(UserProfilePageDestination(20).route, routes.last())
        }
        compose.runOnIdle { scroll(4, 0) }
        val ordinaryMention = "我和 @青禾 都走过这条路。"
        val ordinary = node(ordinaryMention).assertIsDisplayed()
        assertDiscussionLinkContrast(view, ordinary, "@青禾")
        val layouts = mutableListOf<TextLayoutResult>()
        ordinary.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { read -> check(read(layouts)) }
        val mentionPosition = layouts.single().getBoundingBox(ordinaryMention.indexOf("青")).center
        ordinary.performTouchInput { click(mentionPosition) }
        compose.runOnIdle { assertEquals(UserProfilePageDestination(30).route, routes.last()) }

        val navigationCount = routes.size
        node("回复 岸上风").assert(!hasClickAction()).performTouchInput { click() }
        node("雨天过桥小心。").performTouchInput { longClick() }
        node("复制").performClick()
        compose.runOnIdle {
            assertEquals(navigationCount, routes.size)
            assertEquals("回复 岸上风 :雨天过桥小心。", copied)
        }
    }

    private fun assertTopicSurfaceAndTransition(name: String) {
        val media = compose.onNodeWithTag("trail-map", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val controls = compose.onNodeWithTag("reply-controls").fetchSemanticsNode().boundsInRoot
        val image = BitmapFactory.decodeFile(File("build/reports/ui-screenshots", name).absolutePath)
        try {
            val gap = media.bottom.toInt() + 1 until controls.top.toInt()
            assertTrue("The media and reply controls need breathing room", gap.count() >= 4)
            val title = node(topicTitle).fetchSemanticsNode().boundsInRoot
            val reply = node(floor.content.single().text).fetchSemanticsNode().boundsInRoot
            val topicColor = image.getPixel(title.left.toInt() - 2, title.center.y.roundToInt())
            val replyColor = image.getPixel(reply.left.toInt() - 2, reply.center.y.roundToInt())
            assertNotEquals("Main post and reply must have distinguishable surfaces", topicColor, replyColor)
            // A full-width flat color transition is expected. A one- or two-pixel rule between the surfaces is not.
            val colors = gap.map { image.getPixel(image.width / 2, it) }
            for (index in 1 until colors.lastIndex) {
                if (colors[index] != colors[index - 1] && colors[index] != colors.last()) {
                    val count = colors.drop(index).takeWhile { it == colors[index] }.size
                    assertTrue("Unexpected hairline between the topic and its replies", count > 2)
                }
            }
        } finally { image.recycle() }
    }

    private fun awaitImage(tag: String) {
        val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val sourceColor = if (tag == "trail-map") Color.rgb(165, 83, 50) else Color.rgb(36, 75, 71)
        // The app's dark image mask reduces these fixture RGB channels to 64%.
        val exposure = if (ThemeUtil.isNightMode()) 0.64f else 1f
        val red = (Color.red(sourceColor) * exposure).roundToInt()
        val green = (Color.green(sourceColor) * exposure).roundToInt()
        val blue = (Color.blue(sourceColor) * exposure).roundToInt()
        var previous: List<Int>? = null
        var stableFrames = 0
        compose.waitUntil(15_000) {
            compose.mainClock.advanceTimeBy(64)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(64))
            compose.runOnIdle {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                try {
                    view.draw(Canvas(bitmap))
                    var endpointPixels = 0
                    for (y in bounds.top.toInt() until bounds.bottom.toInt() step 2) {
                        for (x in bounds.left.toInt() until bounds.right.toInt() step 2) {
                            val color = bitmap.getPixel(x, y)
                            if (abs(Color.red(color) - red) <= 3 && abs(Color.green(color) - green) <= 3 &&
                                abs(Color.blue(color) - blue) <= 3) endpointPixels++
                        }
                    }
                    val sampled = buildList {
                        for (i in 1..12) for (j in 1..5) add(bitmap.getPixel(
                            (bounds.left + bounds.width * i / 13).toInt(), (bounds.top + bounds.height * j / 6).toInt(),
                        ))
                    }
                    stableFrames = if (endpointPixels >= 8 && sampled == previous) stableFrames + 1 else 0
                    previous = sampled
                    stableFrames >= 2
                } finally { bitmap.recycle() }
            }
        }
    }
}
