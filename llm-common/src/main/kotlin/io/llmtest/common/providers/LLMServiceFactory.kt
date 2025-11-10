package io.llmtest.common.providers

import io.llmtest.common.service.LLMService

/**
 * Factory to create appropriate LLM service based on available API keys
 */
object LLMServiceFactory {
    fun create(): LLMService {
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

