package com.huanchengfly.tieba.post.ui.page.reply

import com.huanchengfly.tieba.post.models.database.Draft
import com.huanchengfly.tieba.post.utils.AccountUtil
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.litepal.LitePal
import com.huanchengfly.tieba.post.toMD5
import java.util.concurrent.Executors

internal object ReplyDrafts {
    data class Target(val hash: String, val legacyHash: String, val accountId: String)

    private val worker = Executors.newSingleThreadExecutor()
    private val dispatcher = worker.asCoroutineDispatcher()
    private val pending = mutableMapOf<String, String>()

    fun target(accountId: String, threadId: Long, postId: Long?, subPostId: Long?): Target {
        val path = "${threadId}_${postId?.takeIf { it > 0 }}_${subPostId?.takeIf { it > 0 }}"
        return Target("reply:${accountId}:$path", path.toMD5(), accountId)
    }

    suspend fun read(target: Target): String? = withContext(dispatcher) {
        LitePal.where("hash = ?", target.hash).findFirst(Draft::class.java)?.content ?: run {
            // The previous schema did not store an owner. Claim it only on a single-account device.
            if (AccountUtil.allAccounts.singleOrNull()?.uid != target.accountId) return@run null
            val previous = LitePal.where("hash = ?", target.legacyHash).findFirst(Draft::class.java)
            if (previous != null) previous.setHash(target.hash).save()
            previous?.content
        }
    }

    @Synchronized
    fun save(target: Target, text: String) {
        if (pending.put(target.hash, text) != null) return
        worker.execute {
            val latest = synchronized(this) { pending.remove(target.hash) } ?: return@execute
            if (latest.isEmpty()) LitePal.deleteAll(Draft::class.java, "hash = ?", target.hash)
            else Draft(target.hash, latest).saveOrUpdate("hash = ?", target.hash)
        }
    }

    suspend fun clear(target: Target) = withContext(dispatcher) {
        LitePal.deleteAll(Draft::class.java, "hash = ?", target.hash)
    }
}
