package io.llmttestrunner.artifact

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Represents a test artifact containing steps and their generated commands.
 * This is the structure that gets saved to YAML/JSON files.
 */
data class TestArtifact(
    val version: String = "1.0",
    val testName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val llmProvider: String = "",
    val llmModel: String = "",
    val steps: List<TestStep> = emptyList()
)

/**
 * Represents a single test step with its natural language description
 * and the generated command.
 */
data class TestStep(
    val id: String,
    val naturalLanguage: String,
    val generatedCommand: TestCommand,
    val screenContext: ScreenContext? = null
)

/**
 * A structured test command that can be executed by framework adapters.
 */
data class TestCommand(
    val action: ActionType,
    val parameters: Map<String, String> = emptyMap()
)

/**
 * Types of actions that can be performed in tests.
 */
enum class ActionType {
    CLICK,
    TYPE_TEXT,
    CLEAR_TEXT,
    SCROLL_DOWN,
    SCROLL_UP,
    SCROLL_TO_ELEMENT,
    VERIFY_TEXT,
    VERIFY_VISIBLE,
    VERIFY_NOT_VISIBLE,
    WAIT,
    CUSTOM
}

/**
 * Screen context captured at the time of artifact generation.
 * This helps LLM understand the UI structure.
 */
data class ScreenContext(
    val timestamp: Long = System.currentTimeMillis(),
    val viewHierarchy: String = "",
    val screenshotPath: String? = null,
    val visibleElements: List<ElementInfo> = emptyList()
)

/**
 * Information about a UI element.
 */
data class ElementInfo(
    val id: String = "",
    val tag: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val className: String = "",
    val bounds: String = "",
    val isClickable: Boolean = false,
    val isEnabled: Boolean = false,
    val isVisible: Boolean = false
)

/**
 * Manager for reading, writing, and validating test artifacts.
 */
class ArtifactManager(
    private val artifactBaseDir: File
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    init {
        if (!artifactBaseDir.exists()) {
            artifactBaseDir.mkdirs()
        }
    }

    /**
     * Load an artifact from file.
     */
    fun loadArtifact(path: String): TestArtifact? {
        val file = resolveArtifactPath(path)
        if (!file.exists()) {
            return null
        }
        
        return try {
            val json = file.readText()
            gson.fromJson(json, TestArtifact::class.java)
        } catch (e: Exception) {
            println("⚠ Failed to load artifact: ${e.message}")
            null
        }
    }

    /**
     * Save an artifact to file.
     */
    fun saveArtifact(path: String, artifact: TestArtifact) {
        val file = resolveArtifactPath(path)
        file.parentFile?.mkdirs()
        
        val json = gson.toJson(artifact)
        file.writeText(json)
        println("✓ Artifact saved to: ${file.absolutePath}")
    }

    /**
     * Check if artifact exists.
     */
    fun artifactExists(path: String): Boolean {
        return resolveArtifactPath(path).exists()
    }

    /**
     * Validate if artifact is up-to-date with given steps.
     * Returns true if the natural language steps match.
     */
    fun isArtifactValid(artifact: TestArtifact?, expectedSteps: List<String>): Boolean {
        if (artifact == null) return false
        if (artifact.steps.size != expectedSteps.size) return false
        
        return artifact.steps.zip(expectedSteps).all { (step, expected) ->
            step.naturalLanguage.trim() == expected.trim()
        }
    }

    /**
     * Resolve artifact path relative to base directory.
     */
    private fun resolveArtifactPath(path: String): File {
        return if (File(path).isAbsolute) {
            File(path)
        } else {
            File(artifactBaseDir, path)
        }
    }

    companion object {
        /**
         * Get default artifact manager for Android instrumentation tests.
         */
        fun getDefault(context: android.content.Context): ArtifactManager {
            val artifactsDir = File(context.getExternalFilesDir(null), "test_artifacts")
            return ArtifactManager(artifactsDir)
        }
    }
}

