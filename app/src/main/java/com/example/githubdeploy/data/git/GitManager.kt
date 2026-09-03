package com.example.githubdeploy.data.git

import com.example.githubdeploy.data.model.AppSettings
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File

/**
 * Wraps local git operations (clone / pull / add / commit / push) using JGit.
 * The GitHub PAT is used as the HTTPS password (username is ignored by GitHub
 * when a token is supplied), which is the standard way to authenticate git
 * operations over HTTPS with a Personal Access Token.
 */
class GitManager(private val localRepoDir: File) {

    private fun credentialsProvider(settings: AppSettings) =
        UsernamePasswordCredentialsProvider(settings.githubToken, "")

    fun isRepoCloned(): Boolean = File(localRepoDir, ".git").exists()

    fun getLocalRepoDir(): File = localRepoDir

    suspend fun cloneOrPull(settings: AppSettings, onProgress: (String) -> Unit) {
        val cloneUrl = "https://github.com/${settings.repoOwner}/${settings.repoName}.git"

        if (!isRepoCloned()) {
            onProgress("Cloning $cloneUrl ...")
            if (localRepoDir.exists()) localRepoDir.deleteRecursively()
            localRepoDir.mkdirs()

            Git.cloneRepository()
                .setURI(cloneUrl)
                .setDirectory(localRepoDir)
                .setCredentialsProvider(credentialsProvider(settings))
                .call()
                .close()

            onProgress("Clone complete.")
        } else {
            onProgress("Pulling latest changes...")
            Git.open(localRepoDir).use { git ->
                git.pull()
                    .setCredentialsProvider(credentialsProvider(settings))
                    .call()
            }
            onProgress("Pull complete.")
        }
    }

    suspend fun commitAndPush(settings: AppSettings, commitMessage: String, onProgress: (String) -> Unit) {
        check(isRepoCloned()) { "Local repository not found. Pull first." }

        Git.open(localRepoDir).use { git ->
            onProgress("Staging changes...")
            git.add().addFilepattern(".").call()

            val status = git.status().call()
            if (status.isClean) {
                onProgress("No changes to commit.")
                return
            }

            onProgress("Committing...")
            git.commit()
                .setMessage(commitMessage)
                .setAuthor("GithubDeployApp", "githubdeploy@app.local")
                .call()

            onProgress("Pushing to remote...")
            git.push()
                .setCredentialsProvider(credentialsProvider(settings))
                .call()

            onProgress("Push complete.")
        }
    }
}
