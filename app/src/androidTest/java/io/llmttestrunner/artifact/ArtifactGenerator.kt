package io.llmttestrunner.artifact

import io.llmttestrunner.llm.LLMService
import java.util.UUID

/**
 * Generates test artifacts from natural language steps using LLM.
 * 
 * This component is responsible for:
 * 1. Taking natural language steps and screen context
 * 2. Using LLM to convert steps to structured commands
 * 3. Creating a test artifact with all the information
 */
class ArtifactGenerator(
    private val llmService: LLMService,
    private val screenStateCapture: ScreenStateCapture
) {
    
    /**
     * Generate a test artifact from natural language steps.
     * 
     * @param testName The name of the test
     * @param steps List of natural language step descriptions
     * @param captureScreenState Whether to capture screen state for each step
     * @return Generated test artifact
     */
    fun generateArtifact(
        testName: String,
        steps: List<String>,
        captureScreenState: Boolean = true
    ): TestArtifact {
        println("🤖 Generating test artifact for: $testName")
        println("   Steps: ${steps.size}")
        
        val screenContext = if (captureScreenState) {
            try {
                screenStateCapture.captureScreenState()
            } catch (e: Exception) {
                println("⚠ Failed to capture screen state: ${e.message}")
                null
            }
        } else {
            null
        }
        
        val testSteps = steps.mapIndexed { index, naturalLanguage ->
            println("   Processing step ${index + 1}/${steps.size}: $naturalLanguage")
            convertStepToCommand(naturalLanguage, screenContext)
        }
        
        return TestArtifact(
            testName = testName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            llmProvider = llmService.javaClass.simpleName,
            llmModel = "unknown", // Can be enhanced to track model name
            steps = testSteps
        )
    }
    
    /**
     * Convert a single natural language step to a structured command.
     */
    private fun convertStepToCommand(
        naturalLanguage: String,
        screenContext: ScreenContext?
    ): TestStep {
        // Build prompt with screen context
        val prompt = buildPrompt(naturalLanguage, screenContext)
        
        // Use LLM to parse the command
        val commandString = llmService.parseCommand(prompt)
        
        // Convert string command to structured TestCommand
        val testCommand = parseCommandString(commandString)
        
        return TestStep(
            id = UUID.randomUUID().toString(),
            naturalLanguage = naturalLanguage,
            generatedCommand = testCommand,
            screenContext = screenContext
        )
    }
    
    /**
     * Build a prompt for the LLM that includes screen context.
     */
    private fun buildPrompt(naturalLanguage: String, screenContext: ScreenContext?): String {
        val prompt = StringBuilder()
        
        if (screenContext != null && screenContext.visibleElements.isNotEmpty()) {
            prompt.append("Current screen state:\n")
            prompt.append(screenStateCapture.generateScreenSummary(screenContext))
            prompt.append("\n\n")
        }
        
        prompt.append("Convert this test instruction to a structured command:\n")
        prompt.append("\"$naturalLanguage\"\n")
        
        return prompt.toString()
    }
    
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

