package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PhishGuardViewModel
import com.example.ui.components.ActionPlaybookCard
import com.example.ui.components.ConfusionMatrixCard
import com.example.ui.components.IndicatorCard
import com.example.ui.components.RiskGauge
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.EarthBorder
import com.example.ui.theme.EarthDark
import com.example.ui.theme.EarthSurface
import com.example.ui.theme.EarthSurfaceVariant
import com.example.ui.theme.RiskDangerContainer
import com.example.ui.theme.RiskDangerRed
import com.example.ui.theme.RiskSafeGreen
import com.example.ui.theme.TextWarmMuted
import com.example.ui.theme.TextWarmPrimary
import com.example.ui.theme.TextWarmSecondary

@OptIn(ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: PhishGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanInput by viewModel.scanInput.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scanStage by viewModel.scanStage.collectAsState()
    val scanResult by viewModel.currentScanResult.collectAsState()
    val confusionMatrix by viewModel.confusionMatrix.collectAsState()
    val activeViewTab by viewModel.activeViewTab.collectAsState()
    val capturedNotifications by viewModel.capturedNotifications.collectAsState()
    val flaggedCount by viewModel.flaggedNotificationsCount.collectAsState()

    val isKeywordsLoaded by viewModel.isKeywordsLoaded.collectAsState()
    val isKeywordsLoading by viewModel.isKeywordsLoading.collectAsState()
    val keywordsLoadError by viewModel.keywordsLoadError.collectAsState()
    val userNotice by viewModel.userNotice.collectAsState()
    val debugResult by viewModel.debugResult.collectAsState()
    val selfTestReports by viewModel.selfTestReports.collectAsState()
    val isSelfTesting by viewModel.isSelfTesting.collectAsState()

    var showValidationModal by remember { mutableStateOf(false) }
    var isDebugExpanded by remember { mutableStateOf(false) }
    var showCustomJsonDialog by remember { mutableStateOf(false) }
    var customJsonInput by remember { mutableStateOf("") }

    val jsonFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            viewModel.loadCustomRulesFromUri(uri)
        }
    }

    if (showCustomJsonDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomJsonDialog = false },
            title = {
                Text("Custom keywords.json", fontWeight = FontWeight.Bold, color = TextWarmPrimary)
            },
            text = {
                Column {
                    Text(
                        "You can import a large, comprehensive keywords.json file directly from storage or paste its raw JSON content below. Rules are validated and saved automatically.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextWarmSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            showCustomJsonDialog = false
                            jsonFilePickerLauncher.launch("*/*")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EarthSurfaceVariant,
                            contentColor = AmberPrimary
                        )
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Select .json File From Storage", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "— OR PASTE RAW JSON BELOW —",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextWarmMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = customJsonInput,
                        onValueChange = { customJsonInput = it },
                        placeholder = { Text("{\n  \"scam\": { ... },\n  \"spam\": { ... },\n  \"important\": { ... }\n}", color = TextWarmMuted) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = EarthDark,
                            unfocusedContainerColor = EarthDark,
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = EarthBorder,
                            focusedTextColor = TextWarmPrimary,
                            unfocusedTextColor = TextWarmPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customJsonInput.isNotBlank()) {
                            viewModel.loadCustomRules(customJsonInput)
                        }
                        showCustomJsonDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary, contentColor = Color(0xFF1E1000))
                ) {
                    Text("Apply Rules", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomJsonDialog = false }) {
                    Text("Cancel", color = TextWarmSecondary)
                }
            },
            containerColor = EarthSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Validation Report Bottom Sheet
    if (showValidationModal) {
        ModalBottomSheet(
            onDismissRequest = { showValidationModal = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = EarthSurface,
            contentColor = TextWarmPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Accuracy & Model Testing Suite",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWarmPrimary
                        )
                    )
                    IconButton(onClick = { showValidationModal = false }) {
                        Icon(Icons.Default.Clear, contentDescription = "Close", tint = TextWarmSecondary)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                ConfusionMatrixCard(matrix = confusionMatrix)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.runAllBenchmarkTests()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AmberPrimary,
                        contentColor = Color(0xFF1E1000)
                    )
                ) {
                    Text("Re-Run 20-Sample Validation Suite", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EarthDark)
    ) {
        // Minimal Top Header
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(AmberPrimary, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Shield",
                        tint = Color(0xFF1E1000),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "PhishGuard India",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            color = TextWarmPrimary
                        )
                    )
                    Text(
                        text = "Instant Scam & Message Verifier",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextWarmSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Validation Telemetry Trigger
            Box(
                modifier = Modifier
                    .background(EarthSurfaceVariant, RoundedCornerShape(18.dp))
                    .border(1.dp, EarthBorder, RoundedCornerShape(18.dp))
                    .clickable { showValidationModal = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("accuracy_report_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Assessment,
                        contentDescription = "Accuracy Report",
                        tint = AmberPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${(confusionMatrix.accuracy * 100).toInt()}% Accuracy",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AmberPrimary,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Two-Segment Switcher: [ Instant Scan ] [ Alerts ]
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeViewTab == 0) AmberPrimary else EarthSurfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (activeViewTab == 0) AmberPrimary else EarthBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.setActiveViewTab(0) }
                    .padding(vertical = 9.dp)
                    .testTag("tab_instant_scan"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = if (activeViewTab == 0) EarthDark else TextWarmSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Instant Scan",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (activeViewTab == 0) EarthDark else TextWarmPrimary
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (activeViewTab == 1) AmberPrimary else EarthSurfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .border(
                        1.dp,
                        if (activeViewTab == 1) AmberPrimary else EarthBorder,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { viewModel.setActiveViewTab(1) }
                    .padding(vertical = 9.dp)
                    .testTag("tab_notification_guard"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = null,
                        tint = if (activeViewTab == 1) EarthDark else if (flaggedCount > 0) RiskDangerRed else TextWarmSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Alerts (${capturedNotifications.size})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (activeViewTab == 1) EarthDark else TextWarmPrimary
                        )
                    )
                    if (flaggedCount > 0) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (activeViewTab == 1) EarthDark else RiskDangerRed,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "$flaggedCount",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (activeViewTab == 1) AmberPrimary else TextWarmPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (activeViewTab == 1) {
            NotificationGuardView(
                viewModel = viewModel,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Requirement 2: Visible error if keywords.json failed to load
                keywordsLoadError?.let { errText ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(RiskDangerContainer, RoundedCornerShape(12.dp))
                                .border(1.dp, RiskDangerRed, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                                .testTag("keywords_load_error_banner")
                        ) {
                            Column {
                                Text(
                                    text = "Rule Engine Error",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = RiskDangerRed
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = errText,
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextWarmPrimary)
                                )
                            }
                        }
                    }
                }

                // Requirement 1 & 6: Notice for empty input / empty share target
                userNotice?.let { notice ->
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EarthSurfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, AmberPrimary, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("user_notice_banner")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = notice,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = AmberPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.dismissUserNotice() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Dismiss",
                                        tint = TextWarmSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Centered Hero Greeting (Visible when no result is active)
                if (scanResult == null) {
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Verify Before You Click or Pay",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWarmPrimary,
                                    textAlign = TextAlign.Center
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Paste any message, OTP SMS, bill notice, or payment request link to check if it's fraudulent, important, or spam.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWarmSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }

                // Centered Single-Action Input Field Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EarthSurface, RoundedCornerShape(18.dp))
                            .border(1.dp, EarthBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Privacy Protected",
                                    tint = RiskSafeGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isKeywordsLoading) "Loading rules..." else "Keywords Rules Active",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
                                        color = if (isKeywordsLoaded) RiskSafeGreen else AmberPrimary
                                    )
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = clipboard.primaryClip
                                        if (clip != null && clip.itemCount > 0) {
                                            val pasteText = clip.getItemAt(0).text?.toString() ?: ""
                                            viewModel.onScanInputChanged(pasteText)
                                        }
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste Clipboard",
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                if (scanInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onScanInputChanged("") },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear Input",
                                            tint = TextWarmMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Centered Input Box
                        OutlinedTextField(
                            value = scanInput,
                            onValueChange = { viewModel.onScanInputChanged(it) },
                            placeholder = {
                                Text(
                                    text = "Paste text or link here...\ne.g. 'Dear customer, your bank KYC has expired...', 'Your OTP is 482915', or refund request",
                                    style = MaterialTheme.typography.bodySmall.copy(color = TextWarmMuted)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .testTag("scan_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = EarthDark,
                                unfocusedContainerColor = EarthDark,
                                focusedBorderColor = AmberPrimary,
                                unfocusedBorderColor = EarthBorder,
                                focusedTextColor = TextWarmPrimary,
                                unfocusedTextColor = TextWarmPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Requirement 2: Analyze is disabled until keywords.json has loaded
                        Button(
                            onClick = { viewModel.runCurrentScan() },
                            enabled = isKeywordsLoaded && !isScanning,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("run_scan_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberPrimary,
                                contentColor = Color(0xFF1E1000),
                                disabledContainerColor = EarthSurfaceVariant,
                                disabledContentColor = TextWarmMuted
                            )
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    color = Color(0xFF1E1000),
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Analyzing Fraud Patterns...",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            } else if (isKeywordsLoading) {
                                CircularProgressIndicator(
                                    color = AmberPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Loading Keywords...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Analyze",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "ANALYZE MESSAGE",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Requirement 9: Run Self-Test Button & Results Card
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EarthSurface, RoundedCornerShape(14.dp))
                            .border(1.dp, EarthBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Button(
                            onClick = { viewModel.runSelfTest() },
                            enabled = isKeywordsLoaded && !isSelfTesting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("run_self_test_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EarthSurfaceVariant,
                                contentColor = AmberPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isSelfTesting) {
                                CircularProgressIndicator(
                                    color = AmberPrimary,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Evaluating 7 Self-Tests...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Assessment,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Run Self-Test (7 Validation Checks)", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (selfTestReports.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            val passCount = selfTestReports.count { it.passed }
                            val totalCount = selfTestReports.size
                            val allPassed = passCount == totalCount

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (allPassed) Color(0xFF1B5E20).copy(alpha = 0.35f) else RiskDangerContainer,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (allPassed) RiskSafeGreen else RiskDangerRed,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (allPassed) "✓ All $totalCount / $totalCount Self-Tests Passed!" else "⚠ $passCount / $totalCount Self-Tests Passed",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (allPassed) RiskSafeGreen else RiskDangerRed
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                selfTestReports.forEach { test ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(EarthDark, RoundedCornerShape(8.dp))
                                            .border(1.dp, if (test.passed) EarthBorder else RiskDangerRed.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Test [${test.letter}]",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = AmberPrimary
                                                    )
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            if (test.passed) RiskSafeGreen.copy(alpha = 0.2f) else RiskDangerRed.copy(alpha = 0.2f),
                                                            RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = if (test.passed) "PASS" else "FAIL",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            fontWeight = FontWeight.Black,
                                                            color = if (test.passed) RiskSafeGreen else RiskDangerRed,
                                                            fontSize = 9.sp
                                                        )
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "\"${test.text}\"",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextWarmSecondary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Expected: ${test.expected} | Actual: ${test.actual} (${test.decisionPath})",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = if (test.passed) TextWarmPrimary else RiskDangerRed,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Requirement 7: Visible "Debug" section (collapsed by default)
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(EarthSurface, RoundedCornerShape(14.dp))
                            .border(1.dp, EarthBorder, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                            .testTag("debug_section")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isDebugExpanded = !isDebugExpanded }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.BugReport,
                                    contentDescription = "Debug",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Debug & Rule Telemetry",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextWarmPrimary
                                    )
                                )
                            }
                            Text(
                                text = if (isDebugExpanded) "Hide ▲" else "Show ▼",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = AmberPrimary
                                )
                            )
                        }

                        AnimatedVisibility(visible = isDebugExpanded) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // 1. Phrases loaded per class
                                Column {
                                    Text(
                                        text = "1. Phrases Loaded Per Class:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                    )
                                    Text(
                                        text = debugResult?.phrasesPerClass
                                            ?: "Scam: 36 phrases (12 groups) | Spam: 18 phrases (5 groups) | Important: 16 phrases (5 groups)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextWarmPrimary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }

                                // 2. Normalised input
                                Column {
                                    Text(
                                        text = "2. Normalised Input:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                    )
                                    Text(
                                        text = debugResult?.normalizedInput?.ifEmpty { "(none)" } ?: "(none)",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextWarmPrimary,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                }

                                // 3. Each matched group and phrase with points
                                Column {
                                    Text(
                                        text = "3. Matched Groups & Phrases with Points:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                    )
                                    if (debugResult?.matchedGroups.isNullOrEmpty()) {
                                        Text(
                                            text = "No rule groups matched",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = TextWarmMuted,
                                                fontSize = 11.sp
                                            )
                                        )
                                    } else {
                                        debugResult?.matchedGroups?.forEach { grp ->
                                            Text(
                                                text = "• [${grp.category.uppercase()}] ${grp.groupName} (+${grp.points} pts)\n  Phrases: ${grp.matchedPhrases.joinToString(", ")}",
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    color = TextWarmPrimary,
                                                    fontSize = 11.sp
                                                )
                                            )
                                        }
                                    }
                                }

                                // 4. Three scores
                                Column {
                                    Text(
                                        text = "4. Scores (Scam / Spam / Important):",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                    )
                                    Text(
                                        text = "Scam Score: ${debugResult?.scamScore ?: 0} | Spam Score: ${debugResult?.spamScore ?: 0} | Important Score: ${debugResult?.importantScore ?: 0}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextWarmPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                // 5. Final category
                                Column {
                                    Text(
                                        text = "5. Final Category:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                    )
                                    Text(
                                        text = debugResult?.category ?: "Unclear",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = TextWarmPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                // 6. Decision path
                                Column {
                                    Text(
                                        text = "6. Decision Path:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                    )
                                    Text(
                                        text = debugResult?.decisionPath ?: "rules",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = AmberPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                // 7. Caught error
                                Column {
                                    Text(
                                        text = "7. Caught Error:",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = AmberPrimary
                                        )
                                    )
                                    Text(
                                        text = debugResult?.error ?: "None",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (debugResult?.error != null) RiskDangerRed else TextWarmMuted,
                                            fontSize = 11.sp
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { jsonFilePickerLauncher.launch("*/*") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EarthSurfaceVariant,
                                            contentColor = AmberPrimary
                                        )
                                    ) {
                                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Import File", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { showCustomJsonDialog = true },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EarthSurfaceVariant,
                                            contentColor = AmberPrimary
                                        )
                                    ) {
                                        Text("Paste JSON", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { viewModel.reloadDefaultRules() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EarthSurfaceVariant,
                                            contentColor = TextWarmSecondary
                                        )
                                    ) {
                                        Text("Reset Rules", fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Quick Preset Examples
                if (scanResult == null) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "OR TAP A PRESET EXAMPLE TO TEST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWarmSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.Center,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val samples = listOfNotNull(
                                    viewModel.allBenchmarkCases.firstOrNull { it.id == "IN-KYC-01" },
                                    viewModel.allBenchmarkCases.firstOrNull { it.id == "IN-AADHAAR-01" },
                                    viewModel.allBenchmarkCases.firstOrNull { it.id == "IN-UPI-03" },
                                    viewModel.allBenchmarkCases.firstOrNull { it.id == "IN-UPI-04" },
                                    viewModel.allBenchmarkCases.firstOrNull { it.id == "IN-UPI-05" },
                                    viewModel.allBenchmarkCases.firstOrNull { it.id == "IN-ELEC-01" },
                                    viewModel.allBenchmarkCases.firstOrNull { it.id == "IN-POLICE-01" },
                                    viewModel.allBenchmarkCases.firstOrNull { it.id == "IN-LEGIT-01" }
                                )
                                samples.forEach { sample ->
                                    Box(
                                        modifier = Modifier
                                            .background(EarthSurfaceVariant, RoundedCornerShape(8.dp))
                                            .border(1.dp, EarthBorder, RoundedCornerShape(8.dp))
                                            .clickable { viewModel.loadPresetScenario(sample) }
                                            .padding(horizontal = 9.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = sample.title,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = TextWarmPrimary
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Active Scan Progress
                if (isScanning) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EarthSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = AmberPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = scanStage,
                                style = MaterialTheme.typography.bodySmall.copy(color = TextWarmPrimary)
                            )
                        }
                    }
                }

                // Result Card (Never says Safe; uses Scam (High/Medium), Important, Spam, or Unclear)
                scanResult?.let { result ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EarthSurface, RoundedCornerShape(16.dp))
                                .border(1.dp, EarthBorder, RoundedCornerShape(16.dp))
                                .padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            RiskGauge(
                                riskScore = result.riskScore,
                                riskLevel = result.riskLevel,
                                category = result.classifiedCategory
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = result.classifiedCategory,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    color = TextWarmPrimary,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = result.summaryExplanation,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = TextWarmSecondary,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.Center
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            TextButton(
                                onClick = { viewModel.onScanInputChanged("") },
                                colors = ButtonDefaults.textButtonColors(contentColor = AmberPrimary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Check Another",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Check Another Message", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (result.indicators.isNotEmpty()) {
                        item {
                            Text(
                                text = "MATCHED INDICATOR GROUPS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWarmSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }

                        items(result.indicators) { indicator ->
                            IndicatorCard(indicator = indicator)
                        }
                    }

                    if (result.recommendedActions.isNotEmpty()) {
                        item {
                            ActionPlaybookCard(actions = result.recommendedActions)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
