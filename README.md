# AiRi

![CI Status](https://github.com/locked-fog/AiRi/actions/workflows/ci.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.2.20-purple)
![Compose](https://img.shields.io/badge/Compose-Desktop-blue)
![License](https://img.shields.io/badge/License-Apache%202.0-green)

> **Artificial Intelligence Recurring Interaction**
> 
> 一个运行在桌面后台的、具备持续思维流与双重意识界面的伴侣型 AI Agent。

## 🧠 Core Concept

AiRi 不仅仅是一个聊天机器人，她是一个拥有完整生命周期的 **Desktop Agent**：

* **双重意识 (Split Consciousness)**:
    * **Subconscious (潜意识)**: 顶部折叠终端，展示底层的思维流、环境感知日志与熵增随机事件。
    * **Conscious (表意识)**: 底部交互界面，用于与用户进行自然语言对话。
* **循环架构 (Recurring Loop)**: 基于 `StreamLLM` 的无限 Re-Act 循环，支持自主思考而非单纯的问答。
* **环境感知 (Sensory System)**: (开发中) 能够“看见”屏幕并感知用户状态。

## 🛠 Tech Stack

* **Language**: Kotlin Multiplatform (JVM Target)
* **UI Framework**: Compose for Desktop (Material 3)
* **Core Logic**: [StreamLLM](https://github.com/locked-fog/StreamLLM) (Custom Agent Loop)
* **Architecture**: Actor Model (Coroutines + Channels)
* **DI**: Koin
* **Quality**: Detekt, JUnit 5, MockK

## 📄 License

This project is licensed under the Apache License 2.0
