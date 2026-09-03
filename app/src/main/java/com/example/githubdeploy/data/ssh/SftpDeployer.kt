package com.example.githubdeploy.data.ssh

import com.example.githubdeploy.data.model.AppSettings
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import java.io.File

/**
 * Uploads a local directory tree to a remote server over SFTP using JSch,
 * recreating remote directories as needed and overwriting existing files.
 */
class SftpDeployer {

    suspend fun deploy(
        settings: AppSettings,
        localDir: File,
        onProgress: (String) -> Unit
    ) {
        val jsch = JSch()
        if (settings.useSshKey && settings.sshPrivateKeyPath.isNotBlank()) {
            jsch.addIdentity(settings.sshPrivateKeyPath)
        }

        val session = jsch.getSession(settings.sshUsername, settings.sshHost, settings.sshPort)
        if (!settings.useSshKey) {
            session.setPassword(settings.sshPassword)
        }
        // NOTE: disables host key verification for convenience. For production use,
        // pin known_hosts or verify the host key fingerprint instead.
        session.setConfig("StrictHostKeyChecking", "no")

        try {
            onProgress("Connecting to ${settings.sshHost}:${settings.sshPort} ...")
            session.connect(15000)

            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect(15000)

            try {
                onProgress("Uploading files to ${settings.remotePath} ...")
                uploadDirectory(channel, localDir, settings.remotePath, onProgress)
                onProgress("Deployment complete.")
            } finally {
                channel.disconnect()
            }
        } finally {
            session.disconnect()
        }
    }

    private fun ensureRemoteDir(channel: ChannelSftp, remoteDir: String) {
        val parts = remoteDir.split("/").filter { it.isNotBlank() }
        var current = ""
        for (part in parts) {
            current += "/$part"
            try {
                channel.stat(current)
            } catch (e: Exception) {
                channel.mkdir(current)
            }
        }
    }

    private fun uploadDirectory(
        channel: ChannelSftp,
        localDir: File,
        remoteDir: String,
        onProgress: (String) -> Unit
    ) {
        ensureRemoteDir(channel, remoteDir)

        val files = localDir.listFiles() ?: return
        for (file in files) {
            if (file.name == ".git") continue
            val remotePath = "$remoteDir/${file.name}"
            if (file.isDirectory) {
                uploadDirectory(channel, file, remotePath, onProgress)
            } else {
                onProgress("Uploading ${file.name}")
                channel.put(file.absolutePath, remotePath, ChannelSftp.OVERWRITE)
            }
        }
    }
}
