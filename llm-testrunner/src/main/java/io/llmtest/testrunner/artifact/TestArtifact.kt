package io.llmtest.testrunner.artifact

import io.llmtest.common.models.TestArtifact
import io.llmtest.common.serialization.JsonSerializer
import java.io.File

data class ScreenContext(
    val timestamp: Long,
    val viewHierarchy: String,
)

class ArtifactManager(
    private val artifactBaseDir: File
) {
    init {
        if (!artifactBaseDir.exists()) {
            artifactBaseDir.mkdirs()
        }
    }

    fun loadArtifact(path: String): TestArtifact? {
        val file = resolveArtifactPath(path)
        if (!file.exists()) {
            return null
        }
        
        return try {
            val json = file.readText()
            JsonSerializer.parseArtifact(json)
        } catch (e: Exception) {
            println("⚠ Failed to load artifact: ${e.message}")
            null
        }
    }

    fun isArtifactValid(artifact: TestArtifact?, expectedSteps: List<String>): Boolean {
        if (artifact == null) return false
        if (artifact.steps.size != expectedSteps.size) return false
        
        return artifact.steps.zip(expectedSteps).all { (step, expected) ->
            step.description.trim() == expected.trim()
        }
    }

    private fun resolveArtifactPath(path: String): File {
        return if (File(path).isAbsolute) {
            File(path)
        } else {
            File(artifactBaseDir, path)
        }
    }

    companion object {
        fun getDefault(context: android.content.Context): ArtifactManager {
            val artifactsDir = File("/data/local/tmp/llm_test_artifacts")
            return ArtifactManager(artifactsDir)
        }
    }
}

