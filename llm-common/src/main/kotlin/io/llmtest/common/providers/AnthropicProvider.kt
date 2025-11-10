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
 * Anthropic Claude implementation
 */
class AnthropicProvider(private val apiKey: String) : LLMService {
    private val logger = LoggerFactory.getLogger(AnthropicProvider::class.java)

    private val client = HttpClient(CIO) {
        expectSuccess = true
    }

    override suspend fun generateCommand(prompt: String): LLMResponse {
        logger.debug("Sending request to Anthropic API")

        val requestBody = mapOf(
            "model" to "claude-3-5-sonnet-20241022",
            "max_tokens" to 200,
            "temperature" to 0.3,
            "messages" to listOf(
                mapOf(
                    "role" to "user",
                    "content" to "You are a test automation assistant. Convert this natural language test instruction to a structured command:\n\n$prompt"
                )
            )
        )

        val response = client.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(JsonSerializer.gson.toJson(requestBody))
        }

        val responseBody = response.bodyAsText()
        val anthropicResponse = JsonSerializer.gson.fromJson(responseBody, AnthropicResponse::class.java)

        val content = anthropicResponse.content.firstOrNull()?.text?.trim()
            ?: throw RuntimeException("Empty response from Anthropic")

        logger.debug("Anthropic response: $content")

        return JsonSerializer.parseLLMResponse(content)
    }

    private data class AnthropicResponse(val content: List<ContentBlock>)
    private data class ContentBlock(val type: String, val text: String)
}
