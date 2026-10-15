package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.HeuristicEngine
import com.example.engine.PrivacySanitizer
import com.example.model.ConfusionMatrix
import com.example.model.ScanType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context matches PhishGuard AI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("PhishGuard AI", appName)
    }

    @Test
    fun `heuristic engine catches electricity bill disconnection scam`() {
        val input = "Dear consumer, your electricity power will be disconnected tonight at 9:30 PM. Call officer at 9876543210"
        val analysis = HeuristicEngine.analyze(input, ScanType.SMS_MESSAGE)
        assertTrue(analysis.heuristicScore >= 45)
        assertTrue(analysis.indicators.any { it.title.contains("Electricity") })
    }

    @Test
    fun `heuristic engine flags malicious APK download links`() {
        val input = "Parivahan e-challan pending ₹500, download app: http://parivahan-echallan.apk"
        val analysis = HeuristicEngine.analyze(input, ScanType.SMS_MESSAGE)
        assertTrue(analysis.heuristicScore >= 45)
        assertTrue(analysis.indicators.any { it.title.contains("APK") })
    }

    @Test
    fun `privacy sanitizer redacts Indian Aadhaar, PAN and Phone`() {
        val raw = "Aadhaar: 4123 4567 8901, PAN: ABCDE1234F, Phone: +91 9876543210"
        val result = PrivacySanitizer.sanitize(raw)
        assertFalse(result.sanitizedText.contains("4123 4567 8901"))
        assertFalse(result.sanitizedText.contains("ABCDE1234F"))
        assertTrue(result.sanitizedText.contains("[REDACTED_AADHAAR]"))
        assertTrue(result.sanitizedText.contains("[REDACTED_PAN_CARD]"))
        assertTrue(result.sanitizedText.contains("[REDACTED_PHONE]"))
        assertEquals(3, result.redactedItemsCount)
    }

    @Test
    fun `confusion matrix calculates accuracy precision and recall correctly`() {
        val matrix = ConfusionMatrix(
            truePositives = 8,
            trueNegatives = 2,
            falsePositives = 0,
            falseNegatives = 0,
            totalTested = 10,
            avgLatencyMs = 30L
        )
        assertEquals(1.0f, matrix.accuracy, 0.001f)
        assertEquals(1.0f, matrix.precision, 0.001f)
        assertEquals(1.0f, matrix.recall, 0.001f)
        assertEquals(1.0f, matrix.f1Score, 0.001f)
    }

    @Test
    fun `heuristic engine flags urgent KYC expiry language in SMS and Email`() {
        val sms = "Dear Customer, your bank account KYC expiry deadline has passed. Update KYC immediately at http://sbi-kyc.icu"
        val smsAnalysis = HeuristicEngine.analyze(sms, ScanType.SMS_MESSAGE)
        assertTrue(smsAnalysis.heuristicScore >= 45)
        assertTrue(smsAnalysis.triggeredRules.any { it.contains("KYC Expiry") })
        assertTrue(smsAnalysis.indicators.any { it.title.contains("KYC Expiry") })

        val email = "URGENT EMAIL: Your HDFC Bank account KYC has expired. Complete your KYC immediately to avoid block."
        val emailAnalysis = HeuristicEngine.analyze(email, ScanType.EMAIL)
        assertTrue(emailAnalysis.heuristicScore >= 45)
        assertTrue(emailAnalysis.indicators.any { it.category == "KYC Phishing" })
    }

    @Test
    fun `heuristic engine flags urgent Aadhaar link required language in SMS and Email`() {
        val sms = "Aadhaar Link Required: Your mobile SIM card will be deactivated tonight. Link your Aadhaar card immediately."
        val analysis = HeuristicEngine.analyze(sms, ScanType.SMS_MESSAGE)
        assertTrue(analysis.heuristicScore >= 45)
        assertTrue(analysis.triggeredRules.any { it.contains("Aadhaar Link") })
        assertTrue(analysis.indicators.any { it.title.contains("Aadhaar Link") })
    }

    @Test
    fun `heuristic engine flags Indian UPI fake collect requests, VPA impersonations, and merchant QR traps`() {
        val collectMsg = "Collect request received: ₹8,500 from army.advance@okhdfcbank. Enter UPI PIN to claim cashback credit."
        val collectAnalysis = HeuristicEngine.analyze(collectMsg, ScanType.SMS_MESSAGE)
        assertTrue(collectAnalysis.heuristicScore >= 45)
        assertTrue(collectAnalysis.triggeredRules.any { it.contains("Collect Request") })

        val vpaMsg = "Send ₹1 refund verification to refund.support.sbi@paytm"
        val vpaAnalysis = HeuristicEngine.analyze(vpaMsg, ScanType.SMS_MESSAGE)
        assertTrue(vpaAnalysis.triggeredRules.any { it.contains("VPA Impersonation") })

        val qrMsg = "Scan this Merchant QR code using PhonePe to receive ₹12,000 refund into your bank account."
        val qrAnalysis = HeuristicEngine.analyze(qrMsg, ScanType.SMS_MESSAGE)
        assertTrue(qrAnalysis.heuristicScore >= 45)
        assertTrue(qrAnalysis.triggeredRules.any { it.contains("Merchant QR") })
    }
}
