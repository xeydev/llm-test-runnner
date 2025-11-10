package io.llmtest.testrunner.llm

import io.llmtest.common.models.LLMResponse
import io.llmtest.common.models.TestArtifact
import io.llmtest.common.serialization.JsonSerializer
import io.llmtest.testrunner.artifact.ScreenContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BridgeLLMService(
    private val bridgeUrl: String = "http://localhost:8888"
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun create(): BridgeLLMService {
            val url = "http://localhost:37546"

            val bridgeService = BridgeLLMService(url)
            if (!bridgeService.checkHealth()) {
                throw RuntimeException("Bridge service not responding")
            }
            return bridgeService
        }
    }

    fun generateCommand(
        step: String,
        screenContext: ScreenContext
    ): LLMResponse {
        val endpoint = "$bridgeUrl/generate-command"

        try {
            val json = JSONObject().apply {
                put("userStep", step)
                put("screenHierarchy", screenContext.viewHierarchy)
            }

            val responseJson = makePostRequest(endpoint, json.toString())

            return JsonSerializer.parseLLMResponse(responseJson)
        } catch (e: Exception) {
            println("Bridge error: ${e.message}")
            throw e
        }
    }

    fun checkHealth(): Boolean {
        return try {
            val response = makeGetRequest("$bridgeUrl/health")
            val json = JSONObject(response)
            json.getString("status") == "ok"
        } catch (e: Exception) {
            false
        }
    }

    fun saveArtifact(testName: String, artifact: TestArtifact) {
        val endpoint = "$bridgeUrl/save-artifact"

        val json = JSONObject().apply {
            put("testName", testName)
            put("artifactJson", JsonSerializer.serializeArtifact(artifact))
        }

        makePostRequest(endpoint, json.toString())
    }

    private fun makeGetRequest(urlString: String): String {
        val request = Request.Builder()
            .url(urlString)
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP error: ${response.code}")
            }
            return response.body?.string() ?: throw RuntimeException("Empty response body")
        }
    }

    private fun makePostRequest(urlString: String, jsonBody: String): String {
        val requestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(urlString)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("Bridge error: ${response.code}")
            }
            return response.body?.string() ?: throw RuntimeException("Empty response body")
        }
    }
}

