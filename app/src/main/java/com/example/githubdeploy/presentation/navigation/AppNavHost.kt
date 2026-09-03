package com.example.githubdeploy.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.githubdeploy.data.repository.AppRepository
import com.example.githubdeploy.presentation.AppViewModelFactory
import com.example.githubdeploy.presentation.main.MainScreen
import com.example.githubdeploy.presentation.main.MainViewModel
import com.example.githubdeploy.presentation.settings.SettingsScreen
import com.example.githubdeploy.presentation.settings.SettingsViewModel

private const val ROUTE_MAIN = "main"
private const val ROUTE_SETTINGS = "settings"

@Composable
fun AppNavHost(repository: AppRepository) {
    val navController = rememberNavController()
    val factory = AppViewModelFactory(repository)

    // First launch (no GitHub settings yet) goes straight to Settings.
    val startDestination = if (repository.getSettings().isGithubConfigured) ROUTE_MAIN else ROUTE_SETTINGS

    NavHost(navController = navController, startDestination = startDestination) {
        composable(ROUTE_MAIN) {
            val viewModel: MainViewModel = viewModel(factory = factory)
            MainScreen(
                viewModel = viewModel,
                onOpenSettings = { navController.navigate(ROUTE_SETTINGS) }
            )
        }
        composable(ROUTE_SETTINGS) {
            val viewModel: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(
                viewModel = viewModel,
                onBack = {
                    navController.navigate(ROUTE_MAIN) {
                        popUpTo(ROUTE_SETTINGS) { inclusive = true }
                    }
                }
            )
        }
    }
}
