package com.lockedfog.airi.domain

import com.lockedfog.airi.data.config.SettingsRepository
import dev.lockedfog.streamllm.StreamLLM
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class AgentKernel(
    private val settingsRepository: SettingsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger = LoggerFactory.getLogger("AgentKernel")
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    init {
        logger.info("Initializing Agent Kernel...")
        startConfigObserver()
    }

    private fun startConfigObserver() {
        scope.launch {
            settingsRepository.configFlow.collectLatest { config ->
                val llmConfig = config.llm
                if (llmConfig.apiKey.isNotBlank()) {
                    reloadBrain(
                        baseUrl = llmConfig.baseUrl,
                        apiKey = llmConfig.apiKey,
                        modelName = llmConfig.mainModel
                    )
                } else {
                    logger.info("Waiting for API Key configuration...")
                }
            }
        }
    }

    private fun reloadBrain(
        baseUrl: String,
        apiKey: String,
        modelName: String
    ) {
        try {
            StreamLLM.close()

            StreamLLM.init(
                baseUrl = baseUrl,
                apiKey = apiKey,
                modelName = modelName,
                timeoutSeconds = 60,
                maxMemoryCount = 10
            )

            logger.info("Cortex connected: $modelName @ $baseUrl")
        } catch (e: IllegalArgumentException) {
            logger.error("Brain initialization failed: Invalid Configuration", e)
        } catch (e: IllegalStateException) {
            logger.error("Brain initialization failed: Illegal State", e)
        }
    }
}
