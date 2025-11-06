package io.llmttestrunner.adapter

import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import io.llmttestrunner.artifact.TestCommand
import io.llmttestrunner.artifact.ActionType
import io.llmttestrunner.artifact.ScreenContext

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

    fun captureScreenState(): ScreenContext
}

/**
 * Base implementation with common command execution logic.
 */
abstract class BaseTestFrameworkAdapter : TestFrameworkAdapter {

    override fun execute(command: TestCommand) {
        println("▶ Executing ${command.action} on ${command.matcher}")

        try {
            when (command.action) {
                ActionType.CLICK -> executeClick(command)
                ActionType.LONG_CLICK -> executeLongClick(command)
                ActionType.DOUBLE_CLICK -> executeDoubleClick(command)
                ActionType.TYPE_TEXT -> executeTypeText(command)
                ActionType.CLEAR_TEXT -> executeClearText(command)
                ActionType.SCROLL_TO -> executeScrollTo(command)
                ActionType.ASSERT_VISIBLE -> executeAssertVisible(command)
                ActionType.ASSERT_TEXT -> executeAssertText(command)
                ActionType.ASSERT_CONTAINS -> executeAssertContains(command)
            }

            // Small delay for stability
            Thread.sleep(300)
        } catch (e: Exception) {
            throw TestExecutionException("Failed to execute ${command.action}: ${e.message}", e)
        }
    }

    protected abstract fun executeClick(command: TestCommand)
    protected abstract fun executeLongClick(command: TestCommand)
    protected abstract fun executeDoubleClick(command: TestCommand)
    protected abstract fun executeTypeText(command: TestCommand)
    protected abstract fun executeClearText(command: TestCommand)
    protected abstract fun executeScrollTo(command: TestCommand)
    protected abstract fun executeAssertVisible(command: TestCommand)
    protected abstract fun executeAssertText(command: TestCommand)
    protected abstract fun executeAssertContains(command: TestCommand)

    /**
     * Capture current screen state.
     */
    override fun captureScreenState(): ScreenContext {
        val hierarchy = captureViewHierarchy()

        return ScreenContext(
            timestamp = System.currentTimeMillis(),
            viewHierarchy = hierarchy,
        )
    }

    protected abstract fun captureViewHierarchy(): String
}

/**
 * Exception thrown when test execution fails.
 */
class TestExecutionException(message: String, cause: Throwable? = null) : Exception(message, cause)

