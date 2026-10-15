package com.example.engine

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.TimeUnit

object KeywordRuleEngine {

    private const val TAG = "KeywordRuleEngine"
    private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    data class KeywordGroup(
        val name: String,
        val points: Int,
        val phrases: List<String>
    )

    data class RuleSet(
        val stripNegatedSharingSentences: List<String>,
        val scamGroups: List<KeywordGroup>,
        val spamGroups: List<KeywordGroup>,
        val importantGroups: List<KeywordGroup>
    ) {
        val totalScamPhrases: Int get() = scamGroups.sumOf { it.phrases.size }
        val totalSpamPhrases: Int get() = spamGroups.sumOf { it.phrases.size }
        val totalImportantPhrases: Int get() = importantGroups.sumOf { it.phrases.size }
    }

    data class MatchedGroupInfo(
        val category: String,
        val groupName: String,
        val points: Int,
        val matchedPhrases: List<String>
    )

    data class AnalysisResult(
        val category: String, // "Scam (High)", "Scam (Medium)", "Scam", "Important", "Spam", "Unclear", or error
        val decisionPath: String, // "rules" or "Gemini"
        val normalizedInput: String,
        val normalizedScamInput: String,
        val scamScore: Int,
        val spamScore: Int,
        val importantScore: Int,
        val matchedGroups: List<MatchedGroupInfo>,
        val phrasesPerClass: String,
        val error: String? = null
    )

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private var activeRuleSet: RuleSet? = null
    private var lastLoadError: String? = null

    val isLoaded: Boolean get() = activeRuleSet != null
    val currentRuleSet: RuleSet? get() = activeRuleSet
    val loadError: String? get() = lastLoadError

    /**
     * Requirement 2: Load keywords.json with await and keep Analyze disabled until loaded.
     * If loading fails, show a visible error. Do not continue with empty rules.
     */
    suspend fun loadRules(context: Context): RuleSet = withContext(Dispatchers.IO) {
        try {
            val customFile = context.getFileStreamPath("custom_keywords.json")
            val jsonString = if (customFile != null && customFile.exists() && customFile.length() > 0) {
                Log.i(TAG, "Loading saved custom keywords from ${customFile.absolutePath}")
                customFile.bufferedReader().use { it.readText() }
            } else {
                context.assets.open("keywords.json").bufferedReader().use { it.readText() }
            }
            loadRulesFromJsonString(jsonString)
        } catch (e: Exception) {
            val err = "Failed to load keywords.json: ${e.message}"
            Log.e(TAG, err, e)
            lastLoadError = err
            activeRuleSet = null
            throw e
        }
    }

    fun loadRulesFromJsonString(jsonString: String): RuleSet {
        val root = JSONObject(jsonString)

        val stripNegated = mutableListOf<String>()
        val stripArray = root.optJSONArray("strip_negated_sharing_sentences")
        if (stripArray != null) {
            for (i in 0 until stripArray.length()) {
                val phrase = stripArray.optString(i)?.trim()
                if (!phrase.isNullOrEmpty()) {
                    stripNegated.add(normalizeBasic(phrase))
                }
            }
        }

        val fpArray = root.optJSONArray("false_positive_protection")
        if (fpArray != null) {
            for (i in 0 until fpArray.length()) {
                val item = fpArray.optJSONObject(i)
                val example = item?.optString("example")
                if (!example.isNullOrBlank()) {
                    stripNegated.add(normalizeBasic(example))
                }
            }
        }

        val scamGroups = mutableListOf<KeywordGroup>()
        scamGroups.addAll(parseGroups(root.opt("scam")))

        // Support raw HEIMDALL categories object if present
        if (root.has("categories")) {
            val categoriesObj = root.optJSONObject("categories")
            if (categoriesObj != null) {
                val keys = categoriesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val groupVal = categoriesObj.optJSONObject(key) ?: continue
                    val phrases = mutableListOf<String>()
                    val suspArray = groupVal.optJSONArray("suspicious_phrases")
                    if (suspArray != null) {
                        for (i in 0 until suspArray.length()) {
                            val p = suspArray.optString(i)?.trim()
                            if (!p.isNullOrEmpty()) phrases.add(normalizeBasic(p))
                        }
                    }
                    val kwArray = groupVal.optJSONArray("keywords")
                    if (kwArray != null) {
                        for (i in 0 until kwArray.length()) {
                            val p = kwArray.optString(i)?.trim()
                            if (!p.isNullOrEmpty()) phrases.add(normalizeBasic(p))
                        }
                    }
                    if (phrases.isNotEmpty()) {
                        scamGroups.add(KeywordGroup(name = key, points = 30, phrases = phrases.distinct()))
                    }
                }
            }
        }

        // Support raw HEIMDALL hinglish_indicators object if present
        if (root.has("hinglish_indicators")) {
            val hinglishObj = root.optJSONObject("hinglish_indicators")
            if (hinglishObj != null) {
                val keys = hinglishObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val phrasesArray = hinglishObj.optJSONArray(key) ?: continue
                    val phrases = mutableListOf<String>()
                    for (i in 0 until phrasesArray.length()) {
                        val p = phrasesArray.optString(i)?.trim()
                        if (!p.isNullOrEmpty()) phrases.add(normalizeBasic(p))
                    }
                    if (phrases.isNotEmpty()) {
                        scamGroups.add(KeywordGroup(name = "hinglish_$key", points = 30, phrases = phrases.distinct()))
                    }
                }
            }
        }

        val spamGroups = parseGroups(root.opt("spam"))
        val importantGroups = parseGroups(root.opt("important"))

        if (scamGroups.isEmpty() && spamGroups.isEmpty() && importantGroups.isEmpty()) {
            throw IllegalStateException("JSON contained no valid groups with phrases")
        }

        val ruleSet = RuleSet(
            stripNegatedSharingSentences = stripNegated,
            scamGroups = scamGroups,
            spamGroups = spamGroups,
            importantGroups = importantGroups
        )
        activeRuleSet = ruleSet
        lastLoadError = null
        Log.d(TAG, "Successfully loaded rules: ${ruleSet.scamGroups.size} scam, ${ruleSet.spamGroups.size} spam, ${ruleSet.importantGroups.size} important groups")
        return ruleSet
    }

    /**
     * Requirement 3: Parse keywords.json by its real structure:
     * for scam, spam, and important, iterate each group object that has a "phrases" array
     * and a "points" value. Ignore entries without "phrases" (combination_bonuses, decision, notes).
     */
    private fun parseGroups(categoryObj: Any?): List<KeywordGroup> {
        val list = mutableListOf<KeywordGroup>()
        when (categoryObj) {
            is JSONObject -> {
                val keys = categoryObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val groupVal = categoryObj.opt(key)
                    if (groupVal is JSONObject) {
                        val phrasesArray = groupVal.optJSONArray("phrases")
                        val points = groupVal.optInt("points", -1)
                        if (phrasesArray != null && points >= 0) {
                            val phrases = mutableListOf<String>()
                            for (i in 0 until phrasesArray.length()) {
                                val p = phrasesArray.optString(i)?.trim()
                                if (!p.isNullOrEmpty()) {
                                    phrases.add(normalizeBasic(p))
                                }
                            }
                            if (phrases.isNotEmpty()) {
                                list.add(KeywordGroup(name = key, points = points, phrases = phrases))
                            }
                        }
                    }
                }
            }
            is JSONArray -> {
                for (i in 0 until categoryObj.length()) {
                    val item = categoryObj.optJSONObject(i) ?: continue
                    val phrasesArray = item.optJSONArray("phrases")
                    val points = item.optInt("points", -1)
                    val name = item.optString("name", "group_$i")
                    if (phrasesArray != null && points >= 0) {
                        val phrases = mutableListOf<String>()
                        for (j in 0 until phrasesArray.length()) {
                            val p = phrasesArray.optString(j)?.trim()
                            if (!p.isNullOrEmpty()) {
                                phrases.add(normalizeBasic(p))
                            }
                        }
                        if (phrases.isNotEmpty()) {
                            list.add(KeywordGroup(name = name, points = points, phrases = phrases))
                        }
                    }
                }
            }
        }
        return list
    }

    /**
     * Requirement 4: Normalise text: lowercase, Unicode NFKC, remove zero-width characters, collapse whitespace.
     */
    fun normalizeBasic(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        val nfkc = Normalizer.normalize(lower, Normalizer.Form.NFKC)
        // Remove zero-width characters (\u200B-\u200D, \uFEFF, \u200E-\u200F, \u202A-\u202E)
        val noZeroWidth = nfkc.replace(Regex("[\\u200B-\\u200D\\uFEFF\\u200E\\u200F\\u202A-\\u202E]"), "")
        // Collapse whitespace
        return noZeroWidth.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Requirement 4 (scam scan only):
     * Remove sentences containing the "strip_negated_sharing_sentences" phrases,
     * but if that leaves empty text, use the original text.
     */
    fun normalizeForScam(text: String, stripPhrases: List<String>): String {
        val normalized = normalizeBasic(text)
        if (stripPhrases.isEmpty() || normalized.isEmpty()) return normalized

        // Split into sentences using common sentence terminators . ! ? \n ;
        val rawSentences = normalized.split(Regex("[.!?\\n;]+"))
        val keptSentences = rawSentences.map { it.trim() }.filter { sentence ->
            if (sentence.isEmpty()) return@filter false
            // Check if sentence contains any strip phrase
            val containsStripPhrase = stripPhrases.any { stripPhrase ->
                phraseMatches(sentence, stripPhrase)
            }
            !containsStripPhrase
        }

        val result = keptSentences.joinToString(". ").trim()
        return if (result.isNotEmpty()) result else normalized
    }

    /**
     * Requirement 5: Match phrases safely:
     * escape every phrase before building a regex;
     * use word boundaries only when the phrase is made of letters, digits, and spaces;
     * use plain substring matching for phrases with symbols like %, ₹, or .apk.
     */
    fun phraseMatches(text: String, phrase: String): Boolean {
        if (phrase.isEmpty()) return false
        val isAlphanumericAndSpaces = phrase.all { it.isLetterOrDigit() || it.isWhitespace() }
        return if (isAlphanumericAndSpaces) {
            val escaped = Regex.escape(phrase)
            val regex = Regex("\\b$escaped\\b", RegexOption.IGNORE_CASE)
            regex.containsMatchIn(text)
        } else {
            text.contains(phrase, ignoreCase = true)
        }
    }

    /**
     * Performs classification adhering strictly to all requirements:
     * - Evaluates scam, spam, important rules
     * - Counts each group once
     * - Falls back to Gemini ONLY when rules give Unclear
     * - Never defaults to Safe
     * - Logs and reports errors
     */
    suspend fun analyze(
        rawInput: String,
        context: Context? = null
    ): AnalysisResult = withContext(Dispatchers.Default) {
        val rules = activeRuleSet ?: run {
            if (context != null) {
                try {
                    loadRules(context)
                } catch (e: Exception) {
                    return@withContext errorResult("Couldn't check this message", e.message ?: "Rules not loaded")
                }
            } else {
                return@withContext errorResult("Couldn't check this message", "Rules not loaded and context unavailable")
            }
        }

        if (rawInput.isBlank()) {
            return@withContext errorResult("Please paste a message to analyze", "Empty input")
        }

        try {
            val normalizedGeneral = normalizeBasic(rawInput)
            val normalizedScam = normalizeForScam(rawInput, rules.stripNegatedSharingSentences)

            val matchedGroups = mutableListOf<MatchedGroupInfo>()

            // 1. Evaluate Scam Groups (using normalizedScam)
            var scamScore = 0
            for (group in rules.scamGroups) {
                val matchedPhrases = group.phrases.filter { phraseMatches(normalizedScam, it) }
                if (matchedPhrases.isNotEmpty()) {
                    scamScore += group.points
                    matchedGroups.add(
                        MatchedGroupInfo(
                            category = "scam",
                            groupName = group.name,
                            points = group.points,
                            matchedPhrases = matchedPhrases
                        )
                    )
                }
            }

            // 2. Evaluate Spam Groups (using normalizedGeneral)
            var spamScore = 0
            for (group in rules.spamGroups) {
                val matchedPhrases = group.phrases.filter { phraseMatches(normalizedGeneral, it) }
                if (matchedPhrases.isNotEmpty()) {
                    spamScore += group.points
                    matchedGroups.add(
                        MatchedGroupInfo(
                            category = "spam",
                            groupName = group.name,
                            points = group.points,
                            matchedPhrases = matchedPhrases
                        )
                    )
                }
            }

            // 3. Evaluate Important Groups (using normalizedGeneral)
            var importantScore = 0
            for (group in rules.importantGroups) {
                val matchedPhrases = group.phrases.filter { phraseMatches(normalizedGeneral, it) }
                if (matchedPhrases.isNotEmpty()) {
                    importantScore += group.points
                    matchedGroups.add(
                        MatchedGroupInfo(
                            category = "important",
                            groupName = group.name,
                            points = group.points,
                            matchedPhrases = matchedPhrases
                        )
                    )
                }
            }

            val phrasesPerClass = "Scam: ${rules.totalScamPhrases} phrases (${rules.scamGroups.size} groups) | " +
                    "Spam: ${rules.totalSpamPhrases} phrases (${rules.spamGroups.size} groups) | " +
                    "Important: ${rules.totalImportantPhrases} phrases (${rules.importantGroups.size} groups)"

            // Decision Logic
            var category = "Unclear"
            var decisionPath = "rules"

            if (scamScore >= 50 && scamScore > importantScore) {
                val isHigh = matchedGroups.any {
                    it.category == "scam" && (
                        it.groupName.contains("kyc") ||
                        it.groupName.contains("blocked") ||
                        it.groupName.contains("phishing") ||
                        it.groupName.contains("arrest") ||
                        it.groupName.contains("apk")
                    )
                } || scamScore >= 70
                category = if (isHigh) "Scam (High)" else "Scam"
                decisionPath = "rules"
            } else if (scamScore >= 20 && scamScore > importantScore && scamScore > spamScore) {
                category = "Scam"
                decisionPath = "rules"
            } else if (importantScore >= 20 && importantScore > scamScore && importantScore > spamScore) {
                category = "Important"
                decisionPath = "rules"
            } else if (spamScore >= 20 && spamScore > scamScore && spamScore > importantScore) {
                category = "Spam"
                decisionPath = "rules"
            } else {
                category = "Unclear"
                decisionPath = "rules"
            }

            // Requirement 8: If Gemini is used, call it ONLY when the rules give Unclear.
            // Accept only these outputs: scam, spam, important, unclear. Any other output, empty output, or API error counts as Unclear, never Safe.
            var caughtError: String? = null
            if (category == "Unclear") {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (!apiKey.isNullOrBlank() && apiKey != "YOUR_GEMINI_API_KEY" && !apiKey.startsWith("YOUR_")) {
                    try {
                        val aiDecision = queryGemini(normalizedGeneral, apiKey)
                        if (aiDecision != null) {
                            category = when (aiDecision) {
                                "scam" -> "Scam"
                                "spam" -> "Spam"
                                "important" -> "Important"
                                "unclear" -> "Unclear"
                                else -> "Unclear"
                            }
                            decisionPath = "Gemini"
                        } else {
                            category = "Unclear"
                            decisionPath = "rules"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Gemini query error: ${e.message}", e)
                        caughtError = "Gemini error: ${e.message}"
                        category = "Unclear"
                        decisionPath = "rules"
                    }
                }
            }

            AnalysisResult(
                category = category,
                decisionPath = decisionPath,
                normalizedInput = normalizedGeneral,
                normalizedScamInput = normalizedScam,
                scamScore = scamScore,
                spamScore = spamScore,
                importantScore = importantScore,
                matchedGroups = matchedGroups,
                phrasesPerClass = phrasesPerClass,
                error = caughtError
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating rules", e)
            errorResult("Couldn't check this message", e.message ?: e.toString())
        }
    }

    private suspend fun queryGemini(text: String, apiKey: String): String? = withContext(Dispatchers.IO) {
        val prompt = "Classify this message into exactly ONE of these four categories: scam, spam, important, unclear.\n" +
                "Respond with only the single category word in lowercase (scam, spam, important, or unclear).\n\n" +
                "Message:\n$text"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.0)
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$GEMINI_URL?key=$apiKey")
            .post(requestBody)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) {
            Log.e(TAG, "Gemini HTTP response unsuccessful: ${response.code}")
            return@withContext "unclear"
        }

        val body = response.body?.string() ?: return@withContext "unclear"
        val jsonRoot = JSONObject(body)
        val candidate = jsonRoot.optJSONArray("candidates")?.optJSONObject(0)
        val content = candidate?.optJSONObject("content")
        val answer = content?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")?.trim()?.lowercase(Locale.ROOT)

        when (answer) {
            "scam" -> "scam"
            "spam" -> "spam"
            "important" -> "important"
            "unclear" -> "unclear"
            else -> "unclear"
        }
    }

    private fun errorResult(userMessage: String, internalError: String): AnalysisResult {
        return AnalysisResult(
            category = "$userMessage: $internalError",
            decisionPath = "error",
            normalizedInput = "",
            normalizedScamInput = "",
            scamScore = 0,
            spamScore = 0,
            importantScore = 0,
            matchedGroups = emptyList(),
            phrasesPerClass = activeRuleSet?.let {
                "Scam: ${it.totalScamPhrases} | Spam: ${it.totalSpamPhrases} | Important: ${it.totalImportantPhrases}"
            } ?: "Rules not loaded",
            error = internalError
        )
    }
}
