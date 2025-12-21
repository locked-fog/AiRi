package com.lockedfog.airi.domain

import com.lockedfog.airi.data.config.SettingsRepository
import com.lockedfog.airi.data.log.LogBuffer.clearThinking
import com.lockedfog.airi.data.log.LogBuffer.updateThinking
import com.lockedfog.airi.domain.entity.Protocol
import com.lockedfog.airi.domain.entity.Protocol.buildPrompt
import dev.lockedfog.streamllm.StreamLLM
import dev.lockedfog.streamllm.dsl.stream
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class AgentKernel(
    private val settingsRepository: SettingsRepository,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger = LoggerFactory.getLogger("AgentKernel")
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val eventChannel = Channel<Protocol.InputEvent>(Channel.UNLIMITED)
    private val isKernelActive = AtomicBoolean(false)

    private var currentBaseUrl: String? = null
    private var currentApiKey: String? = null
    private var currentModel: String? = null
    init {
        logger.info("Initializing Agent Kernel...")
        startConfigObserver()
        startMainLoop()
    }

    fun postEvent(event: Protocol.InputEvent) {
        scope.launch {
            eventChannel.send(event)
            logger.debug("Event received: [{}]", event.source)
        }
    }

    fun sendUserMessage(text: String) {
        postEvent(Protocol.InputEvent(Protocol.EventSource.USER, text))
    }

    private fun startConfigObserver() {
        scope.launch {
            settingsRepository.configFlow.collectLatest { config ->
                val llmConfig = config.llm
                if (llmConfig.baseUrl != currentBaseUrl ||
                    llmConfig.apiKey != currentApiKey ||
                    llmConfig.mainModel != currentModel
                ) {
                    if (llmConfig.apiKey.isNotBlank()) {
                        reloadBrain(
                            baseUrl = llmConfig.baseUrl,
                            apiKey = llmConfig.apiKey,
                            modelName = llmConfig.mainModel
                        )
                        logger.info("Load LLM successfully")
                        isKernelActive.set(true)
                    } else {
                        isKernelActive.set(false)
                        logger.info("Waiting for API Key configuration...")
                    }
                }
            }
        }
    }

    private fun reloadBrain(
        baseUrl: String,
        apiKey: String,
        modelName: String
    ) {
        currentModel = modelName
        currentApiKey = apiKey
        currentBaseUrl = baseUrl
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

    private fun startMainLoop() = scope.launch {
        val memoryId = "main"
        eventChannel.send(Protocol.InputEvent(Protocol.EventSource.PROTOCOL, "first start up"))
        while (isActive) {
            logger.info("try start a round")

            if (!isKernelActive.get()) {
                logger.warn("Kernel dormant, event ignored.")
                @Suppress("MagicNumber")
                delay(500)
                continue
            }

            val batch = mutableListOf<Protocol.InputEvent>()
            @Suppress("MagicNumber")
            delay(200)

            var nextEvent = eventChannel.tryReceive().getOrNull()
            while (nextEvent != null) {
                batch.add(nextEvent)
                nextEvent = eventChannel.tryReceive().getOrNull()
            }

            val prompt = buildPrompt(batch)

            try {
                processStreamRequest(memoryId, prompt)
            } catch (e: IOException) {
                logger.error("Network IO error during reasoning", e)
                @Suppress("MagicNumber")
                delay(200)
            } catch (e: SerializationException) {
                logger.error("Protocol/Data serialization failed", e)
            } catch (e: IllegalArgumentException) {
                logger.error("Invalid arguments in LLM request", e)
            }
        }
    }

    private suspend fun processStreamRequest(memoryId: String, prompt: String) {
        logger.info("build-up prompt: $prompt")
        val thoughtBuffer = StringBuilder()

        val fullResponse = StringBuilder()

        stream {
            switchMemory(memoryId)
            setSystemPrompt(memoryId, Protocol.SYSTEM_PROMPT)

            fullResponse.append(
                prompt.stream { token ->
                    thoughtBuffer.append(token)
                    updateThinking(thoughtBuffer.toString())
                }
            )
        }

        clearThinking()

        logger.info(fullResponse.toString())
    }
}
