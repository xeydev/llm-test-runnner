package io.llmttestrunner.llm

import io.llmttestrunner.artifact.ScreenContext
import org.json.JSONArray

/**
 * Interface for LLM service providers.
 * 
 * Implementations can use different LLM providers (OpenAI, Anthropic, etc.)
 * to parse natural language into structured test commands.
 */
interface LLMService {

    fun generateCommand(step: String, screenContext: ScreenContext): JSONArray
}

