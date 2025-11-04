package io.llmttestrunner.llm

/**
 * Factory for creating LLM service instances.
 */
object LLMProvider {

    enum class Provider {
        OPENAI,
        ANTHROPIC,
        MOCK // For testing without API calls
    }

    /**
     * Create an LLM service instance.
     */
    fun create(provider: Provider, config: LLMConfig): LLMService {
        return when (provider) {
            Provider.OPENAI -> OpenAIService(config)
            Provider.ANTHROPIC -> AnthropicService(config)
            Provider.MOCK -> MockLLMService()
        }
    }

    /**
     * Create OpenAI service with default settings.
     */
    fun createOpenAI(apiKey: String, model: String = "gpt-4o"): LLMService {
        return create(
            Provider.OPENAI,
            LLMConfig(
                apiKey = apiKey,
                model = model,
                temperature = 0.2,
                maxTokens = 500
            )
        )
    }

    /**
     * Create Anthropic service with default settings.
     */
    fun createAnthropic(apiKey: String, model: String = "claude-3-5-sonnet-20241022"): LLMService {
        return create(
            Provider.ANTHROPIC,
            LLMConfig(
                apiKey = apiKey,
                model = model,
                temperature = 0.2,
                maxTokens = 500
            )
        )
    }

    /**
     * Create mock service for testing.
     */
    fun createMock(): LLMService {
        return MockLLMService()
    }
}

/**
 * Mock LLM service for testing without API calls.
 *
 * Uses simple pattern matching to simulate LLM responses.
 */
class MockLLMService : LLMService {

    override fun parseCommands(naturalLanguage: String): List<String> {
        val lower = naturalLanguage.lowercase()
        val commands = mutableListOf<String>()

        // Pattern matching for common scenarios
        when {
            lower.contains("type") && lower.contains("hello") -> {
                commands.add("type text 'hello world'")
            }

            lower.contains("enter") || lower.contains("input") -> {
                val text = extractTextInQuotes(naturalLanguage) ?: "test input"
                commands.add("type text '$text'")
            }
        }

        when {
            lower.contains("click") || lower.contains("press") || lower.contains("tap") -> {
                if (lower.contains("submit") || lower.contains("button")) {
                    commands.add("click button")
                } else {
                    commands.add("click submitButton")
                }
            }
        }

        when {
            lower.contains("scroll") -> {
                if (lower.contains("until") || lower.contains("find")) {
                    val text = extractTextInQuotes(naturalLanguage) ?: "Submit"
                    commands.add("scroll until '$text' visible")
                } else if (lower.contains("down")) {
                    commands.add("scroll down")
                } else if (lower.contains("up")) {
                    commands.add("scroll up")
                } else {
                    commands.add("scroll down")
                }
            }
        }

        when {
            lower.contains("verify") || lower.contains("check") || lower.contains("assert") -> {
                val text = extractTextInQuotes(naturalLanguage)
                    ?: if (lower.contains("submit")) "Last submitted:" else "text"
                commands.add("verify text '$text'")
            }
        }

        when {
            lower.contains("wait") -> {
                val seconds = extractNumber(naturalLanguage) ?: 2
                commands.add("wait $seconds")
            }
        }

        return commands.ifEmpty {
            // Default fallback
            listOf("type text 'test'", "click button")
        }
    }

    override fun parseCommand(instruction: String): String {
        val commands = parseCommands(instruction)
        return commands.firstOrNull() ?: "click button"
    }

    override fun validateCommand(command: String): Boolean {
        val validPrefixes = listOf(
            "click", "tap", "type", "enter", "scroll",
            "verify", "assert", "wait"
        )
        return validPrefixes.any { command.lowercase().startsWith(it) }
    }

    private fun extractTextInQuotes(text: String): String? {
        val regex = Regex("['\"]([^'\"]*)['\"]")
        return regex.find(text)?.groupValues?.get(1)
    }

    private fun extractNumber(text: String): Int? {
        val regex = Regex("\\d+")
        return regex.find(text)?.value?.toInt()
    }
}

/**
 * Helper to get API keys from environment or configuration.
 */
object APIKeyManager {

    /**
     * Get OpenAI API key from environment variable or property.
     */
    fun getOpenAIKey(): String? {
        return System.getenv("OPENAI_API_KEY")
            ?: System.getProperty("openai.api.key")
    }

    /**
     * Get Anthropic API key from environment variable or property.
     */
    fun getAnthropicKey(): String? {
        return System.getenv("ANTHROPIC_API_KEY")
            ?: System.getProperty("anthropic.api.key")
    }

    /**
     * Check if OpenAI key is available.
     */
    fun hasOpenAIKey(): Boolean = !getOpenAIKey().isNullOrBlank()

    /**
     * Check if Anthropic key is available.
     */
    fun hasAnthropicKey(): Boolean = !getAnthropicKey().isNullOrBlank()

    /**
     * Create appropriate LLM service based on available API keys.
     * Falls back to mock service if no keys are available.
     */
    fun createAvailableService(): LLMService {
        return when {
            hasOpenAIKey() -> {
                println("✓ Using OpenAI for command parsing")
                LLMProvider.createOpenAI(getOpenAIKey()!!)
            }

            hasAnthropicKey() -> {
                println("✓ Using Anthropic Claude for command parsing")
                LLMProvider.createAnthropic(getAnthropicKey()!!)
            }

            else -> throw RuntimeException("Set OPENAI_API_KEY or ANTHROPIC_API_KEY to use real LLM")
        }
    }
}

