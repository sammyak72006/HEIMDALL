package com.example.data

import com.example.model.BenchmarkCase
import com.example.model.FraudScenario
import com.example.model.ScanType

object BenchmarkRepository {

    val testSuite: List<BenchmarkCase> = listOf(
        // === CATEGORY 1: BANK KYC & PAN BLOCK SCAMS ===
        BenchmarkCase(
            id = "IN-BANK-01",
            title = "SBI YONO PAN Update Required SMS",
            category = FraudScenario.BANK_KYC_PAN_UPDATE,
            input = "Dear SBI Customer, your YONO netbanking account will be blocked today. Please update your PAN Card immediately by clicking http://sbi-yono-pan-update.xyz to avoid deactivation.",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("sbi", "blocked", ".xyz"),
            notes = "Classic Indian bank smishing template using fake SBI link on .xyz TLD."
        ),
        BenchmarkCase(
            id = "IN-BANK-02",
            title = "HDFC Credit Card Reward Points Expiry",
            category = FraudScenario.BANK_KYC_PAN_UPDATE,
            input = "HDFC Bank Alert: Your 9,850 reward points worth ₹4,925 are expiring tonight. Redeem into cash immediately at https://hdfc-netbanking-rewards.top/claim before midnight.",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("hdfc", "reward points", ".top"),
            notes = "Reward point lure harvesting credit card number, expiry, CVV, and OTP."
        ),
        BenchmarkCase(
            id = "IN-KYC-01",
            title = "Urgent KYC Expiry Suspension SMS",
            category = FraudScenario.BANK_KYC_PAN_UPDATE,
            input = "Dear Customer, your bank account KYC expiry deadline has passed. Your debit card and netbanking will be suspended within 24 hours. Update KYC immediately at http://sbi-kyc-renewal.icu to avoid permanent deactivation.",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("kyc expiry", "suspended", "update kyc"),
            notes = "Common Indian smishing vector demanding immediate KYC update to prevent account freeze."
        ),
        BenchmarkCase(
            id = "IN-KYC-02",
            title = "Urgent Re-KYC Expiry Notice Email",
            category = FraudScenario.BANK_KYC_PAN_UPDATE,
            input = "URGENT EMAIL NOTICE: Your HDFC Bank account KYC has expired. Under RBI guidelines your account is placed on temporary hold. Complete your KYC immediately today at https://hdfc-online-kyc-verify.buzz before 11:59 PM.",
            scanType = ScanType.EMAIL,
            groundTruthMalicious = true,
            expectedKeywords = listOf("kyc has expired", "complete your kyc immediately"),
            notes = "Email phishing spoofing RBI guidelines and bank KYC expiry."
        ),
        BenchmarkCase(
            id = "IN-AADHAAR-01",
            title = "SIM Block Aadhaar Link Required SMS",
            category = FraudScenario.BANK_KYC_PAN_UPDATE,
            input = "Aadhaar Link Required: Dear User, your mobile SIM card will be deactivated tonight because your Aadhaar link is not completed. Link your Aadhaar card immediately at http://telecom-aadhaar-kyc.cfd to prevent disconnection.",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("aadhaar link required", "link your aadhaar card immediately"),
            notes = "Urgent Aadhaar linking threat vector coercing user into submitting Aadhaar biometric details."
        ),
        BenchmarkCase(
            id = "IN-AADHAAR-02",
            title = "Bank Account Aadhaar Link Required Email",
            category = FraudScenario.BANK_KYC_PAN_UPDATE,
            input = "Dear Customer, Mandatory Aadhaar linking is required for your savings account. Link Aadhaar to avoid suspension within 24 hours. Submit Aadhaar number at http://pnb-aadhaar-link.top",
            scanType = ScanType.EMAIL,
            groundTruthMalicious = true,
            expectedKeywords = listOf("aadhaar linking is required", "link aadhaar to avoid suspension"),
            notes = "Urgent bank Aadhaar link required email threat."
        ),

        // === CATEGORY 2: ELECTRICITY BILL DISCONNECTION SCAM ===
        BenchmarkCase(
            id = "IN-ELEC-01",
            title = "Electricity Power Cut at 9:30 PM Tonight",
            category = FraudScenario.ELECTRICITY_BILL_DISCONNECT,
            input = "Dear consumer, your electricity power will be disconnected tonight at 9:30 PM from electricity office because your previous month bill was not updated. Please immediately contact our electricity officer at 9823412345.",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("electricity", "disconnected", "9:30 pm"),
            notes = "One of India's most reported cyber scams, tricking victims into calling fraudster or installing QuickSupport."
        ),
        BenchmarkCase(
            id = "IN-ELEC-02",
            title = "BESCOM Bangalore Power Cut Warning",
            category = FraudScenario.ELECTRICITY_BILL_DISCONNECT,
            input = "BESCOM Alert: Immediate action required. Light will be cut within 2 hours due to unpaid dues. Pay now at http://bescom-officer-bill.xyz or your connection will be removed permanently.",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("bescom", "light will be cut", ".xyz"),
            notes = "Impersonation of Bangalore Electricity Supply Company."
        ),

        // === CATEGORY 3: UPI & QR CODE SCAMS ===
        BenchmarkCase(
            id = "IN-UPI-01",
            title = "PhonePe ₹2,500 Scratch Card Cashback",
            category = FraudScenario.UPI_QR_CASHBACK_FRAUD,
            input = "Congratulations! You have won ₹2,500 cashback in PhonePe Festival Lucky Draw. Click https://paytm-cashback-reward2026.live to claim and enter your UPI PIN to credit your bank account instantly.",
            scanType = ScanType.WHATSAPP_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("cashback", "upi pin", ".live"),
            notes = "UPI fraud tricking victim into entering UPI PIN to 'receive' money."
        ),
        BenchmarkCase(
            id = "IN-UPI-02",
            title = "OLX Buyer Advance QR Code Scan",
            category = FraudScenario.UPI_QR_CASHBACK_FRAUD,
            input = "I am interested in buying your sofa on OLX. I am sending you a QR code for ₹15,000 advance payment. Just scan this QR code and approve in Google Pay to receive payment in your account.",
            scanType = ScanType.WHATSAPP_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("scan this qr", "advance payment", "google pay"),
            notes = "OLX QR code reverse-charge trick where victim loses money instead of receiving."
        ),
        BenchmarkCase(
            id = "IN-UPI-03",
            title = "Fake UPI Collect Request Scam",
            category = FraudScenario.UPI_QR_CASHBACK_FRAUD,
            input = "Collect request received: ₹8,500 from army.advance.officer@okhdfcbank on PhonePe. Open PhonePe, approve payment request and enter your UPI PIN to claim festival cashback credit into your bank account.",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("collect request", "approve payment", "enter your upi pin"),
            notes = "Fake UPI collect request deceiving victim into authorizing payment to receive money."
        ),
        BenchmarkCase(
            id = "IN-UPI-04",
            title = "Fake SBI Refund VPA Impersonation",
            category = FraudScenario.UPI_QR_CASHBACK_FRAUD,
            input = "SBI Customer Support Notice: Your failed ATM transaction refund of ₹4,000 is ready. Send 1 Rupee verification request to our official refund desk at refund.support.sbi@paytm to verify and initiate instant NEFT reversal.",
            scanType = ScanType.WHATSAPP_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("refund.support.sbi@paytm", "verification request"),
            notes = "VPA impersonation using consumer Paytm handle imitating official State Bank of India support."
        ),
        BenchmarkCase(
            id = "IN-UPI-05",
            title = "Fraudulent Merchant QR Advance Trap",
            category = FraudScenario.UPI_QR_CASHBACK_FRAUD,
            input = "Scan this Merchant QR code using GPay or Paytm to receive ₹12,000 token deposit for apartment rent. Do not worry if app asks for PIN, it is only for RBI KYC verification.",
            scanType = ScanType.WHATSAPP_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("scan this merchant qr", "receive", "pin"),
            notes = "Merchant QR code fraud claiming scanning QR code deposits money into bank account."
        ),

        // === CATEGORY 4: DIGITAL ARREST & POLICE EXTORTION ===
        BenchmarkCase(
            id = "IN-POLICE-01",
            title = "CBI & Customs Narcotics Parcel Notice",
            category = FraudScenario.DIGITAL_ARREST_EXTORTION,
            input = "URGENT: FedEX parcel with your Aadhaar ID seized at Mumbai Airport containing 140 grams MDMA and 5 fake passports. CBI cyber crime branch has issued a Digital Arrest warrant. Connect immediately on Skype ID: @cbi_cyber_cell or local police will raid within 1 hour.",
            scanType = ScanType.GENERAL_TEXT,
            groundTruthMalicious = true,
            expectedKeywords = listOf("digital arrest", "cbi", "narcotics"),
            notes = "Digital arrest extortion scam causing immense psychological terror and financial drain."
        ),
        BenchmarkCase(
            id = "IN-POLICE-02",
            title = "Supreme Court / ED Money Laundering Notice",
            category = FraudScenario.DIGITAL_ARREST_EXTORTION,
            input = "Enforcement Directorate Notice: 16 suspicious bank accounts linked to your PAN under PMLA Act. High Court ordered 24-hour Digital Arrest. Do not disconnect this call or tell family. Transfer verification deposit to RBI escrow account.",
            scanType = ScanType.GENERAL_TEXT,
            groundTruthMalicious = true,
            expectedKeywords = listOf("digital arrest", "enforcement directorate", "transfer"),
            notes = "Extortion using fake Supreme Court letters and video call police sets."
        ),

        // === CATEGORY 5: PART-TIME JOB & YOUTUBE TASK SCAMS ===
        BenchmarkCase(
            id = "IN-JOB-01",
            title = "Earn ₹3,000 Daily Liking YouTube Videos",
            category = FraudScenario.TELEGRAM_JOB_TASK_SCAM,
            input = "Namaste! I am Priya from Global Digital Media. We offer simple work from home job for students and housewives. Earn ₹50 per like, ₹2,000 to ₹4,500 daily just by liking YouTube videos and rating hotels on Google Maps. Contact HR on Telegram: @youtube_like_task_earn",
            scanType = ScanType.WHATSAPP_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("part time", "youtube", "telegram"),
            notes = "The notorious task scam where victims are paid small amounts initially, then scammed of lakhs."
        ),
        BenchmarkCase(
            id = "IN-JOB-02",
            title = "Amazon Reviewer Prepaid Order Task",
            category = FraudScenario.TELEGRAM_JOB_TASK_SCAM,
            input = "Amazon Merchant Task: Complete 5 product review orders today and get 30% instant commission. Deposit ₹5,000 into merchant account to activate VIP task code on Telegram.",
            scanType = ScanType.WHATSAPP_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("deposit", "commission", "telegram"),
            notes = "Prepaid task scheme leading to total deposit forfeiture."
        ),

        // === CATEGORY 6: TRAFFIC E-CHALLAN APK SCAM ===
        BenchmarkCase(
            id = "IN-CHALLAN-01",
            title = "Parivahan Pending Traffic Challan APK SMS",
            category = FraudScenario.TRAFFIC_ECHALLAN_APK,
            input = "Parivahan Sewa Alert: Vehicle No MH02CB4590 has unpaid traffic challan of ₹1,000 for red light jump. Pay immediately to avoid court summons. Install Parivahan app: http://parivahan-echallan-pay.apk",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = true,
            expectedKeywords = listOf("challan", "parivahan", ".apk"),
            notes = "Dangerous malware spreading via SMS in India disguised as traffic e-challan app."
        ),

        // === CATEGORY 7: LEGITIMATE INDIAN SERVICES (CONTROLS) ===
        BenchmarkCase(
            id = "IN-LEGIT-01",
            title = "Official SBI Netbanking OTP SMS",
            category = FraudScenario.LEGITIMATE_COMMUNICATION,
            input = "482910 is your OTP for transaction of INR 3,500.00 at Swiggy. NEVER share OTP with anyone, including SBI employees. SBI never asks for OTP or PIN.",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = false,
            expectedKeywords = listOf("otp", "never share"),
            notes = "Authentic banking OTP SMS with explicit anti-fraud warning and no external link."
        ),
        BenchmarkCase(
            id = "IN-LEGIT-02",
            title = "Official IRCTC Train Ticket Confirmation",
            category = FraudScenario.LEGITIMATE_COMMUNICATION,
            input = "IRCTC: PNR 2849102938, Train 12951 NDLS TEJAS RAJ, 18-Oct-26, 3A, Coach B4, Berth 23 (MB). Charting Status: Confirmed. Check status on https://www.irctc.co.in",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = false,
            expectedKeywords = listOf("irctc.co.in", "pnr"),
            notes = "Authentic railway booking update pointing to canonical irctc.co.in domain."
        ),
        BenchmarkCase(
            id = "IN-LEGIT-03",
            title = "Official HDFC Bank Account Credit Alert",
            category = FraudScenario.LEGITIMATE_COMMUNICATION,
            input = "Your A/C XXXXX1928 has been credited with INR 45,000.00 on 14-OCT-26 by NEFT salary transfer. Avail balance: INR 68,420.00. - HDFC Bank",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = false,
            expectedKeywords = listOf("credited", "hdfc bank"),
            notes = "Standard informative transaction notification without links or call requests."
        ),
        BenchmarkCase(
            id = "IN-LEGIT-04",
            title = "Official Income Tax Dept Assessment Notice",
            category = FraudScenario.LEGITIMATE_COMMUNICATION,
            input = "Income Tax Department: Intimation under Section 143(1) for PAN ABCDE1234F for AY 2025-26 has been sent to your registered email. View details on https://eportal.incometax.gov.in",
            scanType = ScanType.SMS_MESSAGE,
            groundTruthMalicious = false,
            expectedKeywords = listOf("incometax.gov.in"),
            notes = "Authentic official .gov.in domain communication."
        )
    )
}
