package io.llmtest.bridge

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.serialization.gson.*
import org.slf4j.LoggerFactory
import java.io.File
import java.security.MessageDigest

/**
 * Main entry point for the LLM Bridge Server
 */
fun main(args: Array<String>) {
    val port = args.getOrNull(0)?.toIntOrNull() !!
    val artifactsPath = args[1]
    
    val config = BridgeConfig(
        port = port,
        artifactsDir = File(artifactsPath).apply { mkdirs() }
    )
    
    startServer(config)
}

fun startServer(config: BridgeConfig) {
    val logger = LoggerFactory.getLogger("BridgeServer")
    
    logger.info("Bridge server starting on port ${config.port}")
    logger.info("Artifacts: ${config.artifactsDir.absolutePath}")
    
    val llmService = LLMServiceFactory.create()
    logger.info("LLM provider: ${llmService.javaClass.simpleName}")
    
    val bridgeService = BridgeService(config, llmService)
    
    embeddedServer(Netty, port = config.port) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }
        
        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
        }
        
        routing {
            get("/health") {
                call.respond(bridgeService.getHealth())
            }
            
            post("/generate-command") {
                val request = call.receive<GenerateCommandRequest>()
                val response = bridgeService.generateCommand(request)
                call.respond(response)
            }
            
            post("/save-artifact") {
                val request = call.receive<SaveArtifactRequest>()
                val response = bridgeService.saveArtifact(request)
                call.respond(response)
            }
            
            post("/reset") {
                bridgeService.reset()
                call.respond(mapOf("success" to true))
            }
        }
    }.start(wait = true)
}

/**
 * Configuration for bridge server
 */
data class BridgeConfig(
    val port: Int,
    val artifactsDir: File
)

/**
 * Main bridge service logic
 */
class BridgeService(
    private val config: BridgeConfig,
    private val llmProvider: LLMProvider
) {
    private val logger = LoggerFactory.getLogger(BridgeService::class.java)
    private val cache = mutableMapOf<String, String>()
    private val gson = Gson()
    
    fun getHealth(): HealthResponse {
        return HealthResponse(
            status = "ok",
            openaiConfigured = llmProvider is OpenAIProvider,
            anthropicConfigured = llmProvider is AnthropicProvider,
            artifactsDir = config.artifactsDir.absolutePath
        )
    }
    
    suspend fun generateCommand(request: GenerateCommandRequest): String {
        val cacheKey = getCacheKey(request.userStep, request.screenHierarchy)
        cache[cacheKey]?.let { cachedCommand ->
            return cachedCommand
        }
        
        val prompt = buildPrompt(request.userStep, request.screenHierarchy)
        
        logger.info("Generating: ${request.userStep}")
        val command = llmProvider.generateCommand(prompt)
        logger.info("→ $command")
        
        cache[cacheKey] = command
        
        return command
    }
    
    fun saveArtifact(request: SaveArtifactRequest): SaveArtifactResponse {
        val filename = if (request.testName.endsWith(".json")) {
            request.testName
        } else {
            "${request.testName}.json"
        }
        
        val file = File(config.artifactsDir, filename)
        file.writeText(gson.toJson(request.artifact))
        
        logger.info("Saved: ${file.name}")
        
        return SaveArtifactResponse(
            success = true,
            path = file.absolutePath
        )
    }
    
    fun reset() {
        // Could clear cache or reset state if needed
    }
    
    private fun getCacheKey(step: String, context: String): String {
        val contextStr = context
        
        val combined = "$step:$contextStr"
        return MessageDigest.getInstance("MD5")
            .digest(combined.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
    
    private fun buildPrompt(step: String, screenContext: String): String {
        return buildString {
            appendLine("`user_step`: $step")
            appendLine("`screen_hierarchy`: $screenContext")
        }
    }
}

// Request/Response models
data class GenerateCommandRequest(
    val userStep: String,
    val screenHierarchy: String,
)

data class SaveArtifactRequest(
    val testName: String,
    val artifact: Map<String, Any>
)

data class SaveArtifactResponse(
    val success: Boolean,
    val path: String
)

data class HealthResponse(
    val status: String,
    val openaiConfigured: Boolean,
    val anthropicConfigured: Boolean,
    val artifactsDir: String
)
