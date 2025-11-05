package io.llmttestrunner

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import io.llmttestrunner.adapter.TestFrameworkAdapter
import io.llmttestrunner.artifact.ArtifactGenerator
import io.llmttestrunner.artifact.ArtifactManager
import io.llmttestrunner.artifact.TestArtifact
import io.llmttestrunner.artifact.TestStep
import io.llmttestrunner.llm.BridgeLLMService

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

            val artifact = executeWithRealTimeGeneration(artifactPath, steps)
            artifactManager.saveArtifact(artifactPath, artifact)
            println("Artifact saved")
        }

        println("Test completed\n")
    }

    /**
     * Execute test with real-time command generation.
     * Each step captures actual screen state for LLM context.
     */
    private fun executeWithRealTimeGeneration(
        testName: String,
        steps: List<String>
    ): TestArtifact {
        val bridgeService = BridgeLLMService.create()

        if (!bridgeService.checkHealth()) {
            throw RuntimeException("Bridge service not responding")
        }

        val generatedSteps = mutableListOf<TestStep>()

        steps.forEachIndexed { index, stepDescription ->
            println("[${index + 1}/${steps.size}] $stepDescription")

            val screenContext = adapter.captureScreenState()
            val commandString = bridgeService.generateCommandWithContext(stepDescription, screenContext)
            
            val generator = ArtifactGenerator()
            val testCommand = generator.parseCommandString(commandString)

            try {
                adapter.execute(testCommand)
            } catch (e: Exception) {
                println("  Failed: ${e.message}")
                throw e
            }

            generatedSteps.add(
                TestStep(
                    id = java.util.UUID.randomUUID().toString(),
                    naturalLanguage = stepDescription,
                    generatedCommand = testCommand,
                    screenContext = screenContext
                )
            )
        }

        return TestArtifact(
            testName = testName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            steps = generatedSteps
        )
    }

    private fun executeArtifact(artifact: TestArtifact) {
        artifact.steps.forEachIndexed { index, step ->
            println("[${index + 1}/${artifact.steps.size}] ${step.naturalLanguage}")

            try {
                adapter.execute(step.generatedCommand)
            } catch (e: Exception) {
                println("  Failed: ${e.message}")
                throw e
            }
        }
    }
}
