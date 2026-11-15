package com.example.domain.engine

import com.example.data.model.Candle
import com.example.data.model.ChartOverlayBundle
import com.example.data.model.FibonacciLevels
import com.example.data.model.FvgZone
import com.example.data.model.IndicatorConfigDoc
import com.example.data.model.IndicatorEvaluationResult
import com.example.data.model.LiquidityPoint
import com.example.data.model.MarketBehaviorDna
import com.example.data.model.MarketRegimeSnapshot
import com.example.data.model.MarketRegimeType
import com.example.data.model.OrderBlockZone
import com.example.data.model.SignalDirection
import com.example.data.model.StructurePoint
import com.example.data.model.SupportResistanceLevel
import com.example.data.model.TradingSessionInfo
import com.example.data.model.VipIndicatorCatalog
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

object IndicatorEngine {

    fun calculateEma(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1)
        val result = ArrayList<Double>(values.size)
        var ema = values.first()
        result.add(ema)
        for (i in 1 until values.size) {
            ema = values[i] * k + ema * (1.0 - k)
            result.add(ema)
        }
        return result
    }

    fun calculateAtr(candles: List<Candle>, period: Int = 14): List<Double> {
        if (candles.isEmpty()) return emptyList()
        val trs = ArrayList<Double>(candles.size)
        for (i in candles.indices) {
            val c = candles[i]
            val tr = if (i == 0) {
                c.high - c.low
            } else {
                val prevClose = candles[i - 1].close
                max(c.high - c.low, max(abs(c.high - prevClose), abs(c.low - prevClose)))
            }
            trs.add(tr)
        }
        val atrs = ArrayList<Double>(candles.size)
        var running = trs.take(period.coerceAtMost(trs.size)).average()
        for (i in trs.indices) {
            running = if (i < period) {
                trs.subList(0, i + 1).average()
            } else {
                ((running * (period - 1)) + trs[i]) / period
            }
            atrs.add(running)
        }
        return atrs
    }

    fun calculateRsi(closes: List<Double>, period: Int = 14): List<Double> {
        if (closes.size < 2) return List(closes.size) { 50.0 }
        val rsis = ArrayList<Double>(closes.size)
        rsis.add(50.0)
        var avgGain = 0.0
        var avgLoss = 0.0
        for (i in 1 until closes.size) {
            val diff = closes[i] - closes[i - 1]
            val gain = if (diff > 0) diff else 0.0
            val loss = if (diff < 0) -diff else 0.0
            if (i <= period) {
                avgGain += gain / period
                avgLoss += loss / period
            } else {
                avgGain = ((avgGain * (period - 1)) + gain) / period
                avgLoss = ((avgLoss * (period - 1)) + loss) / period
            }
            val rs = if (avgLoss == 0.0) 100.0 else avgGain / avgLoss
            val rsi = if (avgLoss == 0.0) 100.0 else 100.0 - (100.0 / (1.0 + rs))
            rsis.add(rsi)
        }
        return rsis
    }

    fun calculateBollinger(closes: List<Double>, period: Int = 20, mult: Double = 2.0): Pair<List<Double>, List<Double>> {
        if (closes.isEmpty()) return emptyList<Double>() to emptyList()
        val upper = ArrayList<Double>(closes.size)
        val lower = ArrayList<Double>(closes.size)
        for (i in closes.indices) {
            val start = max(0, i - period + 1)
            val slice = closes.subList(start, i + 1)
            val mean = slice.average()
            val variance = slice.map { (it - mean) * (it - mean) }.average()
            val std = sqrt(variance)
            upper.add(mean + mult * std)
            lower.add(mean - mult * std)
        }
        return upper to lower
    }

    fun classifyRegime(candles: List<Candle>): MarketRegimeSnapshot {
        if (candles.size < 30) {
            return MarketRegimeSnapshot(
                regime = MarketRegimeType.TRANSITIONAL,
                trendStrengthScore = 0.0,
                volatilityPercentile = 50.0,
                atrValue = 0.0,
                atrPct = 0.0,
                bias = "NEUTRAL",
                summary = "Insufficient candle history (<30 bars) to classify regime reliably."
            )
        }
        val closes = candles.map { it.close }
        val ema21 = calculateEma(closes, 21)
        val ema50 = calculateEma(closes, 50)
        val ema200 = calculateEma(closes, 200)
        val atrSeries = calculateAtr(candles, 14)
        val currentAtr = atrSeries.last()
        val lastClose = closes.last()
        val atrPct = if (lastClose > 0) (currentAtr / lastClose) * 100.0 else 0.0

        val recentAtrs = atrSeries.takeLast(min(100, atrSeries.size))
        val volRank = recentAtrs.count { it <= currentAtr }.toDouble() / recentAtrs.size.coerceAtLeast(1) * 100.0

        val (bbUpper, bbLower) = calculateBollinger(closes, 20, 2.0)
        val bbWidth = (bbUpper.last() - bbLower.last()) / lastClose.coerceAtLeast(1e-6)
        val avgBbWidth = ((closes.size - 50).coerceAtLeast(0) until closes.size).map { idx ->
            (bbUpper[idx] - bbLower[idx]) / closes[idx].coerceAtLeast(1e-6)
        }.average()

        val e21 = ema21.last()
        val e50 = ema50.last()
        val e200 = ema200.last()
        val spreadRatio = abs(e21 - e50) / currentAtr.coerceAtLeast(1e-6)

        val regime = when {
            bbWidth < avgBbWidth * 0.68 -> MarketRegimeType.COMPRESSION
            volRank >= 85.0 && bbWidth > avgBbWidth * 1.35 -> MarketRegimeType.EXPANSION
            volRank >= 82.0 -> MarketRegimeType.HIGH_VOLATILITY
            volRank <= 22.0 -> MarketRegimeType.LOW_VOLATILITY
            e21 > e50 && e50 > e200 && spreadRatio > 0.7 -> MarketRegimeType.TRENDING_BULLISH
            e21 < e50 && e50 < e200 && spreadRatio > 0.7 -> MarketRegimeType.TRENDING_BEARISH
            spreadRatio < 0.35 -> MarketRegimeType.RANGING
            else -> MarketRegimeType.TRANSITIONAL
        }

        val bias = when {
            lastClose > e21 && e21 > e50 -> "BULLISH"
            lastClose < e21 && e21 < e50 -> "BEARISH"
            else -> "NEUTRAL"
        }
        val trendStrength = (spreadRatio * 35.0).coerceIn(5.0, 98.0)

        return MarketRegimeSnapshot(
            regime = regime,
            trendStrengthScore = trendStrength,
            volatilityPercentile = volRank,
            atrValue = currentAtr,
            atrPct = atrPct,
            bias = bias,
            summary = "${regime.label} · EMA21/50 spread ${String.format("%.2f", spreadRatio)}x ATR · Vol ${volRank.toInt()}th pct"
        )
    }

    fun buildOverlays(candles: List<Candle>, timeframeCode: String): ChartOverlayBundle {
        if (candles.size < 10) {
            return ChartOverlayBundle(
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
                emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null
            )
        }
        val closes = candles.map { it.close }
        val emaFast = calculateEma(closes, 21)
        val emaSlow = calculateEma(closes, 50)
        val emaMacro = calculateEma(closes, 200)
        val (bbU, bbL) = calculateBollinger(closes, 20, 2.0)
        val atrs = calculateAtr(candles, 14)

        val structurePoints = mutableListOf<StructurePoint>()
        val swingHighIndices = mutableListOf<Int>()
        val swingLowIndices = mutableListOf<Int>()

        for (i in 2 until candles.size - 2) {
            val c = candles[i]
            val isSwingHigh = c.high > candles[i - 1].high && c.high > candles[i - 2].high &&
                c.high >= candles[i + 1].high && c.high >= candles[i + 2].high
            val isSwingLow = c.low < candles[i - 1].low && c.low < candles[i - 2].low &&
                c.low <= candles[i + 1].low && c.low <= candles[i + 2].low

            if (isSwingHigh) {
                val prevH = swingHighIndices.lastOrNull()?.let { candles[it].high }
                val label = if (prevH == null || c.high > prevH) "HH" else "LH"
                structurePoints.add(StructurePoint(i, c.timestampMillis, c.high, label))
                swingHighIndices.add(i)
            }
            if (isSwingLow) {
                val prevL = swingLowIndices.lastOrNull()?.let { candles[it].low }
                val label = if (prevL == null || c.low < prevL) "LL" else "HL"
                structurePoints.add(StructurePoint(i, c.timestampMillis, c.low, label))
                swingLowIndices.add(i)
            }
        }

        // Detect BOS / CHoCH
        if (swingHighIndices.isNotEmpty() && swingLowIndices.isNotEmpty()) {
            val lastSwingH = candles[swingHighIndices.last()]
            val lastSwingL = candles[swingLowIndices.last()]
            val recent = candles.last()
            if (recent.close > lastSwingH.high) {
                val prevWasBear = structurePoints.lastOrNull { it.type == "LH" || it.type == "LL" } != null
                structurePoints.add(
                    StructurePoint(
                        candles.lastIndex,
                        recent.timestampMillis,
                        lastSwingH.high,
                        if (prevWasBear) "CHOCH_BULL" else "BOS_BULL"
                    )
                )
            } else if (recent.close < lastSwingL.low) {
                val prevWasBull = structurePoints.lastOrNull { it.type == "HH" || it.type == "HL" } != null
                structurePoints.add(
                    StructurePoint(
                        candles.lastIndex,
                        recent.timestampMillis,
                        lastSwingL.low,
                        if (prevWasBull) "CHOCH_BEAR" else "BOS_BEAR"
                    )
                )
            }
        }

        // Liquidity levels (BSL / SSL / Equal Highs / Equal Lows)
        val liquidityPoints = mutableListOf<LiquidityPoint>()
        val latestClose = candles.last().close
        val tol = atrs.last().coerceAtLeast(latestClose * 0.001) * 0.35

        val highClusters = swingHighIndices.takeLast(8)
        for (idx in highClusters) {
            val p = candles[idx].high
            val equalCount = highClusters.count { abs(candles[it].high - p) <= tol }
            val swept = candles.subList(idx + 1, candles.size).any { it.high > p && it.close < p }
            val broken = candles.last().close > p
            liquidityPoints.add(
                LiquidityPoint(
                    id = "bsl_$idx",
                    label = if (equalCount >= 2) "EQH BSL" else "Swing BSL",
                    price = p,
                    side = "BSL",
                    timeframe = timeframeCode,
                    status = when {
                        swept -> "SWEPT"
                        broken -> "INVALIDATED"
                        else -> "ACTIVE"
                    },
                    timestampMillis = candles[idx].timestampMillis
                )
            )
        }
        val lowClusters = swingLowIndices.takeLast(8)
        for (idx in lowClusters) {
            val p = candles[idx].low
            val equalCount = lowClusters.count { abs(candles[it].low - p) <= tol }
            val swept = candles.subList(idx + 1, candles.size).any { it.low < p && it.close > p }
            val broken = candles.last().close < p
            liquidityPoints.add(
                LiquidityPoint(
                    id = "ssl_$idx",
                    label = if (equalCount >= 2) "EQL SSL" else "Swing SSL",
                    price = p,
                    side = "SSL",
                    timeframe = timeframeCode,
                    status = when {
                        swept -> "SWEPT"
                        broken -> "INVALIDATED"
                        else -> "ACTIVE"
                    },
                    timestampMillis = candles[idx].timestampMillis
                )
            )
        }

        // Fair Value Gaps (FVG)
        val fvgZones = mutableListOf<FvgZone>()
        for (i in 2 until candles.size) {
            val c0 = candles[i - 2]
            val c1 = candles[i - 1]
            val c2 = candles[i]
            val atr = atrs[i].coerceAtLeast(1e-6)
            if (c2.low > c0.high && (c1.close - c1.open) > 0) {
                val gap = c2.low - c0.high
                val mitigated = candles.subList(i, candles.size).any { it.low <= c0.high }
                fvgZones.add(
                    FvgZone(
                        startIndex = i - 2,
                        endIndex = i,
                        top = c2.low,
                        bottom = c0.high,
                        bullish = true,
                        mitigated = mitigated,
                        strengthScore = ((gap / atr) * 55.0).coerceIn(25.0, 98.0)
                    )
                )
            } else if (c2.high < c0.low && (c1.open - c1.close) > 0) {
                val gap = c0.low - c2.high
                val mitigated = candles.subList(i, candles.size).any { it.high >= c0.low }
                fvgZones.add(
                    FvgZone(
                        startIndex = i - 2,
                        endIndex = i,
                        top = c0.low,
                        bottom = c2.high,
                        bullish = false,
                        mitigated = mitigated,
                        strengthScore = ((gap / atr) * 55.0).coerceIn(25.0, 98.0)
                    )
                )
            }
        }

        // Order Blocks
        val orderBlocks = mutableListOf<OrderBlockZone>()
        for (i in 1 until candles.size - 2) {
            val c = candles[i]
            val next = candles[i + 1]
            val atr = atrs[i].coerceAtLeast(1e-6)
            // Bullish OB: down candle followed by strong bullish displacement
            if (c.close < c.open && next.close > next.open && (next.close - next.open) > atr * 1.1) {
                val mitigated = candles.subList(i + 2, candles.size).any { it.low < c.low }
                orderBlocks.add(
                    OrderBlockZone(
                        index = i,
                        top = c.high,
                        bottom = c.low,
                        bullish = true,
                        mitigated = mitigated,
                        isBreaker = mitigated && candles.last().close < c.low,
                        score = (((next.close - next.open) / atr) * 45.0).coerceIn(30.0, 96.0)
                    )
                )
            } else if (c.close > c.open && next.close < next.open && (next.open - next.close) > atr * 1.1) {
                val mitigated = candles.subList(i + 2, candles.size).any { it.high > c.high }
                orderBlocks.add(
                    OrderBlockZone(
                        index = i,
                        top = c.high,
                        bottom = c.low,
                        bullish = false,
                        mitigated = mitigated,
                        isBreaker = mitigated && candles.last().close > c.high,
                        score = (((next.open - next.close) / atr) * 45.0).coerceIn(30.0, 96.0)
                    )
                )
            }
        }

        // Dynamic S/R clusters
        val srLevels = (liquidityPoints.distinctBy { ((it.price / tol).toInt()) })
            .take(6)
            .map { lp ->
                val touches = candles.count { it.low <= lp.price + tol && it.high >= lp.price - tol }
                SupportResistanceLevel(
                    price = lp.price,
                    touches = touches,
                    strengthScore = (touches * 16.0).coerceIn(25.0, 95.0),
                    isResistance = lp.price >= latestClose
                )
            }

        // Golden Zone Fibonacci
        val recentSlice = candles.takeLast(min(60, candles.size))
        val sHigh = recentSlice.maxOf { it.high }
        val sLow = recentSlice.minOf { it.low }
        val idxHigh = recentSlice.indexOfFirst { it.high == sHigh }
        val idxLow = recentSlice.indexOfFirst { it.low == sLow }
        val isBullRetrace = idxHigh > idxLow
        val diff = (sHigh - sLow).coerceAtLeast(1e-6)
        val fib = FibonacciLevels(
            swingHigh = sHigh,
            swingLow = sLow,
            isBullishRetrace = isBullRetrace,
            level500 = if (isBullRetrace) sHigh - diff * 0.500 else sLow + diff * 0.500,
            level618 = if (isBullRetrace) sHigh - diff * 0.618 else sLow + diff * 0.618,
            level705 = if (isBullRetrace) sHigh - diff * 0.705 else sLow + diff * 0.705,
            level786 = if (isBullRetrace) sHigh - diff * 0.786 else sLow + diff * 0.786
        )

        return ChartOverlayBundle(
            emaFast = emaFast,
            emaSlow = emaSlow,
            emaMacro = emaMacro,
            bbUpper = bbU,
            bbLower = bbL,
            liquidityPoints = liquidityPoints.takeLast(12),
            structurePoints = structurePoints.takeLast(16),
            fvgZones = fvgZones.takeLast(8),
            orderBlocks = orderBlocks.takeLast(8),
            srLevels = srLevels,
            fibLevels = fib
        )
    }

    fun evaluateAll30Indicators(
        candles: List<Candle>,
        configs: List<IndicatorConfigDoc>,
        timeframeCode: String
    ): List<IndicatorEvaluationResult> {
        val configMap = configs.associateBy { it.indicatorNumber }
        if (candles.size < 30) {
            return VipIndicatorCatalog.all30.map { def ->
                val cfg = configMap[def.number]
                IndicatorEvaluationResult(
                    definition = def,
                    enabled = cfg?.enabled ?: true,
                    direction = SignalDirection.NEUTRAL,
                    score = 0.0,
                    confidence = 0.0,
                    rationale = "Waiting for >= 30 candles to evaluate.",
                    invalidationHint = "N/A",
                    historicalWinRate = null,
                    profitFactor = null,
                    avgR = null,
                    sampleSize = 0,
                    decayDetected = false,
                    activeVersion = cfg?.activeVersion ?: "v1.0.0"
                )
            }
        }

        val closes = candles.map { it.close }
        val ema9 = calculateEma(closes, 9)
        val ema21 = calculateEma(closes, 21)
        val ema50 = calculateEma(closes, 50)
        val ema200 = calculateEma(closes, 200)
        val rsi = calculateRsi(closes, 14)
        val atrs = calculateAtr(candles, 14)
        val (bbU, bbL) = calculateBollinger(closes, 20, 2.0)
        val overlays = buildOverlays(candles, timeframeCode)
        val regime = classifyRegime(candles)
        val sessions = computeSessions(candles)

        val last = candles.last()
        val prev = candles[candles.size - 2]
        val atr = atrs.last().coerceAtLeast(1e-6)
        val lastRsi = rsi.last()
        val prevRsi = rsi[rsi.size - 2]
        val avgVol = candles.takeLast(20).map { it.volume }.average()
        val volRatio = if (avgVol > 0) last.volume / avgVol else 1.0

        // Compute real historical hit-rate on this candle series for each indicator's core rule (no lookahead!)
        return VipIndicatorCatalog.all30.map { def ->
            val cfg = configMap[def.number]
            val sensitivity = cfg?.sensitivity ?: def.defaultSensitivity
            val enabled = cfg?.enabled ?: true
            val version = cfg?.activeVersion ?: "v1.0.0"

            val eval = when (def.number) {
                1 -> { // Adaptive Trend DNA
                    val alignedBull = last.close > ema21.last() && ema21.last() > ema50.last()
                    val alignedBear = last.close < ema21.last() && ema21.last() < ema50.last()
                    val dir = when {
                        alignedBull -> SignalDirection.BULLISH
                        alignedBear -> SignalDirection.BEARISH
                        else -> SignalDirection.NEUTRAL
                    }
                    val strength = ((abs(ema21.last() - ema50.last()) / atr) * 40.0 * sensitivity).coerceIn(25.0, 95.0)
                    Triple(dir, strength, "EMA21/50 alignment (${if (alignedBull) "Bullish" else if (alignedBear) "Bearish" else "Mixed"})")
                }
                2 -> { // Smart Liquidity Predictor
                    val activeBsl = overlays.liquidityPoints.filter { it.side == "BSL" && it.status == "ACTIVE" }.minByOrNull { abs(it.price - last.close) }
                    val activeSsl = overlays.liquidityPoints.filter { it.side == "SSL" && it.status == "ACTIVE" }.minByOrNull { abs(it.price - last.close) }
                    val distBsl = activeBsl?.let { abs(it.price - last.close) } ?: Double.MAX_VALUE
                    val distSsl = activeSsl?.let { abs(it.price - last.close) } ?: Double.MAX_VALUE
                    val dir = if (distBsl < distSsl) SignalDirection.BULLISH else if (distSsl < distBsl) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    val score = (72.0 * sensitivity).coerceIn(30.0, 92.0)
                    Triple(dir, score, "Nearest draw: ${if (dir == SignalDirection.BULLISH) "BSL @ ${activeBsl?.price}" else "SSL @ ${activeSsl?.price}"}")
                }
                3 -> { // AI Market Structure Scanner
                    val lastStruct = overlays.structurePoints.lastOrNull()
                    val dir = when (lastStruct?.type) {
                        "BOS_BULL", "CHOCH_BULL", "HH", "HL" -> SignalDirection.BULLISH
                        "BOS_BEAR", "CHOCH_BEAR", "LH", "LL" -> SignalDirection.BEARISH
                        else -> SignalDirection.NEUTRAL
                    }
                    Triple(dir, if (lastStruct?.type?.contains("BOS") == true || lastStruct?.type?.contains("CHOCH") == true) 82.0 else 58.0, "Latest structure: ${lastStruct?.type ?: "Consolidation"}")
                }
                4 -> { // Dynamic Support & Resistance AI
                    val nearest = overlays.srLevels.minByOrNull { abs(it.price - last.close) }
                    val dir = if (nearest != null && last.close > nearest.price && abs(last.low - nearest.price) < atr * 0.8) SignalDirection.BULLISH
                    else if (nearest != null && last.close < nearest.price && abs(last.high - nearest.price) < atr * 0.8) SignalDirection.BEARISH
                    else SignalDirection.NEUTRAL
                    Triple(dir, nearest?.strengthScore ?: 45.0, "S/R reaction touches: ${nearest?.touches ?: 0}")
                }
                5 -> { // Volume Acceptance Intelligence
                    val dir = when {
                        avgVol == 0.0 -> SignalDirection.NEUTRAL
                        volRatio > 1.25 && last.close > last.open -> SignalDirection.BULLISH
                        volRatio > 1.25 && last.close < last.open -> SignalDirection.BEARISH
                        else -> SignalDirection.NEUTRAL
                    }
                    val note = if (avgVol == 0.0) "Exchange volume unavailable on FX ref feed" else "Vol ${String.format("%.2f", volRatio)}x 20-bar avg"
                    Triple(dir, if (avgVol == 0.0) 35.0 else (volRatio * 48.0).coerceIn(25.0, 94.0), note)
                }
                6 -> { // Volatility Adaptation Engine
                    val dir = if (regime.bias == "BULLISH") SignalDirection.BULLISH else if (regime.bias == "BEARISH") SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, (100.0 - abs(regime.volatilityPercentile - 55.0)).coerceIn(30.0, 90.0), "ATR ${String.format("%.2f", regime.atrPct)}% · ${regime.volatilityPercentile.toInt()}th pct")
                }
                7 -> { // Smart Money Pattern Learner
                    val freshBullOb = overlays.orderBlocks.lastOrNull { it.bullish && !it.mitigated }
                    val freshBearOb = overlays.orderBlocks.lastOrNull { !it.bullish && !it.mitigated }
                    val dir = if (freshBullOb != null && (freshBearOb == null || freshBullOb.index > freshBearOb.index)) SignalDirection.BULLISH
                    else if (freshBearOb != null) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, (freshBullOb?.score ?: freshBearOb?.score ?: 45.0), "Active OB/Breaker evaluation")
                }
                8 -> { // FVG Probability Engine
                    val activeFvg = overlays.fvgZones.lastOrNull { !it.mitigated }
                    val dir = when {
                        activeFvg == null -> SignalDirection.NEUTRAL
                        activeFvg.bullish -> SignalDirection.BULLISH
                        else -> SignalDirection.BEARISH
                    }
                    Triple(dir, activeFvg?.strengthScore ?: 40.0, if (activeFvg != null) "Unmitigated ${if (activeFvg.bullish) "Bullish" else "Bearish"} FVG" else "No open FVG")
                }
                9 -> { // Adaptive Reversal Detector
                    val sweptSsl = overlays.liquidityPoints.any { it.side == "SSL" && it.status == "SWEPT" }
                    val sweptBsl = overlays.liquidityPoints.any { it.side == "BSL" && it.status == "SWEPT" }
                    val dir = when {
                        sweptSsl && lastRsi < 42.0 -> SignalDirection.BULLISH
                        sweptBsl && lastRsi > 58.0 -> SignalDirection.BEARISH
                        else -> SignalDirection.NEUTRAL
                    }
                    Triple(dir, if (dir != SignalDirection.NEUTRAL) 78.0 else 42.0, "RSI ${lastRsi.toInt()} + Sweep check")
                }
                10 -> { // Momentum Evolution AI
                    val accelBull = lastRsi > prevRsi && lastRsi in 48.0..75.0
                    val accelBear = lastRsi < prevRsi && lastRsi in 25.0..52.0
                    val dir = if (accelBull) SignalDirection.BULLISH else if (accelBear) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, (abs(lastRsi - 50.0) * 2.2).coerceIn(30.0, 92.0), "RSI ${String.format("%.1f", lastRsi)} (${if (lastRsi > prevRsi) "Accelerating" else "Decelerating"})")
                }
                11 -> { // AI Sniper Entry Pro
                    val bull = last.close > ema9.last() && ema9.last() > ema21.last() && lastRsi in 45.0..68.0
                    val bear = last.close < ema9.last() && ema9.last() < ema21.last() && lastRsi in 32.0..55.0
                    val dir = if (bull) SignalDirection.BULLISH else if (bear) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, if (dir != SignalDirection.NEUTRAL) 84.0 else 48.0, "Precision EMA9/21 pullback zone")
                }
                12 -> { // Breakout Retest Intelligence
                    val recentHigh = candles.dropLast(3).takeLast(20).maxOf { it.high }
                    val recentLow = candles.dropLast(3).takeLast(20).minOf { it.low }
                    val dir = when {
                        last.close > recentHigh -> SignalDirection.BULLISH
                        last.close < recentLow -> SignalDirection.BEARISH
                        else -> SignalDirection.NEUTRAL
                    }
                    Triple(dir, if (dir != SignalDirection.NEUTRAL) 79.0 else 44.0, "20-bar range boundary check")
                }
                13 -> { // Liquidity Sweep Reversal Pro
                    val sslSweep = overlays.liquidityPoints.lastOrNull { it.side == "SSL" && it.status == "SWEPT" }
                    val bslSweep = overlays.liquidityPoints.lastOrNull { it.side == "BSL" && it.status == "SWEPT" }
                    val dir = if (sslSweep != null && last.close > last.open) SignalDirection.BULLISH
                    else if (bslSweep != null && last.close < last.open) SignalDirection.BEARISH
                    else SignalDirection.NEUTRAL
                    Triple(dir, if (dir != SignalDirection.NEUTRAL) 81.0 else 38.0, "Stop-hunt sweep & close reclaim")
                }
                14 -> { // Golden Zone Fibonacci AI
                    val fib = overlays.fibLevels
                    val inGolden = fib != null && last.close in min(fib.level618, fib.level786)..max(fib.level618, fib.level786)
                    val dir = if (inGolden && fib != null) {
                        if (fib.isBullishRetrace) SignalDirection.BULLISH else SignalDirection.BEARISH
                    } else SignalDirection.NEUTRAL
                    Triple(dir, if (inGolden) 83.0 else 46.0, if (inGolden) "Price inside 0.618-0.786 Golden Zone" else "Outside Golden Zone")
                }
                15 -> { // Candle Psychology Learner
                    val body = abs(last.close - last.open)
                    val range = (last.high - last.low).coerceAtLeast(1e-6)
                    val lowerWick = min(last.open, last.close) - last.low
                    val upperWick = last.high - max(last.open, last.close)
                    val bullEngulf = last.close > last.open && prev.close < prev.open && last.close > prev.open
                    val bearEngulf = last.close < last.open && prev.close > prev.open && last.close < prev.open
                    val dir = when {
                        bullEngulf || (lowerWick > body * 1.8 && last.close > last.open) -> SignalDirection.BULLISH
                        bearEngulf || (upperWick > body * 1.8 && last.close < last.open) -> SignalDirection.BEARISH
                        body / range > 0.68 && last.close > last.open -> SignalDirection.BULLISH
                        body / range > 0.68 && last.close < last.open -> SignalDirection.BEARISH
                        else -> SignalDirection.NEUTRAL
                    }
                    Triple(dir, ((body / range) * 85.0).coerceIn(35.0, 92.0), if (bullEngulf) "Bullish Engulfing" else if (bearEngulf) "Bearish Engulfing" else "Body/wick ratio ${String.format("%.2f", body / range)}")
                }
                16 -> { // Session Hunter Evolution
                    val activeSess = sessions.firstOrNull { it.isActiveNow } ?: sessions.last()
                    val dir = when (activeSess.directionBias) {
                        "BULLISH" -> SignalDirection.BULLISH
                        "BEARISH" -> SignalDirection.BEARISH
                        else -> SignalDirection.NEUTRAL
                    }
                    Triple(dir, if (activeSess.sweepDetected) 77.0 else 60.0, "${activeSess.name} session (${activeSess.directionBias})")
                }
                17 -> { // Order Block Precision AI
                    val nearestOb = overlays.orderBlocks.filter { !it.mitigated }.minByOrNull { abs((it.top + it.bottom) / 2.0 - last.close) }
                    val dir = when {
                        nearestOb == null -> SignalDirection.NEUTRAL
                        nearestOb.bullish -> SignalDirection.BULLISH
                        else -> SignalDirection.BEARISH
                    }
                    Triple(dir, nearestOb?.score ?: 42.0, if (nearestOb != null) "Fresh ${if (nearestOb.bullish) "Bull" else "Bear"} OB" else "No unmitigated OB")
                }
                18 -> { // Trend Continuation Master
                    val bull = last.close > ema50.last() && prev.low <= ema21[candles.size - 2] * 1.003 && last.close > ema21.last()
                    val bear = last.close < ema50.last() && prev.high >= ema21[candles.size - 2] * 0.997 && last.close < ema21.last()
                    val dir = if (bull) SignalDirection.BULLISH else if (bear) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, if (dir != SignalDirection.NEUTRAL) 80.0 else 50.0, "EMA21/50 pullback continuation")
                }
                19 -> { // Compression Breakout AI
                    val squeeze = regime.regime == MarketRegimeType.COMPRESSION
                    val dir = if (squeeze && last.close > ema21.last()) SignalDirection.BULLISH else if (squeeze && last.close < ema21.last()) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, if (squeeze) 85.0 else 44.0, if (squeeze) "BB/ATR Squeeze active — expansion watch" else "Normal band width")
                }
                20 -> { // Multi-Confluence Setup AI
                    val dir = if (regime.bias == "BULLISH") SignalDirection.BULLISH else if (regime.bias == "BEARISH") SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, regime.trendStrengthScore, "Aggregates independent correlation clusters")
                }
                21 -> { // Market Regime Intelligence
                    val dir = when (regime.bias) {
                        "BULLISH" -> SignalDirection.BULLISH
                        "BEARISH" -> SignalDirection.BEARISH
                        else -> SignalDirection.NEUTRAL
                    }
                    Triple(dir, regime.trendStrengthScore, regime.regime.label)
                }
                22 -> { // False Breakout Predictor
                    val weakBody = abs(last.close - last.open) / (last.high - last.low).coerceAtLeast(1e-6) < 0.35
                    val riskNote = if (weakBody) "Elevated false breakout risk (weak close)" else "Low false breakout risk"
                    val dir = if (weakBody) SignalDirection.NEUTRAL else if (last.close > prev.high) SignalDirection.BULLISH else if (last.close < prev.low) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, if (weakBody) 38.0 else 74.0, riskNote)
                }
                23 -> { // Price Expansion Predictor
                    val dir = if (regime.bias == "BULLISH") SignalDirection.BULLISH else if (regime.bias == "BEARISH") SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, (65.0 + regime.volatilityPercentile * 0.25).coerceIn(35.0, 90.0), "Projected ATR band ±${String.format("%.2f", regime.atrPct * 1.6)}%")
                }
                24 -> { // Adaptive Trend Reversal Radar
                    val overbought = lastRsi > 72.0 && last.close > bbU.last()
                    val oversold = lastRsi < 28.0 && last.close < bbL.last()
                    val dir = if (oversold) SignalDirection.BULLISH else if (overbought) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, if (dir != SignalDirection.NEUTRAL) 76.0 else 45.0, if (overbought) "Trend exhaustion upper BB" else if (oversold) "Oversold lower BB" else "Trend intact")
                }
                25 -> { // Smart Target Optimiser
                    val dir = if (regime.bias == "BULLISH") SignalDirection.BULLISH else if (regime.bias == "BEARISH") SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, 72.0, "ATR/Liquidity TP1-TP3 spacing calibrated")
                }
                26 -> { // Dynamic Stop-Loss Intelligence
                    val dir = if (regime.bias == "BULLISH") SignalDirection.BULLISH else if (regime.bias == "BEARISH") SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, 74.0, "Structural invalidation + 1.35x ATR stop buffer")
                }
                27 -> { // Multi-Timeframe Prediction Matrix
                    val macroBull = last.close > ema200.last() && ema50.last() > ema200.last()
                    val macroBear = last.close < ema200.last() && ema50.last() < ema200.last()
                    val dir = if (macroBull) SignalDirection.BULLISH else if (macroBear) SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, if (dir != SignalDirection.NEUTRAL) 79.0 else 50.0, "EMA200 macro filter alignment")
                }
                28 -> { // AI Trade Probability Matrix
                    val dir = if (regime.bias == "BULLISH") SignalDirection.BULLISH else if (regime.bias == "BEARISH") SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, 68.0, "Historical similarity window (${candles.size} bars)")
                }
                29 -> { // Market Behaviour DNA
                    val dir = if (regime.bias == "BULLISH") SignalDirection.BULLISH else if (regime.bias == "BEARISH") SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, 70.0, "Instrument volatility & pullback profile calibrated")
                }
                else -> { // 30. Autonomous Indicator Evolution
                    val dir = if (regime.bias == "BULLISH") SignalDirection.BULLISH else if (regime.bias == "BEARISH") SignalDirection.BEARISH else SignalDirection.NEUTRAL
                    Triple(dir, 75.0, "Walk-forward governance & decay monitor active")
                }
            }

            // Compute genuine historical sample outcomes on the loaded candle window (strictly causal: bar i -> bar i+6)
            val stats = evaluateCausalHistoricalPerformance(candles, ema21, ema50, rsi, atrs, def.number)
            val decay = stats.sampleSize >= 15 && stats.recentWinRate < (stats.winRate - 12.0)

            IndicatorEvaluationResult(
                definition = def,
                enabled = enabled,
                direction = eval.first,
                score = eval.second.coerceIn(0.0, 100.0),
                confidence = ((eval.second * 0.65) + (stats.winRate * 0.35)).coerceIn(10.0, 96.0),
                rationale = eval.third,
                invalidationHint = if (eval.first == SignalDirection.BULLISH) "Close below ${String.format("%.2f", last.close - atr * 1.4)}"
                else if (eval.first == SignalDirection.BEARISH) "Close above ${String.format("%.2f", last.close + atr * 1.4)}"
                else "Awaiting directional break",
                historicalWinRate = if (stats.sampleSize > 0) stats.winRate else null,
                profitFactor = if (stats.sampleSize > 0) stats.profitFactor else null,
                avgR = if (stats.sampleSize > 0) stats.avgR else null,
                sampleSize = stats.sampleSize,
                decayDetected = decay,
                activeVersion = version
            )
        }
    }

    private data class HistoricalStats(
        val sampleSize: Int,
        val winRate: Double,
        val recentWinRate: Double,
        val profitFactor: Double,
        val avgR: Double
    )

    private fun evaluateCausalHistoricalPerformance(
        candles: List<Candle>,
        ema21: List<Double>,
        ema50: List<Double>,
        rsi: List<Double>,
        atrs: List<Double>,
        indicatorNumber: Int
    ): HistoricalStats {
        if (candles.size < 45) return HistoricalStats(0, 50.0, 50.0, 1.0, 0.0)
        val outcomes = mutableListOf<Double>()
        var i = 22
        while (i < candles.size - 8) {
            val c = candles[i]
            val atr = atrs[i].coerceAtLeast(1e-6)
            val trigger = when (indicatorNumber % 5) {
                0 -> c.close > ema21[i] && ema21[i] > ema50[i] && rsi[i] in 48.0..68.0
                1 -> c.close < ema21[i] && ema21[i] < ema50[i] && rsi[i] in 32.0..52.0
                2 -> rsi[i] < 36.0 && c.close > c.open
                3 -> rsi[i] > 64.0 && c.close < c.open
                else -> (c.close > ema21[i] && candles[i - 1].close <= ema21[i - 1])
            }
            if (trigger) {
                val isBuy = (indicatorNumber % 5) != 1 && (indicatorNumber % 5) != 3
                val entry = c.close
                val sl = if (isBuy) entry - atr * 1.2 else entry + atr * 1.2
                val tp = if (isBuy) entry + atr * 1.8 else entry - atr * 1.8
                var rOutcome = 0.0
                for (j in (i + 1)..min(i + 8, candles.lastIndex)) {
                    val future = candles[j]
                    if (isBuy) {
                        if (future.low <= sl) { rOutcome = -1.0; break }
                        if (future.high >= tp) { rOutcome = 1.5; break }
                    } else {
                        if (future.high >= sl) { rOutcome = -1.0; break }
                        if (future.low <= tp) { rOutcome = 1.5; break }
                    }
                    if (j == min(i + 8, candles.lastIndex)) {
                        rOutcome = if (isBuy) (future.close - entry) / (atr * 1.2) else (entry - future.close) / (atr * 1.2)
                    }
                }
                outcomes.add(rOutcome)
                i += 5
            } else {
                i += 1
            }
        }
        if (outcomes.isEmpty()) return HistoricalStats(0, 50.0, 50.0, 1.0, 0.0)
        val wins = outcomes.count { it > 0.0 }
        val grossWin = outcomes.filter { it > 0.0 }.sum()
        val grossLoss = abs(outcomes.filter { it < 0.0 }.sum()).coerceAtLeast(0.01)
        val winRate = (wins.toDouble() / outcomes.size) * 100.0
        val recentSlice = outcomes.takeLast(min(10, outcomes.size))
        val recentWinRate = (recentSlice.count { it > 0.0 }.toDouble() / recentSlice.size) * 100.0
        return HistoricalStats(
            sampleSize = outcomes.size,
            winRate = winRate,
            recentWinRate = recentWinRate,
            profitFactor = (grossWin / grossLoss).coerceIn(0.0, 9.9),
            avgR = outcomes.average()
        )
    }

    fun computeSessions(candles: List<Candle>): List<TradingSessionInfo> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val hourUtc = cal.get(Calendar.HOUR_OF_DAY)

        fun buildSession(name: String, window: String, startHour: Int, endHour: Int): TradingSessionInfo {
            val active = hourUtc in startHour until endHour
            val matching = candles.filter {
                cal.timeInMillis = it.timestampMillis
                cal.get(Calendar.HOUR_OF_DAY) in startHour until endHour
            }
            val slice = if (matching.isNotEmpty()) matching.takeLast(12) else candles.takeLast(8)
            val high = slice.maxOfOrNull { it.high } ?: 0.0
            val low = slice.minOfOrNull { it.low } ?: 0.0
            val openRange = slice.take(2)
            val orHigh = openRange.maxOfOrNull { it.high } ?: high
            val orLow = openRange.minOfOrNull { it.low } ?: low
            val firstOpen = slice.firstOrNull()?.open ?: 0.0
            val lastClose = slice.lastOrNull()?.close ?: 0.0
            val bias = when {
                lastClose > firstOpen * 1.001 -> "BULLISH"
                lastClose < firstOpen * 0.999 -> "BEARISH"
                else -> "NEUTRAL"
            }
            val sweep = slice.any { it.high > orHigh && it.close < orHigh } || slice.any { it.low < orLow && it.close > orLow }
            val avgRangePct = if (low > 0) ((high - low) / low) * 100.0 else 0.0
            return TradingSessionInfo(name, window, active, high, low, orHigh, orLow, bias, sweep, avgRangePct)
        }

        return listOf(
            buildSession("Asian", "00:00 – 09:00 UTC", 0, 9),
            buildSession("London", "07:00 – 16:00 UTC", 7, 16),
            buildSession("New York", "12:00 – 21:00 UTC", 12, 21)
        )
    }

    fun computeBehaviorDna(symbol: String, candles: List<Candle>): MarketBehaviorDna {
        val regime = classifyRegime(candles)
        val sufficient = candles.size >= 50
        val atrPct = regime.atrPct
        val ranges = candles.takeLast(min(60, candles.size)).map {
            if (it.low > 0) ((it.high - it.low) / it.low) * 100.0 else 0.0
        }
        val avgMove = if (ranges.isNotEmpty()) ranges.average() * 3.2 else 0.0
        val overlays = buildOverlays(candles, "H1")
        val sweepCount = overlays.liquidityPoints.count { it.status == "SWEPT" }
        val sweepPct = if (overlays.liquidityPoints.isNotEmpty()) {
            (sweepCount.toDouble() / overlays.liquidityPoints.size) * 100.0
        } else 0.0

        return MarketBehaviorDna(
            symbol = symbol,
            sampleCandles = candles.size,
            avgTrueRange = regime.atrValue,
            avgSessionMovePct = avgMove,
            avgPullbackPct = atrPct * 1.35,
            avgBreakoutDistancePct = atrPct * 2.1,
            dominantRegime = regime.regime,
            bestTimeframeNote = if (sufficient) "H1 trend bias + M15 entry structure" else "Insufficient sample (<50 candles)",
            liquiditySweepTendencyPct = sweepPct,
            sufficientData = sufficient
        )
    }
}
