package io.llmtest.common.models

data class LLMAction(
    val action: String,
    val value: String? = null,
    val matcher: LLMMatcher
)

data class LLMMatcher(
    val type: String,        
    val value: String,
    val rationale: String? = null 
)

data class LLMResponse(
    val status: String,
    val actions: List<LLMAction> = emptyList(),
    val message: String? = null
)

