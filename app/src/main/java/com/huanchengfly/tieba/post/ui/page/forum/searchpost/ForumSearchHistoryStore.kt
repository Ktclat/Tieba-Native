package com.huanchengfly.tieba.post.ui.page.forum.searchpost

import com.huanchengfly.tieba.post.models.database.SearchPostHistory
import org.litepal.LitePal
import org.litepal.extension.deleteAll
import org.litepal.extension.find

internal object ForumSearchHistoryStore {
    fun list(forumName: String): List<SearchPostHistory> =
        LitePal.where("forumName = ?", forumName)
            .order("timestamp DESC, id DESC")
            .find<SearchPostHistory>()
            .distinctBy { it.content }

    fun record(forumName: String, keyword: String): List<SearchPostHistory> {
        val content = keyword.trim()
        if (content.isNotEmpty()) {
            check(SearchPostHistory(content, forumName).saveOrUpdate(
                "forumName = ? AND content = ?", forumName, content
            ))
        }
        return list(forumName)
    }

    fun delete(forumName: String, content: String) {
        LitePal.deleteAll<SearchPostHistory>("forumName = ? AND content = ?", forumName, content)
    }

    fun clear(forumName: String) {
        LitePal.deleteAll<SearchPostHistory>("forumName = ?", forumName)
    }
}
