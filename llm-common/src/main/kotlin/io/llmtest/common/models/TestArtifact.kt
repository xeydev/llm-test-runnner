package io.llmtest.common.models

data class TestArtifact(
    val timestamp: Long,
    val steps: List<TestStep>
)

data class TestStep(
    val description: String,
    val actions: List<LLMAction>
)

