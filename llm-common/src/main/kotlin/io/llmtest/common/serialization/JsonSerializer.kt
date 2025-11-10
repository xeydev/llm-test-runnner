package io.llmtest.common.serialization

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.llmtest.common.models.*

object JsonSerializer {
   
    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    fun parseLLMResponse(json: String): LLMResponse {
        return gson.fromJson(json, LLMResponse::class.java)
    }
    
    fun serializeLLMResponse(response: LLMResponse): String {
        return gson.toJson(response)
    }

    fun parseArtifact(json: String): TestArtifact {
        return gson.fromJson(json, TestArtifact::class.java)
    }
    
    fun serializeArtifact(artifact: TestArtifact): String {
        return gson.toJson(artifact)
    }

    fun convertToTestCommand(action: LLMAction): TestCommand {
        return TestCommand(
            action = ActionType.fromString(action.action),
            value = action.value,
            matcher = Matcher(
                type = MatcherType.fromString(action.matcher.type),
                value = action.matcher.value,
                rationale = action.matcher.rationale 
            )
        )
    }

    fun convertToTestCommands(actions: List<LLMAction>): List<TestCommand> {
        return actions.map { convertToTestCommand(it) }
    }
}

