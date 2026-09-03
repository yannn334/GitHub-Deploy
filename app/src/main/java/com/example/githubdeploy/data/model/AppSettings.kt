package com.example.githubdeploy.data.model

/**
 * All user-configurable settings for the app. Persisted encrypted via SettingsStore.
 */
data class AppSettings(
    val githubToken: String = "",
    val repoOwner: String = "",
    val repoName: String = "",
    val sshHost: String = "",
    val sshPort: Int = 22,
    val sshUsername: String = "",
    val sshPassword: String = "",
    val useSshKey: Boolean = false,
    val sshPrivateKeyPath: String = "",
    val remotePath: String = "/var/www/html"
) {
    val isGithubConfigured: Boolean
        get() = githubToken.isNotBlank() && repoOwner.isNotBlank() && repoName.isNotBlank()

    val isSshConfigured: Boolean
        get() = sshHost.isNotBlank() && sshUsername.isNotBlank() &&
                (sshPassword.isNotBlank() || (useSshKey && sshPrivateKeyPath.isNotBlank()))
}
