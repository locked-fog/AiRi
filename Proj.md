# 📘 Project AiRi 开发手册与执行计划书 (v5.0 Foundation)

**项目名称**: AiRi (Artificial Intelligence Recurring Interaction)
**项目代号**: Project_AiRi
**核心定义**: 一个运行在桌面后台的伴侣型 AI Agent。架构上严格区分**核心意识 (Kernel)** 与 **外围感知 (Peripherals)**，通过标准化的协议进行交互。
**技术底座**: Kotlin Multiplatform (JVM), Compose for Desktop, StreamLLM

---

## 1. 系统架构设计 (System Architecture)

系统被划分为 **Kernel (核心域)** 和 **Peripherals (外围域)** 两大板块。

### 1.1 核心域 (Kernel Domain)

这是 AiRi 的生命维持系统，必须保持最小化、高稳定性和原子性。

| 模块 | 职责 |
| --- | --- |
| **Config Manager** | **配置中枢**。管理模型 API Key、系统提示词、外围开关等。文件持久化存储。 |
| **Main Actor** | **主脑**。运行 StreamLLM 循环，只处理标准化的 `InputEvent`，输出 `Thought` 或 `ToolCall`。 |
| **Speaker Actor** | **表达**。负责将主脑意图转化为自然语言，流式推送到 UI。 |
| **UI Actor** | **界面**。渲染设置页、聊天页和控制台。 |
| **Event Bus** | **总线**。基于 Kotlin Channel 的标准化消息高速公路。 |

### 1.2 外围域 (Peripheral Domain)

这是 AiRi 的感官和手脚。它们通过 **APP (AiRi Peripheral Protocol)** 协议与核心域通信。

| 模块 | 类型 | 职责 | 接入方式 |
| --- | --- | --- | --- |
| **Sensory System** | Producer | 视觉(Omni)、听觉等。产生环境数据。 | 自动注入 (Automatic Injection) 到主脑上下文。 |
| **Entropy System** | Producer | 网络漫游、记忆闪回。产生随机事件。 | 自动注入 (Automatic Injection) 到主脑上下文。 |
| **Tool Kit** | Consumer | 搜索、系统控制等。 | 通过 Tool Call 被主脑主动调用。 |

---

## 2. 数据交换规范 (AiRi Peripheral Protocol)

在开发任何外围功能前，必须先实现此规范。这是未来插件化的基石。

### 2.1 感知输入协议 (`PeripheralEvent`)

所有试图进入主脑的信息（无论是用户说话、眼睛看到、还是爬虫抓到），必须封装为此对象。

```kotlin
data class PeripheralEvent(
    val id: String = UUID.randomUUID().toString(),
    val source: EventSource, // e.g., USER, VISION, SYSTEM, ENTROPY
    val type: EventType,     // e.g., TEXT, IMAGE, AUDIO_TRANSCRIPT
    val content: Any,        // 实际负载 (String, Bitmap, etc.)
    val priority: Priority,  // LOW (Log only), NORMAL (Context), HIGH (Wake up)
    val timestamp: Long = System.currentTimeMillis()
)

```

### 2.2 工具注入协议 (`ToolDefinition`)

所有外围功能如果希望被 AiRi 主动使用，必须符合 StreamLLM 的 Tool 标准，并包装为配置项。

```kotlin
data class ExternalTool(
    val name: String,
    val description: String,
    val parameters: String, // JSON Schema
    val enabled: Boolean = true,
    val executor: suspend (String) -> String
)

```

---

## 3. 模块详细设计 (Phase 1 & 2 Focus)

### 3.1 设置与配置中心 (`SettingsManager`)

**目标**: 摆脱硬编码，允许用户在 UI 配置 API Key 和行为。

* **技术**: `kotlinx.serialization` + 本地 JSON 文件 (`config.json`)。
* **配置项结构**:
  * `LLM Config`: BaseURL, API Key, Model Name (Core & Vision).
  * `System Config`: Target Screen ID, Max Memory Size.
  * `Personality`: System Prompt 模板。

### 3.2 界面重构 (`UI Actor`)

**目标**: 在现有终端/聊天布局基础上，增加**设置面板**。

* **Settings Screen**:
  * 使用 Material 3 风格的表单。
  * 支持“保存并热重载” (Hot Reload)，无需重启应用即可切换模型。

* **Navigation**:
  * 简单的侧边栏或顶部菜单：`Chat` | `Logs` | `Settings`。

### 3.3 CI/CD 流水线 (Github Actions)

**目标**: 确保每次 Commit 都经过风格检查和编译测试。

* **Workflows**:
  1. **Check**: `./gradlew detekt` (代码风格)。
  2. **Build**: `./gradlew packageDistributionForCurrentOS` (确保能打包)。
  3. **Test**: `./gradlew test` (单元测试)。

---

## 4. 开发执行计划 (Roadmap v5.0)

### Phase 1: 基础设施 (Foundation)

* [ ] **Settings Module**: 实现 `SettingsRepository`，支持读写 JSON 配置。
* [ ] **Settings UI**: 开发配置界面，支持输入 API Key 和选择模型（OpenAI/DeepSeek/SiliconFlow）。
* [ ] **CI/CD**: 配置 `.github/workflows/check_and_build.yml`。
* [ ] **Dependency Injection**: 完善 Koin 模块，注入 `SettingsRepository`。

### Phase 2: 核心意识与协议 (Core & Protocol)

* [ ] **Main Loop Refactor**: 重构 `MainActor`，使其基于 `SettingsManager` 的配置动态加载 StreamLLM 客户端。
* [ ] **Protocol Implementation**: 定义 `PeripheralEvent` 和 `EventBus`。
* [ ] **Chat Interface**: 完善底部聊天输入框，将其作为 `source=USER` 的 Event 发送入总线。
* [ ] **Speaker Implementation**: 实现流式输出工具，打通“思考-表达-UI”链路。

### Phase 3: 感官与外围 (Peripherals) - [后续开发]

* [ ] **Vision Service**: 基于协议实现视觉模块，将截图封装为 `PeripheralEvent`。
* [ ] **Entropy Service**: 基于协议实现随机事件。
* [ ] **Web Tool**: 封装搜索功能为 `ExternalTool`。

---

## 5. 关键依赖清单 (Baseline)

```kotlin
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

```