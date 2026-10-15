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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.PhishGuardViewModel
import com.example.ui.components.ActionPlaybookCard
import com.example.ui.components.ConfusionMatrixCard
import com.example.ui.components.IndicatorCard
import com.example.ui.components.RiskGauge
import com.example.ui.screens.NotificationGuardView
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.EarthBorder
import com.example.ui.theme.EarthDark
import com.example.ui.theme.EarthSurface
import com.example.ui.theme.EarthSurfaceVariant
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

    var showValidationModal by remember { mutableStateOf(false) }

    // Validation Report Bottom Sheet (keeps main dashboard 100% single-action without persistent navigation)
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
        // Minimal Top Header with Discreet Telemetry Button
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
                        text = "Instant Scam & Fake KYC Verifier",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextWarmSecondary,
                            fontSize = 10.sp
                        )
                    )
                }
            }

            // Discreet Validation Telemetry Trigger (No persistent secondary nav bar)
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

        // Clean Two-Segment Switcher: [ 🛡️ Single-Action Scan ] [ 🔔 Notification Guard (N) ]
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
                // If flagged notification exists, subtle reminder banner
                if (flaggedCount > 0) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(EarthSurfaceVariant, RoundedCornerShape(12.dp))
                                .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .clickable { viewModel.setActiveViewTab(1) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("banner_flagged_notifications_reminder")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$flaggedCount suspicious alert detected",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = TextWarmPrimary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                }
                                Text(
                                    text = "Review & Flag →",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = AmberPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

        // Centered Hero Greeting (Visible when no result is active)
        if (scanResult == null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
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
                        text = "Paste any suspicious SMS, electricity bill warning, fake bank KYC message, or payment request link to check if it's safe.",
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
                            text = "Aadhaar & PAN Privately Masked",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                color = TextWarmSecondary
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
                            text = "Paste text or link here...\ne.g. 'Dear customer, your bank KYC has expired...', Aadhaar link required, or UPI collect request",
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

                // Single Main Action Button
                Button(
                    onClick = { viewModel.runCurrentScan() },
                    enabled = scanInput.isNotBlank() && !isScanning,
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
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Verify",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VERIFY FOR FRAUD",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }

        // Quick Tap Indian Scam Examples (Centered)
        if (scanResult == null) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "OR TAP A COMMON INDIAN SCAM TO TEST",
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

        // Result Card (Uncluttered, Centered)
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
                        riskLevel = result.riskLevel
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = result.scenario.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
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
                        text = "SUSPICIOUS SIGNALS IDENTIFIED",
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

            item {
                ActionPlaybookCard(actions = result.recommendedActions)
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
}
}
