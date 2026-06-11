package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PaywallScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuditViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(dynamicColor = false) {
        val viewModel: AuditViewModel = viewModel()
        val currentScreen by viewModel.currentScreen.collectAsState()

        when (currentScreen) {
          Screen.Auth -> AuthScreen(viewModel = viewModel)
          Screen.Paywall -> PaywallScreen(viewModel = viewModel)
          Screen.Dashboard -> DashboardScreen(viewModel = viewModel)
        }
      }
    }
  }
}
