package com.example.model

data class CapturedNotification(
    val id: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val content: String,
    val timestamp: Long,
    val isFlagged: Boolean = false,
    val flagReason: String? = null,
    val flaggedAt: Long? = null,
    val riskScore: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.LOW,
    val detectedScamType: String? = null
)
