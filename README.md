# GitHub Deploy (Android)

GitHub Deploy is a full-featured, native Android application built with Kotlin and Jetpack Compose designed to manage GitHub repositories and automate deployment pipelines directly from a mobile device. The application unifies Git version control operations, GitHub REST API release management, and remote file synchronization over the SFTP protocol into a single mobile interface.

---

## Overview & Key Capabilities

This application is engineered specifically for developers, system administrators, and webmasters who need to update, commit, tag, and deploy static websites, web applications, or project files to remote hosting environments without requiring access to a desktop workstation or CLI terminal.

* **Complete GitHub Synchronization:** Clones remote repositories directly into local, app-isolated storage and executes fast-forward pulls (`git pull`) using the JGit engine to keep local working copies synchronized.
* **Git Push Workflow:** Automatically stages untracked and modified files, commits changes with a custom commit message, and pushes code to the remote branch authenticated via a GitHub Personal Access Token (PAT).
* **Release Management:** Interacts directly with the GitHub REST API to cut tagged releases, generate release metadata, and automatically package the current repository state into a downloadable ZIP asset attached to the release.
* **SFTP Deployment Engine:** Recursively transfers and syncs all repository assets to a targeted remote server directory over SSH/SFTP using the JSch library, overwriting existing files to ensure instant live updates.
* **Real-time Activity Logging:** Features a dedicated status console that provides line-by-line execution feedback for network operations, Git tasks, API responses, and SFTP file transfers.

---

## Architecture & Security Considerations

* **On-Device Encryption:** Sensitive credentials—including GitHub Access Tokens, SSH passwords, and private keys—are encrypted at rest using Android's `EncryptedSharedPreferences`, backed by hardware-level keys in the Android Keystore.
* **Background Threading:** All network operations, Git commands, and file transfer tasks run strictly off the main thread using Kotlin Coroutines (`Dispatchers.IO`) to maintain a fluid, responsive 60fps UI.
* **Modern Android Stack:** Developed using MVVM architecture, Material 3 design components, ViewModel state management (`StateFlow`), and Navigation-Compose for seamless screen transitions.

---

## Configuration & Credentials

Application settings are configured in the **Settings** view. Ensure all credentials and server details are correctly entered before initiating deployment tasks.

### GitHub Parameters
* **GitHub PAT:** Personal Access Token used for HTTPS Git operations and REST API calls.
  * **Fine-grained Tokens:** Require `Contents` (Read & Write) and `Releases` (Read & Write) permissions for the targeted repository.
  * **Classic Tokens:** Require the full `repo` scope.
* **Repository Owner / Name:** Specified strictly in `owner/repository` format (e.g., `octocat/hello-world`).

### SSH / SFTP Parameters
* **SSH Host:** Target server IP address (IPv4, local subnet address, or a mesh VPN IP such as Tailscale).
* **SSH Port:** Default SSH port `22` or your server's custom SSH port.
* **Authentication Method:** Supports standard SSH username/password authentication or a local file path to a private key file stored on the device.
* **Remote Path:** The absolute destination path on the remote server where web assets should be deployed (e.g., `/DATA/AppData/my-app` or `/var/www/html`).

---

## Troubleshooting Permission Errors (SFTP Deployment)

If the deployment process fails during file upload with the following log entry:

```text
Error: Permission denied

Write this commands in SSH to give permissions
sudo chown -R $USER:$USER /path/to/target/directory
sudo chmod -R 775 /path/to/target/directory
