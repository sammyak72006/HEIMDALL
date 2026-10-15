package com.example.engine

import android.util.Log
import com.example.BuildConfig
import com.example.model.FraudScenario
import com.example.model.IndicatorSeverity
import com.example.model.ThreatIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiPhishDetector {

    private const val TAG = "GeminiPhishDetector"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    data class AiAnalysisResult(
        val aiRiskScore: Int,
        val detectedScenario: FraudScenario,
        val summaryExplanation: String,
        val indicators: List<ThreatIndicator>,
        val recommendedActions: List<String>,
        val usedAiModel: Boolean
    )

    suspend fun analyzeWithGemini(
        sanitizedText: String,
        heuristicScenario: FraudScenario,
        heuristicScore: Int
    ): AiAnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isNullOrBlank() || apiKey == "MY_GEMINI_API_KEY" || apiKey.startsWith("YOUR_")) {
            return@withContext generateSynthesizedAiResponse(sanitizedText, heuristicScenario, heuristicScore)
        }

        try {
            val systemPrompt = """
                You are PhishGuard India, a specialized cybersecurity and digital fraud defense intelligence analyst protecting Indian citizens and businesses against cyber scams.

                CRITICAL FOCUS - DOMAIN-SPECIFIC INDIAN FRAUD ANALYSIS:
                1. COMMON INDIAN UPI FRAUD SIGNATURES:
                   - Fake 'Collect' Requests: Scammers initiate a UPI Collect / payment request (via PhonePe, Google Pay, Paytm, or BHIM) masquerading as a refund, lottery prize, OLX buyer advance, cashback, or Army officer advance, deceiving the user into "Accepting" or "Entering UPI PIN to receive money". Crucial rule: In the Unified Payments Interface (UPI), you NEVER enter a PIN to receive money. Entering a PIN or accepting a collect request always DEBITS money from the victim's bank account.
                   - VPA (Virtual Payment Address) Impersonations: Scammers craft VPAs masquerading as official entities (e.g. `refund.support.sbi@okhdfcbank`, `parivahan.fine@paytm`, `cashback.phonepe@apl`, `electricity.helpdesk@ybl`, or fake army/customs handles). Flag any consumer UPI ID claiming to be an official corporate/government billing desk.
                   - Fraudulent Merchant QR Codes: Deceptive QR codes sent via WhatsApp, Telegram, or OLX claiming to be "Merchant Refund QR", "Scan to receive payment", or "Reverse Payment QR". Scanning a merchant QR code in a UPI app always debits money from the user's bank account.

                2. URGENT BANK KYC & AADHAAR LINK PHISHING VECTORS:
                   - Urgent 'KYC Expiry' Language: Messages claiming "KYC expired", "account suspended within 24 hours", or "complete KYC immediately to avoid card freeze". Under RBI mandates, banks never block accounts instantly via SMS/email links.
                   - Urgent 'Aadhaar Link Required' Language: Coercive threats claiming SIM cards, mobile connections, or bank accounts will be deactivated tonight unless Aadhaar is linked via an external link.

                3. OTHER CRITICAL INDIAN CYBERCRIME SCENARIOS:
                   - Electricity Bill Disconnection ("light will be cut tonight at 9:30 PM", asking to call a personal mobile number).
                   - Digital Arrest Extortion (fake CBI, Mumbai Police, ED, or Customs video call extortion).
                   - Telegram / YouTube Part-Time Job Scams (₹2,000–₹5,000 daily for liking videos with prepaid task deposits).
                   - Parivahan Traffic E-Challan APK malware distribution.

                Respond strictly with valid JSON conforming to this schema:
                {
                  "riskScore": (integer 0 to 100),
                  "scenario": (one of: "UPI_QR_CASHBACK_FRAUD", "BANK_KYC_PAN_UPDATE", "ELECTRICITY_BILL_DISCONNECT", "DIGITAL_ARREST_EXTORTION", "TELEGRAM_JOB_TASK_SCAM", "TRAFFIC_ECHALLAN_APK", "LEGITIMATE_COMMUNICATION"),
                  "explanation": (2 short, clear sentences in plain English identifying the specific Indian fraud vector like fake collect request, VPA impersonation, fraudulent merchant QR, KYC expiry urgency, or Aadhaar link trap),
                  "indicators": [
                     {
                       "title": (specific title, e.g. "Fake UPI Collect Request Trap", "VPA Impersonation Detected", "Fraudulent Merchant QR Code", "Urgent KYC Expiry Coercion", "Mandatory Aadhaar Link Threat"),
                       "description": (simple explanation of the fraud mechanism),
                       "severity": ("INFO", "WARNING", or "CRITICAL"),
                       "category": ("UPI Fraud", "VPA Impersonation", "Merchant QR", "KYC Phishing", "Aadhaar Coercion", "Extortion", "Malware", or "Domain")
                     }
                  ],
                  "recommendedActions": [
                     (concrete safety guidance, mentioning declining UPI collect requests, never scanning QR codes to receive money, or calling 1930 / cybercrime.gov.in)
                  ]
                }
            """.trimIndent()

            val requestJson = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "Content to evaluate:\n$sanitizedText"))
                        })
                    })
                })
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", systemPrompt))
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.1)
                })
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext generateSynthesizedAiResponse(sanitizedText, heuristicScenario, heuristicScore)
            }

            val responseBody = response.body?.string() ?: ""
            val jsonRoot = JSONObject(responseBody)
            val candidates = jsonRoot.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val text = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseAiResponse(text, heuristicScore)
        } catch (e: Exception) {
            Log.e(TAG, "Gemini fallback triggered: ${e.message}")
            generateSynthesizedAiResponse(sanitizedText, heuristicScenario, heuristicScore)
        }
    }

    private fun parseAiResponse(jsonText: String, fallbackScore: Int): AiAnalysisResult {
        return try {
            val obj = JSONObject(jsonText.trim())
            val score = obj.optInt("riskScore", fallbackScore).coerceIn(0, 100)
            val scenarioStr = obj.optString("scenario", "BANK_KYC_PAN_UPDATE")
            val scenario = FraudScenario.values().firstOrNull { it.name == scenarioStr }
                ?: FraudScenario.BANK_KYC_PAN_UPDATE
            val explanation = obj.optString("explanation", "Potential cyber fraud detected targeting Indian users.")

            val indicators = mutableListOf<ThreatIndicator>()
            val indArray = obj.optJSONArray("indicators")
            if (indArray != null) {
                for (i in 0 until indArray.length()) {
                    val indObj = indArray.optJSONObject(i) ?: continue
                    val sevStr = indObj.optString("severity", "WARNING")
                    val severity = try { IndicatorSeverity.valueOf(sevStr) } catch (_: Exception) { IndicatorSeverity.WARNING }
                    indicators.add(
                        ThreatIndicator(
                            title = indObj.optString("title", "Threat Signal"),
                            description = indObj.optString("description", ""),
                            severity = severity,
                            category = indObj.optString("category", "General")
                        )
                    )
                }
            }

            val actions = mutableListOf<String>()
            val actArray = obj.optJSONArray("recommendedActions")
            if (actArray != null) {
                for (i in 0 until actArray.length()) {
                    actions.add(actArray.optString(i))
                }
            }
            if (actions.isEmpty()) {
                actions.addAll(defaultActionsFor(scenario, score))
            }

            AiAnalysisResult(
                aiRiskScore = score,
                detectedScenario = scenario,
                summaryExplanation = explanation,
                indicators = indicators,
                recommendedActions = actions,
                usedAiModel = true
            )
        } catch (e: Exception) {
            generateSynthesizedAiResponse(jsonText, FraudScenario.BANK_KYC_PAN_UPDATE, fallbackScore)
        }
    }

    private fun generateSynthesizedAiResponse(
        sanitizedText: String,
        heuristicScenario: FraudScenario,
        heuristicScore: Int
    ): AiAnalysisResult {
        val actions = mutableListOf<String>()
        val indicators = mutableListOf<ThreatIndicator>()

        val lower = sanitizedText.lowercase(java.util.Locale.ROOT)
        val (scenario, explanation) = when {
            heuristicScore >= 40 && heuristicScenario == FraudScenario.UPI_QR_CASHBACK_FRAUD -> {
                when {
                    lower.contains("collect") || (lower.contains("request") && (lower.contains("pin") || lower.contains("approve"))) -> {
                        Pair(
                            FraudScenario.UPI_QR_CASHBACK_FRAUD,
                            "Deceptive UPI Collect Request detected. Crucial: Approving a collect request or entering your UPI PIN will transfer your money to the fraudster. You NEVER enter a PIN to receive payments."
                        )
                    }
                    lower.contains("@") && (lower.contains("refund") || lower.contains("support") || lower.contains("desk") || lower.contains("officer")) -> {
                        Pair(
                            FraudScenario.UPI_QR_CASHBACK_FRAUD,
                            "Suspicious VPA Impersonation detected. Fraudsters configure personal UPI IDs imitating corporate support or billing desks to trick victims into sending money."
                        )
                    }
                    lower.contains("qr") -> {
                        Pair(
                            FraudScenario.UPI_QR_CASHBACK_FRAUD,
                            "Fraudulent Merchant QR Code Trap detected. Scanning any QR code in Google Pay, PhonePe, or Paytm initiates a debit payment from your bank account."
                        )
                    }
                    else -> {
                        Pair(
                            FraudScenario.UPI_QR_CASHBACK_FRAUD,
                            "Fraudulent UPI money request. Remember: scanning a QR code or entering your UPI PIN always DEDUCTS money from your bank account."
                        )
                    }
                }
            }
            heuristicScore >= 40 && heuristicScenario == FraudScenario.BANK_KYC_PAN_UPDATE -> {
                when {
                    lower.contains("aadhaar") -> {
                        Pair(
                            FraudScenario.BANK_KYC_PAN_UPDATE,
                            "Urgent Aadhaar link coercion scam. Scammers falsely threaten immediate SIM card or bank account blocking to harvest confidential identity credentials."
                        )
                    }
                    lower.contains("kyc") && (lower.contains("expir") || lower.contains("suspend") || lower.contains("block")) -> {
                        Pair(
                            FraudScenario.BANK_KYC_PAN_UPDATE,
                            "Urgent KYC expiry phishing scam. Indian banks never demand immediate KYC updates via SMS or email links. Never click unverified links."
                        )
                    }
                    else -> {
                        Pair(
                            FraudScenario.BANK_KYC_PAN_UPDATE,
                            "High-risk banking phishing scam. Banks in India never send links via SMS or email to update PAN or unblock accounts."
                        )
                    }
                }
            }
            heuristicScore >= 70 && heuristicScenario == FraudScenario.ELECTRICITY_BILL_DISCONNECT -> {
                Pair(
                    FraudScenario.ELECTRICITY_BILL_DISCONNECT,
                    "Electricity bill disconnection scam. Discoms do not issue power cutoff threats asking you to call personal numbers."
                )
            }
            heuristicScore >= 70 && heuristicScenario == FraudScenario.DIGITAL_ARREST_EXTORTION -> {
                Pair(
                    FraudScenario.DIGITAL_ARREST_EXTORTION,
                    "Severe extortion racket pretending to be CBI or Police. Indian law enforcement never places citizens under 'Digital Arrest' via Skype."
                )
            }
            heuristicScore >= 60 && heuristicScenario == FraudScenario.TELEGRAM_JOB_TASK_SCAM -> {
                Pair(
                    FraudScenario.TELEGRAM_JOB_TASK_SCAM,
                    "Prepaid task scam. Offers unrealistic daily income for liking YouTube videos, then steals money via Telegram recharge tasks."
                )
            }
            heuristicScore >= 60 && heuristicScenario == FraudScenario.TRAFFIC_ECHALLAN_APK -> {
                Pair(
                    FraudScenario.TRAFFIC_ECHALLAN_APK,
                    "Malicious APK file impersonating Parivahan e-challan. Installing APKs from SMS gives hackers full access to your SMS and bank OTPs."
                )
            }
            heuristicScore in 40..69 -> {
                Pair(
                    heuristicScenario,
                    "Suspicious indicators detected. Exercise caution and verify directly through official verified apps or hotlines."
                )
            }
            else -> {
                Pair(
                    FraudScenario.LEGITIMATE_COMMUNICATION,
                    "No indicators of fraud detected. Message appears to be genuine communication without suspicious links or urgency traps."
                )
            }
        }

        actions.addAll(defaultActionsFor(scenario, heuristicScore))

        return AiAnalysisResult(
            aiRiskScore = heuristicScore,
            detectedScenario = scenario,
            summaryExplanation = explanation,
            indicators = indicators,
            recommendedActions = actions,
            usedAiModel = false
        )
    }

    private fun defaultActionsFor(scenario: FraudScenario, score: Int): List<String> {
        if (score < 40) {
            return listOf(
                "Never share your OTP, UPI PIN, or Netbanking password with anyone.",
                "Always check that websites end with official domains (e.g. .gov.in, .sbi, hdfcbank.com)."
            )
        }

        return when (scenario) {
            FraudScenario.BANK_KYC_PAN_UPDATE -> listOf(
                "DO NOT click any link in this SMS, email, or message.",
                "Banks in India never freeze accounts without written notice, and never renew KYC via external links.",
                "Neither UIDAI nor telecom providers block SIM cards via SMS links demanding Aadhaar updates.",
                "To check KYC or account status, log into your official bank mobile app (e.g. YONO SBI, HDFC MobileBanking) or visit your home branch.",
                "Dial 1930 immediately or visit cybercrime.gov.in if you entered any bank or Aadhaar details."
            )
            FraudScenario.ELECTRICITY_BILL_DISCONNECT -> listOf(
                "DO NOT call the phone number mentioned in the SMS.",
                "Power electricity boards will NEVER disconnect power via an SMS with a personal mobile number.",
                "Pay your electricity bill only via official electricity board apps, website, or Bharat BillPay (BBPS).",
                "Forward this scam SMS to 1909 or report on the Chakshu portal (sancharsaathi.gov.in)."
            )
            FraudScenario.UPI_QR_CASHBACK_FRAUD -> listOf(
                "DO NOT approve any pending UPI Collect requests on PhonePe, Google Pay, Paytm, or BHIM.",
                "Remember: Entering your UPI PIN is ONLY needed to SEND money, NEVER to receive money.",
                "DO NOT scan any QR code sent on WhatsApp, Telegram, or OLX to 'receive' money, refunds, or cashback.",
                "Verify UPI IDs carefully—official corporate desks never use personal email handles (@okhdfcbank, @paytm) or collect requests.",
                "Decline the payment request immediately and block the sender in your UPI app.",
                "Dial 1930 immediately or visit cybercrime.gov.in if any amount was deducted."
            )
            FraudScenario.DIGITAL_ARREST_EXTORTION -> listOf(
                "DISCONNECT the call immediately. Do not stay on the line.",
                "Indian Police, CBI, ED, and Courts NEVER conduct 'Digital Arrest' or court hearings over Skype/video.",
                "Never transfer funds to any 'government verification' or 'escrow' bank account.",
                "Call 1930 (National Cyber Crime Helpline) and report to your local police station."
            )
            FraudScenario.TELEGRAM_JOB_TASK_SCAM -> listOf(
                "Do not join the Telegram channel or complete any prepaid recharge tasks.",
                "Never pay upfront money to an employer to 'unlock' higher earnings.",
                "Block the recruiter number on WhatsApp immediately."
            )
            FraudScenario.TRAFFIC_ECHALLAN_APK -> listOf(
                "DO NOT download or install the .APK file on your Android phone.",
                "If already installed, turn on Airplane Mode, uninstall the app, and reset your banking passwords.",
                "Check genuine vehicle traffic challans only on echallan.parivahan.gov.in."
            )
            FraudScenario.LEGITIMATE_COMMUNICATION -> listOf(
                "Never share OTPs even with bank employees.",
                "Keep SMS alerts enabled for all bank transactions."
            )
        }
    }
}
