package com.example.githubdeploy.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubdeploy.data.model.AppSettings
import com.example.githubdeploy.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class OperationState {
    object Idle : OperationState()
    data class InProgress(val message: String) : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}

class MainViewModel(private val repository: AppRepository) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.settingsFlow

    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log

    fun isRepoCloned(): Boolean = repository.isRepoCloned()

    private fun appendLog(message: String) {
        _log.value = _log.value + message
    }

    private fun runOperation(block: suspend (onProgress: (String) -> Unit) -> Unit) {
        viewModelScope.launch {
            _operationState.value = OperationState.InProgress("Starting...")
            try {
                block { message ->
                    appendLog(message)
                    _operationState.value = OperationState.InProgress(message)
                }
                _operationState.value = OperationState.Success("Done.")
            } catch (e: Exception) {
                appendLog("Error: ${e.message}")
                _operationState.value = OperationState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun pullFromGithub() = runOperation { onProgress ->
        repository.pullFromGithub(onProgress)
    }

    fun pushToGithub(commitMessage: String) = runOperation { onProgress ->
        repository.pushToGithub(commitMessage, onProgress)
    }

    fun createRelease(tag: String, name: String, description: String, uploadZip: Boolean) = runOperation { onProgress ->
        repository.createRelease(tag, name, description, uploadZip, onProgress)
    }

    fun deployToServer() = runOperation { onProgress ->
        repository.deployToServer(onProgress)
    }

    fun clearLog() {
        _log.value = emptyList()
    }

    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}
