package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BenchmarkCase
import com.example.model.FraudScenario
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.ui.PhishGuardViewModel
import com.example.ui.components.ConfusionMatrixCard
import com.example.ui.theme.RiskDangerRed
import com.example.ui.theme.RiskSafeGreen
import com.example.ui.theme.SaffronPrimary
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard

@Composable
fun TestLabScreen(
    viewModel: PhishGuardViewModel,
    modifier: Modifier = Modifier
) {
    val benchmarkResults by viewModel.benchmarkResults.collectAsState()
    val isRunningBatch by viewModel.isRunningBatchBenchmark.collectAsState()
    val activeTestingCaseId by viewModel.activeTestingCaseId.collectAsState()
    val confusionMatrix by viewModel.confusionMatrix.collectAsState()

    var selectedFilterCategory by remember { mutableStateOf<FraudScenario?>(null) }

    val filteredCases = if (selectedFilterCategory == null) {
        viewModel.allBenchmarkCases
    } else {
        viewModel.allBenchmarkCases.filter { it.category == selectedFilterCategory }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF121417))
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            Column {
                Text(
                    text = "Testing & Verification Lab",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                )
                Text(
                    text = "Accuracy telemetry tested across real Indian scams & legitimate messages",
                    style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF9CA3AF))
                )
            }
        }

        item {
            ConfusionMatrixCard(
                matrix = confusionMatrix,
                modifier = Modifier.testTag("confusion_matrix_card")
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.runAllBenchmarkTests() },
                    enabled = !isRunningBatch,
                    modifier = Modifier
                        .weight(2f)
                        .height(46.dp)
                        .testTag("run_all_tests_button"),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SaffronPrimary,
                        contentColor = Color(0xFF1E1500)
                    )
                ) {
                    if (isRunningBatch) {
                        CircularProgressIndicator(
                            color = Color(0xFF1E1500),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Testing All Cases...",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Run All",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TEST ALL EXAMPLES",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.resetBenchmarkTelemetry() },
                    enabled = !isRunningBatch && confusionMatrix.totalTested > 0,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9CA3AF))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "RESET",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    CategoryChip(
                        label = "All (${viewModel.allBenchmarkCases.size})",
                        isSelected = selectedFilterCategory == null,
                        onClick = { selectedFilterCategory = null }
                    )
                }
                items(FraudScenario.values()) { scenario ->
                    val count = viewModel.allBenchmarkCases.count { it.category == scenario }
                    if (count > 0) {
                        CategoryChip(
                            label = "${scenario.displayName.take(16)}... ($count)",
                            isSelected = selectedFilterCategory == scenario,
                            onClick = { selectedFilterCategory = scenario }
                        )
                    }
                }
            }
        }

        items(filteredCases) { testCase ->
            val result = benchmarkResults[testCase.id]
            val isEvaluatingThis = activeTestingCaseId == testCase.id

            BenchmarkCaseItem(
                testCase = testCase,
                result = result,
                isEvaluating = isEvaluatingThis,
                onRunTest = { viewModel.runSingleBenchmarkCase(testCase) },
                onLoadInScanner = { viewModel.loadPresetScenario(testCase) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) SaffronPrimary.copy(alpha = 0.2f) else SurfaceCard,
                RoundedCornerShape(8.dp)
            )
            .border(
                1.dp,
                if (isSelected) SaffronPrimary else SurfaceBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) SaffronPrimary else Color(0xFF9CA3AF)
            )
        )
    }
}

@Composable
private fun BenchmarkCaseItem(
    testCase: BenchmarkCase,
    result: ScanResult?,
    isEvaluating: Boolean,
    onRunTest: () -> Unit,
    onLoadInScanner: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceCard, RoundedCornerShape(10.dp))
            .border(1.dp, SurfaceBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = testCase.category.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = SaffronPrimary
                )
            )

            val gtColor = if (testCase.groundTruthMalicious) RiskDangerRed else RiskSafeGreen
            val gtText = if (testCase.groundTruthMalicious) "Actual: Scam" else "Actual: Legit"
            Box(
                modifier = Modifier
                    .background(gtColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = gtText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = gtColor
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = testCase.title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = testCase.input,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.sp,
                color = Color(0xFF9CA3AF)
            ),
            maxLines = 2
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (result != null) {
                val predictedMalicious = result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.MEDIUM
                val isCorrect = predictedMalicious == testCase.groundTruthMalicious

                val matrixClassification = when {
                    testCase.groundTruthMalicious && predictedMalicious -> "True Positive (Scam Caught)"
                    !testCase.groundTruthMalicious && !predictedMalicious -> "True Negative (Benign Verified)"
                    !testCase.groundTruthMalicious && predictedMalicious -> "False Alarm"
                    else -> "Missed Scam"
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = "Result",
                        tint = if (isCorrect) RiskSafeGreen else RiskDangerRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = matrixClassification,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isCorrect) RiskSafeGreen else RiskDangerRed
                        )
                    )
                }
            } else if (isEvaluating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(13.dp),
                        color = SaffronPrimary,
                        strokeWidth = 1.5.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Testing...",
                        style = MaterialTheme.typography.labelSmall.copy(color = SaffronPrimary)
                    )
                }
            } else {
                Text(
                    text = "Not tested yet",
                    style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF6B7280))
                )
            }

            Row {
                Text(
                    text = "Test",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = SaffronPrimary
                    ),
                    modifier = Modifier
                        .clickable(onClick = onRunTest)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )

                Text(
                    text = "Check in Scan",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE5E7EB)
                    ),
                    modifier = Modifier
                        .clickable(onClick = onLoadInScanner)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}
