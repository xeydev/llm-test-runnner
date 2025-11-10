package io.llmttestrunner

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kaspersky.components.composesupport.config.withComposeSupport
import com.kaspersky.kaspresso.kaspresso.Kaspresso
import com.kaspersky.kaspresso.testcases.api.testcase.TestCase
import io.llmtest.testrunner.llmTest
import io.llmtest.testrunner.adapter.ComposeTestAdapter
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleLLMTest : TestCase(
    kaspressoBuilder = Kaspresso.Builder.withComposeSupport()
) {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testBasicSubmit() = llmTest(
        artifact = "artifacts/basic_submit.json",
        adapter = ComposeTestAdapter(composeTestRule),
    ) {
        step("Type text 'Hello World'")
        step("Click button")
        step("Verify text 'Last submitted: Hello World'")
    }

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
}

