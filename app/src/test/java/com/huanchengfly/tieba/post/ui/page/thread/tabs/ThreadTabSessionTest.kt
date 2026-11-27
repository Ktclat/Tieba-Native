package com.huanchengfly.tieba.post.ui.page.thread.tabs

import android.app.Application
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import com.huanchengfly.tieba.post.ui.page.thread.ThreadFloorKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ThreadTabSessionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun recreatingTheActivityRestoresTabsAndChangingAccountStartsANewSession() {
        val restore = StateRestorationTester(compose)
        val account = mutableStateOf("reader-one")
        lateinit var session: ThreadTabs
        restore.setContent { session = key(account.value) { rememberThreadTabs() } }
        val oldSession = compose.runOnIdle {
            val old = session.activate(ThreadTab(10, "第一帖", onlyAuthor = true))
            session.mark(10, old.generation, ThreadFloorKey(18, 4, latest = true), 75)
            session.activate(ThreadTab(11, "第二帖"))
            session
        }
        restore.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertNotSame(oldSession, session)
            assertEquals(oldSession.entries, session.entries)
            account.value = "reader-two"
        }
        compose.runOnIdle { assertTrue(session.entries.isEmpty()) }
    }
}
