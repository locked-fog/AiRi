package com.lockedfog.airi.ui.viewmodel

import com.lockedfog.airi.data.config.AppConfig
import com.lockedfog.airi.data.config.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

class SettingsViewModel(
    private val scope: CoroutineScope,
    private val repository: SettingsRepository = SettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger = LoggerFactory.getLogger("SettingsViewModel")
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val _state = MutableStateFlow(SettingsState(config = repository.currentConfig))
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    fun updateConfig(newConfig: AppConfig) {
        _state.update {
            it.copy(
                config = newConfig,
                connectionSuccess = false,
                connectionError = null,
                canSave = true
            )
        }
    }

    fun testConnection() {
        val currentLlm = _state.value.config.llm
        if (currentLlm.apiKey.isBlank()) {
            _state.update { it.copy(connectionError = "API Key cannot be empty") }
            return
        }

        scope.launch(ioDispatcher) {
            _state.update { it.copy(isTestingConnection = true, connectionError = null) }

            try {
                val baseUrl = currentLlm.baseUrl.trim().removeSuffix("/")
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$baseUrl/models"))
                    .header("Authorization", "Bearer ${currentLlm.apiKey}")
                    .header("Content-Type", "application/json")
                    .GET()
                    .build()

                logger.info("Testing connection to $baseUrl/models")
                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

                @Suppress("MagicNumber")
                if (response.statusCode() == 200) {
                    val models = parseModelList(response.body())

                    if (models == null) {
                        // Case 1: JSON 解析炸了
                        _state.update {
                            it.copy(isTestingConnection = false, connectionError = "Failed to parse server response.")
                        }
                    } else if (models.isEmpty()) {
                        // Case 2: 连接成功但无模型
                        _state.update {
                            it.copy(isTestingConnection = false, connectionError = "Connected, but found no models.")
                        }
                    } else {
                        // Case 3: 真正成功
                        _state.update {
                            it.copy(
                                isTestingConnection = false,
                                connectionSuccess = true,
                                availableModels = models
                            )
                        }
                        logger.info("Connection success. Found ${models.size} models.")
                    }
                } else {
                    val errorMsg = "HTTP ${response.statusCode()}: ${response.body()}"
                    _state.update {
                        it.copy(isTestingConnection = false, connectionError = errorMsg)
                    }
                    logger.warn("Connection failed: $errorMsg")
                }
            } catch (e: IllegalArgumentException) {
                // 处理 URL 格式错误
                _state.update { it.copy(isTestingConnection = false, connectionError = "Invalid URL: ${e.message}") }
                logger.warn("Invalid URL format", e)
            } catch (e: IOException) {
                // 处理网络 IO 错误 (DNS, Timeout, No Route)
                _state.update { it.copy(isTestingConnection = false, connectionError = "Network Error: ${e.message}") }
                logger.warn("Network IO error", e)
            } catch (e: InterruptedException) {
                // 处理线程中断
                _state.update { it.copy(isTestingConnection = false, connectionError = "Request Cancelled") }
                logger.warn("Interrupted!", e)
            }
        }
    }

    private fun parseModelList(jsonBody: String): List<String>? {
        return try {
            val root = jsonParser.parseToJsonElement(jsonBody).jsonObject
            val dataArray = root["data"]?.jsonArray
            dataArray?.mapNotNull {
                it.jsonObject["id"]?.jsonPrimitive?.content
            }?.sorted() ?: emptyList()
        } catch (e: SerializationException) {
            logger.error("JSON parsing failed", e)
            null
        } catch (e: IllegalArgumentException) {
            logger.error("JSON structure mismatch", e)
            null
        }
    }

    fun save() {
        repository.saveConfig(_state.value.config)
        _state.update { it.copy(canSave = false) } // 保存后禁用按钮
    }
}
