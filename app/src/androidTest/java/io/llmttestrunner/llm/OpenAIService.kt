package io.llmttestrunner.llm

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI GPT implementation of LLMService.
 * 
 * Uses OpenAI's Chat Completions API to parse natural language into test commands.
 * Supports GPT-4, GPT-4-turbo, GPT-3.5-turbo models.
 */
class OpenAIService(private val config: LLMConfig) : LLMService {
    
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
        
        val response = callOpenAI(prompt)
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
        
        return callOpenAI(prompt).trim()
    }
    
    override fun validateCommand(command: String): Boolean {
        // Basic validation - could be enhanced with LLM
        val validPrefixes = listOf(
            "click", "tap", "type", "enter", "scroll", 
            "verify", "assert", "wait"
        )
        return validPrefixes.any { command.lowercase().startsWith(it) }
    }
    
    private fun callOpenAI(userPrompt: String): String {
        val request = OpenAIRequest(
            model = config.model,
            messages = listOf(
                Message("system", AvailableCommands.SYSTEM_PROMPT),
                Message("user", userPrompt)
            ),
            temperature = config.temperature,
            maxTokens = config.maxTokens
        )
        //asd
        val json = gson.toJson(request)
        val body = json.toRequestBody(mediaType)
        
        val httpRequest = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            client.newCall(httpRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    throw IOException("OpenAI API error: ${response.code} - $errorBody")
                }
                
                val responseBody = response.body?.string() 
                    ?: throw IOException("Empty response from OpenAI")
                
                val openAIResponse = gson.fromJson(responseBody, OpenAIResponse::class.java)
                return openAIResponse.choices.firstOrNull()?.message?.content?.trim() 
                    ?: throw IOException("No content in OpenAI response")
            }
        } catch (e: Exception) {
            throw IOException("Failed to call OpenAI API: ${e.message}", e)
        }
    }
}

// Data classes for OpenAI API
private data class OpenAIRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double,
    @SerializedName("max_tokens")
    val maxTokens: Int
)

private data class Message(
    val role: String,
    val content: String
)

private data class OpenAIResponse(
    val id: String,
    val choices: List<Choice>,
    val usage: Usage?
)

private data class Choice(
    val message: Message,
    @SerializedName("finish_reason")
    val finishReason: String
)

private data class Usage(
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)

