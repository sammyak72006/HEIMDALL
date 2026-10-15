package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BenchmarkRepository
import com.example.data.NotificationEntity
import com.example.data.ThreatFeedRepository
import com.example.engine.DetectionCoordinator
import com.example.engine.HeuristicEngine
import com.example.model.BenchmarkCase
import com.example.model.CapturedNotification
import com.example.model.ConfusionMatrix
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.model.ThreatFeedItem
import com.example.service.NotificationReaderService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class PhishGuardViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val threatFeedRepository = ThreatFeedRepository(database.threatFeedDao())
    private val coordinator = DetectionCoordinator(database, threatFeedRepository)

    private val _currentTab = MutableStateFlow(0)
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _scanInput = MutableStateFlow("")
    val scanInput: StateFlow<String> = _scanInput.asStateFlow()

    private val _selectedScanType = MutableStateFlow(ScanType.SMS_MESSAGE)
    val selectedScanType: StateFlow<ScanType> = _selectedScanType.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scanStage = MutableStateFlow("")
    val scanStage: StateFlow<String> = _scanStage.asStateFlow()

    private val _currentScanResult = MutableStateFlow<ScanResult?>(null)
    val currentScanResult: StateFlow<ScanResult?> = _currentScanResult.asStateFlow()

    val threatFeeds: StateFlow<List<ThreatFeedItem>> = threatFeedRepository.allThreats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _threatSearchQuery = MutableStateFlow("")
    val threatSearchQuery: StateFlow<String> = _threatSearchQuery.asStateFlow()

    val scanHistory: StateFlow<List<ScanResult>> = database.scanResultDao().getAllScans()
        .map { list -> list.map { it.toDomainModel() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allBenchmarkCases: List<BenchmarkCase> = BenchmarkRepository.testSuite

    private val _benchmarkResults = MutableStateFlow<Map<String, ScanResult>>(emptyMap())
    val benchmarkResults: StateFlow<Map<String, ScanResult>> = _benchmarkResults.asStateFlow()

    private val _isRunningBatchBenchmark = MutableStateFlow(false)
    val isRunningBatchBenchmark: StateFlow<Boolean> = _isRunningBatchBenchmark.asStateFlow()

    private val _activeTestingCaseId = MutableStateFlow<String?>(null)
    val activeTestingCaseId: StateFlow<String?> = _activeTestingCaseId.asStateFlow()

    private val _confusionMatrix = MutableStateFlow(ConfusionMatrix())
    val confusionMatrix: StateFlow<ConfusionMatrix> = _confusionMatrix.asStateFlow()

    val capturedNotifications: StateFlow<List<CapturedNotification>> = database.notificationDao()
        .getAllNotifications()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flaggedNotificationsCount: StateFlow<Int> = database.notificationDao()
        .getFlaggedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isNotificationListenerActive = MutableStateFlow(false)
    val isNotificationListenerActive: StateFlow<Boolean> = _isNotificationListenerActive.asStateFlow()

    private val _activeViewTab = MutableStateFlow(0) // 0: Single-Action Scanner, 1: Notification Guard
    val activeViewTab: StateFlow<Int> = _activeViewTab.asStateFlow()

    private val _notificationFilter = MutableStateFlow("ALL") // "ALL" or "FLAGGED"
    val notificationFilter: StateFlow<String> = _notificationFilter.asStateFlow()

    init {
        viewModelScope.launch {
            threatFeedRepository.populateInitialFeedsIfEmpty()
            seedInitialNotificationsIfEmpty()
            runInitialBaselineCalibration()
            checkNotificationPermission()
        }
    }

    private suspend fun runInitialBaselineCalibration() {
        val initialSubset = allBenchmarkCases.take(6)
        val initialMap = mutableMapOf<String, ScanResult>()
        for (case in initialSubset) {
            val result = coordinator.performFullScan(case.input, case.scanType)
            initialMap[case.id] = result
        }
        _benchmarkResults.value = initialMap
        recalculateConfusionMatrix(initialMap)
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun onScanInputChanged(text: String) {
        _scanInput.value = text
    }

    fun onScanTypeChanged(type: ScanType) {
        _selectedScanType.value = type
    }

    fun onThreatSearchChanged(query: String) {
        _threatSearchQuery.value = query
    }

    fun loadPresetScenario(case: BenchmarkCase) {
        _scanInput.value = case.input
        _selectedScanType.value = case.scanType
        _currentTab.value = 0
    }

    fun runCurrentScan() {
        val input = _scanInput.value.trim()
        if (input.isBlank() || _isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            _currentScanResult.value = null

            _scanStage.value = "Tier 1: Running On-Device Heuristic Engine..."
            delay(250)

            _scanStage.value = "Tier 2: Querying IOC Threat Feeds & Homoglyph Registry..."
            delay(250)

            _scanStage.value = "Tier 3: Sanitizing PII & Executing Gemini AI Context Reasoning..."
            val result = coordinator.performFullScan(input, _selectedScanType.value)

            _scanStage.value = "Threat Assessment Finalized"
            delay(150)

            _currentScanResult.value = result
            _isScanning.value = false
        }
    }

    fun runSingleBenchmarkCase(case: BenchmarkCase) {
        if (_isRunningBatchBenchmark.value) return

        viewModelScope.launch {
            _activeTestingCaseId.value = case.id
            val result = coordinator.performFullScan(case.input, case.scanType)

            val updatedMap = _benchmarkResults.value.toMutableMap()
            updatedMap[case.id] = result
            _benchmarkResults.value = updatedMap

            recalculateConfusionMatrix(updatedMap)
            _activeTestingCaseId.value = null
        }
    }

    fun runAllBenchmarkTests() {
        if (_isRunningBatchBenchmark.value) return

        viewModelScope.launch {
            _isRunningBatchBenchmark.value = true
            val map = _benchmarkResults.value.toMutableMap()

            for (case in allBenchmarkCases) {
                _activeTestingCaseId.value = case.id
                val result = coordinator.performFullScan(case.input, case.scanType)
                map[case.id] = result
                _benchmarkResults.value = map.toMap()
                recalculateConfusionMatrix(map)
                delay(60)
            }

            _activeTestingCaseId.value = null
            _isRunningBatchBenchmark.value = false
        }
    }

    fun resetBenchmarkTelemetry() {
        _benchmarkResults.value = emptyMap()
        _confusionMatrix.value = ConfusionMatrix()
    }

    private fun recalculateConfusionMatrix(results: Map<String, ScanResult>) {
        var tp = 0
        var fp = 0
        var tn = 0
        var fn = 0
        var totalLatency = 0L

        for (case in allBenchmarkCases) {
            val result = results[case.id] ?: continue
            val predictedMalicious = result.riskLevel == RiskLevel.HIGH || result.riskLevel == RiskLevel.MEDIUM
            val actualMalicious = case.groundTruthMalicious

            totalLatency += result.analysisTimeMs

            when {
                actualMalicious && predictedMalicious -> tp++
                !actualMalicious && predictedMalicious -> fp++
                !actualMalicious && !predictedMalicious -> tn++
                actualMalicious && !predictedMalicious -> fn++
            }
        }

        val total = tp + fp + tn + fn
        val avgLatency = if (total > 0) totalLatency / total else 0L

        _confusionMatrix.value = ConfusionMatrix(
            truePositives = tp,
            trueNegatives = tn,
            falsePositives = fp,
            falseNegatives = fn,
            totalTested = total,
            avgLatencyMs = avgLatency
        )
    }

    fun clearScanHistory() {
        viewModelScope.launch {
            database.scanResultDao().clearAll()
        }
    }

    fun inspectHistoryItem(result: ScanResult) {
        _currentScanResult.value = result
        _currentTab.value = 0
    }

    fun setActiveViewTab(tab: Int) {
        _activeViewTab.value = tab
    }

    fun setNotificationFilter(filter: String) {
        _notificationFilter.value = filter
    }

    fun checkNotificationPermission() {
        _isNotificationListenerActive.value = NotificationReaderService.isPermissionGranted(getApplication())
    }

    fun flagNotification(id: String, reason: String) {
        viewModelScope.launch {
            database.notificationDao().updateFlagStatus(
                id = id,
                isFlagged = true,
                reason = reason,
                flaggedAt = System.currentTimeMillis()
            )
        }
    }

    fun unflagNotification(id: String) {
        viewModelScope.launch {
            database.notificationDao().updateFlagStatus(
                id = id,
                isFlagged = false,
                reason = null,
                flaggedAt = null
            )
        }
    }

    fun deleteNotification(id: String) {
        viewModelScope.launch {
            database.notificationDao().deleteById(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            database.notificationDao().clearAll()
        }
    }

    fun scanCapturedNotification(notification: CapturedNotification) {
        val combined = if (notification.title.isNotBlank()) {
            "${notification.title}\n${notification.content}"
        } else {
            notification.content
        }
        val scanType = when {
            notification.packageName.contains("whatsapp", ignoreCase = true) -> ScanType.WHATSAPP_MESSAGE
            notification.packageName.contains("messaging", ignoreCase = true) -> ScanType.SMS_MESSAGE
            notification.packageName.contains("mail", ignoreCase = true) -> ScanType.EMAIL
            else -> ScanType.SMS_MESSAGE
        }
        _scanInput.value = combined
        _selectedScanType.value = scanType
        _activeViewTab.value = 0 // Switch back to single-action scanner
        runCurrentScan()
    }

    fun simulateSampleNotification(index: Int = -1) {
        viewModelScope.launch {
            val samples = getSampleNotificationTemplates()
            val chosen = if (index in samples.indices) samples[index] else samples.random()

            // Run on-device heuristics to score incoming alert
            val textToAnalyze = "${chosen.title}: ${chosen.content}"
            val analysis = HeuristicEngine.analyze(textToAnalyze, ScanType.SMS_MESSAGE)
            val score = analysis.heuristicScore
            val level = when {
                score >= 70 -> RiskLevel.HIGH
                score >= 40 -> RiskLevel.MEDIUM
                else -> RiskLevel.LOW
            }
            val threatTitle = analysis.indicators.firstOrNull()?.title

            val newNotification = chosen.copy(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                riskScore = score,
                riskLevel = level,
                detectedScamType = threatTitle,
                isFlagged = level == RiskLevel.HIGH,
                flagReason = if (level == RiskLevel.HIGH) threatTitle ?: "High Risk Phishing Signature" else null,
                flaggedAt = if (level == RiskLevel.HIGH) System.currentTimeMillis() else null
            )
            database.notificationDao().insert(NotificationEntity.fromDomain(newNotification))
        }
    }

    private suspend fun seedInitialNotificationsIfEmpty() {
        if (database.notificationDao().getCount() == 0) {
            val samples = getSampleNotificationTemplates()
            database.notificationDao().insertAll(samples.map { NotificationEntity.fromDomain(it) })
        }
    }

    private fun getSampleNotificationTemplates(): List<CapturedNotification> {
        val now = System.currentTimeMillis()
        return listOf(
            CapturedNotification(
                id = "NOTIF-SEED-01",
                packageName = "com.google.android.apps.nbu.paisa.user",
                appName = "Google Pay",
                title = "Payment Collect Request: ₹4,999",
                content = "Collect request of ₹4,999 received from sbi-refund-desk@upi. Enter UPI PIN to claim festival cashback bonus.",
                timestamp = now - 180_000,
                isFlagged = true,
                flagReason = "Fake UPI Collect Request Trap",
                flaggedAt = now - 170_000,
                riskScore = 85,
                riskLevel = RiskLevel.HIGH,
                detectedScamType = "Fake UPI Collect Request Scam"
            ),
            CapturedNotification(
                id = "NOTIF-SEED-02",
                packageName = "com.google.android.apps.messaging",
                appName = "Messages",
                title = "AD-SBIYNO Alert",
                content = "Dear Customer, your SBI account KYC has expired. Your YONO banking services will be permanently suspended within 24 hours. Update Aadhaar now: http://sbi-kyc-renewal.icu",
                timestamp = now - 900_000,
                isFlagged = true,
                flagReason = "Urgent Bank KYC / Aadhaar Expiry Scam",
                flaggedAt = now - 850_000,
                riskScore = 95,
                riskLevel = RiskLevel.HIGH,
                detectedScamType = "Urgent KYC Expiry Phishing"
            ),
            CapturedNotification(
                id = "NOTIF-SEED-03",
                packageName = "com.whatsapp",
                appName = "WhatsApp",
                title = "+91 91234 56789",
                content = "Hello! Earn ₹3,000 - ₹5,000 daily from home. Just like YouTube videos and take screenshots. Daily UPI payment guaranteed. Contact HR on Telegram @earnindia.",
                timestamp = now - 3_600_000,
                isFlagged = false,
                flagReason = null,
                flaggedAt = null,
                riskScore = 60,
                riskLevel = RiskLevel.MEDIUM,
                detectedScamType = "Part-Time Job / Task Scam"
            ),
            CapturedNotification(
                id = "NOTIF-SEED-04",
                packageName = "com.phonepe.app",
                appName = "PhonePe",
                title = "Scan to Receive Money: ₹1,500",
                content = "Buyer sent QR Code for your OLX item. Scan this QR code and approve request in PhonePe to receive ₹1,500.",
                timestamp = now - 7_200_000,
                isFlagged = true,
                flagReason = "Fraudulent Merchant QR Code Scam",
                flaggedAt = now - 7_150_000,
                riskScore = 90,
                riskLevel = RiskLevel.HIGH,
                detectedScamType = "Merchant QR / Pay Scam"
            ),
            CapturedNotification(
                id = "NOTIF-SEED-05",
                packageName = "com.hdfcbank.mobile",
                appName = "HDFC Bank",
                title = "Txn Alert: INR 650.00",
                content = "Your HDFC Bank Debit Card ending 3912 was used at Supermarket on 15-Oct-26 for INR 650.00. Avail Bal: INR 32,450.00. If not you, SMS BLOCK to 5676712.",
                timestamp = now - 14_400_000,
                isFlagged = false,
                flagReason = null,
                flaggedAt = null,
                riskScore = 0,
                riskLevel = RiskLevel.LOW,
                detectedScamType = null
            )
        )
    }
}
