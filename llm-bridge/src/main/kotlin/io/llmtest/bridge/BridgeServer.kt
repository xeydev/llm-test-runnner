package io.llmtest.bridge

import io.ktor.http.HttpHeaders
import io.ktor.serialization.gson.gson
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.llmtest.common.providers.AnthropicProvider
import io.llmtest.common.providers.LLMServiceFactory
import io.llmtest.common.providers.OpenAIProvider
import io.llmtest.common.serialization.JsonSerializer
import io.llmtest.common.service.LLMService
import org.slf4j.LoggerFactory
import java.io.File

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
    private val llmService: LLMService
) {
    private val logger = LoggerFactory.getLogger(BridgeService::class.java)
    
    fun getHealth(): HealthResponse {
        return HealthResponse(
            status = "ok",
            openaiConfigured = llmService is OpenAIProvider,
            anthropicConfigured = llmService is AnthropicProvider,
            artifactsDir = config.artifactsDir.absolutePath
        )
    }
    
    suspend fun generateCommand(request: GenerateCommandRequest): String {
        val prompt = buildPrompt(request.userStep, request.screenHierarchy)
        
        logger.info("Generating: ${request.userStep}")
        val llmResponse = llmService.generateCommand(prompt)
        
        return JsonSerializer.serializeLLMResponse(llmResponse)
    }
    
    fun saveArtifact(request: SaveArtifactRequest): SaveArtifactResponse {
        val filename = if (request.testName.endsWith(".json")) {
            request.testName
        } else {
            "${request.testName}.json"
        }
        
        val file = File(config.artifactsDir, filename)
        
        val artifact = JsonSerializer.parseArtifact(request.artifactJson)
        file.writeText(JsonSerializer.serializeArtifact(artifact))
        
        logger.info("Saved: ${file.name}")
        
        return SaveArtifactResponse(
            success = true,
            path = file.absolutePath
        )
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
    val artifactJson: String
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
