package com.lockedfog.airi.data.config

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

object SettingsRepository {
    private val logger = LoggerFactory.getLogger("SettingsRepository")

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        coerceInputValues = true
    }

    // 允许测试修改
    var configDir: File = File(System.getProperty("user.home"), ".airi")

    private val configFile: File
        get() = File(configDir, "config.json")

    private var _currentConfig: AppConfig = loadConfig()

    private val _configFlow = MutableStateFlow(_currentConfig)
    val configFlow: StateFlow<AppConfig> = _configFlow.asStateFlow()

    val currentConfig: AppConfig
        get() = _configFlow.value

    /**
     * 从磁盘加载配置。
     * 具备容错能力：遇到损坏文件会记录日志并返回默认配置，而不崩溃。
     */
    fun loadConfig(): AppConfig {
        if (!configFile.exists()) {
            logger.info("Config file not found, creating default.")
            return AppConfig()
        }

        return try {
            val content = configFile.readText()
            json.decodeFromString<AppConfig>(content)
        } catch (e: SerializationException) {
            logger.error("Config JSON corrupted. Loading defaults.", e)
            AppConfig()
        } catch (e: IllegalArgumentException) {
            logger.error("Config contains invalid values (e.g. enum mismatch). Loading defaults.", e)
            AppConfig()
        } catch (e: IOException) {
            logger.error("Failed to read config file.", e)
            AppConfig()
        }
    }

    /**
     * 保存配置。
     * 异常策略：直接抛出，由调用方（UI/ViewModel）处理并提示用户。
     * @throws IOException 写入失败时抛出
     * @throws SerializationException 序列化失败时抛出
     */
    fun saveConfig(newConfig: AppConfig) {
        // 确保父目录存在
        if (!configDir.exists() && !configDir.mkdirs()) {
            throw IOException("Failed to create config directory: ${configDir.absolutePath}")
        }

        val content = json.encodeToString(newConfig)
        configFile.writeText(content)

        // 只有写入成功后才更新内存缓存
        _currentConfig = newConfig
        _configFlow.value = newConfig

        logger.info("Config saved successfully.")
    }

    fun isFirstRun(): Boolean {
        return currentConfig.llm.apiKey.isBlank()
    }
}
