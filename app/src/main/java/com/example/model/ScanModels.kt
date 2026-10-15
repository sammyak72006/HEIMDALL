package com.example.model

enum class ScanType(val label: String) {
    SMS_MESSAGE("SMS / Text"),
    EMAIL("Email"),
    WHATSAPP_MESSAGE("WhatsApp"),
    URL("Website / Link"),
    GENERAL_TEXT("General Message")
}

enum class RiskLevel(val label: String) {
    LOW("Safe / Low Risk"),
    MEDIUM("Caution / Suspicious"),
    HIGH("Danger / Scam Alert")
}

enum class FraudScenario(val displayName: String, val description: String) {
    BANK_KYC_PAN_UPDATE(
        "Bank KYC & PAN Block Scam",
        "Fake SMS pretending to be SBI, HDFC, ICICI, or PNB claiming account suspension unless PAN/KYC is updated via link."
    ),
    ELECTRICITY_BILL_DISCONNECT(
        "Electricity Bill Disconnection Scam",
        "Threatening power disconnection tonight by Mahavitaran, BESCOM, UPPCL, or BSES unless an unpaid bill is paid to a mobile number."
    ),
    UPI_QR_CASHBACK_FRAUD(
        "UPI & QR Code Money Request Scam",
        "Tricking victims into scanning a QR code or entering their UPI PIN to 'receive' money, lottery, or cashback."
    ),
    DIGITAL_ARREST_EXTORTION(
        "Digital Arrest & Police Extortion",
        "Fake calls or notices from CBI, ED, Mumbai Police, or Customs claiming illegal parcels, demanding money to avoid 'Digital Arrest'."
    ),
    TELEGRAM_JOB_TASK_SCAM(
        "Part-Time YouTube & Task Scam",
        "Unsolicited job offers promising ₹2,000–₹5,000/day for liking videos or rating hotels on Telegram or WhatsApp."
    ),
    TRAFFIC_ECHALLAN_APK(
        "Fake Traffic E-Challan APK Scam",
        "Fake Parivahan SMS claiming unpaid traffic fine, sending a malicious Android APK file that steals OTPs."
    ),
    LEGITIMATE_COMMUNICATION(
        "Legitimate Communication",
        "Verified communication from recognized Indian banks, government portals, or services without deception."
    )
}

enum class IndicatorSeverity {
    INFO,
    WARNING,
    CRITICAL
}

data class ThreatIndicator(
    val title: String,
    val description: String,
    val severity: IndicatorSeverity,
    val category: String,
    val technicalDetail: String? = null
)

data class ScanResult(
    val id: String,
    val timestamp: Long,
    val scanType: ScanType,
    val originalInput: String,
    val sanitizedInput: String,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val scenario: FraudScenario,
    val summaryExplanation: String,
    val indicators: List<ThreatIndicator>,
    val recommendedActions: List<String>,
    val heuristicScore: Int,
    val aiScore: Int,
    val analysisTimeMs: Long,
    val threatFeedMatch: String? = null
)

data class BenchmarkCase(
    val id: String,
    val title: String,
    val category: FraudScenario,
    val input: String,
    val scanType: ScanType,
    val groundTruthMalicious: Boolean,
    val expectedKeywords: List<String>,
    val notes: String
)

data class ConfusionMatrix(
    val truePositives: Int = 0,
    val trueNegatives: Int = 0,
    val falsePositives: Int = 0,
    val falseNegatives: Int = 0,
    val totalTested: Int = 0,
    val avgLatencyMs: Long = 0L
) {
    val accuracy: Float
        get() = if (totalTested == 0) 0f else (truePositives + trueNegatives).toFloat() / totalTested

    val precision: Float
        get() = if ((truePositives + falsePositives) == 0) 0f else truePositives.toFloat() / (truePositives + falsePositives)

    val recall: Float
        get() = if ((truePositives + falseNegatives) == 0) 0f else truePositives.toFloat() / (truePositives + falseNegatives)

    val f1Score: Float
        get() = if ((precision + recall) == 0f) 0f else 2 * (precision * recall) / (precision + recall)
}

data class ThreatFeedItem(
    val id: String,
    val pattern: String,
    val type: String,
    val category: FraudScenario,
    val severity: IndicatorSeverity,
    val reason: String,
    val dateAdded: String
)
