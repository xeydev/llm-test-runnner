package io.llmtest.common.service

import io.llmtest.common.models.LLMResponse

interface LLMService {

    suspend fun generateCommand(prompt: String): LLMResponse
}

