package com.example.data.remote

import com.example.BuildConfig
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

data class ToneSuggestionResult(
    val suggestedText: String,
    val toneNotes: String,
    val usedLiveGemini: Boolean
)

class CalmToneAssistantService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun rewriteForCalmCoParenting(draftMessage: String, childName: String): ToneSuggestionResult =
        withContext(Dispatchers.IO) {
            val cleanDraft = draftMessage.trim()
            if (cleanDraft.isEmpty()) {
                return@withContext ToneSuggestionResult(
                    suggestedText = "Hi, could we please confirm the upcoming custody schedule details for $childName? Thank you.",
                    toneNotes = "Brief, child-focused starter prompt.",
                    usedLiveGemini = false
                )
            }

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext localBiffRewrite(cleanDraft, childName)
            }

            val systemPrompt = """
                You are a family-mediation communication assistant for CustodyLog.
                Rewrite the user's draft message to their co-parent using the BIFF method (Brief, Informative, Friendly, Firm).
                Rules:
                1. Keep the tone calm, neutral, respectful, and strictly focused on the child ($childName) and logistics.
                2. Remove blame, sarcasm, emotional accusations, or legal threats.
                3. Preserve all factual dates, times, locations, and questions.
                4. Return ONLY the rewritten message text, ready for the parent to review before sending.
            """.trimIndent()

            val payload = JSONObject().apply {
                put(
                    "systemInstruction",
                    JSONObject().apply {
                        put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", systemPrompt))
                        )
                    }
                )
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().apply {
                            put(
                                "parts",
                                JSONArray().put(
                                    JSONObject().put("text", "Draft message to rewrite:\n$cleanDraft")
                                )
                            )
                        }
                    )
                )
                put(
                    "generationConfig",
                    JSONObject().apply {
                        put("temperature", 0.3)
                    }
                )
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val request = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val bodyStr = response.body?.string().orEmpty()
                    if (!response.isSuccessful || bodyStr.isBlank()) {
                        return@withContext localBiffRewrite(cleanDraft, childName)
                    }
                    val json = JSONObject(bodyStr)
                    val text = json.optJSONArray("candidates")
                        ?.optJSONObject(0)
                        ?.optJSONObject("content")
                        ?.optJSONArray("parts")
                        ?.optJSONObject(0)
                        ?.optString("text")
                        ?.trim()
                    if (!text.isNullOrBlank()) {
                        ToneSuggestionResult(
                            suggestedText = text,
                            toneNotes = "Rewritten with Gemini 3.5 Flash using neutral, child-focused BIFF phrasing. Review before sending.",
                            usedLiveGemini = true
                        )
                    } else {
                        localBiffRewrite(cleanDraft, childName)
                    }
                }
            } catch (e: Exception) {
                localBiffRewrite(cleanDraft, childName)
            }
        }

    private fun localBiffRewrite(draft: String, childName: String): ToneSuggestionResult {
        var cleaned = draft
            .replace(Regex("(?i)\\byou always\\b"), "in recent instances")
            .replace(Regex("(?i)\\byou never\\b"), "it has been difficult when")
            .replace(Regex("(?i)\\byou forgot\\b"), "please note")
            .replace(Regex("(?i)\\bunacceptable\\b"), "challenging for $childName's schedule")
            .replace(Regex("(?i)\\bridiculous\\b"), "concerning")
            .replace(Regex("!+"), ".")
            .trim()

        if (!cleaned.endsWith(".") && !cleaned.endsWith("?")) {
            cleaned = "$cleaned."
        }
        val biffMessage = "Hi — regarding $childName: $cleaned Please let me know what works best so we can keep the schedule consistent. Thank you."
        return ToneSuggestionResult(
            suggestedText = biffMessage,
            toneNotes = "Neutral BIFF (Brief, Informative, Friendly, Firm) suggestion. Configure GEMINI_API_KEY in AI Studio Secrets for cloud AI phrasing.",
            usedLiveGemini = false
        )
    }
}
