package com.huanchengfly.tieba.post.ui.common.theme

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalContentAlpha as M2Alpha
import androidx.compose.material.LocalContentColor as M2Color
import androidx.compose.material.LocalTextStyle as M2Style
import androidx.compose.material.MaterialTheme as M2Theme
import androidx.compose.material3.LocalContentColor as M3Color
import androidx.compose.material3.LocalTextStyle as M3Style
import androidx.compose.material3.MaterialTheme as M3Theme
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.junit4.createComposeRule
import com.huanchengfly.tieba.post.ui.common.theme.compose.ProvideUnifiedContentColor
import com.huanchengfly.tieba.post.ui.common.theme.compose.ProvideUnifiedTextStyle
import com.huanchengfly.tieba.post.ui.widgets.compose.Button
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class MaterialInteropTest {
    @get:Rule val compose = createComposeRule()

    @Test fun bothMaterialLibrariesReceiveSameEffectiveAlphaAndTextSize() {
        var oldColor = Color.Unspecified
        var newColor = Color.Unspecified
        var oldSize = 0.sp
        var newSize = 0.sp
        val expected = Color(0xFF234567).copy(alpha = 0.4f)
        compose.setContent {
            M2Theme { M3Theme {
                ProvideUnifiedContentColor(Color(0xFF234567), 0.4f) {
                    ProvideUnifiedTextStyle(TextStyle(fontSize = 21.sp)) {
                        val legacy = M2Color.current.copy(alpha = M2Alpha.current)
                        val modern = M3Color.current
                        val legacySize = M2Style.current.fontSize
                        val modernSize = M3Style.current.fontSize
                        SideEffect {
                            oldColor = legacy; newColor = modern
                            oldSize = legacySize; newSize = modernSize
                        }
                    }
                }
            } }
        }
        compose.runOnIdle {
            assertEquals(expected, oldColor); assertEquals(expected, newColor)
            assertEquals(21.sp, oldSize); assertEquals(21.sp, newSize)
        }
    }

    @Test fun m3ButtonSuppliesItsForegroundToLegacyTextSlots() {
        var legacy = Color.Unspecified
        var modern = Color.Unspecified
        compose.setContent {
            M2Theme { M3Theme {
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF223366), contentColor = Color.White
                )) {
                    val old = M2Color.current.copy(alpha = M2Alpha.current)
                    val new = M3Color.current
                    SideEffect { legacy = old; modern = new }
                    Column {
                        androidx.compose.material.Text("Legacy label")
                        androidx.compose.material3.Text("Native label")
                    }
                }
            } }
        }
        compose.runOnIdle { assertEquals(Color.White, legacy); assertEquals(Color.White, modern) }
    }
}
