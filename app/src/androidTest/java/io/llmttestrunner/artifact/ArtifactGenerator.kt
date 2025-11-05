package io.llmttestrunner.artifact

import io.llmttestrunner.llm.LLMService

/**
 * Generates test artifacts from natural language steps using LLM.
 * 
 * This component is responsible for:
 * 1. Taking natural language steps and screen context
 * 2. Using LLM to convert steps to structured commands
 * 3. Creating a test artifact with all the information
 */
class ArtifactGenerator(
) {

    /**
     * Parse a command string into a structured TestCommand.
     * 
     * Expected format examples:
     * - "click button"
     * - "type text 'hello world'"
     * - "scroll until 'Submit' visible"
     * - "verify text 'Welcome'"
     */
    fun parseCommandString(commandString: String): TestCommand {
        val lower = commandString.trim().lowercase()
        
        return when {
            // Click commands
            lower.startsWith("click ") || lower.startsWith("tap ") -> {
                val parts = commandString.split(" ", limit = 2)
                if (parts.size < 2) {
                    TestCommand(ActionType.CLICK, mapOf("target" to "submitButton"))
                } else {
                    val target = parts[1].trim().trim('"', '\'')
                    val useText = target.contains(" ") || target.matches(Regex("[A-Z].*"))
                    TestCommand(
                        ActionType.CLICK,
                        mapOf(
                            "target" to target,
                            "useText" to useText.toString()
                        )
                    )
                }
            }
            
            // Type text commands
            lower.startsWith("type ") || lower.startsWith("enter ") -> {
                val text = extractQuotedText(commandString) ?: ""
                val intoIndex = lower.indexOf(" into ")
                
                if (intoIndex > 0) {
                    val target = commandString.substring(intoIndex + 6).trim().trim('"', '\'')
                    TestCommand(
                        ActionType.TYPE_TEXT,
                        mapOf(
                            "text" to text,
                            "target" to target
                        )
                    )
                } else {
                    TestCommand(
                        ActionType.TYPE_TEXT,
                        mapOf("text" to text)
                    )
                }
            }
            
            // Clear text
            lower.startsWith("clear ") -> {
                val target = commandString.substring(6).trim().trim('"', '\'')
                TestCommand(ActionType.CLEAR_TEXT, mapOf("target" to target))
            }
            
            // Scroll commands
            lower.startsWith("scroll until") || lower.startsWith("scroll to") -> {
                val text = extractQuotedText(commandString)
                if (text != null) {
                    TestCommand(ActionType.SCROLL_TO_ELEMENT, mapOf("text" to text))
                } else {
                    TestCommand(ActionType.SCROLL_DOWN)
                }
            }
            lower == "scroll down" -> {
                TestCommand(ActionType.SCROLL_DOWN)
            }
            lower == "scroll up" -> {
                TestCommand(ActionType.SCROLL_UP)
            }
            
            // Verify commands
            lower.startsWith("verify text") || lower.startsWith("assert text") -> {
                val text = extractQuotedText(commandString) ?: ""
                TestCommand(ActionType.VERIFY_TEXT, mapOf("text" to text))
            }
            lower.startsWith("verify visible") || lower.startsWith("assert visible") -> {
                val target = commandString.split(" ", limit = 3).getOrNull(2)?.trim('"', '\'') ?: ""
                TestCommand(ActionType.VERIFY_VISIBLE, mapOf("target" to target))
            }
            lower.startsWith("verify not visible") || lower.startsWith("assert not visible") -> {
                val target = commandString.split(" ", limit = 4).getOrNull(3)?.trim('"', '\'') ?: ""
                TestCommand(ActionType.VERIFY_NOT_VISIBLE, mapOf("target" to target))
            }
            
            // Wait command
            lower.startsWith("wait ") -> {
                val seconds = extractNumber(commandString) ?: 1
                TestCommand(ActionType.WAIT, mapOf("seconds" to seconds.toString()))
            }
            
            else -> {
                // Default fallback - treat as custom command
                TestCommand(ActionType.CUSTOM, mapOf("command" to commandString))
            }
        }
    }
    
    /**
     * Extract text within quotes.
     */
    private fun extractQuotedText(text: String): String? {
        val regex = Regex("['\"]([^'\"]*)['\"]")
        return regex.find(text)?.groupValues?.get(1)
    }
    
    /**
     * Extract a number from text.
     */
    private fun extractNumber(text: String): Int? {
        val regex = Regex("\\d+")
        return regex.find(text)?.value?.toIntOrNull()
    }
}

