package com.propertycrm.app.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.propertycrm.app.ai.AICopilotEngine
import com.propertycrm.app.ai.AIGatewayClient
import com.propertycrm.app.ai.AgentExecutionResult
import com.propertycrm.app.ai.ChatMessage
import com.propertycrm.app.data.local.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AICopilotViewModel(
    private val aiCopilotEngine: AICopilotEngine,
    private val aiClient: AIGatewayClient,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                role = "assistant",
                content = "Hello! I am your Property CRM AI Copilot. You can tell me to register new customers, schedule site visits, record call outcomes, search leads, or summarize your pipeline. What would you like to do?"
            )
        )
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _lastExecution = MutableStateFlow<AgentExecutionResult?>(null)
    val lastExecution: StateFlow<AgentExecutionResult?> = _lastExecution.asStateFlow()

    val hasApiKey: StateFlow<Boolean> = preferenceManager.aiApiKey.map {
        it.isNotBlank()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val activeModel: StateFlow<String> = preferenceManager.aiModel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "gpt-4o-mini"
    )

    fun sendCommand(userInput: String) {
        val prompt = userInput.trim()
        if (prompt.isBlank() || _isLoading.value) return

        val userMsg = ChatMessage(role = "user", content = prompt)
        _messages.value = _messages.value + userMsg
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val result = aiCopilotEngine.processUserCommand(
                    userInstruction = prompt,
                    conversationHistory = _messages.value
                )
                _lastExecution.value = result

                val assistantMsg = ChatMessage(
                    role = "assistant",
                    content = result.replyText
                )
                _messages.value = _messages.value + assistantMsg
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    role = "assistant",
                    content = "Command execution encountered an error: ${e.message}"
                )
                _messages.value = _messages.value + errorMsg
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearChat() {
        _messages.value = listOf(
            ChatMessage(
                role = "assistant",
                content = "Chat cleared. What can I help you with next?"
            )
        )
        _lastExecution.value = null
    }
}
