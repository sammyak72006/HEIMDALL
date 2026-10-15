package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.PhishGuardViewModel
import com.example.ui.screens.ScannerScreen
import com.example.ui.theme.EarthDark
import com.example.ui.theme.PhishGuardTheme

class MainActivity : ComponentActivity() {

    private val viewModel: PhishGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhishGuardTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PhishGuardViewModel) {
    // Single-action centered scanning interface without secondary navigation bars
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = EarthDark
    ) { innerPadding ->
        ScannerScreen(
            viewModel = viewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
