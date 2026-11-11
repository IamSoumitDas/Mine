package com.soumit.mine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.soumit.mine.auth.AuthRoute
import com.soumit.mine.auth.AuthScreen
import com.soumit.mine.auth.AuthUiState
import com.soumit.mine.auth.AuthViewModel
import com.soumit.mine.ui.theme.MineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MineTheme {
                val authViewModel: AuthViewModel = viewModel()
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AuthRoute(
                        viewModel = authViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Preview(name = "App Sign In Preview", showBackground = true)
@Composable
fun MainAuthPreview() {
    MineTheme {
        AuthScreen(
            uiState = AuthUiState(),
            onSignInEmail = { _, _ -> },
            onSignUpEmail = { _, _, _, _ -> },
            onForgotPassword = {},
            onDismissError = {}
        )
    }
}
