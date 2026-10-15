package com.example.engine

import com.example.model.FraudScenario
import com.example.model.IndicatorSeverity
import com.example.model.ScanType
import com.example.model.ThreatIndicator
import java.net.URI
import java.util.Locale
import kotlin.math.log2

object HeuristicEngine {

    data class HeuristicAnalysis(
        val heuristicScore: Int,
        val detectedScenario: FraudScenario,
        val indicators: List<ThreatIndicator>,
        val triggeredRules: List<String>
    )

    private val SUSPICIOUS_TLDS = setOf(
        "xyz", "top", "tk", "icu", "buzz", "cfd", "rest", "gq", "ml", "ga", "work", "click", "cc", "fit", "surf", "vip", "live"
    )

    private val INDIAN_TARGET_BRANDS = mapOf(
        "sbi" to listOf("sb1", "sbi-kyc", "sbi-yono-update", "yono-sbi-login", "sbi-netbanking-verify"),
        "hdfc" to listOf("hdfc-kyc", "hdfc-rewards-claim", "hdfc-pan-update", "hdfc-online-secure"),
        "icici" to listOf("icici-reward", "icici-kyc-verify", "icici-alert"),
        "axis" to listOf("axis-kyc-update", "axisbank-support"),
        "paytm" to listOf("paytm-kyc", "paytm-cashback-claim", "paytm-lottery"),
        "phonepe" to listOf("phonepe-rewards", "phonepe-cashback", "phonepe-support"),
        "parivahan" to listOf("parivahan-echallan", "echallan-parivahan-pay", "traffic-challan-pay")
    )

    private val ELECTRICITY_SCAM_PATTERNS = listOf(
        Regex("(?i)\\b(electricity\\s*(bill|power|connection)|light\\s*will\\s*be\\s*disconnect|power\\s*cut)\\b"),
        Regex("(?i)\\b(disconnected\\s*tonight|at\\s*9[:.]30\\s*(pm)?|within\\s*few\\s*hours)\\b"),
        Regex("(?i)\\b(mahavitaran|bescom|uppcl|bses|dhbvn|tneb|electricity\\s*officer)\\b")
    )

    // Indian Vector 1: Urgent KYC Expiry Patterns (SMS, Email & Message)
    private val KYC_EXPIRY_URGENT_PATTERNS = listOf(
        Regex("(?i)\\b(kyc\\s*(is\\s*)?(expiry|expired|expires|expiring|lapsed|overdue|suspended|invalid))\\b"),
        Regex("(?i)\\b(complete\\s*(your\\s*)?kyc\\s*(immediately|urgently|today|within\\s*\\d+\\s*(hours?|hrs?)))\\b"),
        Regex("(?i)\\b(re-?kyc\\s*(verification\\s*)?(is\\s*)?(pending|mandatory|required|expired|suspended))\\b"),
        Regex("(?i)\\b(account\\s*(blocked|freeze|frozen|suspended|hold)\\s*(due\\s*to|for|pending)\\s*kyc)\\b"),
        Regex("(?i)\\b(urgent\\s*[:\\-]?\\s*(your\\s*)?kyc\\s*(is\\s*)?(expiring|expired|mandatory|pending))\\b"),
        Regex("(?i)\\b(update\\s*kyc\\s*immediately\\s*to\\s*avoid\\s*(block|deactivation|freeze))\\b")
    )

    // Indian Vector 2: Urgent Aadhaar Link Required Patterns (SMS, Email & Message)
    private val AADHAAR_LINK_URGENT_PATTERNS = listOf(
        Regex("(?i)\\b(aadhaar(\\s*card)?\\s*(link|linking)\\s*(is\\s*)?(required|mandatory|pending|expired|overdue))\\b"),
        Regex("(?i)\\b(link\\s*(your\\s*)?aadhaar(\\s*card)?\\s*(immediately|urgently|today|within\\s*\\d+\\s*(hours?|hrs?)))\\b"),
        Regex("(?i)\\b(link\\s*(pan\\s*(card)?\\s*and\\s*)?aadhaar\\s*(to\\s*avoid\\s*(block|suspension|deactivation|freeze)|before\\s*deadline))\\b"),
        Regex("(?i)\\b((sim|mobile|account|card)\\s*(will\\s*be\\s*)?(blocked|deactivated|disconnected)\\s*(if|due\\s*to)\\s*aadhaar\\s*(not|unlinked))\\b"),
        Regex("(?i)\\b(aadhaar\\s*(verification|authentication)\\s*(pending|required\\s*immediately|mandatory))\\b")
    )

    // Indian Vector 3: UPI Fake Collect Requests, VPA Impersonations & Merchant QR Codes
    private val UPI_COLLECT_PATTERNS = listOf(
        Regex("(?i)\\b(collect\\s*request|request(ed)?\\s*money|money\\s*request\\s*received)\\b"),
        Regex("(?i)\\b(approve|accept)\\s*(request|payment|transaction)\\s*to\\s*(receive|claim|credit|get)\\b"),
        Regex("(?i)\\benter\\s*(your\\s*)?(upi\\s*pin|mpin)\\s*to\\s*(receive|credit|accept|claim)\\b")
    )

    private val UPI_MERCHANT_QR_PATTERNS = listOf(
        Regex("(?i)\\b(merchant\\s*qr(\\s*code)?|scan\\s*(this\\s*)?qr(\\s*code)?\\s*to\\s*(receive|get|claim|credit|refund))\\b"),
        Regex("(?i)\\b(reverse\\s*qr|reverse\\s*payment\\s*qr|army\\s*officer\\s*qr|olx\\s*qr)\\b")
    )

    private val UPI_QR_PATTERNS = listOf(
        Regex("(?i)\\b(scan\\s*qr\\s*code\\s*to\\s*receive|enter\\s*(upi\\s*pin|mpin)\\s*to\\s*credit)\\b"),
        Regex("(?i)\\b(cashback\\s*of\\s*₹|lottery\\s*reward\\s*₹|received\\s*₹[0-9,]+\\s*cashback)\\b"),
        Regex("(?i)\\b(claim\\s*in\\s*phonepe|claim\\s*in\\s*gpay|claim\\s*in\\s*paytm)\\b")
    )

    private val DIGITAL_ARREST_PATTERNS = listOf(
        Regex("(?i)\\b(digital\\s*arrest|cbi\\s*officer|cyber\\s*crime\\s*branch|mumbai\\s*police|ed\\s*notice)\\b"),
        Regex("(?i)\\b(parcel\\s*(held|seized)\\s*in\\s*customs|drugs\\s*found|illegal\\s*passport|narcotics)\\b"),
        Regex("(?i)\\b(connect\\s*on\\s*skype|do\\s*not\\s*disconnect\\s*call|confidential\\s*investigation)\\b")
    )

    private val BANK_KYC_PATTERNS = listOf(
        Regex("(?i)\\b(dear\\s*customer|dear\\s*sbi|dear\\s*hdfc|dear\\s*user)\\b"),
        Regex("(?i)\\b(account\\s*(blocked|suspended|deactivated|on\\s*hold)|yono\\s*account\\s*closed)\\b"),
        Regex("(?i)\\b(update\\s*(pan|kyc|aadhaar)|link\\s*pan\\s*card|verify\\s*kyc)\\b")
    )

    private val TASK_JOB_PATTERNS = listOf(
        Regex("(?i)\\b(part\\s*time\\s*job|earn\\s*₹[0-9,]+(\\s*daily|-₹[0-9,]+)|work\\s*from\\s*home)\\b"),
        Regex("(?i)\\b(like\\s*youtube\\s*videos|hotel\\s*review\\s*task|rating\\s*google\\s*maps)\\b"),
        Regex("(?i)\\b(contact\\s*on\\s*telegram|message\\s*hr\\s*on\\s*whatsapp)\\b")
    )

    private val ECHALLAN_APK_PATTERNS = listOf(
        Regex("(?i)\\b(e-?challan|traffic\\s*police\\s*fine|parivahan\\s*challan|vehicle\\s*fine)\\b"),
        Regex("(?i)\\b(\\.apk|download\\s*parivahan|install\\s*app\\s*to\\s*pay)\\b")
    )

    fun analyze(input: String, scanType: ScanType): HeuristicAnalysis {
        val indicators = mutableListOf<ThreatIndicator>()
        val triggeredRules = mutableListOf<String>()
        var score = 0
        var detectedScenario = FraudScenario.LEGITIMATE_COMMUNICATION

        val lowerText = input.lowercase(Locale.ROOT)

        // 1. URL Structure & Domain Checks
        val urls = extractUrls(input)
        if (urls.isNotEmpty() || scanType == ScanType.URL) {
            val primaryUrl = if (urls.isNotEmpty()) urls.first() else input
            analyzeUrl(primaryUrl, indicators, triggeredRules)?.let { score += it }
        }

        // 2. APK Malicious Application Sideloading
        if (lowerText.contains(".apk") || lowerText.contains("download apk")) {
            score += 45
            triggeredRules.add("Malicious Android APK Distribution")
            indicators.add(
                ThreatIndicator(
                    title = "Suspicious APK App Download",
                    description = "Message attempts to install an unknown Android APK file outside Google Play Store. Fraudsters use this to steal bank OTPs and monitor SMS.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Malware",
                    technicalDetail = "Direct APK download link detected in SMS/message"
                )
            )
            detectedScenario = FraudScenario.TRAFFIC_ECHALLAN_APK
        }

        // 3. Indian Brand Typosquatting
        for ((brand, lookalikes) in INDIAN_TARGET_BRANDS) {
            for (lookalike in lookalikes) {
                if (lowerText.contains(lookalike)) {
                    score += 40
                    triggeredRules.add("Indian Brand Spoofing: $lookalike")
                    indicators.add(
                        ThreatIndicator(
                            title = "Fake $brand Domain / Name",
                            description = "Detected suspicious look-alike address '$lookalike' imitating official $brand portal.",
                            severity = IndicatorSeverity.CRITICAL,
                            category = "Domain",
                            technicalDetail = "Phishing domain targeted against: $brand"
                        )
                    )
                    if (detectedScenario == FraudScenario.LEGITIMATE_COMMUNICATION) {
                        detectedScenario = FraudScenario.BANK_KYC_PAN_UPDATE
                    }
                }
            }
        }

        // 4. Electricity Bill Disconnection Scam
        var elecHits = 0
        for (pattern in ELECTRICITY_SCAM_PATTERNS) {
            if (pattern.containsMatchIn(input)) elecHits++
        }
        if (elecHits >= 2) {
            score += 45
            triggeredRules.add("Electricity Bill Disconnection Threat")
            indicators.add(
                ThreatIndicator(
                    title = "Electricity Disconnection Scam",
                    description = "Classic electricity fraud message threatening sudden power cut tonight at 9:30 PM. Power discoms never send disconnection notices via personal mobile numbers.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Urgency",
                    technicalDetail = "Matches widespread nationwide electricity bill smishing template"
                )
            )
            detectedScenario = FraudScenario.ELECTRICITY_BILL_DISCONNECT
        }

        // 5. Urgent KYC Expiry Phishing Check (SMS & Email Vector)
        var kycExpiryHits = 0
        for (pattern in KYC_EXPIRY_URGENT_PATTERNS) {
            if (pattern.containsMatchIn(input)) kycExpiryHits++
        }
        if (kycExpiryHits >= 1) {
            score += 45
            triggeredRules.add("Urgent KYC Expiry Phishing Vector")
            indicators.add(
                ThreatIndicator(
                    title = "Urgent KYC Expiry Coercion",
                    description = "RBI Mandate Advisory: Indian banks (SBI, HDFC, ICICI, etc.) NEVER threaten immediate account suspension, debit card freeze, or netbanking deactivation via SMS or email links due to KYC expiry. Periodic Re-KYC is only done via official banking portals or in-person at bank branches.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "KYC Phishing",
                    technicalDetail = "Flagged urgent 'KYC expiry' language in ${scanType.label}"
                )
            )
            detectedScenario = FraudScenario.BANK_KYC_PAN_UPDATE
        }

        // 6. Urgent Aadhaar Link Required Phishing Check (SMS & Email Vector)
        var aadhaarLinkHits = 0
        for (pattern in AADHAAR_LINK_URGENT_PATTERNS) {
            if (pattern.containsMatchIn(input)) aadhaarLinkHits++
        }
        if (aadhaarLinkHits >= 1) {
            score += 45
            triggeredRules.add("Urgent Aadhaar Link Required Vector")
            indicators.add(
                ThreatIndicator(
                    title = "Urgent Aadhaar Link Required Trap",
                    description = "UIDAI & Telecom Authority Advisory: Fraudsters create panic claiming your SIM card, phone connection, or bank account will be blocked tonight unless your Aadhaar is linked via an external link. Genuine authorities never demand Aadhaar linking via unsolicited SMS or email links.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Aadhaar Phishing",
                    technicalDetail = "Urgent 'Aadhaar link required' coercive threat detected"
                )
            )
            if (detectedScenario == FraudScenario.LEGITIMATE_COMMUNICATION) {
                detectedScenario = FraudScenario.BANK_KYC_PAN_UPDATE
            }
        }

        // 7. UPI Fake 'Collect' Requests
        var upiCollectHits = 0
        for (pattern in UPI_COLLECT_PATTERNS) {
            if (pattern.containsMatchIn(input)) upiCollectHits++
        }
        if (upiCollectHits >= 1) {
            score += 50
            triggeredRules.add("Fake UPI Collect Request Signature")
            indicators.add(
                ThreatIndicator(
                    title = "Deceptive UPI 'Collect' Request Trap",
                    description = "NPCI Golden Rule: You NEVER enter your UPI PIN or approve a 'Collect' request to RECEIVE money. Approving a collect request immediately DEDUCTS money from your bank account to the scammer.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "UPI Collect",
                    technicalDetail = "Fraudulent collect request disguised as payment receipt"
                )
            )
            detectedScenario = FraudScenario.UPI_QR_CASHBACK_FRAUD
        }

        // 8. UPI VPA Impersonation
        val vpaRegex = Regex("(?i)\\b([a-zA-Z0-9.\\-_]+@(okhdfcbank|okaxis|oksbi|okicici|paytm|ybl|ibl|apl|axl|upi))\\b")
        val foundVpas = vpaRegex.findAll(input).map { it.value }.toList()
        for (vpa in foundVpas) {
            val lowerVpa = vpa.lowercase(Locale.ROOT)
            val isSuspiciousPrefix = listOf("refund", "support", "helpdesk", "cashback", "reward", "officer", "challan", "parivahan", "discom", "sbi", "hdfc", "paytm", "phonepe", "army", "customs")
                .any { lowerVpa.contains(it) }
            if (isSuspiciousPrefix) {
                score += 40
                triggeredRules.add("VPA Impersonation: $vpa")
                indicators.add(
                    ThreatIndicator(
                        title = "Suspicious VPA Impersonation ($vpa)",
                        description = "Virtual Payment Address '$vpa' appears to impersonate an official corporate desk or government entity. Scammers register look-alike VPAs on consumer UPI apps to trick users into transferring money.",
                        severity = IndicatorSeverity.CRITICAL,
                        category = "VPA Spoofing",
                        technicalDetail = "Deceptive VPA handle: $vpa"
                    )
                )
                if (detectedScenario == FraudScenario.LEGITIMATE_COMMUNICATION) {
                    detectedScenario = FraudScenario.UPI_QR_CASHBACK_FRAUD
                }
            }
        }

        // 9. Fraudulent Merchant QR Codes
        var qrHits = 0
        for (pattern in UPI_MERCHANT_QR_PATTERNS) {
            if (pattern.containsMatchIn(input)) qrHits++
        }
        if (qrHits >= 1) {
            score += 45
            triggeredRules.add("Fraudulent Merchant QR Code Signature")
            indicators.add(
                ThreatIndicator(
                    title = "Fraudulent Merchant QR Code Trap",
                    description = "Scanning a merchant QR code in Google Pay, PhonePe, or Paytm only DEBITS funds from your bank. QR codes cannot be used to receive money, refunds, or advance deposits.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Merchant QR",
                    technicalDetail = "Scam claims scanning merchant QR code credits funds"
                )
            )
            detectedScenario = FraudScenario.UPI_QR_CASHBACK_FRAUD
        }

        // 10. General UPI / QR Code Money Theft
        var upiHits = 0
        for (pattern in UPI_QR_PATTERNS) {
            if (pattern.containsMatchIn(input)) upiHits++
        }
        if (upiHits >= 1 && detectedScenario != FraudScenario.UPI_QR_CASHBACK_FRAUD) {
            score += 45
            triggeredRules.add("UPI PIN / QR Code Receipt Trick")
            indicators.add(
                ThreatIndicator(
                    title = "UPI Money Request / QR Code Trap",
                    description = "Golden Rule: You NEVER need to enter your UPI PIN or scan a QR code to RECEIVE money. Entering PIN always deducts money from your bank account.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Financial",
                    technicalDetail = "Lure attempts to invert payment direction under guise of reward"
                )
            )
            detectedScenario = FraudScenario.UPI_QR_CASHBACK_FRAUD
        }

        // 11. Digital Arrest / Police Extortion
        var digitalArrestHits = 0
        for (pattern in DIGITAL_ARREST_PATTERNS) {
            if (pattern.containsMatchIn(input)) digitalArrestHits++
        }
        if (digitalArrestHits >= 1) {
            score += 50
            triggeredRules.add("Digital Arrest / Law Enforcement Impersonation")
            indicators.add(
                ThreatIndicator(
                    title = "Fake 'Digital Arrest' Extortion",
                    description = "Indian law enforcement, CBI, Police, and Courts DO NOT conduct 'Digital Arrest' or court hearings over Skype/WhatsApp video calls. This is a severe criminal extortion racket.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Extortion",
                    technicalDetail = "Coercion using fake police/customs warrants"
                )
            )
            detectedScenario = FraudScenario.DIGITAL_ARREST_EXTORTION
        }

        // 12. Bank KYC / PAN Card Block
        var kycHits = 0
        for (pattern in BANK_KYC_PATTERNS) {
            if (pattern.containsMatchIn(input)) kycHits++
        }
        if (kycHits >= 2) {
            score += 40
            triggeredRules.add("Bank KYC & PAN Card Blocking Threat")
            indicators.add(
                ThreatIndicator(
                    title = "Fake Bank Account / KYC Suspension",
                    description = "Banks like SBI, HDFC, or ICICI never send SMS with unverified links threatening immediate account closure. KYC is always done inside verified banking branches or official apps.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Credential",
                    technicalDetail = "Threatens account suspension to force link click"
                )
            )
            if (detectedScenario == FraudScenario.LEGITIMATE_COMMUNICATION) {
                detectedScenario = FraudScenario.BANK_KYC_PAN_UPDATE
            }
        }

        // 8. Part-time Job / YouTube Task Scam
        var taskHits = 0
        for (pattern in TASK_JOB_PATTERNS) {
            if (pattern.containsMatchIn(input)) taskHits++
        }
        if (taskHits >= 2) {
            score += 40
            triggeredRules.add("YouTube Video Liking / Telegram Task Scam")
            indicators.add(
                ThreatIndicator(
                    title = "Part-Time Task Scam",
                    description = "Offers unrealistic daily income (₹2,000–₹5,000) for simple online tasks, shifting communication to Telegram to demand prepaid investment deposits.",
                    severity = IndicatorSeverity.CRITICAL,
                    category = "Employment",
                    technicalDetail = "High-volume recruitment fraud via Telegram bots"
                )
            )
            detectedScenario = FraudScenario.TELEGRAM_JOB_TASK_SCAM
        }

        // 9. Traffic Challan E-Challan APK
        var challanHits = 0
        for (pattern in ECHALLAN_APK_PATTERNS) {
            if (pattern.containsMatchIn(input)) challanHits++
        }
        if (challanHits >= 2) {
            score += 40
            if (detectedScenario == FraudScenario.LEGITIMATE_COMMUNICATION) {
                detectedScenario = FraudScenario.TRAFFIC_ECHALLAN_APK
            }
        }

        // 10. Legitimate Control Verifications (Govt & Bank Whitelist)
        val isGovInDomain = lowerText.contains(".gov.in") || lowerText.contains(".nic.in")
        val isOfficialSbi = lowerText.contains("onlinesbi.sbi") || lowerText.contains("sbi.co.in")
        val isOfficialHdfc = lowerText.contains("hdfcbank.com")
        val isIrctc = lowerText.contains("irctc.co.in")

        if ((isGovInDomain || isOfficialSbi || isOfficialHdfc || isIrctc) && score < 30) {
            score = 0
            detectedScenario = FraudScenario.LEGITIMATE_COMMUNICATION
        }

        val finalScore = score.coerceIn(0, 100)

        return HeuristicAnalysis(
            heuristicScore = finalScore,
            detectedScenario = detectedScenario,
            indicators = indicators,
            triggeredRules = triggeredRules
        )
    }

    private fun analyzeUrl(
        urlString: String,
        indicators: MutableList<ThreatIndicator>,
        triggeredRules: MutableList<String>
    ): Int? {
        var urlRisk = 0
        val cleanUrl = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
            "https://$urlString"
        } else urlString

        try {
            val uri = URI(cleanUrl)
            val host = uri.host?.lowercase(Locale.ROOT) ?: return null

            val isIpAddress = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$").matches(host)
            if (isIpAddress) {
                urlRisk += 40
                triggeredRules.add("Raw IP Address used instead of website domain")
                indicators.add(
                    ThreatIndicator(
                        title = "Unregistered IP Host Address",
                        description = "Link directs to an IP number ($host) instead of an established domain, commonly used for disposable phishing portals.",
                        severity = IndicatorSeverity.CRITICAL,
                        category = "Domain",
                        technicalDetail = "Direct IP host: $host"
                    )
                )
            }

            val parts = host.split(".")
            if (parts.size >= 2) {
                val tld = parts.last()
                if (SUSPICIOUS_TLDS.contains(tld)) {
                    urlRisk += 25
                    triggeredRules.add("High-Risk Domain Extension: .$tld")
                    indicators.add(
                        ThreatIndicator(
                            title = "High-Risk Domain Extension (.$tld)",
                            description = "Uses a cheap top-level domain (.$tld) heavily abused by online fraudsters.",
                            severity = IndicatorSeverity.WARNING,
                            category = "Domain",
                            technicalDetail = "Flagged TLD on abuse registries"
                        )
                    )
                }

                if (parts.size > 3) {
                    urlRisk += 20
                    triggeredRules.add("Subdomain Stacking (Masking true address)")
                    indicators.add(
                        ThreatIndicator(
                            title = "Subdomain Masking",
                            description = "The address hides its real domain using excessive prefix words to trick mobile users.",
                            severity = IndicatorSeverity.WARNING,
                            category = "Domain",
                            technicalDetail = "Host parts: ${parts.size} in '$host'"
                        )
                    )
                }
            }

            val entropy = calculateShannonEntropy(host)
            if (entropy > 3.8 && host.length > 14) {
                urlRisk += 20
                triggeredRules.add("High Randomness in Domain Name")
            }

            if (cleanUrl.startsWith("http://")) {
                urlRisk += 15
                triggeredRules.add("Insecure HTTP (No lock symbol)")
                indicators.add(
                    ThreatIndicator(
                        title = "Insecure Connection (HTTP)",
                        description = "Site does not use secure HTTPS encryption, making it unsafe for any financial or login information.",
                        severity = IndicatorSeverity.WARNING,
                        category = "Security",
                        technicalDetail = "Plaintext HTTP transport"
                    )
                )
            }
        } catch (_: Exception) {
            urlRisk += 20
        }

        return urlRisk
    }

    private fun extractUrls(text: String): List<String> {
        val urlRegex = Regex("(https?://[\\w-]+(\\.[\\w-]+)+(/[^\\s]*)?|www\\.[\\w-]+(\\.[\\w-]+)+(/[^\\s]*)?)")
        return urlRegex.findAll(text).map { it.value }.toList()
    }

    private fun calculateShannonEntropy(input: String): Double {
        if (input.isEmpty()) return 0.0
        val map = mutableMapOf<Char, Int>()
        for (c in input) map[c] = map.getOrDefault(c, 0) + 1
        var entropy = 0.0
        val len = input.length.toDouble()
        for (count in map.values) {
            val freq = count.toDouble() / len
            entropy -= freq * (log2(freq))
        }
        return entropy
    }
}
