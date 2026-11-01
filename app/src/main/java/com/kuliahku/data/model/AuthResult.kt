package com.kuliahku.data.model

sealed interface AuthResult {
    data class Success(val uid: String, val isNewUser: Boolean) : AuthResult
    data class Error(val message: String, val cause: Throwable? = null) : AuthResult
}