package com.kuliahku.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuliahku.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun loginWithEmptyFields_showsValidationError() {
        // Asumsi sudah di WelcomeScreen
        composeRule.onNodeWithText("Ayo Kita Mulai").performClick()
        composeRule.onNodeWithText("Login").performClick()
        composeRule.onNodeWithText("Email/NIM dan password wajib diisi.").assertIsDisplayed()
    }

    @Test
    fun loginWithInvalidEmail_showsFormatError() {
        composeRule.onNodeWithText("Email/NIM").performTextInput("invalid")
        composeRule.onNodeWithText("Format email tidak valid.").assertIsDisplayed()
    }
}