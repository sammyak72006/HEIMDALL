package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.RiskLevel
import com.example.ui.theme.EarthBorder
import com.example.ui.theme.RiskDangerContainer
import com.example.ui.theme.RiskDangerRed
import com.example.ui.theme.RiskSafeContainer
import com.example.ui.theme.RiskSafeGreen
import com.example.ui.theme.RiskWarningAmber
import com.example.ui.theme.RiskWarningContainer

@Composable
fun RiskGauge(
    riskScore: Int,
    riskLevel: RiskLevel,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(riskScore) {
        animatedProgress.animateTo(
            targetValue = (riskScore / 100f).coerceIn(0f, 1f),
            animationSpec = tween(durationMillis = 650)
        )
    }

    val (statusColor, containerColor, labelText, symbol) = when (riskLevel) {
        RiskLevel.LOW -> Quadruple(RiskSafeGreen, RiskSafeContainer, "SAFE • LOW RISK", "✓")
        RiskLevel.MEDIUM -> Quadruple(RiskWarningAmber, RiskWarningContainer, "SUSPICIOUS • CAUTION", "!")
        RiskLevel.HIGH -> Quadruple(RiskDangerRed, RiskDangerContainer, "DANGER • HIGH SCAM RISK", "⚠")
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(140.dp)
        ) {
            Canvas(modifier = Modifier.size(130.dp)) {
                val strokeWidth = 11.dp.toPx()
                drawArc(
                    color = Color(0xFF26211C),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                val sweep = 270f * animatedProgress.value
                drawArc(
                    color = statusColor,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${(animatedProgress.value * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Black,
                        color = statusColor
                    )
                )
                Text(
                    text = "Risk Level",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFFB5ABA0),
                        fontSize = 10.sp
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .background(containerColor.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$symbol  $labelText",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            )
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
