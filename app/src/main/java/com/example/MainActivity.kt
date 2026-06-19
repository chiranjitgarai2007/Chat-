package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val viewModel = ViewModelProvider(this, ChatViewModel.Factory(application))[ChatViewModel::class.java]

        setContent {
            val darkThemeOverride by viewModel.darkThemeOverride.collectAsStateWithLifecycle()
            val themeToUse = darkThemeOverride ?: androidx.compose.foundation.isSystemInDarkTheme()

            MyApplicationTheme(darkTheme = themeToUse) {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    modifier = Modifier.fillMaxSize(),
                    label = "ScreenTransition"
                ) { screen ->
                    when (screen) {
                        Screen.ONBOARDING -> OnboardingScreen(viewModel)
                        Screen.SIGN_IN -> SignInScreen(viewModel)
                        Screen.SIGN_UP -> SignUpScreen(viewModel)
                        Screen.DASHBOARD -> DashboardScreen(viewModel)
                        Screen.CHAT -> ChatDetailScreen(viewModel)
                        Screen.ADMIN_DASHBOARD -> AdminScreen(viewModel)
                    }
                }
            }
        }
    }
}
