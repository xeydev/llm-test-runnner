package io.llmttestrunner.adapter

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import io.llmttestrunner.artifact.ActionType

/**
 * Test framework adapter for Jetpack Compose Testing.
 * 
 * Provides integration with androidx.compose.ui.test APIs.
 */
class ComposeTestAdapter(
    private val composeTestRule: ComposeTestRule
) : BaseTestFrameworkAdapter() {
    
    override fun supports(actionType: ActionType): Boolean {
        return when (actionType) {
            ActionType.CLICK,
            ActionType.TYPE_TEXT,
            ActionType.CLEAR_TEXT,
            ActionType.SCROLL_DOWN,
            ActionType.SCROLL_UP,
            ActionType.SCROLL_TO_ELEMENT,
            ActionType.VERIFY_TEXT,
            ActionType.VERIFY_VISIBLE,
            ActionType.VERIFY_NOT_VISIBLE,
            ActionType.WAIT -> true
            ActionType.CUSTOM -> false
        }
    }
    
    override fun getFrameworkName(): String = "Jetpack Compose Testing"
    
    override fun executeClick(params: Map<String, String>) {
        val target = params["target"] ?: params["tag"] ?: "submitButton"
        val useText = params["useText"]?.toBoolean() ?: false
        
        if (useText) {
            val text = params["text"] ?: target
            composeTestRule.onNodeWithText(text).performClick()
        } else {
            composeTestRule.onNodeWithTag(target).performClick()
        }
    }
    
    override fun executeTypeText(params: Map<String, String>) {
        val text = params["text"] ?: throw IllegalArgumentException("text parameter required")
        val target = params["target"] ?: params["tag"] ?: "textField"
        val clearFirst = params["clearFirst"]?.toBoolean() ?: false
        
        composeTestRule.onNodeWithTag(target).apply {
            if (clearFirst) {
                performTextClearance()
            }
            performTextInput(text)
        }
    }
    
    override fun executeClearText(params: Map<String, String>) {
        val target = params["target"] ?: params["tag"] ?: "textField"
        composeTestRule.onNodeWithTag(target).performTextClearance()
    }
    
    override fun executeScrollDown(params: Map<String, String>) {
        val target = params["target"] ?: params["tag"] ?: "scrollableColumn"
        composeTestRule.onNodeWithTag(target).performTouchInput {
            swipeUp()
        }
    }
    
    override fun executeScrollUp(params: Map<String, String>) {
        val target = params["target"] ?: params["tag"] ?: "scrollableColumn"
        composeTestRule.onNodeWithTag(target).performTouchInput {
            swipeDown()
        }
    }
    
    override fun executeScrollToElement(params: Map<String, String>) {
        val scrollableTarget = params["scrollable"] ?: params["scrollableTag"] ?: "scrollableColumn"
        val elementText = params["text"] ?: params["elementText"]
        val elementTag = params["tag"] ?: params["elementTag"]
        
        when {
            elementText != null -> {
                composeTestRule.onNodeWithTag(scrollableTarget)
                    .performScrollToNode(hasText(elementText))
            }
            elementTag != null -> {
                composeTestRule.onNodeWithTag(scrollableTarget)
                    .performScrollToNode(hasTestTag(elementTag))
            }
            else -> throw IllegalArgumentException("Either text or tag parameter required for scroll to element")
        }
    }
    
    override fun executeVerifyText(params: Map<String, String>) {
        val text = params["text"] ?: throw IllegalArgumentException("text parameter required")
        val shouldExist = params["shouldExist"]?.toBoolean() ?: true
        
        val node = composeTestRule.onNodeWithText(text)
        if (shouldExist) {
            node.assertIsDisplayed()
        } else {
            node.assertDoesNotExist()
        }
    }
    
    override fun executeVerifyVisible(params: Map<String, String>) {
        val target = params["target"] ?: params["tag"] 
            ?: throw IllegalArgumentException("target or tag parameter required")
        val useText = params["useText"]?.toBoolean() ?: false
        
        val node = if (useText) {
            composeTestRule.onNodeWithText(target)
        } else {
            composeTestRule.onNodeWithTag(target)
        }
        
        node.assertIsDisplayed()
    }
    
    override fun executeVerifyNotVisible(params: Map<String, String>) {
        val target = params["target"] ?: params["tag"] 
            ?: throw IllegalArgumentException("target or tag parameter required")
        val useText = params["useText"]?.toBoolean() ?: false
        
        val node = if (useText) {
            composeTestRule.onNodeWithText(target)
        } else {
            composeTestRule.onNodeWithTag(target)
        }
        
        node.assertDoesNotExist()
    }
}

