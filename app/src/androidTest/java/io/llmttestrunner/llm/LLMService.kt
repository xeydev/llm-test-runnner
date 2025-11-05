package io.llmttestrunner.llm

/**
 * Interface for LLM service providers.
 * 
 * Implementations can use different LLM providers (OpenAI, Anthropic, etc.)
 * to parse natural language into structured test commands.
 */
interface LLMService {

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

