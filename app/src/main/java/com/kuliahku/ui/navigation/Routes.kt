package com.kuliahku.ui.navigation

object Routes {
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val SCAN = "scan"
    const val REFERENCE = "reference"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"

    const val TASKS = "tasks"
    const val NOTES = "notes"
    const val CALENDAR = "calendar"
    const val FINANCE = "finance"
    const val ADD_TASK = "add_task"
    const val SUBMIT_TASK = "submit_task/{taskId}"
    const val ADD_EVENT = "add_event"
    const val ADD_TRANSACTION = "add_transaction"
    const val TRANSACTION_DETAIL = "transaction_detail/{txId}"
    const val RECORD_AUDIO = "record_audio"
    const val MATERIAL_RESULT = "material_result/{materialId}"
    const val COMPONENT_PREVIEW = "component_preview"

    fun submitTask(taskId: String) = "submit_task/$taskId"
    fun materialResult(materialId: String) = "material_result/$materialId"
}