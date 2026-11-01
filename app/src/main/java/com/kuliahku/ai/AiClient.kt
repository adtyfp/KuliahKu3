
package com.kuliahku.ai

import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.generationConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiClient @Inject constructor() {

    private val config = generationConfig {
        temperature = 0.4f
        maxOutputTokens = 4096
    }

    private val model: GenerativeModel by lazy {
        Firebase.vertexAI.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = config
        )
    }

    private val jsonModel: GenerativeModel by lazy {
        Firebase.vertexAI.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                temperature = 0.5f
                responseMimeType = "application/json"
                maxOutputTokens = 8192
            }
        )
    }

    fun get(): GenerativeModel = model

    fun jsonModel(): GenerativeModel = jsonModel

    companion object {
        const val MODEL_NAME = "gemini-3.8-flash"
        const val LABEL_AI = "Dibuat oleh Gemini AI"
    }
}
