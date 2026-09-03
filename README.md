# GitHub Deploy (Android)

A Kotlin + Jetpack Compose app that lets you pull/push a GitHub repository, cut a
GitHub Release, and deploy the repo's files to a remote server over SFTP — all
from your phone.

## Features

- **Settings screen** — GitHub PAT, repo owner/name, SSH host/port/user/password
  (or private key path), and remote deploy path. Saved encrypted on-device via
  `EncryptedSharedPreferences`.
- **Pull from GitHub** — clones the repo into app-internal storage the first
  time, `git pull`s on subsequent runs (JGit).
- **Push to GitHub** — stages all changes, commits with a message you type, and
  pushes (JGit), authenticating with your PAT.
- **Create Release** — creates a tagged GitHub Release via the REST API and
  optionally zips the repo and uploads it as a release asset.
- **Deploy to Server** — recursively uploads the local repo's files to the
  configured remote path via SFTP (JSch), overwriting existing files.
- MVVM architecture, Material 3 UI, `ViewModel` + `StateFlow`, all network/git/
  SSH work runs on `Dispatchers.IO` coroutines off the main thread.

## Project structure

```
app/src/main/java/com/example/githubdeploy/
├── data/
│   ├── model/AppSettings.kt          # Settings data class
│   ├── local/SettingsStore.kt        # EncryptedSharedPreferences persistence
│   ├── remote/                       # Retrofit: GitHubApiService, models, client
│   ├── git/GitManager.kt             # JGit clone/pull/commit/push
│   ├── ssh/SftpDeployer.kt           # JSch recursive SFTP upload
│   └── repository/AppRepository.kt   # Coordinates the above for the UI
├── presentation/
│   ├── main/                         # MainViewModel + MainScreen (buttons, log)
│   ├── settings/                     # SettingsViewModel + SettingsScreen
│   ├── navigation/AppNavHost.kt      # Two-screen Navigation-Compose graph
│   └── AppViewModelFactory.kt
├── ui/theme/                         # Material 3 theme
├── MainActivity.kt
└── GithubDeployApplication.kt
```

## Building

**Requirements:** Android Studio (Koala or newer recommended), JDK 17, Android
SDK 34.

1. Open the `GithubDeployApp/` folder as a project in Android Studio, or from a
   terminal:
   ```bash
   cd GithubDeployApp
   ./gradlew assembleDebug
   ```
   (If `gradlew` is missing, open the project once in Android Studio — it will
   generate the wrapper — or run `gradle wrapper` if you have Gradle installed
   locally.)
2. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.
3. Install on a device/emulator:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Using the app

1. On first launch you're taken straight to **Settings**. Fill in:
   - **GitHub PAT** — a token with `repo` scope (classic) or equivalent fine-
     grained permissions (contents + releases read/write).
   - **Repository Owner / Name** — e.g. owner `octocat`, repo `hello-world`.
   - **SSH Host/Port/Username** and either a **password** or, if you toggle
     "Use private key", a **path to a private key file** already present on
     the device's filesystem.
   - **Remote Path** — where the site lives on the server, e.g. `/var/www/html`.
2. Tap **Save Settings** to go to the main screen.
3. **Pull from GitHub** — clones (first time) or pulls the repo into the app's
   private storage.
4. Edit files as needed (outside the app, e.g. by pointing another tool at the
   app's private storage, or extend this app with a file editor), then
   **Push to GitHub** — enter a commit message; the app stages, commits, and
   pushes all changes.
5. **Create Release** — enter a tag (e.g. `v1.0.0`), name, and description.
   Optionally upload a ZIP of the current repo as a release asset.
6. **Deploy to Server** — uploads the local repo's files to the configured
   remote path via SFTP, overwriting existing files.

The **Activity Log** at the bottom of the main screen shows progress messages
for whichever operation is running, and a snackbar reports success/failure.

## Security notes

- Secrets (PAT, SSH password) are stored using `EncryptedSharedPreferences`
  (AndroidX Security), which encrypts both keys and values at rest using a
  key held in the Android Keystore.
- SFTP host-key checking is disabled for convenience
  (`StrictHostKeyChecking=no` in `SftpDeployer`). For production use, pin the
  server's host key fingerprint instead of disabling verification.
- The GitHub PAT is used as the HTTPS password for git operations (JGit) and
  as a `Bearer` token for REST API calls — this is the standard way to
  authenticate with a Personal Access Token.
- Consider scoping your PAT as narrowly as possible (a fine-grained token
  limited to the one repository, with only Contents and Releases permissions).

## Known limitations / possible extensions

- No in-app file editor — the app manages git/SFTP operations on whatever is
  in the cloned repo's folder; pair it with a file manager or extend the app
  with an editor screen if you want to modify files in-app.
- The "list releases" and "get repository contents" GitHub endpoints are
  implemented in `GitHubApiService` for completeness but not yet wired into
  the UI — a natural next step is a "Releases" list screen.
- Conflict resolution during `git pull` is left to JGit's defaults (fast-
  forward when possible); a real app would surface merge conflicts to the
  user instead of letting the pull fail.
- Minimum SDK 26 (Android 8.0), targets/compiles against SDK 34.
