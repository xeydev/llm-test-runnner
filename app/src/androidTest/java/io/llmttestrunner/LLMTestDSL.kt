    package io.llmttestrunner

import android.content.Context
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import io.llmttestrunner.adapter.ComposeTestAdapter
import io.llmttestrunner.adapter.TestFrameworkAdapter
import io.llmttestrunner.artifact.*
import io.llmttestrunner.llm.APIKeyManager
import io.llmttestrunner.llm.LLMService

/**
 * DSL for creating LLM-powered tests with artifact caching.
 * 
 * Usage:
 * ```
 * fun test() = llmTest(artifact = "path/to/artifact.json") {
 *     name("My Test")
 *     step("Click button")
 *     step("Scroll down")
 *     step("Type text 'hello'")
 * }
 * ```
 */
fun llmTest(
    artifact: String,
    composeTestRule: ComposeTestRule? = null,
    adapter: TestFrameworkAdapter? = null,
    block: TestStepBuilder.() -> Unit
) {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val builder = TestStepBuilder()
    builder.block()
    
    val steps = builder.getSteps()

    // Initialize components
    val artifactManager = ArtifactManager.getDefault(context)
    val actualAdapter = adapter ?: composeTestRule?.let { ComposeTestAdapter(it) }
        ?: throw IllegalArgumentException("Either adapter or composeTestRule must be provided")
    
    // Create orchestrator (LLM service will be created lazily only if needed)
    val orchestrator = LLMTestOrchestrator(
        artifactManager = artifactManager,
        adapter = actualAdapter,
        context = context
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
 * Orchestrates the execution of LLM-powered tests.
 * 
 * Responsible for:
 * 1. Loading existing artifacts
 * 2. Validating if artifacts are up-to-date
 * 3. Generating new artifacts using LLM when needed (lazy initialization)
 * 4. Executing test commands through the adapter
 */
class LLMTestOrchestrator(
    private val artifactManager: ArtifactManager,
    private val adapter: TestFrameworkAdapter,
    private val context: Context
) {
    
    /**
     * Execute a test with the given steps.
     * 
     * If artifact exists and is up-to-date, use it.
     * Otherwise, generate new artifact using LLM.
     */
    fun executeTest(
        artifactPath: String,
        steps: List<String>
    ) {
        println("═══════════════════════════════════════════════════")
        println("🧪 LLM Test: $artifactPath")
        println("═══════════════════════════════════════════════════")
        println("Artifact: $artifactPath")
        println("Steps: ${steps.size}")
        println()
        
        // Load or generate artifact
        val artifact = loadOrGenerateArtifact(artifactPath, steps)
        
        // Execute the test
        executeArtifact(artifact)
        
        println()
        println("═══════════════════════════════════════════════════")
        println("✅ Test completed successfully")
        println("═══════════════════════════════════════════════════")
    }
    
    /**
     * Load existing artifact or generate a new one.
     * LLM service is only created if artifact generation is needed.
     */
    private fun loadOrGenerateArtifact(
        artifactPath: String,
        steps: List<String>
    ): TestArtifact {
        // Try to load existing artifact
        val existingArtifact = artifactManager.loadArtifact(artifactPath)
        
        // Check if artifact is valid and up-to-date
        if (existingArtifact != null && artifactManager.isArtifactValid(existingArtifact, steps)) {
            println("✓ Using existing artifact (up-to-date)")
            println("  Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(existingArtifact.createdAt)}")
            println("  Provider: ${existingArtifact.llmProvider}")
            println()
            return existingArtifact
        }
        
        // Artifact needs regeneration - create LLM service now
        if (existingArtifact != null) {
            println("⚠ Artifact exists but is outdated")
        } else {
            println("ℹ No artifact found")
        }
        println("🤖 Generating new artifact using LLM...")
        println("   Initializing LLM service...")
        
        // Lazy creation: LLM service is only created when needed
        val llmService = APIKeyManager.createAvailableService()
        println("   ✓ LLM service ready")
        println()
        
        val screenStateCapture = ScreenStateCapture()
        val generator = ArtifactGenerator(llmService, screenStateCapture)
        
        val newArtifact = generator.generateArtifact(
            testName = artifactPath,
            steps = steps,
            captureScreenState = true // Enabled: provides UI context to LLM
        )
        
        // Save the artifact
        artifactManager.saveArtifact(artifactPath, newArtifact)
        
        println()
        println("✓ Artifact generated and saved")
        println()
        
        return newArtifact
    }
    
    /**
     * Execute all commands in the artifact.
     */
    private fun executeArtifact(artifact: TestArtifact) {
        println("▶ Executing ${artifact.steps.size} step(s)...")
        println()
        
        artifact.steps.forEachIndexed { index, step ->
            println("Step ${index + 1}/${artifact.steps.size}: ${step.naturalLanguage}")
            println("  Command: ${step.generatedCommand.action} ${step.generatedCommand.parameters}")
            
            try {
                adapter.execute(step.generatedCommand)
                println("  ✓ Success")
            } catch (e: Exception) {
                println("  ✗ Failed: ${e.message}")
                throw e
            }
            
            println()
        }
    }
}
