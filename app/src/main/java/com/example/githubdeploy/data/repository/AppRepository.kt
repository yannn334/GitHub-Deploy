package com.example.githubdeploy.data.repository

import android.content.Context
import com.example.githubdeploy.data.git.GitManager
import com.example.githubdeploy.data.local.SettingsStore
import com.example.githubdeploy.data.model.AppSettings
import com.example.githubdeploy.data.remote.CreateReleaseRequest
import com.example.githubdeploy.data.remote.GitHubApiService
import com.example.githubdeploy.data.remote.RetrofitInstance
import com.example.githubdeploy.data.ssh.SftpDeployer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Single entry point for the presentation layer. Coordinates GitHub REST calls (Retrofit),
 * local git operations (JGit), and SFTP deployment (JSch) on top of the persisted settings.
 */
class AppRepository(private val context: Context) {

    private val settingsStore = SettingsStore(context)
    private val gitManager = GitManager(File(context.filesDir, "repo"))
    private val sftpDeployer = SftpDeployer()

    val settingsFlow: StateFlow<AppSettings> = settingsStore.settingsFlow

    private val api: GitHubApiService by lazy {
        RetrofitInstance.create { settingsStore.getSettings().githubToken }
    }

    fun getSettings(): AppSettings = settingsStore.getSettings()

    fun saveSettings(settings: AppSettings) = settingsStore.saveSettings(settings)

    fun isRepoCloned(): Boolean = gitManager.isRepoCloned()

    fun getLocalRepoDir(): File = gitManager.getLocalRepoDir()

    suspend fun pullFromGithub(onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        val settings = settingsStore.getSettings()
        require(settings.isGithubConfigured) { "GitHub settings are not configured." }
        gitManager.cloneOrPull(settings, onProgress)
    }

    suspend fun pushToGithub(commitMessage: String, onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        val settings = settingsStore.getSettings()
        require(settings.isGithubConfigured) { "GitHub settings are not configured." }
        gitManager.commitAndPush(settings, commitMessage, onProgress)
    }

    suspend fun createRelease(
        tagName: String,
        releaseName: String,
        description: String,
        uploadZip: Boolean,
        onProgress: (String) -> Unit
    ): String = withContext(Dispatchers.IO) {
        val settings = settingsStore.getSettings()
        require(settings.isGithubConfigured) { "GitHub settings are not configured." }

        onProgress("Creating release $tagName ...")
        val response = api.createRelease(
            settings.repoOwner,
            settings.repoName,
            CreateReleaseRequest(
                tagName = tagName,
                name = releaseName,
                body = description
            )
        )
        if (!response.isSuccessful) {
            throw IllegalStateException(
                "Failed to create release: ${response.code()} ${response.errorBody()?.string()}"
            )
        }
        val release = response.body() ?: throw IllegalStateException("Empty response from GitHub.")
        onProgress("Release created: ${release.htmlUrl}")

        if (uploadZip) {
            onProgress("Zipping repository...")
            val zipFile = zipDirectory(gitManager.getLocalRepoDir(), "${settings.repoName}-$tagName.zip")

            onProgress("Uploading release asset...")
            // upload_url arrives as a URI template like ".../assets{?name,label}" - strip the template part.
            val cleanUploadUrl = release.uploadUrl.substringBefore("{")
            val fullUploadUrl = "$cleanUploadUrl?name=${zipFile.name}"

            val requestBody = zipFile.asRequestBody("application/zip".toMediaType())
            val uploadResponse = api.uploadReleaseAsset(fullUploadUrl, requestBody)
            if (!uploadResponse.isSuccessful) {
                throw IllegalStateException("Failed to upload asset: ${uploadResponse.code()}")
            }
            onProgress("Asset uploaded: ${uploadResponse.body()?.browserDownloadUrl}")
        }

        release.htmlUrl
    }

    suspend fun deployToServer(onProgress: (String) -> Unit) = withContext(Dispatchers.IO) {
        val settings = settingsStore.getSettings()
        require(settings.isSshConfigured) { "SSH settings are not configured." }
        require(gitManager.isRepoCloned()) { "No local repository found. Pull first." }
        sftpDeployer.deploy(settings, gitManager.getLocalRepoDir(), onProgress)
    }

    private fun zipDirectory(sourceDir: File, zipName: String): File {
        val zipFile = File(context.cacheDir, zipName)
        if (zipFile.exists()) zipFile.delete()

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            sourceDir.walkTopDown()
                .filter { it.isFile && !it.path.contains(".git${File.separator}") }
                .forEach { file ->
                    val entryName = file.relativeTo(sourceDir).path
                    zos.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { input -> input.copyTo(zos) }
                    zos.closeEntry()
                }
        }
        return zipFile
    }
}
