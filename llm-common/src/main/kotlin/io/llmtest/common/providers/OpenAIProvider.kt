package io.llmtest.common.providers

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.llmtest.common.models.LLMResponse
import io.llmtest.common.serialization.JsonSerializer
import io.llmtest.common.service.LLMService
import org.slf4j.LoggerFactory

/**
 * OpenAI GPT implementation
 */
class OpenAIProvider(private val apiKey: String) : LLMService {
    private val logger = LoggerFactory.getLogger(OpenAIProvider::class.java)

    private val client = HttpClient(CIO) {
        expectSuccess = true
    }

    override suspend fun generateCommand(prompt: String): LLMResponse {
        logger.debug("Sending request to OpenAI API")

        val requestBody = mapOf(
            "model" to "gpt-4o-mini",
            "messages" to listOf(
                mapOf("role" to "system", "content" to Prompts.MASTER_PROMPT),
                mapOf("role" to "user", "content" to prompt)
            ),
            "temperature" to 0.3,
            "max_completion_tokens" to 200
        )

        val response = client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(JsonSerializer.gson.toJson(requestBody))
        }

        val responseBody = response.bodyAsText()
        val openAIResponse = JsonSerializer.gson.fromJson(responseBody, OpenAIResponse::class.java)

        val content = openAIResponse.choices.firstOrNull()?.message?.content?.trim()
            ?: throw RuntimeException("Empty response from OpenAI")

        logger.debug("OpenAI response: $content")

        return JsonSerializer.parseLLMResponse(content)
    }

    private data class OpenAIResponse(val choices: List<Choice>)
    private data class Choice(val message: Message)
    private data class Message(val content: String)
}
