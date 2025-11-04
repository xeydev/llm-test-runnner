package io.llmttestrunner.adapter

import io.llmttestrunner.artifact.TestCommand
import io.llmttestrunner.artifact.ActionType

/**
 * Interface for test framework adapters.
 * 
 * Implementations provide integration with different testing frameworks
 * like Espresso, Kaspresso, Compose Testing, Robolectric, etc.
 */
interface TestFrameworkAdapter {
    
    /**
     * Execute a test command using the underlying test framework.
     */
    fun execute(command: TestCommand)
    
    /**
     * Check if this adapter supports the given action type.
     */
    fun supports(actionType: ActionType): Boolean
    
    /**
     * Get the name of the testing framework this adapter supports.
     */
    fun getFrameworkName(): String
}

/**
 * Base implementation with common command execution logic.
 */
abstract class BaseTestFrameworkAdapter : TestFrameworkAdapter {
    
    override fun execute(command: TestCommand) {
        println("▶ Executing ${command.action}: ${command.parameters}")
        
        try {
            when (command.action) {
                ActionType.CLICK -> executeClick(command.parameters)
                ActionType.TYPE_TEXT -> executeTypeText(command.parameters)
                ActionType.CLEAR_TEXT -> executeClearText(command.parameters)
                ActionType.SCROLL_DOWN -> executeScrollDown(command.parameters)
                ActionType.SCROLL_UP -> executeScrollUp(command.parameters)
                ActionType.SCROLL_TO_ELEMENT -> executeScrollToElement(command.parameters)
                ActionType.VERIFY_TEXT -> executeVerifyText(command.parameters)
                ActionType.VERIFY_VISIBLE -> executeVerifyVisible(command.parameters)
                ActionType.VERIFY_NOT_VISIBLE -> executeVerifyNotVisible(command.parameters)
                ActionType.WAIT -> executeWait(command.parameters)
                ActionType.CUSTOM -> executeCustom(command.parameters)
            }
            
            // Small delay for stability
            Thread.sleep(300)
        } catch (e: Exception) {
            throw TestExecutionException("Failed to execute ${command.action}: ${e.message}", e)
        }
    }
    
    protected abstract fun executeClick(params: Map<String, String>)
    protected abstract fun executeTypeText(params: Map<String, String>)
    protected abstract fun executeClearText(params: Map<String, String>)
    protected abstract fun executeScrollDown(params: Map<String, String>)
    protected abstract fun executeScrollUp(params: Map<String, String>)
    protected abstract fun executeScrollToElement(params: Map<String, String>)
    protected abstract fun executeVerifyText(params: Map<String, String>)
    protected abstract fun executeVerifyVisible(params: Map<String, String>)
    protected abstract fun executeVerifyNotVisible(params: Map<String, String>)
    
    protected open fun executeWait(params: Map<String, String>) {
        val seconds = params["seconds"]?.toIntOrNull() ?: 1
        Thread.sleep(seconds * 1000L)
    }
    
    protected open fun executeCustom(params: Map<String, String>) {
        throw UnsupportedOperationException("Custom action not implemented")
    }
}

/**
 * Exception thrown when test execution fails.
 */
class TestExecutionException(message: String, cause: Throwable? = null) : Exception(message, cause)

