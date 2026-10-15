package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.data.AppDatabase
import com.example.data.BenchmarkRepository
import com.example.data.NotificationEntity
import com.example.data.ScanResultEntity
import com.example.data.ThreatFeedRepository
import com.example.engine.KeywordRuleEngine
import com.example.model.BenchmarkCase
import com.example.model.CapturedNotification
import com.example.model.ConfusionMatrix
import com.example.model.FraudScenario
import com.example.model.IndicatorSeverity
import com.example.model.RiskLevel
import com.example.model.ScanResult
import com.example.model.ScanType
import com.example.model.ThreatFeedItem
import com.example.model.ThreatIndicator
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

    // Requirement 2: Load keywords.json with await and keep Analyze disabled until it has loaded.
    // If loading fails, show a visible error. Do not continue with empty rules.
    private val _isKeywordsLoading = MutableStateFlow(true)
    val isKeywordsLoading: StateFlow<Boolean> = _isKeywordsLoading.asStateFlow()

    private val _isKeywordsLoaded = MutableStateFlow(false)
    val isKeywordsLoaded: StateFlow<Boolean> = _isKeywordsLoaded.asStateFlow()

    private val _keywordsLoadError = MutableStateFlow<String?>(null)
    val keywordsLoadError: StateFlow<String?> = _keywordsLoadError.asStateFlow()

    // Requirement 1 & 6: User notices for empty input or empty share targets
    private val _userNotice = MutableStateFlow<String?>(null)
    val userNotice: StateFlow<String?> = _userNotice.asStateFlow()

    // Requirement 7: Debug telemetry state
    private val _debugResult = MutableStateFlow<KeywordRuleEngine.AnalysisResult?>(null)
    val debugResult: StateFlow<KeywordRuleEngine.AnalysisResult?> = _debugResult.asStateFlow()

    // Requirement 9: Self-test structure and execution state
    data class SelfTestCase(
        val letter: String,
        val text: String,
        val expected: String
    )

    data class SelfTestReport(
        val letter: String,
        val text: String,
        val expected: String,
        val actual: String,
        val passed: Boolean,
        val decisionPath: String,
        val scores: String,
        val matchedSummary: String
    )

    val selfTestCases = listOf(
        SelfTestCase("a", "Dear customer, your SBI KYC expired. Update KYC now at http://sbi-kyc-update.xyz or your account will be blocked within 24 hours.", "Scam (High)"),
        SelfTestCase("b", "Your OTP is 482915. Do not share it with anyone.", "Important"),
        SelfTestCase("c", "Share your OTP to receive your refund of Rs 5000. Approve the request now.", "Scam"),
        SelfTestCase("d", "Rs 2,000 debited from A/c XX1234 on 15-Oct. Avl Bal Rs 10,500. UPI Ref 123456789.", "Important"),
        SelfTestCase("e", "MEGA SALE! Flat 50% off. Shop now. T&C apply. Reply STOP to opt out.", "Spam"),
        SelfTestCase("f", "Congratulations! You have won a lottery. Pay a small fee to claim your prize.", "Scam"),
        SelfTestCase("g", "hey are we meeting at 5?", "Unclear")
    )

    private val _selfTestReports = MutableStateFlow<List<SelfTestReport>>(emptyList())
    val selfTestReports: StateFlow<List<SelfTestReport>> = _selfTestReports.asStateFlow()

    private val _isSelfTesting = MutableStateFlow(false)
    val isSelfTesting: StateFlow<Boolean> = _isSelfTesting.asStateFlow()

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
            loadKeywordsRules()
            threatFeedRepository.populateInitialFeedsIfEmpty()
            seedInitialNotificationsIfEmpty()
            checkNotificationPermission()
        }
    }

    /**
     * Requirement 2: Load keywords.json with await and keep Analyze disabled until it has loaded.
     */
    private suspend fun loadKeywordsRules() {
        try {
            _isKeywordsLoading.value = true
            _keywordsLoadError.value = null
            KeywordRuleEngine.loadRules(getApplication())
            _isKeywordsLoaded.value = true
            Log.i("PhishGuardViewModel", "Keywords rules successfully loaded and ready for analysis")
        } catch (e: Exception) {
            val err = "Failed to load keywords.json: ${e.message}"
            Log.e("PhishGuardViewModel", err, e)
            _keywordsLoadError.value = err
            _isKeywordsLoaded.value = false
        } finally {
            _isKeywordsLoading.value = false
        }
    }

    fun loadCustomRules(jsonString: String) {
        try {
            _keywordsLoadError.value = null
            val ruleSet = KeywordRuleEngine.loadRulesFromJsonString(jsonString)
            try {
                getApplication<Application>().openFileOutput("custom_keywords.json", Context.MODE_PRIVATE).use { fos ->
                    fos.write(jsonString.toByteArray(Charsets.UTF_8))
                }
            } catch (ioe: Exception) {
                Log.w("PhishGuardViewModel", "Could not persist custom_keywords.json to storage", ioe)
            }
            _isKeywordsLoaded.value = true
            _userNotice.value = "Loaded & saved custom rules (${ruleSet.scamGroups.size} scam, ${ruleSet.spamGroups.size} spam, ${ruleSet.importantGroups.size} important groups)."
        } catch (e: Exception) {
            _keywordsLoadError.value = "Custom JSON error: ${e.message}"
            _userNotice.value = "Failed to parse custom JSON: ${e.message}"
        }
    }

    fun loadCustomRulesFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonString = getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                } ?: throw IllegalArgumentException("Could not read content from selected file")

                withContext(Dispatchers.Main) {
                    loadCustomRules(jsonString)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _keywordsLoadError.value = "File import error: ${e.message}"
                    _userNotice.value = "Failed to import JSON file: ${e.message}"
                }
            }
        }
    }

    fun reloadDefaultRules() {
        viewModelScope.launch {
            try {
                val file = getApplication<Application>().getFileStreamPath("custom_keywords.json")
                if (file != null && file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                Log.w("PhishGuardViewModel", "Error deleting custom_keywords.json", e)
            }
            loadKeywordsRules()
            _userNotice.value = "Reset to default app keywords.json."
        }
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun onScanInputChanged(text: String) {
        _scanInput.value = text
        if (text.isNotBlank()) {
            _userNotice.value = null
        }
    }

    fun onSharedTextReceived(text: String, isEmpty: Boolean) {
        if (isEmpty) {
            _scanInput.value = ""
            _userNotice.value = "No text found in shared content. Please paste a message to analyze."
        } else {
            _scanInput.value = text
            _userNotice.value = null
        }
    }

    fun dismissUserNotice() {
        _userNotice.value = null
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
        _userNotice.value = null
    }

    /**
     * Requirement 1, 3, 4, 5, 7, 8:
     * - Never default to Safe.
     * - Use: Scam (High/Medium), Important, Spam, or Unclear.
     * - If empty, ask user to paste a message.
     * - If error, show "Couldn't check this message" and error.
     */
    fun runCurrentScan() {
        val input = _scanInput.value.trim()
        if (input.isBlank()) {
            _userNotice.value = "Please paste a message to analyze."
            return
        }
        if (!_isKeywordsLoaded.value) {
            _userNotice.value = "Keywords rules have not finished loading yet."
            return
        }
        if (_isScanning.value) return

        viewModelScope.launch {
            _isScanning.value = true
            _currentScanResult.value = null
            _userNotice.value = null

            _scanStage.value = "Evaluating Rule Engine & Groups..."
            delay(150)

            val startTime = System.currentTimeMillis()
            val ruleResult = KeywordRuleEngine.analyze(input, getApplication())
            _debugResult.value = ruleResult

            val latency = System.currentTimeMillis() - startTime
            _scanStage.value = "Classification Finalized (${ruleResult.category})"
            delay(100)

            val category = ruleResult.category
            val riskLevel = when {
                category == "Scam (High)" -> RiskLevel.HIGH
                category.startsWith("Scam") -> RiskLevel.MEDIUM
                category == "Important" -> RiskLevel.IMPORTANT
                category == "Spam" -> RiskLevel.SPAM
                else -> RiskLevel.UNCLEAR
            }

            val riskScore = when {
                category == "Scam (High)" -> maxOf(85, ruleResult.scamScore)
                category.startsWith("Scam") -> maxOf(50, ruleResult.scamScore)
                category == "Important" -> 15
                category == "Spam" -> 35
                else -> 0
            }

            val scenario = when {
                category.startsWith("Scam") -> {
                    if (ruleResult.matchedGroups.any { it.groupName.contains("kyc") }) {
                        FraudScenario.BANK_KYC_PAN_UPDATE
                    } else if (ruleResult.matchedGroups.any { it.groupName.contains("otp") || it.groupName.contains("refund") }) {
                        FraudScenario.UPI_QR_CASHBACK_FRAUD
                    } else {
                        FraudScenario.BANK_KYC_PAN_UPDATE
                    }
                }
                else -> FraudScenario.LEGITIMATE_COMMUNICATION
            }

            val summaryExplanation = when {
                category.startsWith("Scam") -> "Classified as $category based on matched scam trigger groups (${ruleResult.decisionPath} decision)."
                category == "Important" -> "Classified as Important transactional/account alert based on matched criteria (${ruleResult.decisionPath} decision)."
                category == "Spam" -> "Classified as Spam promotional communication based on advertising patterns (${ruleResult.decisionPath} decision)."
                category == "Unclear" -> "No definitive fraud or transaction rules matched. Classified as Unclear (${ruleResult.decisionPath} decision)."
                else -> category // Error case: Couldn't check this message: ...
            }

            val indicators = ruleResult.matchedGroups.map { grp ->
                ThreatIndicator(
                    title = "[${grp.category.uppercase()}] ${grp.groupName} (+${grp.points} pts)",
                    description = "Matched phrase(s): ${grp.matchedPhrases.joinToString()}",
                    severity = if (grp.category == "scam") IndicatorSeverity.CRITICAL else IndicatorSeverity.WARNING,
                    category = grp.category.replaceFirstChar { it.uppercase() }
                )
            }

            val recommendedActions = when {
                category.startsWith("Scam") -> listOf(
                    "DO NOT click any links, open APK downloads, or call phone numbers in this message.",
                    "NEVER share OTPs or approve UPI collect payment requests to receive funds.",
                    "Report immediately to 1930 or cybercrime.gov.in."
                )
                category == "Important" -> listOf(
                    "Keep OTPs private. Never share your OTP with anyone calling on phone.",
                    "Verify transactional updates directly inside your official bank app."
                )
                category == "Spam" -> listOf(
                    "Do not click promotional links from unknown bulk senders.",
                    "Reply STOP if from verified shortcode or block sender."
                )
                else -> listOf(
                    "Verify sender identity directly through official trusted channels.",
                    "Never enter confidential credentials on unverified websites."
                )
            }

            val finalResult = ScanResult(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                scanType = _selectedScanType.value,
                originalInput = input,
                sanitizedInput = ruleResult.normalizedInput,
                riskScore = riskScore,
                riskLevel = riskLevel,
                scenario = scenario,
                summaryExplanation = summaryExplanation,
                indicators = indicators,
                recommendedActions = recommendedActions,
                heuristicScore = ruleResult.scamScore,
                aiScore = if (ruleResult.decisionPath == "Gemini") 50 else 0,
                analysisTimeMs = latency,
                threatFeedMatch = null,
                classifiedCategory = category,
                decisionPath = ruleResult.decisionPath,
                ruleAnalysis = ruleResult,
                errorMessage = ruleResult.error
            )

            _currentScanResult.value = finalResult
            _isScanning.value = false

            // Persist to database
            viewModelScope.launch {
                try {
                    database.scanResultDao().insertScan(ScanResultEntity.fromDomainModel(finalResult))
                } catch (e: Exception) {
                    Log.w("PhishGuardViewModel", "Could not persist scan to Room: ${e.message}")
                }
            }
        }
    }

    /**
     * Requirement 9: Run self-test button classifying the 7 messages.
     * Shows pass or fail against the expected category.
     */
    fun runSelfTest() {
        if (!_isKeywordsLoaded.value || _isSelfTesting.value) return

        viewModelScope.launch {
            _isSelfTesting.value = true
            val reports = mutableListOf<SelfTestReport>()

            for (case in selfTestCases) {
                val res = KeywordRuleEngine.analyze(case.text, getApplication())
                val passed = matchesSelfTest(actual = res.category, expected = case.expected)
                val scoreStr = "Scam: ${res.scamScore} | Spam: ${res.spamScore} | Imp: ${res.importantScore}"
                val matchedStr = res.matchedGroups.joinToString(", ") { "${it.groupName} (+${it.points})" }.ifEmpty { "None" }

                reports.add(
                    SelfTestReport(
                        letter = case.letter,
                        text = case.text,
                        expected = case.expected,
                        actual = res.category,
                        passed = passed,
                        decisionPath = res.decisionPath,
                        scores = scoreStr,
                        matchedSummary = matchedStr
                    )
                )
            }

            _selfTestReports.value = reports
            _isSelfTesting.value = false
        }
    }

    private fun matchesSelfTest(actual: String, expected: String): Boolean {
        if (actual.equals(expected, ignoreCase = true)) return true
        if (expected.equals("Scam", ignoreCase = true) && actual.startsWith("Scam", ignoreCase = true)) return true
        if (expected.equals("Scam (High)", ignoreCase = true) && actual.equals("Scam (High)", ignoreCase = true)) return true
        return false
    }

    fun runSingleBenchmarkCase(case: BenchmarkCase) {
        if (_isRunningBatchBenchmark.value) return

        viewModelScope.launch {
            _activeTestingCaseId.value = case.id
            val res = KeywordRuleEngine.analyze(case.input, getApplication())
            val riskLevel = when {
                res.category == "Scam (High)" -> RiskLevel.HIGH
                res.category.startsWith("Scam") -> RiskLevel.MEDIUM
                res.category == "Important" -> RiskLevel.IMPORTANT
                res.category == "Spam" -> RiskLevel.SPAM
                else -> RiskLevel.UNCLEAR
            }
            val scanRes = ScanResult(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                scanType = case.scanType,
                originalInput = case.input,
                sanitizedInput = res.normalizedInput,
                riskScore = if (res.category.startsWith("Scam")) 80 else 10,
                riskLevel = riskLevel,
                scenario = case.category,
                summaryExplanation = res.category,
                indicators = emptyList(),
                recommendedActions = emptyList(),
                heuristicScore = res.scamScore,
                aiScore = 0,
                analysisTimeMs = 50L,
                classifiedCategory = res.category,
                decisionPath = res.decisionPath,
                ruleAnalysis = res
            )

            val updatedMap = _benchmarkResults.value.toMutableMap()
            updatedMap[case.id] = scanRes
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
                val res = KeywordRuleEngine.analyze(case.input, getApplication())
                val riskLevel = when {
                    res.category == "Scam (High)" -> RiskLevel.HIGH
                    res.category.startsWith("Scam") -> RiskLevel.MEDIUM
                    res.category == "Important" -> RiskLevel.IMPORTANT
                    res.category == "Spam" -> RiskLevel.SPAM
                    else -> RiskLevel.UNCLEAR
                }
                val scanRes = ScanResult(
                    id = UUID.randomUUID().toString(),
                    timestamp = System.currentTimeMillis(),
                    scanType = case.scanType,
                    originalInput = case.input,
                    sanitizedInput = res.normalizedInput,
                    riskScore = if (res.category.startsWith("Scam")) 80 else 10,
                    riskLevel = riskLevel,
                    scenario = case.category,
                    summaryExplanation = res.category,
                    indicators = emptyList(),
                    recommendedActions = emptyList(),
                    heuristicScore = res.scamScore,
                    aiScore = 0,
                    analysisTimeMs = 50L,
                    classifiedCategory = res.category,
                    decisionPath = res.decisionPath,
                    ruleAnalysis = res
                )
                map[case.id] = scanRes
                _benchmarkResults.value = map.toMap()
                recalculateConfusionMatrix(map)
                delay(40)
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
        _activeViewTab.value = 0
        runCurrentScan()
    }

    fun simulateSampleNotification(index: Int = -1) {
        viewModelScope.launch {
            val samples = getSampleNotificationTemplates()
            val chosen = if (index in samples.indices) samples[index] else samples.random()

            val textToAnalyze = "${chosen.title}: ${chosen.content}"
            val ruleRes = KeywordRuleEngine.analyze(textToAnalyze, getApplication())
            val isScam = ruleRes.category.startsWith("Scam")
            val level = if (isScam) RiskLevel.HIGH else RiskLevel.LOW

            val newNotification = chosen.copy(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                riskScore = ruleRes.scamScore,
                riskLevel = level,
                detectedScamType = if (isScam) ruleRes.category else null,
                isFlagged = isScam,
                flagReason = if (isScam) ruleRes.category else null,
                flaggedAt = if (isScam) System.currentTimeMillis() else null
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
