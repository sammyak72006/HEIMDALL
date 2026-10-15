package com.example.engine

import com.example.data.AppDatabase
import com.example.data.ScanResultEntity
import com.example.data.ThreatFeedRepository
import com.example.model.IndicatorSeverity
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.model.ThreatIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class DetectionCoordinator(
    private val database: AppDatabase,
    private val threatFeedRepository: ThreatFeedRepository
) {

    suspend fun performFullScan(
        input: String,
        scanType: ScanType
    ): ScanResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        val sanitization = PrivacySanitizer.sanitize(input)
        val sanitizedText = sanitization.sanitizedText

        val heuristic = HeuristicEngine.analyze(sanitizedText, scanType)

        val threatMatch = threatFeedRepository.checkThreatMatch(sanitizedText)

        val aiResult = GeminiPhishDetector.analyzeWithGemini(
            sanitizedText = sanitizedText,
            heuristicScenario = heuristic.detectedScenario,
            heuristicScore = heuristic.heuristicScore
        )

        val indicators = mutableListOf<ThreatIndicator>()
        indicators.addAll(heuristic.indicators)

        if (sanitization.redactedItemsCount > 0) {
            indicators.add(
                0,
                ThreatIndicator(
                    title = "Privacy Protection Engaged",
                    description = "Protected ${sanitization.redactedItemsCount} sensitive item(s) (${sanitization.redactedCategories.joinToString()}) with on-device redaction before analysis.",
                    severity = IndicatorSeverity.INFO,
                    category = "Privacy",
                    technicalDetail = "Locally masked PII regex tokens"
                )
            )
        }

        if (threatMatch != null) {
            indicators.add(
                0,
                ThreatIndicator(
                    title = "Threat Intelligence Feed Hit",
                    description = "Matched known malicious pattern '${threatMatch.pattern}': ${threatMatch.reason}",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Threat Feed",
                    technicalDetail = "IOC Database ID: ${threatMatch.id}"
                )
            )
        }

        for (aiInd in aiResult.indicators) {
            if (indicators.none { it.title.equals(aiInd.title, ignoreCase = true) }) {
                indicators.add(aiInd)
            }
        }

        var combinedScore = when {
            threatMatch != null -> (heuristic.heuristicScore.coerceAtLeast(85) + aiResult.aiRiskScore) / 2
            aiResult.usedAiModel -> (heuristic.heuristicScore * 0.45 + aiResult.aiRiskScore * 0.55).toInt()
            else -> heuristic.heuristicScore
        }

        if (threatMatch != null) {
            combinedScore = combinedScore.coerceAtLeast(90)
        }
        val finalRiskScore = combinedScore.coerceIn(0, 100)

        val riskLevel = when {
            finalRiskScore >= 70 -> RiskLevel.HIGH
            finalRiskScore >= 40 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val scenario = if (threatMatch != null) threatMatch.category else {
            if (aiResult.detectedScenario != com.example.model.FraudScenario.LEGITIMATE_COMMUNICATION) {
                aiResult.detectedScenario
            } else {
                heuristic.detectedScenario
            }
        }

        val finalResult = ScanResult(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            scanType = scanType,
            originalInput = input,
            sanitizedInput = sanitizedText,
            riskScore = finalRiskScore,
            riskLevel = riskLevel,
            scenario = scenario,
            summaryExplanation = aiResult.summaryExplanation,
            indicators = indicators,
            recommendedActions = aiResult.recommendedActions,
            heuristicScore = heuristic.heuristicScore,
            aiScore = aiResult.aiRiskScore,
            analysisTimeMs = System.currentTimeMillis() - startTime,
            threatFeedMatch = threatMatch?.pattern
        )

        database.scanResultDao().insertScan(ScanResultEntity.fromDomainModel(finalResult))

        finalResult
    }
}
