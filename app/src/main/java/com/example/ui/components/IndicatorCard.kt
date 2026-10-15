package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.IndicatorSeverity
import com.example.model.ThreatIndicator
import com.example.ui.theme.EarthSurfaceVariant
import com.example.ui.theme.RiskDangerRed
import com.example.ui.theme.RiskSafeGreen
import com.example.ui.theme.RiskWarningAmber

@Composable
fun IndicatorCard(
    indicator: ThreatIndicator,
    modifier: Modifier = Modifier
) {
    val (icon, tint, borderColor) = when (indicator.severity) {
        IndicatorSeverity.CRITICAL -> Triple(Icons.Default.Warning, RiskDangerRed, RiskDangerRed.copy(alpha = 0.35f))
        IndicatorSeverity.WARNING -> Triple(Icons.Default.Warning, RiskWarningAmber, RiskWarningAmber.copy(alpha = 0.3f))
        IndicatorSeverity.INFO -> Triple(Icons.Default.Info, RiskSafeGreen, RiskSafeGreen.copy(alpha = 0.3f))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(EarthSurfaceVariant, RoundedCornerShape(10.dp))
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = indicator.severity.name,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = indicator.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFAF5EF)
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = indicator.description,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color(0xFFB5ABA0),
                    lineHeight = 16.sp
                )
            )
        }
    }
}
