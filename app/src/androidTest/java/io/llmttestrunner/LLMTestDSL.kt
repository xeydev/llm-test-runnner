package io.llmttestrunner

import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.llmttestrunner.adapter.TestFrameworkAdapter
import io.llmttestrunner.artifact.ActionType
import io.llmttestrunner.artifact.ArtifactManager
import io.llmttestrunner.artifact.Matcher
import io.llmttestrunner.artifact.MatcherType
import io.llmttestrunner.artifact.TestArtifact
import io.llmttestrunner.artifact.TestCommand
import io.llmttestrunner.artifact.TestStep
import io.llmttestrunner.llm.BridgeLLMService
import io.llmttestrunner.llm.LLMService
import org.json.JSONArray

fun llmTest(
    artifact: String,
    adapter: TestFrameworkAdapter,
    block: TestStepBuilder.() -> Unit
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val builder = TestStepBuilder()
    builder.block()

    val steps = builder.getSteps()

    // Initialize components
    val artifactManager = ArtifactManager.getDefault(context)

    // Create orchestrator (LLM service will be created lazily only if needed)
    val orchestrator = LLMTestOrchestrator(
        artifactManager = artifactManager,
        adapter = adapter,
    )

    // Execute the test
    orchestrator.executeTest(
        artifactPath = artifact,
        steps = steps
    )
}

/**
 * Builder for test steps.
 */
class TestStepBuilder {
    private val steps = mutableListOf<String>()

    /**
     * Add a test step with natural language description.
     * Usage: step("Click button")
     *
     * Also supports infix notation outside of TestCase context: step "Click button"
     */
    infix fun step(description: String) {
        steps.add(description)
    }

    internal fun getSteps(): List<String> = steps
}

/**
 * Orchestrates the execution of LLM-powered tests using bridge architecture.
 *
 * New approach:
 * 1. Check for existing artifact (use if up-to-date)
 * 2. If regeneration needed: Execute test step-by-step with real-time LLM generation
 * 3. Each step: capture screen → call bridge → execute command
 * 4. Save complete artifact at the end
 *
 * This allows LLM to see actual screen state at each step for better command generation.
 */
class LLMTestOrchestrator(
    private val artifactManager: ArtifactManager,
    private val adapter: TestFrameworkAdapter,
) {

    /**
     * Execute a test with the given steps.
     *
     * If artifact exists and is up-to-date, use it.
     * Otherwise, generate commands in real-time using bridge service.
     */
    fun executeTest(
        artifactPath: String,
        steps: List<String>
    ) {
        println("Test: $artifactPath (${steps.size} steps)")

        val existingArtifact = artifactManager.loadArtifact(artifactPath)

        if (existingArtifact != null && artifactManager.isArtifactValid(existingArtifact, steps)) {
            println("Using cached artifact")
            executeArtifact(existingArtifact)
        } else {
            if (existingArtifact != null) {
                println("Artifact outdated, regenerating...")
            } else {
                println("Generating new artifact...")
            }

            val artifact = executeWithRealTimeGeneration(steps)
            // artifactManager.saveArtifact(artifactPath, artifact)
            println("Artifact saved")
        }

        println("Test completed\n")
    }

    /**
     * Execute test with real-time command generation.
     * Each step captures actual screen state for LLM context.
     */
    private fun executeWithRealTimeGeneration(steps: List<String>): TestArtifact {
        val bridgeService: LLMService = BridgeLLMService.create()

        val generatedSteps = mutableListOf<TestStep>()

        steps.forEachIndexed { index, stepDescription ->
            println("[${index + 1}/${steps.size}] $stepDescription")

            val screenContext = adapter.captureScreenState()
            val commands = bridgeService.generateCommand(stepDescription, screenContext)

            val testCommands = parseCommands(commands)

            try {
                testCommands.forEach { testCommand ->
                    adapter.execute(testCommand)
                }
            } catch (e: Exception) {
                println("  Failed: ${e.message}")
                throw e
            }
            generatedSteps.add(
                TestStep(
                    naturalLanguage = stepDescription,
                    generatedCommands = testCommands,
                    screenContext = screenContext
                )
            )
        }

        return TestArtifact(
            createdAt = System.currentTimeMillis(),
            steps = generatedSteps
        )
    }

    private fun executeArtifact(artifact: TestArtifact) {
        artifact.steps.forEachIndexed { index, step ->
            println("[${index + 1}/${artifact.steps.size}] ${step.naturalLanguage}")

            try {
                step.generatedCommands.forEach {
                    adapter.execute(it)
                }
            } catch (e: Exception) {
                println("  Failed: ${e.message}")
                throw e
            }
        }
    }

    private fun parseCommands(jsonList: JSONArray): List<TestCommand> {
        val testCommands = mutableListOf<TestCommand>()
        for (i in 0 until jsonList.length()) {
            val json = jsonList.getJSONObject(i)
            val matcherJson = json.getJSONObject("matcher")
            val matcher = Matcher(
                type = parseMatcherType(matcherJson.getString("type")),
                value = matcherJson.getString("value")
            )

            testCommands.add(
                TestCommand(
                    action = parseActionType(json.getString("action")),
                    value = json.getString("value"),
                    matcher = matcher
                )
            )
        }
        return testCommands
    }

    private fun parseActionType(action: String): ActionType {
        return when (action) {
            "click" -> ActionType.CLICK
            "longClick" -> ActionType.LONG_CLICK
            "doubleClick" -> ActionType.DOUBLE_CLICK
            "typeText" -> ActionType.TYPE_TEXT
            "clearText" -> ActionType.CLEAR_TEXT
            "scrollTo" -> ActionType.SCROLL_TO
            "assertVisible" -> ActionType.ASSERT_VISIBLE
            "assertText" -> ActionType.ASSERT_TEXT
            "assertContains" -> ActionType.ASSERT_CONTAINS
            else -> throw IllegalArgumentException("Unsupported action: $action")
        }
    }

    private fun parseMatcherType(type: String): MatcherType {
        return when (type) {
            "testTag" -> MatcherType.TEST_TAG
            "hierarchy" -> MatcherType.HIERARCHY
            "text" -> MatcherType.TEXT
            "contentDescription" -> MatcherType.CONTENT_DESCRIPTION
            else -> throw IllegalArgumentException("Unsupported matcher type: $type")
        }
    }
}
