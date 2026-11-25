package com.huanchengfly.tieba.post.utils

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.models.database.Account
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.litepal.LitePal
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class AccountLoginPersistenceTest {
    private val context get() = ApplicationProvider.getApplicationContext<Application>()
    private val selection get() = context.getSharedPreferences("accountData", Context.MODE_PRIVATE)

    @Before fun startAsGuest() {
        LitePal.initialize(context)
        LitePal.deleteAll(Account::class.java)
        selection.edit().clear().commit()
        AccountUtil.init(context)
    }

    private fun verified(uid: String, session: String) = Account(
        uid = uid, name = "用户$uid", nameShow = "昵称$uid", bduss = "fixture-$session",
        sToken = "", cookie = "BDUSS=fixture-$session", tbs = "offline-$session",
    )

    @Test fun addingAnotherAccountSelectsItsDurableRowAndRestoresItAtStartup() {
        val previous = AccountUtil.persistLogin(context, verified("101", "first"))
        val added = AccountUtil.persistLogin(context, verified("202", "second"))
        assertTrue(previous.id > 0 && added.id > 0 && previous.id != added.id)
        assertEquals(added.id, selection.getInt("now", -1))
        assertEquals("202", AccountUtil.getUid())
        assertEquals(setOf("101", "202"), AccountUtil.allAccounts.map { it.uid }.toSet())

        AccountUtil.init(context)
        assertEquals(added.id, AccountUtil.currentAccount?.id)
        assertEquals("fixture-second", AccountUtil.getBduss())
        assertTrue(AccountUtil.switchAccount(context, previous.id))
        AccountUtil.init(context)
        assertEquals("101", AccountUtil.getUid())
        assertEquals("fixture-first", AccountUtil.getBduss())
    }

    @Test fun reauthenticatingAnExistingUidRenewsTheSameRecordBeforeSuccess() {
        val old = AccountUtil.persistLogin(context, verified("101", "expired").copy(sToken = "fixture-old-optional"))
        AccountUtil.persistLogin(context, verified("202", "other"))
        val renewed = AccountUtil.persistLogin(context, verified("101", "renewed"))

        assertEquals(old.id, renewed.id)
        assertEquals(2, LitePal.count(Account::class.java))
        assertEquals("fixture-renewed", AccountUtil.currentAccount?.bduss)
        assertEquals("fixture-renewed", AccountUtil.allAccounts.single { it.uid == "101" }.bduss)
        AccountUtil.init(context)
        assertEquals("101", AccountUtil.getUid())
        assertEquals("BDUSS=fixture-renewed", AccountUtil.getCookie())
        assertEquals("", AccountUtil.getSToken())
    }

    @Test fun missingSelectionCannotReactivateAnUnrelatedAccount() {
        val saved = AccountUtil.persistLogin(context, verified("101", "first"))
        assertFalse(AccountUtil.switchAccount(context, Int.MAX_VALUE))
        assertEquals(saved.id, selection.getInt("now", -1))
        assertEquals(saved.id, AccountUtil.currentAccount?.id)

        selection.edit().putInt("now", Int.MAX_VALUE).commit()
        AccountUtil.init(context)
        assertNull(AccountUtil.currentAccount)
        assertEquals(listOf(saved.id), AccountUtil.allAccounts.map { it.id })
        assertTrue(AccountUtil.switchAccount(context, saved.id))
        AccountUtil.init(context)
        assertEquals(saved.id, AccountUtil.currentAccount?.id)
    }

    @Test fun aLateProfileCannotOverwriteTheRenewedSessionOrAnUnrelatedUser() {
        val old = AccountUtil.persistLogin(context, verified("101", "expired"))
        val renewed = AccountUtil.persistLogin(context, verified("101", "renewed"))
        val originalProfile = AccountUtil.currentAccount?.nameShow
        val lateProfile = User(id = 101, nameShow = "过期资料")
        assertTrue(runCatching { AccountUtil.saveProfile(old, lateProfile) }.exceptionOrNull() is TiebaNotLoggedInException)
        val unrelatedProfile = User(id = 202, nameShow = "其他账号")
        assertTrue(runCatching { AccountUtil.saveProfile(renewed, unrelatedProfile) }.exceptionOrNull() is IllegalStateException)

        AccountUtil.init(context)
        assertEquals(renewed.id, AccountUtil.currentAccount?.id)
        assertEquals("fixture-renewed", AccountUtil.getBduss())
        assertEquals(originalProfile, AccountUtil.currentAccount?.nameShow)
        AccountUtil.saveProfile(renewed, User(id = 101, nameShow = "最新昵称"))
        AccountUtil.init(context)
        assertEquals("最新昵称", AccountUtil.currentAccount?.nameShow)
        assertEquals("fixture-renewed", AccountUtil.getBduss())
    }

    @Test fun incompleteLoginPreservesTheSelectedAccountAndAFreshLoginCanRetry() {
        val saved = AccountUtil.persistLogin(context, verified("101", "first"))
        for (invalid in listOf(verified("0", "unsigned"), verified("202", "missing").copy(bduss = ""))) {
            val failure = runCatching { AccountUtil.persistLogin(context, invalid) }.exceptionOrNull()
            assertTrue(failure is IllegalArgumentException)
            assertEquals(saved.id, selection.getInt("now", -1))
            assertEquals(saved.id, AccountUtil.currentAccount?.id)
            assertEquals(listOf(saved.uid), LitePal.findAll(Account::class.java).map { it.uid })
        }
        AccountUtil.persistLogin(context, verified("202", "retry"))
        AccountUtil.init(context)
        assertEquals("202", AccountUtil.getUid())
        assertEquals("fixture-retry", AccountUtil.getBduss())
    }

    @Test fun logoutRestoresOnlyTheRemainingAccountAndThenReturnsToGuest() {
        AccountUtil.persistLogin(context, verified("101", "first"))
        AccountUtil.persistLogin(context, verified("202", "second"))
        AccountUtil.exit(context)
        assertEquals(listOf("101"), AccountUtil.allAccounts.map { it.uid })
        assertEquals("101", AccountUtil.getUid())
        AccountUtil.init(context)
        assertEquals("101", AccountUtil.getUid())

        AccountUtil.exit(context)
        assertTrue(AccountUtil.allAccounts.isEmpty())
        assertNull(AccountUtil.currentAccount)
        AccountUtil.init(context)
        assertFalse(AccountUtil.isLoggedIn())
    }
}
