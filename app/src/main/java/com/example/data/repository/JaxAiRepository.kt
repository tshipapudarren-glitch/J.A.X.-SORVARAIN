package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class VisionChartReport(
    val bias: String,
    val marketStructure: String,
    val keyLevels: List<String>,
    val setupProposal: String,
    val riskWarning: String,
    val confidenceNote: String
)

class JaxAiRepository {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.35f
            }
        )
    }

    private val visionSchema by lazy {
        Schema.obj(
            properties = mapOf(
                "bias" to Schema.string("Overall directional bias observed on the chart (BULLISH, BEARISH, or NEUTRAL)"),
                "marketStructure" to Schema.string("Summary of BOS, CHoCH, trend, order blocks, or FVG structures visible"),
                "keyLevels" to Schema.array(
                    items = Schema.string(),
                    description = "Important visible price levels, liquidity pools, or support/resistance zones"
                ),
                "setupProposal" to Schema.string("Actionable trading plan or wait condition with explicit invalidation level"),
                "riskWarning" to Schema.string("Uncertainty, conflicting signals, or missing timeframe/volume caveats"),
                "confidenceNote" to Schema.string("Transparent confidence assessment without guaranteeing future price action")
            )
        )
    }

    private val visionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = visionSchema
                temperature = 0.25f
            }
        )
    }

    fun streamCopilotResponse(
        userQuestion: String,
        systemContextSnapshot: String
    ): Flow<String> = flow {
        val prompt = """
            You are J.A.X. Trading Copilot, the institutional quantitative research assistant inside J.A.X. Omniverse.
            Rules:
            - Ground every answer strictly in the LIVE TELEMETRY & INDICATOR CONTEXT below.
            - Never invent prices, fake win rates, or guarantee future profits.
            - If data for a requested instrument or timeframe is unavailable or disconnected, state that clearly.
            - Be concise, structured, and analytical.

            [LIVE TELEMETRY & INDICATOR CONTEXT]
            $systemContextSnapshot

            [TRADER QUESTION]
            $userQuestion
        """.trimIndent()

        val sb = StringBuilder()
        try {
            withTimeout(60_000L) {
                chatModel.generateContentStream(prompt).collect { chunk ->
                    val part = chunk.text.orEmpty()
                    if (part.isNotEmpty()) {
                        sb.append(part)
                        emit(sb.toString())
                    }
                }
            }
            if (sb.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Copilot stream timed out", e)
            throw IllegalStateException("AI request timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Copilot stream failed", e)
            throw e
        }
    }

    suspend fun analyzeChartScreenshot(
        bitmap: Bitmap,
        symbol: String,
        timeframeCode: String,
        liveContext: String
    ): VisionChartReport {
        try {
            val response = withTimeout(60_000L) {
                visionModel.generateContent(
                    content {
                        image(bitmap)
                        text(
                            """
                            Analyze this trading chart screenshot for $symbol ($timeframeCode) using institutional Smart Money, Liquidity, Market Structure, and Risk Management principles.
                            Supplement only with verified context below where relevant, and clearly highlight uncertainty or invalidation conditions.
                            Context:
                            $liveContext
                            """.trimIndent()
                        )
                    }
                )
            }
            val rawJson = response.text ?: throw IllegalStateException("Empty vision response from AI.")
            val obj = JSONObject(rawJson)
            val levelsArr = obj.optJSONArray("keyLevels")
            val levels = if (levelsArr != null) {
                (0 until levelsArr.length()).map { levelsArr.getString(it) }
            } else emptyList()

            return VisionChartReport(
                bias = obj.optString("bias", "NEUTRAL"),
                marketStructure = obj.optString("marketStructure", "Unable to parse structure."),
                keyLevels = levels,
                setupProposal = obj.optString("setupProposal", "Await confirmation."),
                riskWarning = obj.optString("riskWarning", "Past patterns do not guarantee future results."),
                confidenceNote = obj.optString("confidenceNote", "Visual estimate only.")
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Vision chart analysis timed out", e)
            throw IllegalStateException("Chart vision analysis timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Vision chart analysis failed", e)
            throw e
        }
    }

    suspend fun generateTextWithLatency(
        systemInstruction: String,
        userPrompt: String
    ): Pair<String, Long> {
        val started = System.currentTimeMillis()
        try {
            val combined = """
                [SYSTEM ROLE]
                $systemInstruction

                [TASK]
                $userPrompt
            """.trimIndent()
            val response = withTimeout(60_000L) {
                chatModel.generateContent(combined)
            }
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                throw IllegalStateException("Gemini returned an empty response.")
            }
            return text to (System.currentTimeMillis() - started)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini generation timed out", e)
            throw IllegalStateException("Gemini request timed out after 60 seconds.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed", e)
            throw e
        }
    }

    private companion object {
        const val TAG = "JaxAiRepository"
    }
}
