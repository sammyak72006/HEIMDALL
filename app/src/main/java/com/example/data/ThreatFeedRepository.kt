package com.example.data

import com.example.model.FraudScenario
import com.example.model.IndicatorSeverity
import com.example.model.ThreatFeedItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThreatFeedRepository(private val threatFeedDao: ThreatFeedDao) {

    val allThreats: Flow<List<ThreatFeedItem>> = threatFeedDao.getAllThreats().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun populateInitialFeedsIfEmpty() {
        if (threatFeedDao.getCount() == 0) {
            val initialFeeds = listOf(
                ThreatFeedEntity(
                    id = "IN-IOC-001",
                    pattern = "sbi-yono-pan-update.xyz",
                    type = "DOMAIN",
                    category = FraudScenario.BANK_KYC_PAN_UPDATE.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Spoofed SBI YONO netbanking form stealing user ID, password, and OTP",
                    dateAdded = "2026-10-14"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-002",
                    pattern = "mahavitaran-bill-pay.click",
                    type = "DOMAIN",
                    category = FraudScenario.ELECTRICITY_BILL_DISCONNECT.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Fake electricity board payment page stealing debit card details",
                    dateAdded = "2026-10-12"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-003",
                    pattern = "parivahan-echallan-pay.apk",
                    type = "APK",
                    category = FraudScenario.TRAFFIC_ECHALLAN_APK.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Malicious Android Trojan spying on incoming SMS and banking OTPs",
                    dateAdded = "2026-10-10"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-004",
                    pattern = "paytm-cashback-reward2026.live",
                    type = "DOMAIN",
                    category = FraudScenario.UPI_QR_CASHBACK_FRAUD.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Pretends to offer ₹2,500 cashback, redirects to UPI app with payment debit request",
                    dateAdded = "2026-10-08"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-005",
                    pattern = "cbi-cybercell-investigation.top",
                    type = "DOMAIN",
                    category = FraudScenario.DIGITAL_ARREST_EXTORTION.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Hosts fake Supreme Court & CBI arrest warrant PDFs used during extortion calls",
                    dateAdded = "2026-10-05"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-006",
                    pattern = "telegram.me/youtube_like_task_earn",
                    type = "URL",
                    category = FraudScenario.TELEGRAM_JOB_TASK_SCAM.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Task fraud recruitment bot paying ₹150 initially, then extorting ₹50,000 for crypto recharge",
                    dateAdded = "2026-10-02"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-007",
                    pattern = "hdfc-netbanking-rewards.top",
                    type = "DOMAIN",
                    category = FraudScenario.BANK_KYC_PAN_UPDATE.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Fake reward points redemption portal harvesting credit card numbers and CVV",
                    dateAdded = "2026-09-28"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-008",
                    pattern = "bescom-officer-bill.xyz",
                    type = "DOMAIN",
                    category = FraudScenario.ELECTRICITY_BILL_DISCONNECT.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Karnataka BESCOM power cut scam server",
                    dateAdded = "2026-09-25"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-009",
                    pattern = "sbi-kyc-renewal.icu",
                    type = "DOMAIN",
                    category = FraudScenario.BANK_KYC_PAN_UPDATE.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Phishing domain threatening 24-hour account suspension for KYC expiry",
                    dateAdded = "2026-10-15"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-010",
                    pattern = "telecom-aadhaar-kyc.cfd",
                    type = "DOMAIN",
                    category = FraudScenario.BANK_KYC_PAN_UPDATE.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Coercive portal threatening SIM card deactivation unless Aadhaar is submitted",
                    dateAdded = "2026-10-15"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-011",
                    pattern = "refund.support.sbi@paytm",
                    type = "VPA",
                    category = FraudScenario.UPI_QR_CASHBACK_FRAUD.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "VPA impersonating State Bank of India customer refund desk",
                    dateAdded = "2026-10-15"
                ),
                ThreatFeedEntity(
                    id = "IN-IOC-012",
                    pattern = "army.advance.officer@okhdfcbank",
                    type = "VPA",
                    category = FraudScenario.UPI_QR_CASHBACK_FRAUD.name,
                    severity = IndicatorSeverity.CRITICAL.name,
                    reason = "Fraudulent UPI collect handle used in OLX / defense officer advance scams",
                    dateAdded = "2026-10-15"
                )
            )
            threatFeedDao.insertAll(initialFeeds)
        }
    }

    suspend fun checkThreatMatch(text: String): ThreatFeedItem? {
        val lower = text.lowercase()
        val all = threatFeedDao.searchThreats("")
        return all.firstOrNull { lower.contains(it.pattern.lowercase()) }?.toDomain()
    }
}
