package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Trader",
    val email: String = "",
    val role: String = "trader",
    val accountBalance: Double = 100000.0,
    val riskPerTradePct: Double = 1.0,
    val dailyLossLimitPct: Double = 3.0,
    val weeklyLossLimitPct: Double = 6.0,
    val maxDrawdownPct: Double = 12.0,
    val liveTradingEnabled: Boolean = false,
    val preferredSymbol: String = "BTCUSDT",
    val preferredTimeframe: String = "H1",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IndicatorConfigDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val indicatorNumber: Int = 1,
    val name: String = "",
    val category: String = "MARKET_INTELLIGENCE",
    val enabled: Boolean = true,
    val sensitivity: Double = 1.0,
    val lookbackPeriod: Int = 20,
    val minConfidence: Double = 60.0,
    val showChartOverlay: Boolean = true,
    val alertsEnabled: Boolean = true,
    val activeVersion: String = "v1.0.0",
    val learningStatus: String = "IDLE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SetupDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "BTCUSDT",
    val timeframe: String = "H1",
    val direction: String = "BUY",
    val status: String = "DEVELOPING",
    val entryPrice: Double = 0.0,
    val entryZoneLow: Double = 0.0,
    val entryZoneHigh: Double = 0.0,
    val stopLoss: Double = 0.0,
    val tp1: Double = 0.0,
    val tp2: Double = 0.0,
    val tp3: Double = 0.0,
    val riskReward: Double = 0.0,
    val confluenceScore: Double = 0.0,
    val regime: String = "TRENDING",
    val sessionName: String = "New York",
    val invalidationLevel: Double = 0.0,
    val explanation: String = "",
    val dnaFingerprint: String = "",
    val confirmingIndicators: List<String> = emptyList(),
    val conflictingIndicators: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class BacktestDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val strategyName: String = "",
    val symbol: String = "BTCUSDT",
    val timeframe: String = "H1",
    val indicatorIds: List<String> = emptyList(),
    val totalTrades: Int = 0,
    val winningTrades: Int = 0,
    val losingTrades: Int = 0,
    val winRate: Double = 0.0,
    val netProfit: Double = 0.0,
    val profitFactor: Double = 0.0,
    val maxDrawdownPct: Double = 0.0,
    val avgR: Double = 0.0,
    val expectancy: Double = 0.0,
    val sharpeRatio: Double = 0.0,
    val spreadPips: Double = 1.2,
    val commissionPct: Double = 0.04,
    val slippagePips: Double = 0.5,
    val walkForwardPassed: Boolean = false,
    val outOfSampleScore: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StrategyDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val indicatorIds: List<String> = emptyList(),
    val entryTimeframe: String = "M15",
    val biasTimeframe: String = "H1",
    val minScore: Double = 65.0,
    val minRiskReward: Double = 2.0,
    val version: Int = 1,
    val isProduction: Boolean = false,
    val status: String = "DRAFT",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class LearningJobDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val indicatorNumber: Int = 1,
    val indicatorName: String = "",
    val symbol: String = "BTCUSDT",
    val timeframe: String = "H1",
    val trainingSamples: Int = 0,
    val validationSamples: Int = 0,
    val previousWinRate: Double = 0.0,
    val candidateWinRate: Double = 0.0,
    val previousExpectancy: Double = 0.0,
    val candidateExpectancy: Double = 0.0,
    val proposedChangeSummary: String = "",
    val modelVersion: String = "v1.1.0",
    val approvalStatus: String = "PENDING_APPROVAL",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class TradeJournalDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "BTCUSDT",
    val timeframe: String = "H1",
    val direction: String = "BUY",
    val source: String = "SIGNAL_TRACKER",
    val entryPrice: Double = 0.0,
    val exitPrice: Double = 0.0,
    val stopLoss: Double = 0.0,
    val tp1: Double = 0.0,
    val pnlR: Double = 0.0,
    val mfeR: Double = 0.0,
    val maeR: Double = 0.0,
    val regime: String = "TRENDING",
    val notes: String = "",
    val status: String = "OPEN",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AlertDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val symbol: String = "BTCUSDT",
    val timeframe: String = "H1",
    val category: String = "SETUP",
    val title: String = "",
    val message: String = "",
    val severity: String = "SIGNAL",
    val acknowledged: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IntegrationDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val type: String = "MT5_BRIDGE",
    val name: String = "",
    val endpointUrl: String = "",
    val status: String = "DISCONNECTED",
    val liveExecutionAllowed: Boolean = false,
    val lastSyncSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ApprovalRequestDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val actionType: String = "DEPLOY_MODEL_VERSION",
    val targetId: String = "",
    val summary: String = "",
    val status: String = "PENDING",
    val requestedBy: String = "J.A.X. Engine",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLogDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val action: String = "",
    val actor: String = "",
    val details: String = "",
    val severity: String = "INFO",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IndicatorProjectDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Smart Money",
    val platform: String = "DUAL",
    val complexity: String = "PRO",
    val devStatus: String = "DRAFT",
    val currentVersion: String = "1.0.0",
    val visibility: String = "PRIVATE",
    val isFavorite: Boolean = false,
    val mtfTimeframes: List<String> = listOf("H1", "M15", "M5"),
    val setupPattern: String = "Liquidity Sweep",
    val closedCandleOnly: Boolean = true,
    val entryColorHex: String = "#D4AF37",
    val slColorHex: String = "#EF4444",
    val tpColorHex: String = "#10B981",
    val equivalenceNotes: String = "",
    val downloadCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IndicatorSourceDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val version: String = "1.0.0",
    val platform: String = "MT5",
    val fileName: String = "",
    val sourceCode: String = "",
    val changeSummary: String = "",
    val createdByAgent: String = "JAX-CORE",
    val isStable: Boolean = true,
    val repaintRiskNotes: String = "",
    val validationPassed: Boolean = false,
    val validationSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IndicatorBuildDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val sourceVersion: String = "1.0.0",
    val platform: String = "MT5",
    val compiler: String = "MetaEditor Worker",
    val buildStatus: String = "COMPILER_UNAVAILABLE",
    val errorsSummary: String = "",
    val warningsSummary: String = "",
    val outputFileName: String = "",
    val hasCompiledBinary: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IndicatorDownloadDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val version: String = "1.0.0",
    val platform: String = "MT5",
    val fileType: String = "MQ5",
    val fileName: String = "",
    val byteSize: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IndicatorPresetDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val symbol: String = "XAUUSD",
    val timeframe: String = "M15",
    val presetName: String = "",
    val settingsSummary: String = "",
    val riskPerTradePct: Double = 1.0,
    val drawingStyleSummary: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AiUsageDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val provider: String = "GEMINI",
    val modelId: String = "",
    val agentName: String = "",
    val mode: String = "AUTO",
    val latencyMs: Int = 0,
    val totalTokens: Int = 0,
    val estimatedCostUsd: Double = 0.0,
    val success: Boolean = true,
    val errorMessage: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AiCouncilRunDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val prompt: String = "",
    val mode: String = "DUAL_AI",
    val geminiSummary: String = "",
    val deepseekSummary: String = "",
    val agreements: String = "",
    val disagreements: String = "",
    val verifiedSummary: String = "",
    val usedProviders: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MarketplaceListingDoc(
    @DocumentId val id: String = "",
    val creatorUid: String = "",
    val creatorName: String = "",
    val projectId: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "Smart Money",
    val platform: String = "DUAL",
    val priceUsd: Double = 0.0,
    val listingStatus: String = "PENDING_REVIEW",
    val version: String = "1.0.0",
    val mql5Source: String = "",
    val pineSource: String = "",
    val releaseNotes: String = "",
    val downloadCount: Int = 0,
    val avgRating: Double = 0.0,
    val reviewCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class IndicatorReviewDoc(
    @DocumentId val id: String = "",
    val userId: String = "",
    val reviewerName: String = "",
    val listingId: String = "",
    val rating: Int = 5,
    val reviewText: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
