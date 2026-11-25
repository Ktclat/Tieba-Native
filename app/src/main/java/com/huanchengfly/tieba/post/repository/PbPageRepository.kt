package com.huanchengfly.tieba.post.repository

import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.models.protos.pbFloor.PbFloorResponseData
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponse
import com.huanchengfly.tieba.post.api.models.protos.pbPage.PbPageResponseData
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaException
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaUnknownException
import com.huanchengfly.tieba.post.ui.page.thread.ThreadPageFrom
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object EmptyDataException : TiebaException("data is empty!") {
    override val code: Int
        get() = -2
}

object PbPageRepository {
    const val ST_TYPE_MENTION = "mention"
    const val ST_TYPE_STORE_THREAD = "store_thread"
    private val ST_TYPES = persistentListOf(ST_TYPE_MENTION, ST_TYPE_STORE_THREAD)

    fun pbPage(
        threadId: Long,
        page: Int = 1,
        postId: Long = 0,
        forumId: Long? = null,
        seeLz: Boolean = false,
        sortType: Int = 0,
        back: Boolean = false,
        from: String = "",
        lastPostId: Long? = null,
    ): Flow<PbPageResponse> =
        TiebaApi.getInstance()
            .pbPageFlow(
                threadId,
                page,
                postId = postId,
                seeLz = seeLz,
                sortType = sortType,
                back = back,
                forumId = forumId,
                stType = from.takeIf { ST_TYPES.contains(it) }.orEmpty(),
                mark = if (from == ThreadPageFrom.FROM_STORE) 1 else 0,
                lastPostId = lastPostId
            )
            .map { response ->
                val data = response.data_ ?: throw TiebaUnknownException
                response.copy(data_ = data.withPostAuthors())
            }
}

/** A page can carry a full profile in user_list and only a partial author on the post. */
internal fun Map<Long, User>.resolveAuthor(embedded: User?, authorId: Long): User? {
    if (embedded != null && (embedded.userhide != 0 || embedded.is_mask != 0)) return embedded
    val id = embedded?.id?.takeIf { it > 0 } ?: authorId
    if (id <= 0) return embedded
    val profile = this[id]
    if (embedded == null) return profile ?: User(id = id)
    if (profile == null && id == embedded.id) return embedded
    return embedded.copy(
        id = id,
        name = embedded.name.ifBlank { profile?.name.orEmpty() },
        nameShow = embedded.nameShow.ifBlank { profile?.nameShow.orEmpty() },
        portrait = embedded.portrait.ifBlank { profile?.portrait.orEmpty() },
        portraith = embedded.portraith.ifBlank { profile?.portraith.orEmpty() },
    )
}

internal fun PbPageResponseData.withPostAuthors(): PbPageResponseData {
    if (post_list.isEmpty()) throw EmptyDataException
    val topic = thread ?: throw TiebaUnknownException
    if (page == null || forum == null || anti == null) throw TiebaUnknownException
    val users = user_list.associateByTo(mutableMapOf()) { it.id }
    val firstFloor = post_list.firstOrNull { it.floor == 1 } ?: first_floor_post
    users.rememberAuthor(firstFloor?.author, firstFloor?.author_id ?: 0)
    val topicAuthorId = topic.authorId.takeIf { it > 0 } ?: firstFloor?.author_id?.takeIf { it > 0 }
        ?: firstFloor?.author?.id ?: 0
    val topicAuthor = users.resolveAuthor(topic.author, topicAuthorId)
        ?: throw TiebaUnknownException
    users.rememberAuthor(topicAuthor, topicAuthor.id)

    fun hydrate(post: Post, first: Boolean): Post {
        val id = post.author_id.takeIf { it > 0 } ?: if (first) topicAuthor.id else 0
        val author = users.resolveAuthor(post.author, id) ?: topicAuthor.takeIf { first }
        return post.copy(
            author = author,
            author_id = author?.id?.takeIf { it > 0 } ?: post.author_id,
            from_forum = forum,
            tid = topic.id,
            sub_post_list = post.sub_post_list?.copy(
                sub_post_list = post.sub_post_list.sub_post_list.map { reply ->
                    reply.copy(author = users.resolveAuthor(reply.author, reply.author_id))
                },
            ),
            origin_thread_info = post.origin_thread_info?.let { origin ->
                origin.copy(author = origin.author?.let { users.resolveAuthor(it, it.id) })
            },
        )
    }

    val posts = post_list.map { hydrate(it, it.floor == 1) }
    return copy(
        thread = topic.copy(author = topicAuthor),
        post_list = posts,
        first_floor_post = posts.firstOrNull { it.floor == 1 } ?: first_floor_post?.let { hydrate(it, first = true) },
    )
}

private fun MutableMap<Long, User>.rememberAuthor(author: User?, authorId: Long) {
    if (author == null || author.userhide != 0 || author.is_mask != 0) return
    val resolved = resolveAuthor(author, authorId) ?: return
    if (resolved.id > 0) put(resolved.id, resolved)
}

/** The full floor endpoint embeds profiles alongside replies instead of providing a user_list. */
internal fun PbFloorResponseData.withFloorAuthors(): PbFloorResponseData {
    val users = mutableMapOf<Long, User>()
    users.rememberAuthor(thread?.author, thread?.authorId ?: 0)
    users.rememberAuthor(post?.author, post?.author_id ?: 0)
    subpost_list.forEach { users.rememberAuthor(it.author, it.author_id) }
    return copy(
        post = post?.copy(author = users.resolveAuthor(post.author, post.author_id)),
        subpost_list = subpost_list.map { it.copy(author = users.resolveAuthor(it.author, it.author_id)) },
    )
}
