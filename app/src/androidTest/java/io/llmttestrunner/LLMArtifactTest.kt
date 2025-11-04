package io.llmttestrunner

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Example tests demonstrating the new artifact-based LLM testing approach.
 * 
 * Key features:
 * 1. Natural language steps defined in test code
 * 2. LLM generates structured commands saved to artifact file
 * 3. Subsequent runs use cached artifact (no LLM calls)
 * 4. LLM regenerates artifact only if steps change
 * 
 * **Setup:**
 * - Set OPENAI_API_KEY or ANTHROPIC_API_KEY environment variable
 * - Or use mock LLM service (automatic fallback)
 */
@RunWith(AndroidJUnit4::class)
class LLMArtifactTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Example 1: Simple submit test
     * 
     * First run: LLM generates artifact
     * Subsequent runs: Uses cached artifact
     */
    @Test
    fun testSimpleSubmit() = llmTest(
        artifact = "simple_submit.json",
        composeTestRule = composeTestRule
    ) {
        step("Type text 'Hello World'")
        step("Click button")
        step("Verify text 'Last submitted: Hello World'")
    }

    /**
     * Example 2: Scroll and submit test
     * 
     * Demonstrates scrolling actions
     */
    @Test
    fun testScrollAndSubmit() = llmTest(
        artifact = "scroll_submit.json",
        composeTestRule = composeTestRule
    ) {
        step("Scroll down")
        step("Scroll until 'Submit' visible")
        step("Scroll up")
        step("Type text 'After scrolling'")
        step("Click button")
        step("Verify text 'Last submitted: After scrolling'")
    }

    /**
     * Example 3: Multi-step workflow
     * 
     * Complex workflow with multiple interactions
     */
    @Test
    fun testComplexWorkflow() = llmTest(
        artifact = "complex_workflow.json",
        composeTestRule = composeTestRule
    ) {
        step("Type text 'Step 1'")
        step("Click button")
        step("Verify text 'Last submitted: Step 1'")
        step("Wait 1")
        step("Type text 'Step 2'")
        step("Click button")
        step("Verify text 'Last submitted: Step 2'")
    }

    /**
     * Example 4: Using Kaspresso extension
     * 
     * Demonstrates integration with Kaspresso TestCase
     */
    @Test
    fun testWithKaspresso() = run {
        llmTest(
            artifact = "kaspresso_test.json",
            composeTestRule = composeTestRule
        ) {
            step("Type text 'Kaspresso rocks'")
            step("Click button")
            step("Verify text 'Last submitted: Kaspresso rocks'")
        }
    }

    /**
     * Example 5: Very natural language
     * 
     * Shows that natural descriptions work well
     */
    @Test
    fun testNaturalLanguage() = llmTest(
        artifact = "natural_language.json",
        composeTestRule = composeTestRule
    ) {
        step("Please type the message 'Testing with natural language'")
        step("Now click the submit button")
        step("Check that the text 'Last submitted: Testing with natural language' appears")
    }

    /**
     * Example 6: Artifact regeneration demo
     * 
     * If you change the steps below, artifact will regenerate automatically.
     * Try changing "Hello" to "Goodbye" and run again.
     */
    @Test
    fun testArtifactRegeneration() = llmTest(
        artifact = "regeneration_demo.json",
        composeTestRule = composeTestRule
    ) {
        step("Type text 'Hello'")  // Change this to trigger regeneration
        step("Click button")
        step("Verify text 'Last submitted: Hello'")
    }
}

