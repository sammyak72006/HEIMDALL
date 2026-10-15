package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.OutlinedFlag
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CapturedNotification
import com.example.model.RiskLevel
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NotificationCard(
    notification: CapturedNotification,
    onFlag: (reason: String) -> Unit,
    onUnflag: () -> Unit,
    onScan: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showFlagDialog by remember { mutableStateOf(false) }

    val appIcon: ImageVector = when {
        notification.appName.contains("Pay", ignoreCase = true) ||
                notification.appName.contains("UPI", ignoreCase = true) ||
                notification.appName.contains("PhonePe", ignoreCase = true) -> Icons.Default.Payment
        notification.appName.contains("Message", ignoreCase = true) ||
                notification.appName.contains("SMS", ignoreCase = true) -> Icons.Default.Sms
        else -> Icons.Default.Notifications
    }

    val timeFormatted = remember(notification.timestamp) {
        val diffMs = System.currentTimeMillis() - notification.timestamp
        val minutes = diffMs / 60_000
        val hours = minutes / 60
        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${hours / 24}d ago"
        }
    }

    // Flagging Dialog
    if (showFlagDialog) {
        val scamCategories = listOf(
            "Fake Bank KYC / Aadhaar Expiry",
            "Fake UPI Collect Request",
            "Fraudulent Merchant QR Code",
            "Electricity Disconnection Scam",
            "Part-Time Work / Lottery Scam",
            "Suspicious Phishing Link"
        )
        AlertDialog(
            onDismissRequest = { showFlagDialog = false },
            containerColor = EarthSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Flag Notification as Scam",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWarmPrimary
                        )
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Select fraud signature to record and help flag similar attacks:",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextWarmSecondary)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        scamCategories.forEach { category ->
                            Box(
                                modifier = Modifier
                                    .background(EarthSurfaceVariant, RoundedCornerShape(8.dp))
                                    .border(1.dp, EarthBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("flag_reason_${category.take(10)}")
                            ) {
                                TextButton(
                                    onClick = {
                                        onFlag(category)
                                        showFlagDialog = false
                                    },
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = AmberPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFlagDialog = false }) {
                    Text("Cancel", color = TextWarmSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (notification.isFlagged) EarthSurfaceVariant else EarthSurface,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (notification.isFlagged) AmberPrimary.copy(alpha = 0.5f) else EarthBorder,
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
    ) {
        // Top row: App Name + Timestamp + Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(AmberDark.copy(alpha = 0.35f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = appIcon,
                        contentDescription = notification.appName,
                        tint = AmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = notification.appName,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextWarmPrimary
                        )
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            color = TextWarmMuted
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Risk Badge
                val (riskBg, riskText, riskLabel) = when {
                    notification.isFlagged -> Triple(
                        AmberPrimary.copy(alpha = 0.2f),
                        AmberPrimary,
                        "FLAGGED"
                    )
                    notification.riskLevel == RiskLevel.HIGH -> Triple(
                        RiskDangerRed.copy(alpha = 0.2f),
                        RiskDangerRed,
                        "HIGH RISK"
                    )
                    notification.riskLevel == RiskLevel.MEDIUM -> Triple(
                        RiskWarningAmber.copy(alpha = 0.2f),
                        RiskWarningAmber,
                        "SUSPICIOUS"
                    )
                    else -> Triple(
                        RiskSafeGreen.copy(alpha = 0.15f),
                        RiskSafeGreen,
                        "SAFE"
                    )
                }

                Box(
                    modifier = Modifier
                        .background(riskBg, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = riskLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = riskText
                        )
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_notif_${notification.id.take(6)}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Notification",
                        tint = TextWarmMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title (if present)
        if (notification.title.isNotBlank()) {
            Text(
                text = notification.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextWarmPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
        }

        // Notification Content
        Text(
            text = notification.content,
            style = MaterialTheme.typography.bodySmall.copy(
                color = TextWarmSecondary,
                lineHeight = 18.sp
            )
        )

        // Flag explanation banner if flagged
        if (notification.isFlagged && !notification.flagReason.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EarthDark, RoundedCornerShape(8.dp))
                    .border(1.dp, AmberPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Flagged: ${notification.flagReason}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AmberPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions Row: Flag / Unflag + Deep Scan
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (notification.isFlagged) {
                OutlinedButton(
                    onClick = onUnflag,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(AmberPrimary.copy(alpha = 0.6f))
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("unflag_button_${notification.id.take(6)}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AmberPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flagged (Tap to Clear)", fontSize = 11.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { showFlagDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWarmPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(EarthBorder)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .testTag("flag_button_${notification.id.take(6)}")
                ) {
                    Icon(
                        imageVector = Icons.Default.OutlinedFlag,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = AmberPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Flag as Scam", fontSize = 11.sp, color = TextWarmPrimary)
                }
            }

            Button(
                onClick = onScan,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    contentColor = EarthDark
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .testTag("scan_notif_${notification.id.take(6)}")
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = EarthDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Verify in Scanner",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
