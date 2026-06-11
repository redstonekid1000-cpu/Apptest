package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PaywallScreen
import com.example.ui.screens.HandwriteScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuditViewModel
import com.example.ui.viewmodel.HandwriteViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme(dynamicColor = false) {
        val auditViewModel: AuditViewModel = viewModel()
        val handwriteViewModel: HandwriteViewModel = viewModel()
        val currentScreen by auditViewModel.currentScreen.collectAsState()

        when (currentScreen) {
          Screen.Auth -> AuthScreen(viewModel = auditViewModel)
          Screen.Paywall -> PaywallScreen(viewModel = auditViewModel)
          Screen.Dashboard -> HandwriteScreen(viewModel = handwriteViewModel, auditViewModel = auditViewModel)
        }
      }
    }
  }
}
