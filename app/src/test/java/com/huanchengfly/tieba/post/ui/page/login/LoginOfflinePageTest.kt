package com.huanchengfly.tieba.post.ui.page.login

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.descendants
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.common.theme.utils.ThemeUtils
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkInfo

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w360dp-h760dp-mdpi")
class LoginOfflinePageTest {
    @get:Rule(order = 0) val app = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    private lateinit var host: View

    private fun browsers() = (host.rootView as ViewGroup).descendants.filterIsInstance<WebView>().toList()

    private fun assertOffline() {
        compose.onNodeWithText("无法打开百度登录页").assertIsDisplayed()
        compose.onNodeWithText("连接网络后即可重试。").assertIsDisplayed()
        compose.onNodeWithText("重试").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, browsers().size) }
    }

    @Suppress("DEPRECATION")
    @Test fun offlineRetriesKeepTheChosenProviderUntilANetworkIsAvailable() {
        val manager = App.INSTANCE.getSystemService(ConnectivityManager::class.java)
        val connectivity = shadowOf(manager)
        connectivity.setActiveNetworkInfo(null)
        connectivity.clearAllNetworks()
        ThemeUtils.init(App.ThemeDelegate)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        compose.setContent {
            val current = LocalView.current
            SideEffect { host = current }
            TiebaLiteTheme {
                CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                    LoginPage(EmptyDestinationsNavigator)
                }
            }
        }
        assertOffline()
        compose.onNodeWithText("重试").performClick()
        assertOffline()
        compose.onNodeWithContentDescription("更多选项").performClick()
        compose.onNodeWithText("使用百度网页版登录").performClick()
        assertOffline()
        compose.onNodeWithText("重试").performClick()
        assertOffline()

        // Android knows about this network but has not yet installed a default route or validated it.
        compose.runOnIdle {
            val wifi = ShadowNetworkInfo.newInstance(
                NetworkInfo.DetailedState.CONNECTED, ConnectivityManager.TYPE_WIFI, 0, true, NetworkInfo.State.CONNECTED,
            )
            connectivity.addNetwork(ShadowNetwork.newInstance(42), wifi)
        }
        compose.onNodeWithText("重试").performClick()
        compose.onNodeWithText("无法打开百度登录页").assertDoesNotExist()
        val browser = compose.runOnIdle {
            browsers().single().also { assertEquals(ALTERNATIVE_LOGIN_URL, shadowOf(it).lastLoadedUrl) }
        }

        // A configuration recomposition must preserve a form the user has already reached.
        val formPage = "https://passport.baidu.com/v2/?login&step=2"
        compose.runOnIdle {
            browser.loadUrl(formPage)
            direction.value = LayoutDirection.Rtl
        }
        compose.runOnIdle {
            assertSame(browser, browsers().single())
            assertEquals(formPage, shadowOf(browser).lastLoadedUrl)
        }
    }
}
