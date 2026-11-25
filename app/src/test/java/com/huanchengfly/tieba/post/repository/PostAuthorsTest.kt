package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.models.protos.Anti
import com.huanchengfly.tieba.post.api.models.protos.OriginThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.Page
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SimpleForum
import com.huanchengfly.tieba.post.api.models.protos.SubPost
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.ThreadInfo
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.displayPortrait
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorResponseData
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponseData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostAuthorsTest {
    @Test fun pageJoinsPartialAuthorsByIdAndPreservesAnonymousOrUnrelatedIdentities() {
        val page = PbPageResponseData(
            page = Page(), anti = Anti(), forum = SimpleForum(), thread = ThreadInfo(authorId = 7),
            user_list = listOf(
                User(id = 7, portrait = "host"), User(id = 8, portrait = "floor"),
                User(id = 9, portraith = "nested"), User(id = 10, portrait = "original"),
            ),
            first_floor_post = Post(floor = 1),
            post_list = listOf(
                Post(floor = 2, author_id = 8, author = User(nameShow = "保留昵称", level_id = 5),
                    origin_thread_info = OriginThreadInfo(title = "转发来源", author = User(id = 10)),
                    sub_post_list = SubPost(sub_post_list = listOf(
                        SubPostList(author_id = 9, author = User(id = 9, name = "回复者")),
                        SubPostList(author_id = 9, author = User(is_mask = 1, name = "匿名")),
                        SubPostList(author_id = 8, author = User(id = 12, portrait = "own")),
                    )),
                ),
                Post(floor = 3),
            ),
        )
        val hydrated = PbPageResponseData.ADAPTER.decode(page.encode()).withPostAuthors()
        assertEquals("host", hydrated.thread!!.author!!.portrait)
        assertEquals("host", hydrated.first_floor_post!!.author!!.portrait)
        val post = hydrated.post_list.first()
        assertEquals(8L, post.author!!.id)
        assertEquals("floor", post.author!!.portrait)
        assertEquals("保留昵称", post.author!!.nameShow)
        assertEquals(5, post.author!!.level_id)
        assertEquals("original", post.origin_thread_info!!.author!!.portrait)
        assertEquals("转发来源", post.origin_thread_info!!.title)
        assertNull(hydrated.post_list.last().origin_thread_info)
        assertNull(hydrated.post_list.last().author)
        val replies = post.sub_post_list!!.sub_post_list
        assertEquals("nested", replies[0].author!!.displayPortrait)
        assertEquals("", replies[1].author!!.displayPortrait)
        assertEquals("匿名", replies[1].author!!.name)
        assertEquals("own", replies[2].author!!.portrait)
    }

    @Test fun firstFloorAndTopicCanSupplyEachOthersPortraitWithoutAUserList() {
        val topic = ThreadInfo(authorId = 8, author = User(id = 8, name = "发帖者"))
        val root = Post(floor = 1, author_id = 8, author = User(portrait = "first"))
        val data = PbPageResponseData(
            page = Page(), anti = Anti(), forum = SimpleForum(), thread = topic, post_list = listOf(root),
        )
        assertEquals("first", data.withPostAuthors().thread!!.author!!.portrait)
        val other = data.copy(thread = topic.copy(author = User(id = 8, portrait = "topic")),
            post_list = listOf(root.copy(author = null)))
        assertEquals("topic", other.withPostAuthors().first_floor_post!!.author!!.portrait)
    }

    @Test fun completeFloorCanResolveARepeatedAuthorWithoutBorrowingAnotherUsersPortrait() {
        val data = PbFloorResponseData(
            post = Post(author_id = 7, author = User(id = 7)),
            subpost_list = listOf(
                SubPostList(author_id = 7, author = User(portraith = "high")),
                SubPostList(author_id = 8, author = User(name = "other")),
            ),
        ).withFloorAuthors()
        assertEquals("high", data.post!!.author!!.displayPortrait)
        assertEquals("high", data.subpost_list.first().author!!.displayPortrait)
        assertEquals("", data.subpost_list.last().author!!.displayPortrait)
    }
}
