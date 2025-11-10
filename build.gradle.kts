// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

// Configuration for bridge server
val bridgeServerPort = 37546
val artifactsDirectory = file("app/src/androidTest/artifacts")

tasks.register<JavaExec>("runBridge") {
    group = "llm-test"
    description = "Run bridge server in current terminal"

    val bridgeProject = project(":llm-bridge")
    dependsOn(bridgeProject.tasks.named("classes"))

    mainClass.set("io.llmtest.bridge.BridgeServerKt")
    classpath = bridgeProject.the<SourceSetContainer>()["main"].runtimeClasspath
    args = listOf(bridgeServerPort.toString(), artifactsDirectory.absolutePath)

    standardInput = System.`in`
    standardOutput = System.out
    errorOutput = System.err
}
