package com.example.network

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object GeminiRepository {
    private const val TAG = "GeminiRepository"

    // Helper to get effective API key (either build config or user override)
    fun getEffectiveApiKey(userKey: String?): String {
        val buildKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Throwable) {
            ""
        }
        return if (!userKey.isNullOrBlank()) userKey else buildKey
    }

    suspend fun checkGrammar(text: String, language: String, userApiKey: String?): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(userApiKey)
        if (apiKey.isBlank()) return@withContext "API Key is missing! Please configure it in Settings."

        val prompt = """
            You are an expert editor. Please proofread the following PowerPoint presentation content for grammar errors.
            The selected target language is: $language.
            
            Please perform the analysis in $language:
            1. List the grammar errors found (with slide number if referenced).
            2. Suggest corrections.
            3. Provide a clean, corrected version of the text.
            
            Keep your response concise, professional, and formatted in clean Markdown.
            
            Content:
            $text
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response generated for grammar check."
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkGrammar", e)
            "Error checking grammar: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun checkSpelling(text: String, language: String, userApiKey: String?): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(userApiKey)
        if (apiKey.isBlank()) return@withContext "API Key is missing! Please configure it in Settings."

        val prompt = """
            You are an expert copyeditor. Please inspect the following PowerPoint presentation content for any spelling errors.
            The selected target language is: $language.
            
            Please perform the analysis in $language:
            1. List the misspelled words and the slides they appear on (if applicable).
            2. Suggest the correct spellings.
            3. If no spelling errors are found, clearly state that the document's spelling is perfect.
            
            Keep your response concise, professional, and formatted in clean Markdown.
            
            Content:
            $text
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response generated for spelling check."
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkSpelling", e)
            "Error checking spelling: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    suspend fun summarize(text: String, language: String, userApiKey: String?): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(userApiKey)
        if (apiKey.isBlank()) return@withContext "API Key is missing! Please configure it in Settings."

        val prompt = """
            Provide a highly professional, cohesive, and concise executive summary of the following PowerPoint presentation.
            The selected target language is: $language.
            
            Please perform the analysis and summary in $language:
            1. Highlight the core objective of the deck.
            2. Provide 3-5 high-impact bulleted key takeaways grouped by themes or slide outline.
            3. Use elegant professional headings and rich Markdown formatting.
            
            Content:
            $text
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response generated for summary."
        } catch (e: Exception) {
            Log.e(TAG, "Error in summarize", e)
            "Error generating summary: ${e.localizedMessage ?: "Unknown error"}"
        }
    }

    data class PlagiarismResult(
        val percentage: Int,
        val analysis: String
    )

    suspend fun checkPlagiarism(text: String, userApiKey: String?): PlagiarismResult = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(userApiKey)
        if (apiKey.isBlank()) {
            return@withContext PlagiarismResult(0, "API Key is missing! Please configure it in Settings.")
        }

        val prompt = """
            You are an academic integrity and AI writing detection expert. Analyze the following PowerPoint presentation content.
            Estimate the percentage likelihood that this content is AI-generated (0 to 100) or plagiarized.
            
            Respond ONLY with a valid JSON object in this exact format (do not include any markdown code blocks, do not include any other text, just raw JSON):
            {
              "percentage": 82,
              "analysis": "A brief 2-3 sentence expert explanation of known AI-generated patterns, repetition, and stylistic markers found in this specific text."
            }
            
            Content:
            $text
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        try {
            val response = RetrofitClient.geminiService.generateContent(apiKey, request)
            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            
            // Try parsing the response
            val cleanedJson = cleanJsonResponse(rawText)
            try {
                val jsonObject = JSONObject(cleanedJson)
                val percentage = jsonObject.getInt("percentage")
                val analysis = jsonObject.getString("analysis")
                PlagiarismResult(percentage, analysis)
            } catch (jsonEx: Exception) {
                Log.e(TAG, "JSON parsing error on: $cleanedJson", jsonEx)
                // Fallback: extract percentage using regex and use raw text as analysis
                val match = Regex("""\d+""").find(rawText)
                val percentage = match?.value?.toIntOrNull() ?: 50
                PlagiarismResult(percentage, rawText.take(300))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in checkPlagiarism", e)
            PlagiarismResult(0, "Error checking plagiarism: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun cleanJsonResponse(rawText: String): String {
        var text = rawText.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json")
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```")
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```")
        }
        return text.trim()
    }
}
