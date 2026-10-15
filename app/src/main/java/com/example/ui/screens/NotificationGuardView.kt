package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.NotificationReaderService
import com.example.ui.PhishGuardViewModel
import com.example.ui.components.NotificationCard
import com.example.ui.theme.AmberDark
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.EarthBorder
import com.example.ui.theme.EarthDark
import com.example.ui.theme.EarthSurface
import com.example.ui.theme.EarthSurfaceVariant
import com.example.ui.theme.RiskDangerRed
import com.example.ui.theme.RiskSafeGreen
import com.example.ui.theme.RiskWarningAmber
import com.example.ui.theme.TextWarmMuted
import com.example.ui.theme.TextWarmPrimary
import com.example.ui.theme.TextWarmSecondary

@Composable
fun NotificationGuardView(
    viewModel: PhishGuardViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifications by viewModel.capturedNotifications.collectAsState()
    val isListenerActive by viewModel.isNotificationListenerActive.collectAsState()
    val filter by viewModel.notificationFilter.collectAsState()
    val flaggedCount by viewModel.flaggedNotificationsCount.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkNotificationPermission()
    }

    val filteredNotifications = when (filter) {
        "FLAGGED" -> notifications.filter { it.isFlagged }
        else -> notifications
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Notification Service Permission & Status Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EarthSurface, RoundedCornerShape(14.dp))
                    .border(
                        1.dp,
                        if (isListenerActive) RiskSafeGreen.copy(alpha = 0.4f) else AmberPrimary.copy(alpha = 0.5f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isListenerActive) RiskSafeGreen.copy(alpha = 0.2f) else AmberDark.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isListenerActive) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = if (isListenerActive) RiskSafeGreen else AmberPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (isListenerActive) "Notification Guard Active" else "Notification Listener Inactive",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextWarmPrimary
                                )
                            )
                            Text(
                                text = if (isListenerActive)
                                    "Actively reading SMS, WhatsApp & payment alerts"
                                else
                                    "Enable notification access to read alerts automatically",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = TextWarmSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }

                    if (!isListenerActive) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { NotificationReaderService.openSettings(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AmberPrimary,
                                contentColor = EarthDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("enable_notification_access_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = EarthDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Filter and Test Controls Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // All Pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (filter == "ALL") AmberPrimary else EarthSurfaceVariant,
                                RoundedCornerShape(18.dp)
                            )
                            .border(
                                1.dp,
                                if (filter == "ALL") AmberPrimary else EarthBorder,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { viewModel.setNotificationFilter("ALL") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_all_notifications")
                    ) {
                        Text(
                            text = "All Alerts (${notifications.size})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (filter == "ALL") EarthDark else TextWarmPrimary
                            )
                        )
                    }

                    // Flagged Scams Pill
                    Box(
                        modifier = Modifier
                            .background(
                                if (filter == "FLAGGED") AmberPrimary else EarthSurfaceVariant,
                                RoundedCornerShape(18.dp)
                            )
                            .border(
                                1.dp,
                                if (filter == "FLAGGED") AmberPrimary else EarthBorder,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable { viewModel.setNotificationFilter("FLAGGED") }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("filter_flagged_notifications")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (flaggedCount > 0) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (filter == "FLAGGED") EarthDark else RiskDangerRed,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                text = "Flagged Scams ($flaggedCount)",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (filter == "FLAGGED") EarthDark else TextWarmPrimary
                                )
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Simulate Sample Notification Button
                    OutlinedButton(
                        onClick = { viewModel.simulateSampleNotification() },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("simulate_notification_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddAlert,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = AmberPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate", fontSize = 10.sp)
                    }

                    if (notifications.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.clearAllNotifications() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWarmMuted),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("clear_notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ClearAll,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = TextWarmMuted
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Clear", fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        // Empty state
        if (filteredNotifications.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = TextWarmMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (filter == "FLAGGED") "No Flagged Scams" else "No Notifications Yet",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWarmPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (filter == "FLAGGED")
                            "You haven't flagged any notifications as scams yet."
                        else
                            "Incoming notifications from SMS, WhatsApp, Google Pay, and banks will be captured and scanned here.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextWarmSecondary,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.simulateSampleNotification() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = EarthDark
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("test_first_notification_button")
                    ) {
                        Text("Simulate Phishing Alert", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(
                items = filteredNotifications,
                key = { it.id }
            ) { notification ->
                NotificationCard(
                    notification = notification,
                    onFlag = { reason -> viewModel.flagNotification(notification.id, reason) },
                    onUnflag = { viewModel.unflagNotification(notification.id) },
                    onScan = { viewModel.scanCapturedNotification(notification) },
                    onDelete = { viewModel.deleteNotification(notification.id) }
                )
            }
        }
    }
}
