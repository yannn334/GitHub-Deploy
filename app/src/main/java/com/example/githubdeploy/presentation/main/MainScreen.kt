package com.example.githubdeploy.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val operationState by viewModel.operationState.collectAsStateWithLifecycle()
    val log by viewModel.log.collectAsStateWithLifecycle()

    var showCommitDialog by remember { mutableStateOf(false) }
    var showReleaseDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val isBusy = operationState is OperationState.InProgress

    LaunchedEffect(operationState) {
        when (val state = operationState) {
            is OperationState.Success -> {
                snackbarHostState.showSnackbar(state.message)
                viewModel.resetOperationState()
            }
            is OperationState.Error -> {
                snackbarHostState.showSnackbar("Error: ${state.message}")
                viewModel.resetOperationState()
            }
            else -> Unit
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("GitHub Deploy") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Repository", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (settings.isGithubConfigured)
                            "${settings.repoOwner}/${settings.repoName}"
                        else
                            "Not configured — open Settings"
                    )
                }
            }

            if (isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    (operationState as OperationState.InProgress).message,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = { viewModel.pullFromGithub() },
                enabled = !isBusy && settings.isGithubConfigured,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Pull from GitHub") }

            Button(
                onClick = { showCommitDialog = true },
                enabled = !isBusy && settings.isGithubConfigured && viewModel.isRepoCloned(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Push to GitHub") }

            Button(
                onClick = { showReleaseDialog = true },
                enabled = !isBusy && settings.isGithubConfigured,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Create Release") }

            Button(
                onClick = { viewModel.deployToServer() },
                enabled = !isBusy && settings.isSshConfigured && viewModel.isRepoCloned(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Deploy to Server") }

            Divider(modifier = Modifier.padding(vertical = 4.dp))

            Text("Activity Log", style = MaterialTheme.typography.titleMedium)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(log.reversed()) { entry ->
                    Text(
                        entry,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }

    if (showCommitDialog) {
        CommitMessageDialog(
            onDismiss = { showCommitDialog = false },
            onConfirm = { message ->
                showCommitDialog = false
                viewModel.pushToGithub(message)
            }
        )
    }

    if (showReleaseDialog) {
        CreateReleaseDialog(
            onDismiss = { showReleaseDialog = false },
            onConfirm = { tag, name, description, uploadZip ->
                showReleaseDialog = false
                viewModel.createRelease(tag, name, description, uploadZip)
            }
        )
    }
}

@Composable
private fun CommitMessageDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var message by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Commit Message") },
        text = {
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Message") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(message.ifBlank { "Update" }) }) {
                Text("Commit & Push")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CreateReleaseDialog(
    onDismiss: () -> Unit,
    onConfirm: (tag: String, name: String, description: String, uploadZip: Boolean) -> Unit
) {
    var tag by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var uploadZip by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Release") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tag,
                    onValueChange = { tag = it },
                    label = { Text("Tag name (e.g. v1.0.0)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Release name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = uploadZip, onCheckedChange = { uploadZip = it })
                    Text("Upload repository ZIP as asset")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(tag, name.ifBlank { tag }, description, uploadZip) },
                enabled = tag.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
