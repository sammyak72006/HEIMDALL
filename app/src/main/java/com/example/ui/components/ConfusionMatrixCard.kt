package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ConfusionMatrix
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.EarthBorder
import com.example.ui.theme.EarthDark
import com.example.ui.theme.EarthSurface
import com.example.ui.theme.EarthSurfaceVariant
import com.example.ui.theme.RiskDangerRed
import com.example.ui.theme.RiskSafeGreen
import com.example.ui.theme.RiskWarningAmber
import com.example.ui.theme.TextWarmPrimary
import com.example.ui.theme.TextWarmSecondary
import java.util.Locale

@Composable
fun ConfusionMatrixCard(
    matrix: ConfusionMatrix,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EarthSurfaceVariant, RoundedCornerShape(14.dp))
            .border(1.dp, EarthBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Assessment,
                contentDescription = "Accuracy Report",
                tint = AmberPrimary,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Model Testing & Accuracy Report",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricPill(
                title = "ACCURACY",
                value = String.format(Locale.US, "%.1f%%", matrix.accuracy * 100),
                color = RiskSafeGreen,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                title = "PRECISION",
                value = String.format(Locale.US, "%.1f%%", matrix.precision * 100),
                color = AmberPrimary,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                title = "RECALL",
                value = String.format(Locale.US, "%.1f%%", matrix.recall * 100),
                color = RiskWarningAmber,
                modifier = Modifier.weight(1f)
            )
            MetricPill(
                title = "F1-SCORE",
                value = String.format(Locale.US, "%.2f", matrix.f1Score),
                color = Color(0xFFE2E8F0),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Simple progress bar
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Tested Cases",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF9CA3AF))
                )
                Text(
                    text = "${matrix.truePositives + matrix.trueNegatives} of ${matrix.totalTested} correct",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { matrix.accuracy.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = RiskSafeGreen,
                trackColor = EarthSurface,
                strokeCap = StrokeCap.Round
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x2 Matrix table
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(modifier = Modifier.weight(1f))
            MatrixHeaderCell(title = "Actual Scam", modifier = Modifier.weight(1.3f))
            MatrixHeaderCell(title = "Actual Legit", modifier = Modifier.weight(1.3f))
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MatrixHeaderCell(title = "Pred. Scam", modifier = Modifier.weight(1f))
            MatrixValueCell(
                count = matrix.truePositives,
                label = "True Scam",
                color = RiskSafeGreen,
                modifier = Modifier.weight(1.3f)
            )
            MatrixValueCell(
                count = matrix.falsePositives,
                label = "False Alarm",
                color = RiskDangerRed,
                modifier = Modifier.weight(1.3f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MatrixHeaderCell(title = "Pred. Legit", modifier = Modifier.weight(1f))
            MatrixValueCell(
                count = matrix.falseNegatives,
                label = "Missed Scam",
                color = RiskWarningAmber,
                modifier = Modifier.weight(1.3f)
            )
            MatrixValueCell(
                count = matrix.trueNegatives,
                label = "True Safe",
                color = RiskSafeGreen,
                modifier = Modifier.weight(1.3f)
            )
        }
    }
}

@Composable
private fun MetricPill(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(EarthDark, RoundedCornerShape(8.dp))
            .border(1.dp, EarthBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = TextWarmSecondary
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                color = color
            )
        )
    }
}

@Composable
private fun MatrixHeaderCell(
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(EarthSurface, RoundedCornerShape(6.dp))
            .border(0.5.dp, EarthBorder, RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = TextWarmPrimary
            )
        )
    }
}

@Composable
private fun MatrixValueCell(
    count: Int,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 8.sp,
                color = color
            )
        )
    }
}
