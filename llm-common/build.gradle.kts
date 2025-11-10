plugins {
    kotlin("jvm")
}

group = "io.llmtest"
version = "0.1.0"

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Ktor Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    
    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
}

kotlin {
    jvmToolchain(17)
}

