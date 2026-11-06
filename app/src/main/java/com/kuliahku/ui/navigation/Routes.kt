
package com.kuliahku.ui.navigation

object Routes {

    // Introduction
    const val SPLASH = "splash"

    // Authentication
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Main tabs
    const val HOME = "home"
    const val SCAN = "scan"
    const val REFERENCE = "reference"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"

    // Settings
    const val SETTINGS = "settings"
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val SETTINGS_LANGUAGE = "settings_language"
    const val SETTINGS_PRIVACY = "settings_privacy"
    const val NOTIFICATIONS_LIST = "notifications_list"

    // Task
    const val TASKS = "tasks"
    const val ADD_TASK = "add_task"
    const val EDIT_TASK = "edit_task/{taskId}"
    const val SUBMIT_TASK = "submit_task/{taskId}"

    // Notes
    const val NOTES = "notes"

    // Calendar
    const val CALENDAR = "calendar"
    const val ADD_EVENT = "add_event"

    // Finance
    const val FINANCE = "finance"
    const val ADD_TRANSACTION = "add_transaction"
    const val TRANSACTION_DETAIL = "transaction_detail/{txId}"

    // Other
    const val RECORD_AUDIO = "record_audio"
    const val MATERIAL_RESULT = "material_result/{materialId}"
    const val COMPONENT_PREVIEW = "component_preview"

    // Navigation helpers
    fun editTask(taskId: String): String {
        return "edit_task/$taskId"
    }

    fun submitTask(taskId: String): String {
        return "submit_task/$taskId"
    }

    fun materialResult(materialId: String): String {
        return "material_result/$materialId"
    }
}
