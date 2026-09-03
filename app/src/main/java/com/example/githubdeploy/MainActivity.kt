package com.example.githubdeploy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.githubdeploy.data.repository.AppRepository
import com.example.githubdeploy.presentation.navigation.AppNavHost
import com.example.githubdeploy.ui.theme.GithubDeployTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: AppRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // In a larger app this would come from a DI container (Hilt/Koin).
        repository = AppRepository(applicationContext)

        setContent {
            GithubDeployTheme {
                AppNavHost(repository = repository)
            }
        }
    }
}
