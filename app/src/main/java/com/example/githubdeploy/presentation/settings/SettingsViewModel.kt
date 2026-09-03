package com.example.githubdeploy.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.githubdeploy.data.model.AppSettings
import com.example.githubdeploy.data.repository.AppRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: AppRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(repository.getSettings())
    val uiState: StateFlow<AppSettings> = _uiState

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved

    fun update(transform: (AppSettings) -> AppSettings) {
        _uiState.value = transform(_uiState.value)
    }

    fun save() {
        viewModelScope.launch {
            repository.saveSettings(_uiState.value)
            _saved.value = true
        }
    }

    fun consumeSavedEvent() {
        _saved.value = false
    }
}
