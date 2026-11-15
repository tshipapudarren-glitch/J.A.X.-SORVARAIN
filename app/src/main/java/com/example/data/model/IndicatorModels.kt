package com.example.data.model

enum class IndicatorCategory(val title: String) {
    MARKET_INTELLIGENCE("Category A — Market Intelligence"),
    VIP_SETUP("Category B — VIP Setup Indicators"),
    PREDICTIVE_QUANT("Category C — Predictive & Quantitative")
}

enum class SignalDirection(val label: String) {
    BULLISH("BULLISH"),
    BEARISH("BEARISH"),
    NEUTRAL("NEUTRAL")
}

data class IndicatorDefinition(
    val number: Int,
    val id: String,
    val name: String,
    val category: IndicatorCategory,
    val capability: String,
    val defaultLookback: Int = 20,
    val defaultSensitivity: Double = 1.0,
    val defaultMinConfidence: Double = 60.0,
    val correlationGroup: String
)

data class IndicatorEvaluationResult(
    val definition: IndicatorDefinition,
    val enabled: Boolean,
    val direction: SignalDirection,
    val score: Double, // 0..100
    val confidence: Double, // 0..100
    val rationale: String,
    val invalidationHint: String,
    val historicalWinRate: Double?,
    val profitFactor: Double?,
    val avgR: Double?,
    val sampleSize: Int,
    val decayDetected: Boolean,
    val activeVersion: String
)

enum class SetupStatus(val label: String) {
    DEVELOPING("Developing"),
    CONFIRMED("Confirmed"),
    INVALIDATED("Invalidated"),
    COMPLETED("Completed")
}

data class SetupPlan(
    val id: String,
    val symbol: String,
    val timeframe: String,
    val direction: String, // BUY or SELL
    val status: SetupStatus,
    val entryPrice: Double,
    val entryZoneLow: Double,
    val entryZoneHigh: Double,
    val stopLoss: Double,
    val tp1: Double,
    val tp2: Double,
    val tp3: Double,
    val riskReward: Double,
    val confluenceScore: Double,
    val historicalWinRate: Double?,
    val sampleSize: Int,
    val regime: String,
    val sessionName: String,
    val invalidationLevel: Double,
    val confirmingIndicators: List<String>,
    val conflictingIndicators: List<String>,
    val explanation: String,
    val dnaFingerprint: String,
    val timestampMillis: Long
)

data class BacktestTrade(
    val index: Int,
    val entryTimeMillis: Long,
    val exitTimeMillis: Long,
    val direction: String,
    val entryPrice: Double,
    val exitPrice: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val pnlDollar: Double,
    val pnlR: Double,
    val mfeR: Double,
    val maeR: Double,
    val sessionName: String,
    val regime: String,
    val exitReason: String
)

data class BacktestResult(
    val strategyName: String,
    val symbol: String,
    val timeframe: String,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val netProfit: Double,
    val profitFactor: Double,
    val maxDrawdownPct: Double,
    val avgR: Double,
    val expectancy: Double,
    val sharpeRatio: Double,
    val recoveryFactor: Double,
    val maxConsecutiveWins: Int,
    val maxConsecutiveLosses: Int,
    val spreadPips: Double,
    val commissionPct: Double,
    val slippagePips: Double,
    val equityCurve: List<Double>,
    val drawdownCurve: List<Double>,
    val trades: List<BacktestTrade>,
    val walkForwardTrainWinRate: Double,
    val walkForwardTestWinRate: Double,
    val walkForwardPassed: Boolean,
    val outOfSampleScore: Double,
    val monteCarloP5DrawdownPct: Double,
    val monteCarloMedianProfit: Double,
    val sampleSufficiencyWarning: String?
)

data class PredictionScenario(
    val symbol: String,
    val timeframe: String,
    val horizonLabel: String,
    val timestampMillis: Long,
    val modelVersion: String,
    val bullishProbabilityPct: Double,
    val bearishProbabilityPct: Double,
    val expectedExpansionPct: Double,
    val volatilityForecastPct: Double,
    val reversalRiskPct: Double,
    val falseBreakoutRiskPct: Double,
    val uncertaintyMarginPct: Double,
    val liquidityTargetUpside: Double,
    val liquidityTargetDownside: Double,
    val invalidationLevel: Double,
    val historicalMatches: Int,
    val notes: String
)

data class MultiTimeframeRow(
    val timeframe: Timeframe,
    val trendBias: String,
    val structureState: String,
    val momentumState: String,
    val volatilityState: String,
    val confluenceScore: Double,
    val dataAvailable: Boolean
)

data class ScannerOpportunity(
    val symbol: String,
    val assetClass: AssetClass,
    val timeframe: String,
    val setupType: String,
    val direction: String,
    val confluenceScore: Double,
    val riskReward: Double,
    val regime: String,
    val lastPrice: Double,
    val change24hPct: Double,
    val dataLive: Boolean,
    val evidenceSummary: String
)

object VipIndicatorCatalog {
    val all30: List<IndicatorDefinition> = listOf(
        // Category A — Market Intelligence (1-10)
        IndicatorDefinition(1, "ind_01", "Adaptive Trend DNA", IndicatorCategory.MARKET_INTELLIGENCE, "Learns EMA, swing, and HTF continuation patterns", 21, 1.0, 60.0, "TREND"),
        IndicatorDefinition(2, "ind_02", "Smart Liquidity Predictor", IndicatorCategory.MARKET_INTELLIGENCE, "Identifies equal highs/lows and liquidity targets", 30, 1.0, 60.0, "LIQUIDITY"),
        IndicatorDefinition(3, "ind_03", "AI Market Structure Scanner", IndicatorCategory.MARKET_INTELLIGENCE, "Detects BOS, CHoCH, internal & external structure", 15, 1.0, 60.0, "STRUCTURE"),
        IndicatorDefinition(4, "ind_04", "Dynamic Support & Resistance AI", IndicatorCategory.MARKET_INTELLIGENCE, "Ranks S/R levels by reaction strength and breaks", 40, 1.0, 55.0, "LEVELS"),
        IndicatorDefinition(5, "ind_05", "Volume Acceptance Intelligence", IndicatorCategory.MARKET_INTELLIGENCE, "Evaluates breakout volume expansion vs failure", 20, 1.0, 58.0, "VOLUME"),
        IndicatorDefinition(6, "ind_06", "Volatility Adaptation Engine", IndicatorCategory.MARKET_INTELLIGENCE, "Detects ATR compression, expansion, and shocks", 14, 1.0, 55.0, "VOLATILITY"),
        IndicatorDefinition(7, "ind_07", "Smart Money Pattern Learner", IndicatorCategory.MARKET_INTELLIGENCE, "Evaluates OB, breaker, mitigation, and sweeps", 25, 1.0, 62.0, "STRUCTURE"),
        IndicatorDefinition(8, "ind_08", "FVG Probability Engine", IndicatorCategory.MARKET_INTELLIGENCE, "Evaluates Fair Value Gap size & mitigation depth", 20, 1.0, 60.0, "IMBALANCE"),
        IndicatorDefinition(9, "ind_09", "Adaptive Reversal Detector", IndicatorCategory.MARKET_INTELLIGENCE, "Detects exhaustion, sweeps, divergence & rejection", 18, 1.0, 65.0, "REVERSAL"),
        IndicatorDefinition(10, "ind_10", "Momentum Evolution AI", IndicatorCategory.MARKET_INTELLIGENCE, "Tracks acceleration, deceleration & trend fatigue", 14, 1.0, 58.0, "MOMENTUM"),
        // Category B — VIP Setup Indicators (11-20)
        IndicatorDefinition(11, "ind_11", "AI Sniper Entry Pro", IndicatorCategory.VIP_SETUP, "Generates Entry, SL, TP1-TP3 precision setups", 20, 1.0, 68.0, "SETUP"),
        IndicatorDefinition(12, "ind_12", "Breakout Retest Intelligence", IndicatorCategory.VIP_SETUP, "Detects range/structural breakouts and retests", 20, 1.0, 62.0, "BREAKOUT"),
        IndicatorDefinition(13, "ind_13", "Liquidity Sweep Reversal Pro", IndicatorCategory.VIP_SETUP, "Detects stop hunts and strong sweep rejections", 24, 1.0, 65.0, "LIQUIDITY"),
        IndicatorDefinition(14, "ind_14", "Golden Zone Fibonacci AI", IndicatorCategory.VIP_SETUP, "Ranks 0.500, 0.618, 0.705, 0.786 retracement zones", 34, 1.0, 60.0, "LEVELS"),
        IndicatorDefinition(15, "ind_15", "Candle Psychology Learner", IndicatorCategory.VIP_SETUP, "Recognises engulfing, pin bars, displacement & inside bars", 10, 1.0, 55.0, "PRICE_ACTION"),
        IndicatorDefinition(16, "ind_16", "Session Hunter Evolution", IndicatorCategory.VIP_SETUP, "Learns Asian, London & NY opening ranges & sweeps", 24, 1.0, 60.0, "SESSION"),
        IndicatorDefinition(17, "ind_17", "Order Block Precision AI", IndicatorCategory.VIP_SETUP, "Ranks fresh bullish/bearish order blocks & breakers", 25, 1.0, 62.0, "STRUCTURE"),
        IndicatorDefinition(18, "ind_18", "Trend Continuation Master", IndicatorCategory.VIP_SETUP, "Finds structured pullback continuation entries", 21, 1.0, 62.0, "TREND"),
        IndicatorDefinition(19, "ind_19", "Compression Breakout AI", IndicatorCategory.VIP_SETUP, "Detects BB/ATR squeeze & imminent expansion", 20, 1.0, 60.0, "VOLATILITY"),
        IndicatorDefinition(20, "ind_20", "Multi-Confluence Setup AI", IndicatorCategory.VIP_SETUP, "Weights independent indicator evidence without double-counting", 20, 1.0, 65.0, "CONFLUENCE"),
        // Category C — Predictive & Quantitative Indicators (21-30)
        IndicatorDefinition(21, "ind_21", "Market Regime Intelligence", IndicatorCategory.PREDICTIVE_QUANT, "Classifies trending, ranging, compression & expansion", 30, 1.0, 60.0, "REGIME"),
        IndicatorDefinition(22, "ind_22", "False Breakout Predictor", IndicatorCategory.PREDICTIVE_QUANT, "Scores breakout failure risk from weak closes & volume", 20, 1.0, 60.0, "BREAKOUT"),
        IndicatorDefinition(23, "ind_23", "Price Expansion Predictor", IndicatorCategory.PREDICTIVE_QUANT, "Estimates expansion horizon & calibrated uncertainty", 20, 1.0, 58.0, "VOLATILITY"),
        IndicatorDefinition(24, "ind_24", "Adaptive Trend Reversal Radar", IndicatorCategory.PREDICTIVE_QUANT, "Monitors trend weakening score & reversal requirements", 21, 1.0, 62.0, "REVERSAL"),
        IndicatorDefinition(25, "ind_25", "Smart Target Optimiser", IndicatorCategory.PREDICTIVE_QUANT, "Computes TP1/TP2/TP3 from ATR, liquidity & swings", 20, 1.0, 60.0, "TARGETING"),
        IndicatorDefinition(26, "ind_26", "Dynamic Stop-Loss Intelligence", IndicatorCategory.PREDICTIVE_QUANT, "Suggests structural + ATR invalidation stop-losses", 14, 1.0, 60.0, "RISK"),
        IndicatorDefinition(27, "ind_27", "Multi-Timeframe Prediction Matrix", IndicatorCategory.PREDICTIVE_QUANT, "Aligns D1, H4, H1, M30, M15, M5, M1 directional bias", 20, 1.0, 60.0, "MTF"),
        IndicatorDefinition(28, "ind_28", "AI Trade Probability Matrix", IndicatorCategory.PREDICTIVE_QUANT, "Evaluates comparable historical setups with sample intervals", 50, 1.0, 60.0, "QUANT"),
        IndicatorDefinition(29, "ind_29", "Market Behaviour DNA", IndicatorCategory.PREDICTIVE_QUANT, "Profiles instrument-specific volatility, pullbacks & sessions", 60, 1.0, 55.0, "DNA"),
        IndicatorDefinition(30, "ind_30", "Autonomous Indicator Evolution", IndicatorCategory.PREDICTIVE_QUANT, "Runs walk-forward validation, decay detection & rollback control", 50, 1.0, 65.0, "EVOLUTION")
    )
}
