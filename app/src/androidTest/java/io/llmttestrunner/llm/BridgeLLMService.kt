package io.llmttestrunner.llm

import io.llmttestrunner.artifact.ScreenContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BridgeLLMService(
    private val bridgeUrl: String = "http://localhost:8888"
) : LLMService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /**
         * Create bridge service with appropriate URL based on environment
         */
        fun create(): BridgeLLMService {
            val url = "http://localhost:37546" // Works through ADB reverse

            val bridgeService = BridgeLLMService(url)
            if (!bridgeService.checkHealth()) {
                throw RuntimeException("Bridge service not responding")
            }
            return bridgeService
        }
    }

    /**
     * Generate command with screen context (used during test execution)
     */
    override fun generateCommand(
        step: String,
        screenContext: ScreenContext
    ): JSONArray {
        val endpoint = "$bridgeUrl/generate-command"

        try {
            val json = JSONObject().apply {
                put("userStep", step)
                put("screenHierarchy", screenContext.viewHierarchy)
            }

            val response = makePostRequest(endpoint, json.toString())
            val responseJson = JSONObject(response)

            if (responseJson.getString("status") != "OK") {
                throw RuntimeException(responseJson.getString("message"))
            } else {
                val commands = responseJson.getJSONArray("actions")
                return commands
            }
        } catch (e: Exception) {
            println("Bridge error: ${e.message}")
            throw e
        }
    }

    /**
     * Check if bridge service is healthy
     */
    fun checkHealth(): Boolean {
        return try {
            val response = makeGetRequest("$bridgeUrl/health")
            val json = JSONObject(response)
            json.getString("status") == "ok"
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Make HTTP GET request
     */
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

    /**
     * Make HTTP POST request
     */
    private fun makePostRequest(urlString: String, jsonBody: String): String {
        val requestBody = jsonBody.toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(urlString)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw RuntimeException("HTTP error: ${response.code}")
            }
            return response.body?.string() ?: throw RuntimeException("Empty response body")
        }
    }
}

