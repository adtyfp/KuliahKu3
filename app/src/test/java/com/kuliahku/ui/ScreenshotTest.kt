package com.kuliahku.ui

import androidx.compose.ui.test.junit4.createComposeRule
import com.github.takahirom.roborazzi.captureRoboImage
import com.kuliahku.ui.components.PrimaryPillButton
import com.kuliahku.ui.components.PriorityBadge
import com.kuliahku.ui.theme.KuliahkuTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33])
class ScreenshotTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun primaryPillButton_light() {
        composeRule.setContent {
            KuliahkuTheme(darkTheme = false) {
                PrimaryPillButton(text = "Login", onClick = {})
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/primary_button_light.png")
    }

    @Test
    fun priorityBadge_dark() {
        composeRule.setContent {
            KuliahkuTheme(darkTheme = true) {
                PriorityBadge("High")
            }
        }
        composeRule.onRoot().captureRoboImage("build/screenshots/priority_badge_dark.png")
    }
}