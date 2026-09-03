package com.example.githubdeploy.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.githubdeploy.data.model.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Persists AppSettings using EncryptedSharedPreferences (AndroidX Security),
 * so secrets such as the GitHub token and SSH password are stored encrypted at rest.
 */
class SettingsStore(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val _settings = MutableStateFlow(loadSettings())
    val settingsFlow: StateFlow<AppSettings> = _settings

    fun getSettings(): AppSettings = _settings.value

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putString(KEY_TOKEN, settings.githubToken)
            .putString(KEY_OWNER, settings.repoOwner)
            .putString(KEY_REPO, settings.repoName)
            .putString(KEY_HOST, settings.sshHost)
            .putInt(KEY_PORT, settings.sshPort)
            .putString(KEY_SSH_USER, settings.sshUsername)
            .putString(KEY_SSH_PASS, settings.sshPassword)
            .putBoolean(KEY_USE_KEY, settings.useSshKey)
            .putString(KEY_KEY_PATH, settings.sshPrivateKeyPath)
            .putString(KEY_REMOTE_PATH, settings.remotePath)
            .apply()
        _settings.value = settings
    }

    private fun loadSettings(): AppSettings = AppSettings(
        githubToken = prefs.getString(KEY_TOKEN, "") ?: "",
        repoOwner = prefs.getString(KEY_OWNER, "") ?: "",
        repoName = prefs.getString(KEY_REPO, "") ?: "",
        sshHost = prefs.getString(KEY_HOST, "") ?: "",
        sshPort = prefs.getInt(KEY_PORT, 22),
        sshUsername = prefs.getString(KEY_SSH_USER, "") ?: "",
        sshPassword = prefs.getString(KEY_SSH_PASS, "") ?: "",
        useSshKey = prefs.getBoolean(KEY_USE_KEY, false),
        sshPrivateKeyPath = prefs.getString(KEY_KEY_PATH, "") ?: "",
        remotePath = prefs.getString(KEY_REMOTE_PATH, "/var/www/html") ?: "/var/www/html"
    )

    companion object {
        private const val PREFS_NAME = "github_deploy_secure_prefs"
        private const val KEY_TOKEN = "github_token"
        private const val KEY_OWNER = "repo_owner"
        private const val KEY_REPO = "repo_name"
        private const val KEY_HOST = "ssh_host"
        private const val KEY_PORT = "ssh_port"
        private const val KEY_SSH_USER = "ssh_username"
        private const val KEY_SSH_PASS = "ssh_password"
        private const val KEY_USE_KEY = "use_ssh_key"
        private const val KEY_KEY_PATH = "ssh_key_path"
        private const val KEY_REMOTE_PATH = "remote_path"
    }
}
