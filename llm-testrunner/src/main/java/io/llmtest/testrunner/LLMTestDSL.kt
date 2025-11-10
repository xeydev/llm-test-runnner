package io.llmtest.testrunner

import androidx.test.platform.app.InstrumentationRegistry
import io.llmtest.common.models.LLMResponse
import io.llmtest.common.models.TestArtifact
import io.llmtest.common.models.TestStep
import io.llmtest.common.serialization.JsonSerializer
import io.llmtest.testrunner.adapter.TestFrameworkAdapter
import io.llmtest.testrunner.artifact.ArtifactManager
import io.llmtest.testrunner.llm.BridgeLLMService

fun llmTest(
    artifact: String,
    adapter: TestFrameworkAdapter,
    block: TestStepBuilder.() -> Unit
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val builder = TestStepBuilder()
    builder.block()

    val steps = builder.getSteps()

    val artifactManager = ArtifactManager.getDefault(context)

    val orchestrator = LLMTestOrchestrator(
        artifactManager = artifactManager,
        adapter = adapter,
    )

    orchestrator.executeTest(
        artifactPath = artifact,
        steps = steps
    )
}

class TestStepBuilder {
    private val steps = mutableListOf<String>()

    infix fun step(description: String) {
        steps.add(description)
    }

    internal fun getSteps(): List<String> = steps
}

class LLMTestOrchestrator(
    private val artifactManager: ArtifactManager,
    private val adapter: TestFrameworkAdapter,
) {

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

            executeWithRealTimeGeneration(steps, artifactPath)
        }

        println("Test completed\n")
    }

    private fun executeWithRealTimeGeneration(steps: List<String>, artifactPath: String) {
        val bridgeService: BridgeLLMService = BridgeLLMService.create()

        val generatedSteps = mutableListOf<TestStep>()

        steps.forEachIndexed { index, stepDescription ->
            println("[${index + 1}/${steps.size}] $stepDescription")

            val screenContext = adapter.captureScreenState()
            
            val llmResponse: LLMResponse = bridgeService.generateCommand(
                stepDescription, 
                screenContext
            )
            
            if (llmResponse.status != "OK") {
                throw RuntimeException("LLM Error: ${llmResponse.message}")
            }

            val executableCommands = JsonSerializer.convertToTestCommands(llmResponse.actions)

            try {
                executableCommands.forEach { testCommand ->
                    adapter.execute(testCommand)
                }
            } catch (e: Exception) {
                println("  Failed: ${e.message}")
                throw e
            }

            generatedSteps.add(
                TestStep(
                    description = stepDescription,
                    actions = llmResponse.actions
                )
            )
        }

        val artifact = TestArtifact(
            timestamp = System.currentTimeMillis(),
            steps = generatedSteps
        )
        
        bridgeService.saveArtifact(artifactPath, artifact)
    }

    private fun executeArtifact(artifact: TestArtifact) {
        artifact.steps.forEachIndexed { index, step ->
            println("[${index + 1}/${artifact.steps.size}] ${step.description}")

            val executableCommands = JsonSerializer.convertToTestCommands(step.actions)

            try {
                executableCommands.forEach { testCommand ->
                    adapter.execute(testCommand)
                }
            } catch (e: Exception) {
                println("  Failed: ${e.message}")
                throw e
            }
        }
    }

    private fun isArtifactValid(artifact: TestArtifact, expectedSteps: List<String>): Boolean {
        if (artifact.steps.size != expectedSteps.size) return false
        
        return artifact.steps.zip(expectedSteps).all { (step, expected) ->
            step.description.trim() == expected.trim()
        }
    }
}

