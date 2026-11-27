package com.huanchengfly.tieba.post.ui.page.thread

import com.huanchengfly.tieba.post.api.models.protos.Agree
import com.huanchengfly.tieba.post.api.models.protos.PbContent
import com.huanchengfly.tieba.post.api.models.protos.Post
import com.huanchengfly.tieba.post.api.models.protos.SubPost
import com.huanchengfly.tieba.post.api.models.protos.SubPostList
import com.huanchengfly.tieba.post.api.models.protos.User

internal object DiscussionSample {
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
    val children = listOf(
        reply(201, lin, qing, "北岸线的爬升集中在哪一段？"),
        reply(202, qing, lin, "石桥之后，缓坡一直到观景台。"),
        reply(203, User(id = 40, name = "听风", nameShow = "听风"), null, "").copy(
            content = listOf(PbContent(text = "我和 "), PbContent(type = 4, uid = 30, text = "@青禾"), PbContent(text = " 都走过这条路。")),
        ),
        reply(204, User(id = 50, name = "春山", nameShow = "春山"), null, "回复 岸上风 :雨天过桥小心。"),
    )
    val topicTitle = "北岸步道周末路线"
    val topic = Post(
        id = 1, floor = 1, time = 1695370100,
        author = User(id = 10, name = "溪口", nameShow = "溪口"),
        content = listOf(PbContent(text = "北岸步道的路线图，供周末出行参考。")),
    )
    val floor = Post(
        id = 12, floor = 12, time = 1695373200,
        author = User(id = 12, name = "远舟", nameShow = "远舟", ip_address = "海南"),
        content = listOf(PbContent(text = "放一张实测海拔剖面。")), agree = Agree(diffAgreeNum = 404),
        sub_post_number = 4, sub_post_list = SubPost(sub_post_list = children),
    )
}
