package io.llmttestrunner.llm

/**
 * Interface for LLM service providers.
 * 
 * Implementations can use different LLM providers (OpenAI, Anthropic, etc.)
 * to parse natural language into structured test commands.
 */
interface LLMService {
    /**
     * Parse a natural language description into a list of test commands.
     * 
     * @param naturalLanguage The user's description of what to test
     * @return List of executable test commands
     */
    fun parseCommands(naturalLanguage: String): List<String>
    
    /**
     * Parse a single natural language instruction into a specific test command.
     * 
     * @param instruction A single instruction like "I want to type 'hello' in the text box"
     * @return A structured command like "type text 'hello'"
     */
    fun parseCommand(instruction: String): String
    
    /**
     * Validate if a command is properly formatted.
     * 
     * @param command The command to validate
     * @return True if the command is valid
     */
    fun validateCommand(command: String): Boolean
}

/**
 * Configuration for LLM services.
 */
data class LLMConfig(
    val apiKey: String,
    val model: String,
    val temperature: Double = 0.2,
    val maxTokens: Int = 500,
    val timeout: Long = 30000L // 30 seconds
)

/**
 * Available test commands that the LLM can generate.
 */
object AvailableCommands {
    const val SYSTEM_PROMPT = """
You are a test automation expert that converts natural language into structured test commands.

Available commands:
1. Click commands:
   - "click button" - Clicks the submit button
   - "click <testTag>" - Clicks any element by its test tag
   - "tap <testTag>" - Alternative click command

2. Text input commands:
   - "type text '<text>'" - Types text into the default text field
   - "type '<text>' into <testTag>" - Types text into a specific field
   - "enter '<text>'" - Alternative text input command

3. Scroll commands:
   - "scroll until '<text>' visible" - Scrolls until text appears
   - "scroll down" - Scrolls down
   - "scroll up" - Scrolls up

4. Verification commands:
   - "verify text '<text>'" - Verifies text is displayed
   - "verify visible <testTag>" - Verifies element is visible
   - "assert text '<text>'" - Alternative verification

5. Wait commands:
   - "wait <seconds>" - Waits for specified seconds

Rules:
- Always use single quotes for text values
- Test tags don't need quotes unless they contain spaces
- Keep commands simple and focused on one action
- Return only the commands, one per line
- No explanations, just the commands
"""
}

