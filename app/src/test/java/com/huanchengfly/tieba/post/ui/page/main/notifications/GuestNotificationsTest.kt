package com.huanchengfly.tieba.post.ui.page.main.notifications

import android.app.Application
import android.view.View
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavOptionsBuilder
import com.huanchengfly.tieba.post.App
import com.huanchengfly.tieba.post.R
import com.huanchengfly.tieba.post.ui.AppContextRule
import com.huanchengfly.tieba.post.ui.captureReviewImage
import com.huanchengfly.tieba.post.ui.common.theme.compose.TiebaLiteTheme
import com.huanchengfly.tieba.post.ui.page.ProvideNavigator
import com.huanchengfly.tieba.post.ui.page.destinations.LoginPageDestination
import com.huanchengfly.tieba.post.utils.AccountUtil.LocalAccount
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.navigation.EmptyDestinationsNavigator
import com.ramcosta.composedestinations.spec.Direction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class, qualifiers = "zh-rCN-w411dp-h923dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GuestNotificationsTest {
    @get:Rule(order = 0) val appContext = AppContextRule()
    @get:Rule(order = 1) val compose = createComposeRule()

    @Test fun guestOnTheMainTabCanOpenLogin() = checkGuest(standalone = false)
    @Test fun guestFollowingAMentionLinkCanOpenLogin() = checkGuest(standalone = true)

    private fun checkGuest(standalone: Boolean) {
        lateinit var host: View
        val routes = mutableListOf<String>()
        val navigator = object : DestinationsNavigator by EmptyDestinationsNavigator {
            override fun navigate(direction: Direction, onlyIfResumed: Boolean, builder: NavOptionsBuilder.() -> Unit) {
                routes += direction.route
            }
        }
        compose.setContent {
            val view = LocalView.current
            SideEffect { host = view }
            CompositionLocalProvider(LocalAccount provides null) {
                TiebaLiteTheme {
                    Surface(Modifier.fillMaxSize()) {
                        ProvideNavigator(navigator) {
                            if (standalone) NotificationsPage(navigator, initialTab = 1) else NotificationsPage()
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("登录后查看消息").assertIsDisplayed()
        compose.onNodeWithText(App.INSTANCE.getString(R.string.title_reply_me)).assertDoesNotExist()
        compose.onNodeWithText(App.INSTANCE.getString(R.string.title_at_me)).assertDoesNotExist()
        if (!standalone) compose.captureReviewImage(host, "notifications-guest-standard-phone.png")
        compose.onNodeWithText("去登录").performClick()
        assertEquals(listOf(LoginPageDestination.route), routes)
    }
}
