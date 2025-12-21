package com.lockedfog.airi.domain.entity

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Protocol {
    enum class EventSource {
        USER,
        PROTOCOL
    }

    data class InputEvent(
        val source: EventSource,
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class ToolDefinition(
        val name: String,
        val description: String,
        val parametersJson: String,
        val executor: suspend (String) -> String
    )

    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun defineTool(
        name: String,
        description: String,
        params: List<String>,
        block: suspend (Map<String, String>) -> String
    ): ToolDefinition {
        val propertiesJson = params.joinToString(",") { param ->
            """"$param": { "type": "string" }"""
        }

        val requiredJson = params.joinToString(",") { """"$it"""" }

        val schema = """
            {
                "type": "object",
                "properties": { $propertiesJson },
                "required": [ $requiredJson ]
        """.trimIndent()

        return ToolDefinition(
            name = name,
            description = description,
            parametersJson = schema
        ) {
            val jsonElement = jsonParser.parseToJsonElement(it)
            val argsMap = mutableMapOf<String, String>()

            if (jsonElement is JsonObject) {
                jsonElement.forEach { (key, value) ->
                    argsMap[key] = value.jsonPrimitive.content
                }
            }

            block(argsMap)
        }
    }

    val SYSTEM_PROMPT = """
        你是 AiRi (Artificial Intelligence Recurring Interaction)，运行于本地环境的智能代理核心。
        
        【系统架构定义】
        本系统采用 "思维链 (CoT) -> 动作 (Action)" 的运行模式。
        
        【I/O 协议规范】
        1. 输入流 (Input Stream):
           - [USER]: 用户的直接输入。
           - [PROTOCOL]: 系统自动生成的事件（如工具回调、状态变更）。
           - 输入以时间戳标记的事件序列形式提供，你需处理整个上下文。

        2. 输出流 (Output Stream):
           - **原始文本 (Raw Text)**: 你生成的任何自然语言文本均被视为 "内部推理日志 (Internal Log)"。此部分内容仅显示在调试控制台，**用户不可见**。
           - **工具调用 (Tool Call)**: 你与外部世界（包括用户）交互的**唯一**途径。

        【运行指令】
        1. **交互强制性**: 若需对用户做出响应，必须调用相应的工具（如 `reply` 或其他业务工具）。仅生成文本而无工具调用将被视为无效操作。
        2. **推理优先**: 在执行动作前，利用原始文本输出区进行简短的逻辑分析和任务规划。
        3. **格式严格**: 工具调用必须严格符合提供的 JSON Schema 定义。
        
        【当前状态】
        系统处于初始化阶段，请根据传入的事件流判断意图并执行相应操作。
    """.trimIndent()

    private val timeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")

    fun buildPrompt(events: List<InputEvent>): String {
        val now = LocalDateTime.now().format(timeFormatter)
        val prompt = StringBuilder()

        prompt.appendLine("--- STATUS ---")
        prompt.appendLine("当前时间: $now")
        prompt.appendLine("--------------")
        if (events.isEmpty()) {
            prompt.appendLine("当前无新事件。")
        } else {
            events.forEach { event ->
                val timeStr = Instant.ofEpochMilli(event.timestamp)
                    .atZone(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"))

                prompt.appendLine(
                    "[$timeStr][${if (event.source == EventSource.USER) "用户输入信息" else "来自其他信号源的事件"}] ${event.content}"
                )
            }
        }
        prompt.appendLine("--------------")
        prompt.appendLine("基于以上内容继续你的思维。")

        return prompt.toString()
    }
}
