package com.example.data.model

enum class ForgePlatform(val label: String) {
    MT5("MetaTrader 5 (.mq5 / .ex5)"),
    TRADINGVIEW("TradingView (Pine v6)"),
    DUAL("Dual Platform (MT5 + Pine v6)")
}

enum class ForgeCategory(val label: String) {
    BREAKOUT("Breakout"),
    SMART_MONEY("Smart Money"),
    PRICE_ACTION("Price Action"),
    AI_INTELLIGENCE("AI Intelligence"),
    SNIPER_SETUP("Sniper Setup"),
    SESSION("Session"),
    VOLATILITY("Volatility"),
    REVERSAL("Reversal"),
    TREND("Trend"),
    VOLUME("Volume"),
    SCALPING("Scalping"),
    SWING("Swing Trading"),
    MULTI_TIMEFRAME("Multi-Timeframe"),
    RISK_MANAGEMENT("Risk Management"),
    QUANTITATIVE("Quantitative")
}

enum class ForgeComplexity(val code: String, val label: String) {
    SIMPLE("SIMPLE", "Simple"),
    ADVANCED("ADVANCED", "Advanced"),
    PRO("PRO", "Professional"),
    AI_ASSISTED("AI_ASSISTED", "AI-Assisted"),
    CUSTOM("CUSTOM", "Custom Code")
}

enum class ForgeBuildStatus(val label: String) {
    WAITING("Waiting"),
    PROCESSING("Processing"),
    COMPILED_SUCCESS("Compiled Successfully"),
    COMPILED_WARNINGS("Compiled with Warnings"),
    COMPILATION_FAILED("Compilation Failed"),
    COMPILER_UNAVAILABLE("Compiler Worker Unavailable"),
    VALIDATED_UNCOMPILED("Validated (Uncompiled Source)")
}

enum class SetupDrawingPattern(val label: String) {
    LIQUIDITY_SWEEP("Liquidity Sweep"),
    BREAKOUT_RETEST("Breakout & Retest"),
    FAIR_VALUE_GAP("Fair Value Gap"),
    ORDER_BLOCK("Order Block"),
    BOS_CHOCH("BOS / CHoCH"),
    SUPPLY_DEMAND("Supply & Demand"),
    SUPPORT_RESISTANCE("Support & Resistance"),
    FIB_REVERSAL("Fibonacci Reversal"),
    ENGULFING("Engulfing Candle"),
    REJECTION("Rejection Candle"),
    TREND_CONTINUATION("Trend Continuation"),
    SESSION_SWEEP("Session Sweep"),
    DISPLACEMENT("Displacement"),
    DOUBLE_TOP_BOTTOM("Double Top / Bottom"),
    COMPRESSION_BREAKOUT("Compression Breakout")
}

data class SetupDrawingConfig(
    val entryColorHex: String = "#D4AF37",
    val slColorHex: String = "#EF4444",
    val tp1ColorHex: String = "#10B981",
    val tp2ColorHex: String = "#34D399",
    val tp3ColorHex: String = "#38BDF8",
    val invalidationColorHex: String = "#F59E0B",
    val lineWidthPx: Int = 2,
    val lineStyle: String = "SOLID", // SOLID, DASHED, DOTTED
    val zoneOpacityPct: Int = 18,
    val durationBars: Int = 24,
    val labelPosition: String = "RIGHT",
    val showSignalArrows: Boolean = true,
    val showRiskRewardBoxes: Boolean = true,
    val closedCandleOnly: Boolean = true
)

data class ForgeValidationFinding(
    val lineNumber: Int,
    val severity: String, // ERROR, WARNING, INFO
    val message: String,
    val suggestion: String
)

data class ForgeValidationReport(
    val platform: ForgePlatform,
    val passed: Boolean,
    val repaintRiskLevel: String, // LOW, MEDIUM, HIGH
    val lookaheadDetected: Boolean,
    val findings: List<ForgeValidationFinding>,
    val summary: String
)

data class RuleEquivalenceItem(
    val aspect: String,
    val mt5Behavior: String,
    val pineBehavior: String,
    val parityStatus: String // EQUIVALENT, ADAPTED, PLATFORM_SPECIFIC
)

data class ForgeTemplateSpec(
    val id: String,
    val name: String,
    val category: ForgeCategory,
    val pattern: SetupDrawingPattern,
    val description: String,
    val defaultTimeframes: List<String>,
    val fastPeriod: Int,
    val slowPeriod: Int,
    val atrPeriod: Int,
    val rrTarget: Double
)

object ForgeTemplates {
    val presets: List<ForgeTemplateSpec> = listOf(
        ForgeTemplateSpec(
            id = "tpl_liq_sniper",
            name = "J.A.X. Liquidity Sweep Sniper",
            category = ForgeCategory.SNIPER_SETUP,
            pattern = SetupDrawingPattern.LIQUIDITY_SWEEP,
            description = "Detects stop-hunt sweeps above/below prior swing liquidity with H1 bias, M15 structure, and M5 closed-candle Entry/SL/TP1-TP3 drawings.",
            defaultTimeframes = listOf("H1", "M15", "M5"),
            fastPeriod = 21,
            slowPeriod = 50,
            atrPeriod = 14,
            rrTarget = 2.4
        ),
        ForgeTemplateSpec(
            id = "tpl_fvg_ob",
            name = "J.A.X. Order Block & FVG Matrix",
            category = ForgeCategory.SMART_MONEY,
            pattern = SetupDrawingPattern.FAIR_VALUE_GAP,
            description = "Maps institutional displacement Fair Value Gaps and unmitigated Order Blocks with invalidation line and dynamic TP levels.",
            defaultTimeframes = listOf("H4", "H1", "M15"),
            fastPeriod = 14,
            slowPeriod = 50,
            atrPeriod = 14,
            rrTarget = 2.5
        ),
        ForgeTemplateSpec(
            id = "tpl_brk_retest",
            name = "J.A.X. Compression Breakout Retest",
            category = ForgeCategory.BREAKOUT,
            pattern = SetupDrawingPattern.BREAKOUT_RETEST,
            description = "Identifies ATR/Bollinger compression breakouts and structural retests with volume/momentum confirmation and R:R target boxes.",
            defaultTimeframes = listOf("H1", "M30", "M15"),
            fastPeriod = 20,
            slowPeriod = 55,
            atrPeriod = 14,
            rrTarget = 2.2
        ),
        ForgeTemplateSpec(
            id = "tpl_session_hunter",
            name = "J.A.X. Killzone Session Hunter",
            category = ForgeCategory.SESSION,
            pattern = SetupDrawingPattern.SESSION_SWEEP,
            description = "Tracks Asian Range sweep during London/NY Killzone opens with closed-candle confirmation and automatic target optimization.",
            defaultTimeframes = listOf("H1", "M15", "M5"),
            fastPeriod = 13,
            slowPeriod = 34,
            atrPeriod = 14,
            rrTarget = 2.0
        )
    )
}
