package com.example.data.model

enum class AssetClass(val label: String) {
    CRYPTO("Crypto"),
    FOREX("Forex"),
    METALS("Metals"),
    INDICES("Indices"),
    COMMODITIES("Commodities"),
    STOCKS("Stocks"),
    FUTURES("Futures"),
    SYNTHETICS("Synthetics")
}

enum class Timeframe(val code: String, val binanceInterval: String, val minutes: Int) {
    D1("D1", "1d", 1440),
    H4("H4", "4h", 240),
    H1("H1", "1h", 60),
    M30("M30", "30m", 30),
    M15("M15", "15m", 15),
    M5("M5", "5m", 5),
    M1("M1", "1m", 1)
}

enum class DataFeedType {
    BINANCE_SPOT,
    FRANKFURTER_FX,
    MT5_BRIDGE_REQUIRED,
    CSV_IMPORTED
}

data class InstrumentSpec(
    val symbol: String,
    val displayName: String,
    val assetClass: AssetClass,
    val feedType: DataFeedType,
    val binanceSymbol: String? = null,
    val fxBase: String? = null,
    val fxQuote: String? = null,
    val priceDecimals: Int = 2,
    val pipSize: Double = 0.01,
    val defaultSpreadPips: Double = 1.2
)

data class Candle(
    val timestampMillis: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val isClosed: Boolean = true
)

enum class ProviderHealthState {
    LIVE_CONNECTED,
    CONFIGURED_STANDBY,
    DISCONNECTED,
    DEGRADED_ERROR
}

data class MarketProviderStatus(
    val id: String,
    val name: String,
    val feedScope: String,
    val state: ProviderHealthState,
    val latencyMs: Long?,
    val lastSyncMillis: Long?,
    val statusDetail: String
)

enum class MarketRegimeType(val label: String) {
    TRENDING_BULLISH("Trending Bullish"),
    TRENDING_BEARISH("Trending Bearish"),
    RANGING("Ranging"),
    HIGH_VOLATILITY("High Volatility"),
    LOW_VOLATILITY("Low Volatility"),
    COMPRESSION("Compression"),
    EXPANSION("Expansion"),
    TRANSITIONAL("Transitional")
}

data class MarketRegimeSnapshot(
    val regime: MarketRegimeType,
    val trendStrengthScore: Double,
    val volatilityPercentile: Double,
    val atrValue: Double,
    val atrPct: Double,
    val bias: String, // BULLISH, BEARISH, NEUTRAL
    val summary: String
)

data class LiquidityPoint(
    val id: String,
    val label: String,
    val price: Double,
    val side: String, // BSL (Buy-Side) or SSL (Sell-Side)
    val timeframe: String,
    val status: String, // ACTIVE, SWEPT, INVALIDATED
    val timestampMillis: Long
)

data class StructurePoint(
    val index: Int,
    val timestampMillis: Long,
    val price: Double,
    val type: String // BOS_BULL, BOS_BEAR, CHOCH_BULL, CHOCH_BEAR, HH, HL, LH, LL
)

data class FvgZone(
    val startIndex: Int,
    val endIndex: Int,
    val top: Double,
    val bottom: Double,
    val bullish: Boolean,
    val mitigated: Boolean,
    val strengthScore: Double
)

data class OrderBlockZone(
    val index: Int,
    val top: Double,
    val bottom: Double,
    val bullish: Boolean,
    val mitigated: Boolean,
    val isBreaker: Boolean,
    val score: Double
)

data class SupportResistanceLevel(
    val price: Double,
    val touches: Int,
    val strengthScore: Double,
    val isResistance: Boolean
)

data class FibonacciLevels(
    val swingHigh: Double,
    val swingLow: Double,
    val isBullishRetrace: Boolean,
    val level500: Double,
    val level618: Double,
    val level705: Double,
    val level786: Double
)

data class ChartOverlayBundle(
    val emaFast: List<Double>,
    val emaSlow: List<Double>,
    val emaMacro: List<Double>,
    val bbUpper: List<Double>,
    val bbLower: List<Double>,
    val liquidityPoints: List<LiquidityPoint>,
    val structurePoints: List<StructurePoint>,
    val fvgZones: List<FvgZone>,
    val orderBlocks: List<OrderBlockZone>,
    val srLevels: List<SupportResistanceLevel>,
    val fibLevels: FibonacciLevels?
)

data class TradingSessionInfo(
    val name: String,
    val utcWindow: String,
    val isActiveNow: Boolean,
    val sessionHigh: Double,
    val sessionLow: Double,
    val openingRangeHigh: Double,
    val openingRangeLow: Double,
    val directionBias: String,
    val sweepDetected: Boolean,
    val avgRangePct: Double
)

data class EconomicEventItem(
    val id: String,
    val title: String,
    val currency: String,
    val impact: String, // HIGH, MEDIUM, LOW
    val scheduledUtc: String,
    val expectedVolatilityNote: String,
    val sourceStatus: String
)

data class MarketBehaviorDna(
    val symbol: String,
    val sampleCandles: Int,
    val avgTrueRange: Double,
    val avgSessionMovePct: Double,
    val avgPullbackPct: Double,
    val avgBreakoutDistancePct: Double,
    val dominantRegime: MarketRegimeType,
    val bestTimeframeNote: String,
    val liquiditySweepTendencyPct: Double,
    val sufficientData: Boolean
)

object SupportedInstruments {
    val catalog: List<InstrumentSpec> = listOf(
        InstrumentSpec("BTCUSDT", "Bitcoin / Tether", AssetClass.CRYPTO, DataFeedType.BINANCE_SPOT, binanceSymbol = "BTCUSDT", priceDecimals = 2, pipSize = 1.0, defaultSpreadPips = 1.5),
        InstrumentSpec("ETHUSDT", "Ethereum / Tether", AssetClass.CRYPTO, DataFeedType.BINANCE_SPOT, binanceSymbol = "ETHUSDT", priceDecimals = 2, pipSize = 0.1, defaultSpreadPips = 0.8),
        InstrumentSpec("SOLUSDT", "Solana / Tether", AssetClass.CRYPTO, DataFeedType.BINANCE_SPOT, binanceSymbol = "SOLUSDT", priceDecimals = 2, pipSize = 0.01, defaultSpreadPips = 0.5),
        InstrumentSpec("PAXGUSDT", "Gold Spot Proxy (PAXG)", AssetClass.METALS, DataFeedType.BINANCE_SPOT, binanceSymbol = "PAXGUSDT", priceDecimals = 2, pipSize = 0.1, defaultSpreadPips = 1.8),
        InstrumentSpec("BNBUSDT", "BNB / Tether", AssetClass.CRYPTO, DataFeedType.BINANCE_SPOT, binanceSymbol = "BNBUSDT", priceDecimals = 2, pipSize = 0.1, defaultSpreadPips = 0.6),
        InstrumentSpec("XRPUSDT", "Ripple / Tether", AssetClass.CRYPTO, DataFeedType.BINANCE_SPOT, binanceSymbol = "XRPUSDT", priceDecimals = 4, pipSize = 0.0001, defaultSpreadPips = 1.2),
        InstrumentSpec("EURUSD", "Euro / US Dollar", AssetClass.FOREX, DataFeedType.FRANKFURTER_FX, fxBase = "EUR", fxQuote = "USD", priceDecimals = 5, pipSize = 0.0001, defaultSpreadPips = 0.8),
        InstrumentSpec("GBPUSD", "British Pound / USD", AssetClass.FOREX, DataFeedType.FRANKFURTER_FX, fxBase = "GBP", fxQuote = "USD", priceDecimals = 5, pipSize = 0.0001, defaultSpreadPips = 1.1),
        InstrumentSpec("USDJPY", "US Dollar / Yen", AssetClass.FOREX, DataFeedType.FRANKFURTER_FX, fxBase = "USD", fxQuote = "JPY", priceDecimals = 3, pipSize = 0.01, defaultSpreadPips = 1.0),
        InstrumentSpec("XAUUSD", "Gold Spot (MT5 Bridge)", AssetClass.METALS, DataFeedType.MT5_BRIDGE_REQUIRED, priceDecimals = 2, pipSize = 0.1, defaultSpreadPips = 2.0),
        InstrumentSpec("US30", "Dow Jones 30 (MT5 Bridge)", AssetClass.INDICES, DataFeedType.MT5_BRIDGE_REQUIRED, priceDecimals = 1, pipSize = 1.0, defaultSpreadPips = 2.5),
        InstrumentSpec("NAS100", "Nasdaq 100 (MT5 Bridge)", AssetClass.INDICES, DataFeedType.MT5_BRIDGE_REQUIRED, priceDecimals = 1, pipSize = 0.5, defaultSpreadPips = 1.8),
        InstrumentSpec("NVDA", "NVIDIA Corp (API/Import)", AssetClass.STOCKS, DataFeedType.MT5_BRIDGE_REQUIRED, priceDecimals = 2, pipSize = 0.01, defaultSpreadPips = 1.0),
        InstrumentSpec("WTI", "Crude Oil WTI (Futures/MT5)", AssetClass.COMMODITIES, DataFeedType.MT5_BRIDGE_REQUIRED, priceDecimals = 2, pipSize = 0.01, defaultSpreadPips = 2.5),
        InstrumentSpec("ES1!", "E-Mini S&P 500 Futures", AssetClass.FUTURES, DataFeedType.MT5_BRIDGE_REQUIRED, priceDecimals = 2, pipSize = 0.25, defaultSpreadPips = 1.0),
        InstrumentSpec("V75", "Volatility 75 (Deriv Bridge)", AssetClass.SYNTHETICS, DataFeedType.MT5_BRIDGE_REQUIRED, priceDecimals = 2, pipSize = 0.1, defaultSpreadPips = 2.0)
    )

    fun find(symbol: String): InstrumentSpec =
        catalog.firstOrNull { it.symbol.equals(symbol, ignoreCase = true) } ?: catalog.first()
}
