package com.example.engine

/**
 * On-device privacy pre-processor adapted for Indian PII.
 * Redacts Aadhaar numbers, PAN cards, UPI handles, Indian mobile numbers,
 * credit cards, and email addresses.
 */
object PrivacySanitizer {

    private val AADHAAR_REGEX = Regex("\\b[2-9]\\d{3}\\s?\\d{4}\\s?\\d{4}\\b")
    private val PAN_REGEX = Regex("\\b[A-Z]{5}\\d{4}[A-Z]\\b")
    private val UPI_REGEX = Regex("\\b[a-zA-Z0-9.\\-_]{2,256}@(okhdfcbank|okaxis|oksbi|paytm|ybl|ibl|apl|axl|upi)\\b")
    private val INDIAN_PHONE_REGEX = Regex("\\b(?:\\+91[- ]?)?[6-9]\\d{9}\\b")
    private val CREDIT_CARD_REGEX = Regex("\\b(?:4[0-9]{12}(?:[0-9]{3})?|5[1-5][0-9]{14}|3[47][0-9]{13}|6(?:011|5[0-9]{2})[0-9]{12})\\b")
    private val EMAIL_REGEX = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b")

    data class SanitizationResult(
        val sanitizedText: String,
        val redactedItemsCount: Int,
        val redactedCategories: List<String>
    )

    fun sanitize(input: String): SanitizationResult {
        var text = input
        var count = 0
        val categories = mutableListOf<String>()

        if (AADHAAR_REGEX.containsMatchIn(text)) {
            text = AADHAAR_REGEX.replace(text, "[REDACTED_AADHAAR]")
            count++
            categories.add("Aadhaar Number")
        }

        if (PAN_REGEX.containsMatchIn(text)) {
            text = PAN_REGEX.replace(text, "[REDACTED_PAN_CARD]")
            count++
            categories.add("PAN Card")
        }

        if (UPI_REGEX.containsMatchIn(text)) {
            text = UPI_REGEX.replace(text, "[REDACTED_UPI_ID]")
            count++
            categories.add("UPI ID")
        }

        if (INDIAN_PHONE_REGEX.containsMatchIn(text)) {
            text = INDIAN_PHONE_REGEX.replace(text, "[REDACTED_PHONE]")
            count++
            if (!categories.contains("Mobile Number")) categories.add("Mobile Number")
        }

        if (CREDIT_CARD_REGEX.containsMatchIn(text)) {
            text = CREDIT_CARD_REGEX.replace(text, "[REDACTED_CARD]")
            count++
            categories.add("Payment Card")
        }

        if (EMAIL_REGEX.containsMatchIn(text)) {
            text = EMAIL_REGEX.replace(text) { match ->
                val parts = match.value.split("@")
                if (parts.size == 2) {
                    val user = parts[0]
                    val domain = parts[1]
                    val maskedUser = if (user.length > 2) user.take(2) + "***" else "***"
                    "$maskedUser@$domain"
                } else {
                    "[REDACTED_EMAIL]"
                }
            }
            count++
            categories.add("Email Address")
        }

        return SanitizationResult(
            sanitizedText = text,
            redactedItemsCount = count,
            redactedCategories = categories.distinct()
        )
    }
}
