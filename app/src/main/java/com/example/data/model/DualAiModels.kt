package com.example.data.model

enum class AiProviderType(val label: String) {
    GEMINI("Google Gemini (Firebase AI Logic)"),
    DEEPSEEK("DeepSeek API")
}

enum class JaxCoreRoutingMode(val code: String, val label: String) {
    AUTO("AUTO", "Auto Router"),
    GEMINI_ONLY("GEMINI_ONLY", "Gemini Mode"),
    DEEPSEEK_ONLY("DEEPSEEK_ONLY", "DeepSeek Mode"),
    DUAL_AI("DUAL_AI", "Dual AI Arena")
}

data class SpecialistAgentDef(
    val id: String,
    val name: String,
    val primaryProvider: String,
    val roleSummary: String,
    val outputFormat: String
)

object JaxSpecialistAgents {
    val all12: List<SpecialistAgentDef> = listOf(
        SpecialistAgentDef("agent_01", "Gemini Chart Analyst", "GEMINI", "Visual chart structure, S/R, sweeps, FVG & OB interpretation", "Structured Vision JSON"),
        SpecialistAgentDef("agent_02", "Gemini Research Agent", "GEMINI", "Multi-factor market thesis, macro catalysts & concept research", "Analytical Brief"),
        SpecialistAgentDef("agent_03", "Gemini Planning Agent", "GEMINI", "Decomposes natural-language indicator specs & missing rules", "Structured Rule Spec"),
        SpecialistAgentDef("agent_04", "Gemini Documentation Agent", "GEMINI", "Generates trader guides, release notes & input docs", "Trader Documentation"),
        SpecialistAgentDef("agent_05", "DeepSeek MQL5 Developer", "DEEPSEEK", "Generates strict MQL5 indicators with OnCalculate & chart objects", ".mq5 Source Code"),
        SpecialistAgentDef("agent_06", "DeepSeek Pine Script Developer", "DEEPSEEK", "Generates Pine Script v6 indicators with non-repainting MTF", ".pine v6 Source Code"),
        SpecialistAgentDef("agent_07", "DeepSeek Quantitative Analyst", "DEEPSEEK", "Audits expectancy, Sharpe, R:R math & signal conditions", "Quant Breakdown"),
        SpecialistAgentDef("agent_08", "DeepSeek Code Debugger", "DEEPSEEK", "Repairs syntax, buffer, type & look-ahead compiler errors", "Repaired Source + Diff"),
        SpecialistAgentDef("agent_09", "Dual AI Code Auditor", "BOTH", "Gemini & DeepSeek cross-audit code for bugs and repainting", "Dual Audit Matrix"),
        SpecialistAgentDef("agent_10", "Dual AI Strategy Investigator", "BOTH", "Debates setup confluence, edge decay & invalidation conditions", "Council Debate"),
        SpecialistAgentDef("agent_11", "Dual AI Indicator Architect", "BOTH", "Gemini designs specification; DeepSeek synthesizes dual-platform code", "Dual Platform Package"),
        SpecialistAgentDef("agent_12", "Dual AI Verification Agent", "BOTH", "Independent verification checking math, assumptions & parity", "Verification Verdict")
    )
}

data class AiProviderTelemetry(
    val provider: AiProviderType,
    val connected: Boolean,
    val statusLabel: String,
    val selectedModel: String,
    val discoveredModels: List<String>,
    val enabled: Boolean = true,
    val requestCount: Int = 0,
    val errorCount: Int = 0,
    val rateLimitEvents: Int = 0,
    val lastLatencyMs: Long? = null,
    val avgLatencyMs: Long? = null,
    val totalTokens: Long = 0L,
    val estimatedCostUsd: Double = 0.0,
    val requestLimitPerHour: Int = 120,
    val spendLimitUsd: Double = 10.0,
    val lastError: String? = null
)

data class DualAiCouncilResult(
    val prompt: String,
    val mode: JaxCoreRoutingMode,
    val agentName: String,
    val geminiResponse: String,
    val deepseekResponse: String,
    val agreements: String,
    val disagreements: String,
    val identifiedErrors: String,
    val finalRecommendation: String,
    val verificationStatus: String,
    val usedProviders: List<String>,
    val timestampMillis: Long
)
