package com.huanchengfly.tieba.post.utils

import android.content.Context
import android.content.Intent
import android.util.Log
import android.webkit.CookieManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.api.TiebaApi
import com.huanchengfly.tieba.post.api.models.LoginBean
import com.huanchengfly.tieba.post.api.models.protos.User
import com.huanchengfly.tieba.post.api.retrofit.exception.TiebaNotLoggedInException
import com.huanchengfly.tieba.post.arch.GlobalEvent
import com.huanchengfly.tieba.post.arch.emitGlobalEvent
import com.huanchengfly.tieba.post.models.database.Account
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import org.litepal.LitePal.findAll
import org.litepal.LitePal.where
import org.litepal.extension.findFirst
import java.util.UUID

@Stable
object AccountUtil {
    const val TAG = "AccountUtil"
    const val ACTION_SWITCH_ACCOUNT = "com.huanchengfly.tieba.post.action.SWITCH_ACCOUNT"

    val LocalAccount = staticCompositionLocalOf<Account?> { null }
    val AllAccounts = staticCompositionLocalOf<List<Account>> { emptyList() }

    private data class Accounts(val selected: Account?, val all: List<Account>)
    private var accounts by mutableStateOf(Accounts(null, emptyList()))
    private val accountScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val currentAccount get() = accounts.selected
    val allAccounts get() = accounts.all

    @Composable
    fun LocalAccountProvider(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalAccount provides currentAccount, AllAccounts provides allAccounts, content = content)
    }

    private fun selection(context: Context) = context.getSharedPreferences("accountData", Context.MODE_PRIVATE)

    @Synchronized
    fun init(context: Context) {
        val rows = findAll(Account::class.java)
        val id = selection(context).getInt("now", -1)
        accounts = Accounts(rows.firstOrNull { it.id == id }, rows)
    }

    @JvmStatic
    fun getLoginInfo(): Account? = currentAccount

    @JvmStatic
    fun <T> getAccountInfo(getter: Account.() -> T): T? = currentAccount?.getter()

    @JvmStatic
    fun getAccountInfoByUid(uid: String): Account? = where("uid = ?", uid).findFirst<Account>()

    @JvmStatic
    fun isLoggedIn(): Boolean = currentAccount != null

    private fun select(context: Context, selected: Account?, rows: List<Account>): Boolean {
        val prefs = selection(context).edit()
        if (selected == null) prefs.remove("now") else prefs.putInt("now", selected.id)
        if (!prefs.commit()) return false
        accounts = Accounts(selected, rows)
        context.sendBroadcast(Intent(ACTION_SWITCH_ACCOUNT).setPackage(context.packageName))
        accountScope.emitGlobalEvent(GlobalEvent.AccountSwitched)
        return true
    }

    @JvmStatic
    @Synchronized
    fun switchAccount(context: Context, id: Int): Boolean {
        val rows = findAll(Account::class.java)
        val selected = rows.firstOrNull { it.id == id } ?: return false
        return select(context, selected, rows)
    }

    suspend fun completeLogin(context: Context, bduss: String, sToken: String, cookie: String): Account =
        withContext(Dispatchers.IO) {
            val verified = verifyAccount(bduss, sToken, cookie)
            ensureActive()
            val saved = persistLogin(context.applicationContext, verified)
            flowOf(saved).onEach(::refreshDetails)
                .catch { Log.w(TAG, "Could not refresh optional account details") }
                .launchIn(accountScope)
            saved
        }

    private suspend fun verifyAccount(bduss: String, sToken: String, cookie: String): Account {
        require(bduss.isNotBlank()) { "请先完成百度登录" }
        val result = TiebaApi.getInstance().loginFlow(bduss, sToken).first()
        return result.verifiedAccount(bduss, sToken, cookie)
    }

    @Synchronized
    internal fun persistLogin(context: Context, verified: Account): Account {
        require(verified.uid.toLongOrNull()?.let { it > 0 } == true && verified.bduss.isNotBlank())
        val row = getAccountInfoByUid(verified.uid) ?: Account(uid = verified.uid)
        saveVerifiedSession(row, verified)
        val rows = findAll(Account::class.java)
        val saved = rows.first { it.uid == verified.uid && it.id > 0 }
        check(select(context, saved, rows)) { "未能保存当前账号，请重试" }
        return saved
    }

    private fun saveVerifiedSession(row: Account, verified: Account) {
        row.bduss = verified.bduss
        row.sToken = verified.sToken
        row.cookie = verified.cookie
        row.name = verified.name
        row.tbs = verified.tbs
        if (verified.portrait.isNotBlank()) row.portrait = verified.portrait
        if (!verified.nameShow.isNullOrBlank()) row.nameShow = verified.nameShow
        if (row.uuid.isNullOrBlank()) row.uuid = UUID.randomUUID().toString()
        row.saveThrows()
    }

    fun fetchAccountFlow(account: Account = checkNotNull(currentAccount)): Flow<Account> = flow {
        val verified = verifyAccount(account.bduss, account.sToken, account.cookie)
        if (account.uid != verified.uid) throw TiebaNotLoggedInException("登录账号已变更，请重新登录")
        val saved = synchronized(this@AccountUtil) {
            val row = storedSession(account) ?: throw TiebaNotLoggedInException("登录状态已变更")
            saveVerifiedSession(row, verified)
            publishRows()
            row
        }
        refreshDetails(saved)
        emit(saved)
    }.flowOn(Dispatchers.IO)

    private fun storedSession(expected: Account): Account? = getAccountInfoByUid(expected.uid)?.takeIf {
        it.id == expected.id && it.bduss == expected.bduss && it.sToken == expected.sToken
    }

    @Synchronized
    internal fun saveProfile(expected: Account, user: User): Account {
        val row = storedSession(expected) ?: throw TiebaNotLoggedInException("登录状态已变更")
        check(user.id.toString() == row.uid) { "资料与当前账号不符" }
        row.apply {
            nameShow = user.nameShow.ifBlank { name }
            portrait = user.portrait.ifBlank { portrait }
            intro = user.intro
            sex = user.sex.toString()
            fansNum = user.fans_num.toString()
            postNum = user.post_num.toString()
            threadNum = user.thread_num.toString()
            concernNum = user.concern_num.toString()
            tbAge = user.tb_age
            age = user.birthday_info?.age?.toString()
            birthdayShowStatus = user.birthday_info?.birthday_show_status?.toString()
            birthdayTime = user.birthday_info?.birthday_time?.toString()
            constellation = user.birthday_info?.constellation
            tiebaUid = user.tieba_uid
            loadSuccess = true
            saveThrows()
        }
        publishRows()
        return row
    }

    private fun publishRows() {
        val selectedId = currentAccount?.id
        val rows = findAll(Account::class.java)
        accounts = Accounts(rows.firstOrNull { it.id == selectedId }, rows)
    }

    // Profile and device metadata can arrive after Passport has confirmed the session.
    private suspend fun refreshDetails(account: Account) {
        val zid = flow { emitAll(SofireUtils.fetchZid()) }.catch { }.firstOrNull()
        val user = flow {
            emitAll(TiebaApi.getInstance().getUserInfoFlow(account.uid.toLong(), account.bduss, account.sToken))
        }.catch { }.firstOrNull()?.data_?.user?.takeIf { it.id.toString() == account.uid }
        if (zid.isNullOrBlank() && user == null) return
        synchronized(this) {
            val row = storedSession(account) ?: return
            if (!zid.isNullOrBlank()) row.zid = zid
            if (user != null) {
                if (user.nameShow.isNotBlank()) row.nameShow = user.nameShow
                if (user.portrait.isNotBlank()) row.portrait = user.portrait
            }
            row.saveThrows()
            publishRows()
        }
    }

    fun parseCookie(cookie: String): Map<String, String> {
        return cookie
            .split(";")
            .map { it.trim().split("=") }
            .filter { it.size > 1 }
            .associate { it.first() to it.drop(1).joinToString("=") }
    }

    @Synchronized
    fun exit(context: Context) {
        val old = currentAccount ?: return
        old.delete()
        val remaining = findAll(Account::class.java)
        val next = remaining.firstOrNull()
        val switched = select(context, next, remaining)
        if (!switched) accounts = Accounts(null, remaining)
        CookieManager.getInstance().removeAllCookies(null)
        if (switched && next != null) {
            Toast.makeText(context, "退出登录成功，已切换至账号 " + (next.nameShow ?: next.name), Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.toast_exit_account_success, Toast.LENGTH_SHORT).show()
        }
    }

    fun getSToken(): String? {
        val account = getLoginInfo()
        return account?.sToken
    }

    fun getCookie(): String? {
        val account = getLoginInfo()
        return account?.cookie
    }

    fun getUid(): String? {
        val account = getLoginInfo()
        return account?.uid
    }

    fun getBduss(): String? {
        val account = getLoginInfo()
        return account?.bduss
    }
}

internal fun LoginBean.verifiedAccount(bduss: String, sToken: String, cookie: String): Account {
    val identity = user
    val uid = identity?.id?.toLongOrNull()
    val signedOut = identity?.isLogin == "0" || identity?.isLogin.equals("false", ignoreCase = true)
    if (errorCode != "0" || signedOut || identity == null || uid == null || uid <= 0) {
        throw TiebaNotLoggedInException("贴吧未确认本次登录，请在百度登录页完成验证后重试")
    }
    return Account(
        uid = uid.toString(),
        name = identity.name.orEmpty(),
        nameShow = identity.nameShow?.takeIf { it.isNotBlank() },
        portrait = identity.portrait.orEmpty(),
        bduss = bduss,
        sToken = sToken,
        tbs = anti?.tbs.orEmpty(),
        cookie = cookie,
    )
}
