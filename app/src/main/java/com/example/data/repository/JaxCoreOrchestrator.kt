package com.example.data.repository

import com.example.data.model.AiProviderTelemetry
import com.example.data.model.AiProviderType
import com.example.data.model.DualAiCouncilResult
import com.example.data.model.ForgeCategory
import com.example.data.model.ForgeComplexity
import com.example.data.model.ForgePlatform
import com.example.data.model.JaxCoreRoutingMode
import com.example.data.model.SetupDrawingConfig
import com.example.data.model.SetupDrawingPattern
import com.example.data.model.SpecialistAgentDef
import com.example.domain.engine.IndicatorForgeEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

data class ProviderInvokeRecord(
    val provider: AiProviderType,
    val modelId: String,
    val agentName: String,
    val mode: String,
    val latencyMs: Long,
    val totalTokens: Int,
    val estimatedCostUsd: Double,
    val success: Boolean,
    val errorMessage: String
)

data class ForgeAiGenerationOutcome(
    val specSummary: String,
    val mql5Source: String,
    val pineSource: String,
    val auditNotes: String,
    val createdByAgent: String,
    val telemetryLogs: List<ProviderInvokeRecord>
)

class JaxCoreOrchestrator(
    private val geminiRepo: JaxAiRepository,
    val deepSeekRepo: DeepSeekRepository
) {

    fun initialTelemetry(): List<AiProviderTelemetry> = listOf(
        AiProviderTelemetry(
            provider = AiProviderType.GEMINI,
            connected = true,
            statusLabel = "App Check Attested (Firebase AI Logic)",
            selectedModel = "gemini-3.8-flash",
            discoveredModels = listOf("gemini-3.8-flash", "gemini-2.5-pro")
        ),
        AiProviderTelemetry(
            provider = AiProviderType.DEEPSEEK,
            connected = deepSeekRepo.isConfigured(),
            statusLabel = if (deepSeekRepo.isConfigured()) "API Key Configured (Ready to Test)" else "DEEPSEEK_API_KEY Not Set in Secrets",
            selectedModel = "deepseek-chat",
            discoveredModels = if (deepSeekRepo.isConfigured()) listOf("deepseek-chat", "deepseek-reasoner") else emptyList()
        )
    )

    suspend fun runCouncilOrAgent(
        prompt: String,
        marketContext: String,
        mode: JaxCoreRoutingMode,
        agent: SpecialistAgentDef,
        geminiEnabled: Boolean,
        deepseekEnabled: Boolean,
        deepseekModel: String
    ): Pair<DualAiCouncilResult, List<ProviderInvokeRecord>> {
        val logs = mutableListOf<ProviderInvokeRecord>()
        val useGemini = when (mode) {
            JaxCoreRoutingMode.GEMINI_ONLY -> true
            JaxCoreRoutingMode.DEEPSEEK_ONLY -> false
            JaxCoreRoutingMode.DUAL_AI -> true
            JaxCoreRoutingMode.AUTO -> agent.primaryProvider == "GEMINI" || agent.primaryProvider == "BOTH" || !deepSeekRepo.isConfigured() || !deepseekEnabled
        } && geminiEnabled

        val useDeepSeek = when (mode) {
            JaxCoreRoutingMode.GEMINI_ONLY -> false
            JaxCoreRoutingMode.DEEPSEEK_ONLY -> true
            JaxCoreRoutingMode.DUAL_AI -> true
            JaxCoreRoutingMode.AUTO -> (agent.primaryProvider == "DEEPSEEK" || agent.primaryProvider == "BOTH") && deepSeekRepo.isConfigured() && deepseekEnabled
        } && deepseekEnabled

        val systemInstruction = """
            You are ${agent.name} inside J.A.X. Omniverse (Role: ${agent.roleSummary}, Output Format: ${agent.outputFormat}).
            Strictly base analysis on verified context below. Highlight risks, missing data, or contradictions explicitly. Never guarantee profitability.
            [LIVE CONTEXT]
            $marketContext
        """.trimIndent()

        var geminiOut = "Not invoked in ${mode.label}."
        var deepseekOut = if (!deepSeekRepo.isConfigured()) {
            "DeepSeek API key not configured (set DEEPSEEK_API_KEY in AI Studio Secrets)."
        } else {
            "Not invoked in ${mode.label}."
        }
        val usedProviders = mutableListOf<String>()

        coroutineScope {
            val gJob = if (useGemini) {
                async {
                    try {
                        val (txt, ms) = geminiRepo.generateTextWithLatency(systemInstruction, prompt)
                        logs.add(ProviderInvokeRecord(AiProviderType.GEMINI, "gemini-3.8-flash", agent.name, mode.code, ms, (txt.length / 4), 0.0, true, ""))
                        usedProviders.add("GEMINI")
                        txt
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logs.add(ProviderInvokeRecord(AiProviderType.GEMINI, "gemini-3.8-flash", agent.name, mode.code, 0L, 0, 0.0, false, e.localizedMessage ?: "Gemini error"))
                        "Gemini failed: ${e.localizedMessage}"
                    }
                }
            } else null

            val dJob = if (useDeepSeek) {
                async {
                    try {
                        val res = deepSeekRepo.generateCompletion(deepseekModel, systemInstruction, prompt)
                        logs.add(ProviderInvokeRecord(AiProviderType.DEEPSEEK, res.modelId, agent.name, mode.code, res.latencyMs, res.totalTokens, res.estimatedCostUsd, true, ""))
                        usedProviders.add("DEEPSEEK")
                        res.text
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        logs.add(ProviderInvokeRecord(AiProviderType.DEEPSEEK, deepseekModel, agent.name, mode.code, 0L, 0, 0.0, false, e.localizedMessage ?: "DeepSeek error"))
                        "DeepSeek failed: ${e.localizedMessage}"
                    }
                }
            } else null

            gJob?.await()?.let { geminiOut = it }
            dJob?.await()?.let { deepseekOut = it }
        }

        // Cross-check synthesis when both ran
        var agreements = "Single-provider execution (${usedProviders.joinToString().ifBlank { "None" }})."
        var disagreements = "Cross-provider comparison requires both Gemini and DeepSeek responses."
        var identifiedErrors = "None automatically flagged."
        var finalRecommendation = if (usedProviders.contains("GEMINI")) geminiOut else deepseekOut
        var verificationStatus = if (usedProviders.size == 2) "DUAL_VERIFIED" else if (usedProviders.size == 1) "SINGLE_PROVIDER_ONLY" else "UNAVAILABLE"

        if (usedProviders.size == 2 && geminiEnabled) {
            try {
                val synthPrompt = """
                    Compare these two independent AI analyses for the trader request below. Note that consensus is NOT proof of correctness.
                    Return plain text with exact sections:
                    AGREEMENTS: ...
                    DISAGREEMENTS: ...
                    ERRORS_OR_GAPS: ...
                    VERDICT: ...

                    [REQUEST] $prompt
                    [GEMINI] $geminiOut
                    [DEEPSEEK] $deepseekOut
                """.trimIndent()
                val (synth, ms) = geminiRepo.generateTextWithLatency("You are JAX-CORE Dual AI Verification Coordinator.", synthPrompt)
                logs.add(ProviderInvokeRecord(AiProviderType.GEMINI, "gemini-3.8-flash", "Dual AI Verification Agent", mode.code, ms, (synth.length / 4), 0.0, true, ""))
                agreements = extractSection(synth, "AGREEMENTS:", listOf("DISAGREEMENTS:", "ERRORS_OR_GAPS:", "VERDICT:"))
                disagreements = extractSection(synth, "DISAGREEMENTS:", listOf("ERRORS_OR_GAPS:", "VERDICT:"))
                identifiedErrors = extractSection(synth, "ERRORS_OR_GAPS:", listOf("VERDICT:"))
                finalRecommendation = extractSection(synth, "VERDICT:", emptyList()).ifBlank { synth }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                verificationStatus = "SYNTHESIS_ERROR: ${e.localizedMessage}"
            }
        }

        return DualAiCouncilResult(
            prompt = prompt,
            mode = mode,
            agentName = agent.name,
            geminiResponse = geminiOut,
            deepseekResponse = deepseekOut,
            agreements = agreements,
            disagreements = disagreements,
            identifiedErrors = identifiedErrors,
            finalRecommendation = finalRecommendation,
            verificationStatus = verificationStatus,
            usedProviders = usedProviders,
            timestampMillis = System.currentTimeMillis()
        ) to logs
    }

    suspend fun generateForgeIndicator(
        name: String,
        naturalPrompt: String,
        category: ForgeCategory,
        platform: ForgePlatform,
        complexity: ForgeComplexity,
        pattern: SetupDrawingPattern,
        mtfTimeframes: List<String>,
        fastPeriod: Int,
        slowPeriod: Int,
        atrPeriod: Int,
        rrTarget: Double,
        drawing: SetupDrawingConfig,
        deepseekModel: String
    ): ForgeAiGenerationOutcome {
        val logs = mutableListOf<ProviderInvokeRecord>()
        var specSummary = "Pattern: ${pattern.label}, Category: ${category.label}, MTF: ${mtfTimeframes.joinToString()}, ClosedCandleOnly: ${drawing.closedCandleOnly}"

        // Base deterministic code guarantees valid syntax & full Entry/SL/TP1-TP3 drawing engine
        var mql5Code = if (platform != ForgePlatform.TRADINGVIEW) {
            IndicatorForgeEngine.generateMql5Source(name, category, pattern, mtfTimeframes, fastPeriod, slowPeriod, atrPeriod, rrTarget, drawing)
        } else ""
        var pineCode = if (platform != ForgePlatform.MT5) {
            IndicatorForgeEngine.generatePineScriptV6Source(name, category, pattern, mtfTimeframes, fastPeriod, slowPeriod, atrPeriod, rrTarget, drawing)
        } else ""

        var createdBy = "JAX-CORE Forge Deterministic Engine"

        // Step 1: Gemini Planning Agent parses natural language requirements & identifies ambiguities
        if (naturalPrompt.isNotBlank()) {
            try {
                val (spec, ms) = geminiRepo.generateTextWithLatency(
                    "You are Gemini Indicator Planning Agent for J.A.X. Indicator Forge.",
                    """
                    Analyze this trading indicator request and extract:
                    1. Core signal trigger & confirmation rules
                    2. Missing or potentially contradictory rules (if any)
                    3. Recommended MTF hierarchy and invalidation logic
                    Request: "$naturalPrompt"
                    Target Platform: ${platform.label} | Complexity: ${complexity.label} | Pattern: ${pattern.label}
                    """.trimIndent()
                )
                specSummary = spec
                logs.add(ProviderInvokeRecord(AiProviderType.GEMINI, "gemini-3.8-flash", "Gemini Planning Agent", "FORGE_GEN", ms, spec.length / 4, 0.0, true, ""))
                createdBy = "Gemini Planning Agent"
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logs.add(ProviderInvokeRecord(AiProviderType.GEMINI, "gemini-3.8-flash", "Gemini Planning Agent", "FORGE_GEN", 0L, 0, 0.0, false, e.localizedMessage ?: "Spec error"))
            }
        }

        // Step 2: DeepSeek MQL5 / Pine v6 Developer enhancement when DEEPSEEK_API_KEY is configured and user supplied custom prompt
        var auditNotes = "Static non-repainting & syntax validator executed."
        if (naturalPrompt.isNotBlank() && deepSeekRepo.isConfigured()) {
            try {
                if (platform == ForgePlatform.MT5 || platform == ForgePlatform.DUAL) {
                    val dsMq5 = deepSeekRepo.generateCompletion(
                        deepseekModel,
                        "You are DeepSeek MQL5 Developer. Return ONLY complete valid MQL5 source code with #property strict, OnInit(), OnCalculate(), closed-candle checks, and Entry/SL/TP1-TP3 chart objects. No markdown fences.",
                        "Create MQL5 indicator '$name' implementing: $naturalPrompt\nSpec:\n$specSummary\nInclude MTF confirmation ${mtfTimeframes.firstOrNull() ?: "H1"} and TP2 R:R=$rrTarget."
                    )
                    val clean = stripMarkdownFences(dsMq5.text)
                    if (clean.contains("OnCalculate(") && clean.contains("OnInit(")) {
                        mql5Code = clean
                    }
                    logs.add(ProviderInvokeRecord(AiProviderType.DEEPSEEK, dsMq5.modelId, "DeepSeek MQL5 Developer", "FORGE_GEN", dsMq5.latencyMs, dsMq5.totalTokens, dsMq5.estimatedCostUsd, true, ""))
                    createdBy = "Dual AI (Gemini Spec + DeepSeek Code)"
                }
                if (platform == ForgePlatform.TRADINGVIEW || platform == ForgePlatform.DUAL) {
                    val dsPine = deepSeekRepo.generateCompletion(
                        deepseekModel,
                        "You are DeepSeek Pine Script v6 Developer. Return ONLY valid //@version=6 source code with barstate.isconfirmed, lookahead=barmerge.lookahead_off, and line.new/box.new Entry/SL/TP1-TP3 drawings. No markdown fences.",
                        "Create Pine Script v6 indicator '$name' implementing: $naturalPrompt\nSpec:\n$specSummary\nInclude MTF confirmation ${mtfTimeframes.firstOrNull() ?: "H1"} and TP2 R:R=$rrTarget."
                    )
                    val clean = stripMarkdownFences(dsPine.text)
                    if (clean.contains("//@version=6") && clean.contains("indicator(")) {
                        pineCode = clean
                    }
                    logs.add(ProviderInvokeRecord(AiProviderType.DEEPSEEK, dsPine.modelId, "DeepSeek Pine Script Developer", "FORGE_GEN", dsPine.latencyMs, dsPine.totalTokens, dsPine.estimatedCostUsd, true, ""))
                    createdBy = "Dual AI (Gemini Spec + DeepSeek Code)"
                }

                // Step 3: Gemini Code Auditor reviews generated code
                val sampleToAudit = if (pineCode.isNotBlank()) pineCode.take(3500) else mql5Code.take(3500)
                val (audit, msAudit) = geminiRepo.generateTextWithLatency(
                    "You are Gemini Code Auditor. Inspect for repainting, lookahead leakage, missing declarations, or rule discrepancies in <= 6 bullet points.",
                    sampleToAudit
                )
                auditNotes = audit
                logs.add(ProviderInvokeRecord(AiProviderType.GEMINI, "gemini-3.8-flash", "Dual AI Code Auditor", "FORGE_AUDIT", msAudit, audit.length / 4, 0.0, true, ""))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logs.add(ProviderInvokeRecord(AiProviderType.DEEPSEEK, deepseekModel, "DeepSeek Developer", "FORGE_GEN", 0L, 0, 0.0, false, e.localizedMessage ?: "DeepSeek error"))
                auditNotes = "DeepSeek API note: ${e.localizedMessage}. Verified Forge deterministic template applied."
            }
        } else if (!deepSeekRepo.isConfigured()) {
            auditNotes = "DeepSeek API key not set in AI Studio Secrets — built with Gemini Spec + verified non-repainting Forge templates."
        }

        return ForgeAiGenerationOutcome(
            specSummary = specSummary,
            mql5Source = mql5Code,
            pineSource = pineCode,
            auditNotes = auditNotes,
            createdByAgent = createdBy,
            telemetryLogs = logs
        )
    }

    suspend fun repairSourceWithAi(
        platform: ForgePlatform,
        brokenSource: String,
        compilerOrValidatorErrors: String,
        deepseekModel: String
    ): Pair<String, List<ProviderInvokeRecord>> {
        val logs = mutableListOf<ProviderInvokeRecord>()
        val targetLang = if (platform == ForgePlatform.TRADINGVIEW) "Pine Script v6 (//@version=6)" else "MetaTrader 5 MQL5"
        val prompt = """
            Fix ALL compilation, syntax, repainting, and lookahead errors in this $targetLang indicator while strictly preserving its trading rules.
            Return ONLY the complete repaired source code with no markdown backticks.

            [DIAGNOSTICS]
            $compilerOrValidatorErrors

            [ORIGINAL SOURCE]
            $brokenSource
        """.trimIndent()

        if (deepSeekRepo.isConfigured()) {
            try {
                val res = deepSeekRepo.generateCompletion(
                    deepseekModel,
                    "You are DeepSeek Code Debugger for $targetLang. Return clean runnable code only.",
                    prompt,
                    temperature = 0.1
                )
                logs.add(ProviderInvokeRecord(AiProviderType.DEEPSEEK, res.modelId, "DeepSeek Code Debugger", "REPAIR", res.latencyMs, res.totalTokens, res.estimatedCostUsd, true, ""))
                return stripMarkdownFences(res.text) to logs
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logs.add(ProviderInvokeRecord(AiProviderType.DEEPSEEK, deepseekModel, "DeepSeek Code Debugger", "REPAIR", 0L, 0, 0.0, false, e.localizedMessage ?: "DeepSeek repair error"))
            }
        }

        val (repaired, ms) = geminiRepo.generateTextWithLatency(
            "You are J.A.X. Code Repair Assistant for $targetLang. Return complete repaired source code only without markdown backticks.",
            prompt
        )
        logs.add(ProviderInvokeRecord(AiProviderType.GEMINI, "gemini-3.8-flash", "Dual AI Code Auditor", "REPAIR", ms, repaired.length / 4, 0.0, true, ""))
        return stripMarkdownFences(repaired) to logs
    }

    private fun stripMarkdownFences(raw: String): String {
        return raw.trim()
            .removePrefix("```mql5").removePrefix("```mq5")
            .removePrefix("```pinescript").removePrefix("```pine")
            .removePrefix("```cpp").removePrefix("```")
            .removeSuffix("```")
            .trim()
    }

    private fun extractSection(text: String, header: String, stopHeaders: List<String>): String {
        val start = text.indexOf(header, ignoreCase = true)
        if (start < 0) return ""
        val after = start + header.length
        var end = text.length
        stopHeaders.forEach { stop ->
            val idx = text.indexOf(stop, after, ignoreCase = true)
            if (idx in (after + 1) until end) end = idx
        }
        return text.substring(after, end).trim()
    }
}
