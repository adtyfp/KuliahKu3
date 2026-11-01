package com.kuliahku.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kuliahku.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AddTaskFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun addTaskWithEmptyTitle_showsError() {
        // Navigasi ke Add Task via debug menu atau asumsi sudah di sana
        composeRule.onNodeWithText("Submit").performClick()
        composeRule.onNodeWithText("Nama tugas wajib diisi.").assertIsDisplayed()
    }

    @Test
    fun addTaskWithValidData_succeeds() {
        composeRule.onNodeWithText("Nama Tugas").performTextInput("Tugas Uji")
        // Pilih deadline via date picker (disederhanakan)
        composeRule.onNodeWithText("Submit").performClick()
        composeRule.onNodeWithText("Tugas disimpan.").assertIsDisplayed()
    }
}