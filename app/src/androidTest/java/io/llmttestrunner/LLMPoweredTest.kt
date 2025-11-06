package io.llmttestrunner

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.llmttestrunner.adapter.ComposeTestAdapter
import io.llmttestrunner.adapter.TestFrameworkAdapter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Tests demonstrating artifact-based LLM testing.
 *
 * Key features:
 * - Natural language steps are converted to structured commands by LLM
 * - Commands are cached in artifact files (JSON)
 * - Subsequent test runs use cached artifacts (no LLM calls)
 * - Artifacts regenerate automatically when steps change
 */
@RunWith(AndroidJUnit4::class)
class LLMPoweredTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Test 1: Basic text input and submission
     */
    @Test
    fun testBasicSubmit() = llmTest(
        artifact = "artifacts/basic_submit.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Type text 'Hello World'")
        step("Click button")
        step("Verify text 'Last submitted: Hello World'")
    }

    /**
     * Test 2: Multiple submissions
     */
    @Test
    fun testMultipleSubmissions() = llmTest(
        artifact = "artifacts/multiple_submissions.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Type text 'First message'")
        step("Click button")
        step("Verify text 'Last submitted: First message'")
        step("Type text 'Second message'")
        step("Click button")
        step("Verify text 'Last submitted: Second message'")
    }

    /**
     * Test 3: Scrolling functionality
     */
    @Test
    fun testScrolling() = llmTest(
        artifact = "artifacts/scrolling.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Scroll down")
        step("Scroll down")
        step("Scroll until 'Submit' visible")
        step("Scroll up")
    }

    /**
     * Test 4: Scroll and submit workflow
     */
    @Test
    fun testScrollAndSubmit() = llmTest(
        artifact = "artifacts/scroll_submit.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Type text 'Test message'")
        step("Click button")
        step("Verify text 'Last submitted: Test message'")
        step("Scroll down")
        step("Scroll until 'Submit' visible")
    }

    /**
     * Test 5: Empty text field submission
     */
    @Test
    fun testEmptySubmit() = llmTest(
        artifact = "artifacts/empty_submit.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Click button")
        step("Verify text 'Last submitted: '")
    }

    /**
     * Test 6: Long text input
     */
    @Test
    fun testLongTextInput() = llmTest(
        artifact = "artifacts/long_text.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Type text 'This is a very long text message that we want to test'")
        step("Click button")
        step("Verify text 'Last submitted: This is a very long text message that we want to test'")
    }

    /**
     * Test 7: Text with special characters
     */
    @Test
    fun testSpecialCharacters() = llmTest(
        artifact = "artifacts/special_chars.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Type text 'Test@123!#'")
        step("Click button")
        step("Verify text 'Last submitted: Test@123!#'")
    }

    /**
     * Test 8: Complete workflow with waiting
     */
    @Test
    fun testWorkflowWithWait() = llmTest(
        artifact = "artifacts/workflow_wait.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Type text 'Step 1'")
        step("Click button")
        step("Wait 1")
        step("Type text 'Step 2'")
        step("Click button")
        step("Verify text 'Last submitted: Step 2'")
    }

    /**
     * Test 9: Verify text field is visible
     */
    @Test
    fun testUIElementsVisible() = llmTest(
        artifact = "artifacts/ui_elements.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Verify visible textField")
        step("Verify visible submitButton")
    }

    /**
     * Test 10: Natural language style commands
     */
    @Test
    fun testNaturalLanguageCommands() = llmTest(
        artifact = "artifacts/natural_language.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Please type 'Natural language test'")
        step("Now click the submit button")
        step("Check that 'Last submitted: Natural language test' is displayed and visible to user")
    }

}

