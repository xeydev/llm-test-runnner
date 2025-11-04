package io.llmttestrunner.llm

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Anthropic Claude implementation of LLMService.
 * 
 * Uses Anthropic's Messages API to parse natural language into test commands.
 * Supports Claude 3 models (Opus, Sonnet, Haiku).
 */
class AnthropicService(private val config: LLMConfig) : LLMService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .readTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeout, TimeUnit.MILLISECONDS)
        .build()
    
    private val gson = Gson()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    
    override fun parseCommands(naturalLanguage: String): List<String> {
        val prompt = """
Convert this user request into test commands:
"$naturalLanguage"

Remember:
- One command per line
- Use single quotes for text
- Be specific and actionable
- Return only commands, no explanations
        """.trimIndent()
        
        val response = callAnthropic(prompt)
        return response.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && !it.startsWith("//") }
    }
    
    override fun parseCommand(instruction: String): String {
        val prompt = """
Convert this instruction into ONE test command:
"$instruction"

Return only the command, nothing else.
        """.trimIndent()
        
        return callAnthropic(prompt).trim()
    }
    
    override fun validateCommand(command: String): Boolean {
        val validPrefixes = listOf(
            "click", "tap", "type", "enter", "scroll", 
            "verify", "assert", "wait"
        )
        return validPrefixes.any { command.lowercase().startsWith(it) }
    }
    
    private fun callAnthropic(userPrompt: String): String {
        val request = AnthropicRequest(
            model = config.model,
            maxTokens = config.maxTokens,
            temperature = config.temperature,
            system = AvailableCommands.SYSTEM_PROMPT,
            messages = listOf(
                AnthropicMessage("user", userPrompt)
            )
        )
        
        val json = gson.toJson(request)
        val body = json.toRequestBody(mediaType)
        
        val httpRequest = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", config.apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()
        
        try {
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    throw IOException("Anthropic API error: ${response.code} - $errorBody")
                }
                
                val responseBody = response.body?.string() 
                    ?: throw IOException("Empty response from Anthropic")
                
                val anthropicResponse = gson.fromJson(responseBody, AnthropicResponse::class.java)
                return anthropicResponse.content.firstOrNull()?.text?.trim()
                    ?: throw IOException("No content in Anthropic response")
            }
        } catch (e: Exception) {
            throw IOException("Failed to call Anthropic API: ${e.message}", e)
        }
    }
}

// Data classes for Anthropic API
private data class AnthropicRequest(
    val model: String,
    @SerializedName("max_tokens")
    val maxTokens: Int,
    val temperature: Double,
    val system: String,
    val messages: List<AnthropicMessage>
)

private data class AnthropicMessage(
    val role: String,
    val content: String
)

private data class AnthropicResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerializedName("stop_reason")
    val stopReason: String?,
    val usage: AnthropicUsage?
)

private data class ContentBlock(
    val type: String,
    val text: String
)

private data class AnthropicUsage(
    @SerializedName("input_tokens")
    val inputTokens: Int,
    @SerializedName("output_tokens")
    val outputTokens: Int
)

