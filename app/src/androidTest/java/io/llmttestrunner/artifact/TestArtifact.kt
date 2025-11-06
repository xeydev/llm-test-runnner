package io.llmttestrunner.artifact

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File

/**
 * Represents a test artifact containing steps and their generated commands.
 * This is the structure that gets saved to YAML/JSON files.
 */
data class TestArtifact(
    val createdAt: Long = System.currentTimeMillis(),
    val steps: List<TestStep> = emptyList()
)

/**
 * Represents a single test step with its natural language description
 * and the generated command.
 */
data class TestStep(
    val naturalLanguage: String,
    val generatedCommands: List<TestCommand>,
    val screenContext: ScreenContext
)

/**
 * Screen context captured at the time of artifact generation.
 * This helps LLM understand the UI structure.
 */
data class ScreenContext(
    val timestamp: Long,
    val viewHierarchy: String,
)

/**
 * Represents a test command with action, optional value, and matcher.
 */
data class TestCommand(
    val action: ActionType,
    val value: String? = null,
    val matcher: Matcher
)

/**
 * Matcher to identify UI elements.
 */
data class Matcher(
    val type: MatcherType,
    val value: String
)

/**
 * Available matcher types for identifying UI elements.
 */
enum class MatcherType {
    TEST_TAG,
    HIERARCHY,
    TEXT,
    CONTENT_DESCRIPTION
}

/**
 * Available action types for test commands.
 */
enum class ActionType {
    CLICK,
    LONG_CLICK,
    DOUBLE_CLICK,
    TYPE_TEXT,
    CLEAR_TEXT,
    SCROLL_TO,
    ASSERT_VISIBLE,
    ASSERT_TEXT,
    ASSERT_CONTAINS
}

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
         * 
         * Note: With the bridge architecture, artifacts are primarily managed 
         * by the bridge server which saves them to the project directory.
         * This manager is kept for backward compatibility and local caching.
         */
        fun getDefault(context: android.content.Context): ArtifactManager {
            // Use /data/local/tmp for temporary storage on device
            // The bridge server handles persistent storage in project directory
            val artifactsDir = File("/data/local/tmp/llm_test_artifacts")
            return ArtifactManager(artifactsDir)
        }
    }
}

