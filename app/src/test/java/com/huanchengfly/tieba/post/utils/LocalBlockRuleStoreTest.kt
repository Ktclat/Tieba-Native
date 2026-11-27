package com.huanchengfly.tieba.post.utils

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LocalBlockRuleStoreTest {
    @get:Rule val directory = TemporaryFolder()

    private fun store(scope: CoroutineScope) = PreferenceDataStoreFactory.create(scope = scope) {
        File(directory.root, "settings.preferences_pb")
    }

    @Test fun atomicIndependentEditsSurviveReopeningAndRejectInvalidReplacements() = runBlocking {
        val unrelatedKey = booleanPreferencesKey("another_setting")
        val writer = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val data = store(writer)
            assertTrue(LocalBlockRuleStore.rules(data).first().isEmpty)
            data.edit { it[unrelatedKey] = true }
            coroutineScope {
                launch { LocalBlockRuleStore.save(data, BlockRuleTarget.FORUM, "^摄影$\n安卓") }
                launch { LocalBlockRuleStore.save(data, BlockRuleTarget.USER, "bot[0-9]+") }
                launch { LocalBlockRuleStore.save(data, BlockRuleTarget.COMMENT, "加群") }
            }
            val failure = runCatching { LocalBlockRuleStore.save(data, BlockRuleTarget.USER, "(ok)\n[") }
            assertTrue(failure.exceptionOrNull() is IllegalArgumentException)
        } finally {
            writer.coroutineContext[Job]!!.cancelAndJoin()
        }
        val reader = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        try {
            val reopened = store(reader)
            val saved = LocalBlockRuleStore.rules(reopened).first()
            assertTrue(saved.blocksForum("摄影"))
            assertEquals("bot[0-9]+", saved.text(BlockRuleTarget.USER))
            assertTrue(saved.blocksUser("BOT153"))
            assertTrue(saved.blocksComment("快来加群"))
            LocalBlockRuleStore.save(reopened, BlockRuleTarget.COMMENT, "")
            val cleared = LocalBlockRuleStore.rules(reopened).first()
            assertFalse(cleared.blocksComment("快来加群"))
            assertTrue(cleared.blocksForum("安卓"))
            assertTrue(cleared.blocksUser("BOT153"))
            assertEquals(true, reopened.data.first()[unrelatedKey])
        } finally {
            reader.coroutineContext[Job]!!.cancelAndJoin()
        }
    }
}
