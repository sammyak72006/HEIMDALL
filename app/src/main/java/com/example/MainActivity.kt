package com.example

import android.content.Intent
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
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity() {

    private val viewModel: PhishGuardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleSharedIntent(intent)
        setContent {
            PhishGuardTheme {
                MainAppContent(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleSharedIntent(intent)
    }

    /**
     * Requirement 6: Shared text:
     * read the title, text, and url fields from the share target,
     * URL-decode them, join them, and trim.
     * Use that combined text as the input. If it is still empty, say so.
     */
    private fun handleSharedIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW) {
            val title = intent.getStringExtra(Intent.EXTRA_TITLE)
                ?: intent.getStringExtra("title")
                ?: ""
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
                ?: intent.getStringExtra("text")
                ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                ?: ""
            val url = intent.getStringExtra("url")
                ?: intent.dataString
                ?: ""

            fun decode(s: String): String {
                if (s.isBlank()) return ""
                return runCatching { URLDecoder.decode(s, StandardCharsets.UTF_8.name()) }.getOrDefault(s)
            }

            val decodedTitle = decode(title)
            val decodedText = decode(text)
            val decodedUrl = decode(url)

            val combined = listOf(decodedTitle, decodedText, decodedUrl)
                .filter { it.isNotBlank() }
                .joinToString(" ")
                .trim()

            if (combined.isEmpty()) {
                viewModel.onSharedTextReceived("", isEmpty = true)
            } else {
                viewModel.onSharedTextReceived(combined, isEmpty = false)
            }
        }
    }
}

@Composable
fun MainAppContent(viewModel: PhishGuardViewModel) {
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
