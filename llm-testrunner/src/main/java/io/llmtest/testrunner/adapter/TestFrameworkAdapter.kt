package io.llmtest.testrunner.adapter

import io.llmtest.common.models.TestCommand
import io.llmtest.common.models.ActionType
import io.llmtest.testrunner.artifact.ScreenContext

interface TestFrameworkAdapter {

    fun execute(command: TestCommand)

    fun captureScreenState(): ScreenContext
}

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

    override fun captureScreenState(): ScreenContext {
        val hierarchy = captureViewHierarchy()

        return ScreenContext(
            timestamp = System.currentTimeMillis(),
            viewHierarchy = hierarchy,
        )
    }

    protected abstract fun captureViewHierarchy(): String
}

class TestExecutionException(message: String, cause: Throwable? = null) : Exception(message, cause)

