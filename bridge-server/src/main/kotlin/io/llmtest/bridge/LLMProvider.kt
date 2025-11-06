package io.llmtest.bridge

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.gson.*
import org.slf4j.LoggerFactory

/**
 * Interface for LLM service implementations
 */
interface LLMProvider {
    suspend fun generateCommand(prompt: String): String
}

/**
 * Factory to create appropriate LLM service based on available API keys
 */
object LLMServiceFactory {
    fun create(): LLMProvider {
        val openAIKey = System.getenv("OPENAI_API_KEY")
        val anthropicKey = System.getenv("ANTHROPIC_API_KEY")

        return when {
            !openAIKey.isNullOrBlank() -> {
                println("✓ Using OpenAI for command generation")
                OpenAIProvider(openAIKey)
            }
            !anthropicKey.isNullOrBlank() -> {
                println("✓ Using Anthropic Claude for command generation")
                AnthropicProvider(anthropicKey)
            }
            else -> {
                throw RuntimeException("No API keys configured. Set OPENAI_API_KEY or ANTHROPIC_API_KEY environment variable.")
            }
        }
    }
}

/**
 * OpenAI GPT implementation
 */
class OpenAIProvider(private val apiKey: String) : LLMProvider {
    private val logger = LoggerFactory.getLogger(OpenAIProvider::class.java)
    private val gson = Gson()
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
        expectSuccess = true
    }

    override suspend fun generateCommand(prompt: String): String {
        logger.debug("Sending request to OpenAI API")
        val requestBody = OpenAIRequest(
            model = "gpt-4o-mini",
            messages = listOf(
                Message(
                    role = "system",
                    content = Prompts.MASTER_PROMPT
                ),
                Message(role = "user", content = prompt)
            ),
            temperature = 0.3,
            maxTokens = 200
        )

        val response = client.post("https://api.openai.com/v1/chat/completions") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $apiKey")
            setBody(gson.toJson(requestBody))
        }

        val responseBody = response.bodyAsText()
        val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)

        val result = openAIResponse.choices.firstOrNull()?.message?.content?.trim()
            ?: throw RuntimeException("Empty response from OpenAI")
        
        logger.debug("OpenAI response: $result")
        return result
    }

    data class OpenAIRequest(
        val model: String,
        val messages: List<Message>,
        val temperature: Double,
        @SerializedName("max_completion_tokens") val maxTokens: Int
    )

    data class Message(
        val role: String,
        val content: String
    )

    data class OpenAIResponse(
        val choices: List<Choice>
    )

    data class Choice(
        val message: Message
    )
}

/**
 * Anthropic Claude implementation
 */
class AnthropicProvider(private val apiKey: String) : LLMProvider {
    private val logger = LoggerFactory.getLogger(AnthropicProvider::class.java)
    private val gson = Gson()
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            gson()
        }
        expectSuccess = true
    }

    override suspend fun generateCommand(prompt: String): String {
        logger.debug("Sending request to Anthropic API")
        val requestBody = AnthropicRequest(
            model = "claude-3-5-sonnet-20241022",
            maxTokens = 200,
            temperature = 0.3,
            messages = listOf(
                AnthropicMessage(
                    role = "user",
                    content = "You are a test automation assistant. Convert this natural language test instruction to a structured command:\n\n$prompt"
                )
            )
        )

        val response = client.post("https://api.anthropic.com/v1/messages") {
            contentType(ContentType.Application.Json)
            header("x-api-key", apiKey)
            header("anthropic-version", "2023-06-01")
            setBody(gson.toJson(requestBody))
        }

        val responseBody = response.bodyAsText()
        val anthropicResponse = gson.fromJson(responseBody, AnthropicResponse::class.java)

        val result = anthropicResponse.content.firstOrNull()?.text?.trim()
            ?: throw RuntimeException("Empty response from Anthropic")
        
        logger.debug("Anthropic response: $result")
        return result
    }

    data class AnthropicRequest(
        val model: String,
        @SerializedName("max_tokens") val maxTokens: Int,
        val temperature: Double,
        val messages: List<AnthropicMessage>
    )

    data class AnthropicMessage(
        val role: String,
        val content: String
    )

    data class AnthropicResponse(
        val content: List<ContentBlock>
    )

    data class ContentBlock(
        val type: String,
        val text: String
    )
}
