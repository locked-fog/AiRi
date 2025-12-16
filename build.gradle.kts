plugins {
    id("org.jetbrains.compose") version "1.9.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20"

    kotlin("jvm") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"

    id("io.gitlab.arturbosch.detekt") version "1.23.8"

    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
}

group = "com.lockedfog.airi"
version = "0.0.1-snapshot"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    google()
}

dependencies {
    implementation("com.github.locked-fog:StreamLLM:v0.4.0") {
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }

    //from https://detekt.dev/
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")

    //from https://github.com/Kotlin/kotlinx.coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")

    // from https://github.com/InsertKoinIO/koin
    implementation("io.insert-koin:koin-core:4.1.1")

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    //from https://www.slf4j.org/
    implementation("org.slf4j:slf4j-api:2.0.16")
    //from https://logback.qos.ch/
    implementation("ch.qos.logback:logback-classic:1.5.22")

    //from https://github.com/Kotlin/kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

    //from https://developer.android.com/jetpack/androidx/releases/room#2.8.4
    val roomVersion = "2.8.4"
    implementation("androidx.room:room-runtime:$roomVersion")
    //from https://developer.android.com/jetpack/androidx/releases/sqlite#2.6.2
    implementation("androidx.sqlite:sqlite-bundled:2.6.2")
    ksp("androidx.room:room-compiler:$roomVersion")

    //from https://jsoup.org/
    implementation("org.jsoup:jsoup:1.21.2")

    //from https://mockk.io/
    implementation("io.mockk:mockk:1.14.7")

    @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
    testImplementation(compose.desktop.uiTestJUnit4)
    testImplementation(kotlin("test"))
}

detekt {
    toolVersion = "1.23.8"
    config.setFrom(file("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
    autoCorrect = true
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

kotlin {
    jvmToolchain(21)
}