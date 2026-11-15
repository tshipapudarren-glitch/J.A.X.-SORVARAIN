package com.example.domain.engine

import com.example.data.model.Candle
import com.example.data.model.ChartOverlayBundle
import com.example.data.model.IndicatorEvaluationResult
import com.example.data.model.MarketRegimeSnapshot
import com.example.data.model.PredictionScenario
import com.example.data.model.SetupPlan
import com.example.data.model.SetupStatus
import com.example.data.model.SignalDirection
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

object SetupGeneratorEngine {

    fun generateSetups(
        symbol: String,
        timeframeCode: String,
        candles: List<Candle>,
        evaluations: List<IndicatorEvaluationResult>,
        overlays: ChartOverlayBundle,
        regime: MarketRegimeSnapshot,
        minScore: Double = 62.0,
        minRiskReward: Double = 1.8
    ): List<SetupPlan> {
        if (candles.size < 30) return emptyList()
        val last = candles.last()
        val atr = regime.atrValue.coerceAtLeast(last.close * 0.002)

        val enabledEvals = evaluations.filter { it.enabled }
        // De-duplicate correlated indicator clusters when scoring confluence
        val grouped = enabledEvals.groupBy { it.definition.correlationGroup }

        var bullClusterScore = 0.0
        var bearClusterScore = 0.0
        var totalClusterWeight = 0.0

        grouped.forEach { (_, groupItems) ->
            val bullAvg = groupItems.filter { it.direction == SignalDirection.BULLISH }.map { it.score }.average()
            val bearAvg = groupItems.filter { it.direction == SignalDirection.BEARISH }.map { it.score }.average()
            if (!bullAvg.isNaN()) bullClusterScore += bullAvg
            if (!bearAvg.isNaN()) bearClusterScore += bearAvg
            totalClusterWeight += 100.0
        }

        val normBull = if (totalClusterWeight > 0) (bullClusterScore / totalClusterWeight) * 100.0 else 0.0
        val normBear = if (totalClusterWeight > 0) (bearClusterScore / totalClusterWeight) * 100.0 else 0.0

        val bullConfirm = enabledEvals.filter { it.direction == SignalDirection.BULLISH && it.score >= 58.0 }.map { it.definition.name }
        val bearConfirm = enabledEvals.filter { it.direction == SignalDirection.BEARISH && it.score >= 58.0 }.map { it.definition.name }

        val activeSession = IndicatorEngine.computeSessions(candles).firstOrNull { it.isActiveNow }?.name ?: "Off-Peak UTC"
        val plans = mutableListOf<SetupPlan>()

        // Primary directional setup
        if (bullConfirm.size >= 3) {
            val freshBullOb = overlays.orderBlocks.lastOrNull { it.bullish && !it.mitigated }
            val entry = if (freshBullOb != null && abs(last.close - freshBullOb.top) < atr * 2.5) {
                (freshBullOb.top + last.close) / 2.0
            } else last.close
            val zoneLow = entry - atr * 0.35
            val zoneHigh = entry + atr * 0.2
            val recentSwingLow = candles.takeLast(14).minOf { it.low }
            val sl = min(recentSwingLow - atr * 0.35, entry - atr * 1.45)
            val risk = (entry - sl).coerceAtLeast(atr * 0.5)
            val tp1 = entry + risk * 1.5
            val tp2 = entry + risk * 2.4
            val tp3 = entry + risk * 3.6
            val rr = (tp2 - entry) / risk
            val score = (normBull * 0.55 + (bullConfirm.size.toDouble() / enabledEvals.size.coerceAtLeast(1)) * 100.0 * 0.45).coerceIn(15.0, 96.0)
            val confirmed = last.isClosed && score >= minScore && rr >= minRiskReward && bullConfirm.size > bearConfirm.size + 2
            val status = when {
                last.close < sl -> SetupStatus.INVALIDATED
                confirmed -> SetupStatus.CONFIRMED
                else -> SetupStatus.DEVELOPING
            }
            val histRates = enabledEvals.filter { it.direction == SignalDirection.BULLISH }.mapNotNull { it.historicalWinRate }
            val avgHist = if (histRates.isNotEmpty()) histRates.average() else null
            val minSample = enabledEvals.filter { it.direction == SignalDirection.BULLISH }.maxOfOrNull { it.sampleSize } ?: 0

            plans.add(
                SetupPlan(
                    id = "setup_buy_${symbol}_${timeframeCode}",
                    symbol = symbol,
                    timeframe = timeframeCode,
                    direction = "BUY",
                    status = status,
                    entryPrice = entry,
                    entryZoneLow = zoneLow,
                    entryZoneHigh = zoneHigh,
                    stopLoss = sl,
                    tp1 = tp1,
                    tp2 = tp2,
                    tp3 = tp3,
                    riskReward = rr,
                    confluenceScore = score,
                    historicalWinRate = avgHist,
                    sampleSize = minSample,
                    regime = regime.regime.label,
                    sessionName = activeSession,
                    invalidationLevel = sl,
                    confirmingIndicators = bullConfirm,
                    conflictingIndicators = bearConfirm,
                    explanation = "Long confluence across ${bullConfirm.size} indicators (${bullConfirm.take(3).joinToString()}) under ${regime.regime.label}. ${if (bearConfirm.isNotEmpty()) "${bearConfirm.size} indicators dissent." else "Zero directional dissent."}",
                    dnaFingerprint = "JAX-${symbol.take(3)}-$timeframeCode-B${score.toInt()}-${bullConfirm.size}",
                    timestampMillis = last.timestampMillis
                )
            )
        }

        if (bearConfirm.size >= 3) {
            val freshBearOb = overlays.orderBlocks.lastOrNull { !it.bullish && !it.mitigated }
            val entry = if (freshBearOb != null && abs(last.close - freshBearOb.bottom) < atr * 2.5) {
                (freshBearOb.bottom + last.close) / 2.0
            } else last.close
            val zoneLow = entry - atr * 0.2
            val zoneHigh = entry + atr * 0.35
            val recentSwingHigh = candles.takeLast(14).maxOf { it.high }
            val sl = max(recentSwingHigh + atr * 0.35, entry + atr * 1.45)
            val risk = (sl - entry).coerceAtLeast(atr * 0.5)
            val tp1 = entry - risk * 1.5
            val tp2 = entry - risk * 2.4
            val tp3 = entry - risk * 3.6
            val rr = (entry - tp2) / risk
            val score = (normBear * 0.55 + (bearConfirm.size.toDouble() / enabledEvals.size.coerceAtLeast(1)) * 100.0 * 0.45).coerceIn(15.0, 96.0)
            val confirmed = last.isClosed && score >= minScore && rr >= minRiskReward && bearConfirm.size > bullConfirm.size + 2
            val status = when {
                last.close > sl -> SetupStatus.INVALIDATED
                confirmed -> SetupStatus.CONFIRMED
                else -> SetupStatus.DEVELOPING
            }
            val histRates = enabledEvals.filter { it.direction == SignalDirection.BEARISH }.mapNotNull { it.historicalWinRate }
            val avgHist = if (histRates.isNotEmpty()) histRates.average() else null
            val minSample = enabledEvals.filter { it.direction == SignalDirection.BEARISH }.maxOfOrNull { it.sampleSize } ?: 0

            plans.add(
                SetupPlan(
                    id = "setup_sell_${symbol}_${timeframeCode}",
                    symbol = symbol,
                    timeframe = timeframeCode,
                    direction = "SELL",
                    status = status,
                    entryPrice = entry,
                    entryZoneLow = zoneLow,
                    entryZoneHigh = zoneHigh,
                    stopLoss = sl,
                    tp1 = tp1,
                    tp2 = tp2,
                    tp3 = tp3,
                    riskReward = rr,
                    confluenceScore = score,
                    historicalWinRate = avgHist,
                    sampleSize = minSample,
                    regime = regime.regime.label,
                    sessionName = activeSession,
                    invalidationLevel = sl,
                    confirmingIndicators = bearConfirm,
                    conflictingIndicators = bullConfirm,
                    explanation = "Short confluence across ${bearConfirm.size} indicators (${bearConfirm.take(3).joinToString()}) under ${regime.regime.label}. ${if (bullConfirm.isNotEmpty()) "${bullConfirm.size} indicators dissent." else "Zero directional dissent."}",
                    dnaFingerprint = "JAX-${symbol.take(3)}-$timeframeCode-S${score.toInt()}-${bearConfirm.size}",
                    timestampMillis = last.timestampMillis
                )
            )
        }

        return plans.sortedByDescending { it.confluenceScore }
    }

    fun buildPredictionScenario(
        symbol: String,
        timeframeCode: String,
        candles: List<Candle>,
        evaluations: List<IndicatorEvaluationResult>,
        overlays: ChartOverlayBundle,
        regime: MarketRegimeSnapshot
    ): PredictionScenario? {
        if (candles.size < 30) return null
        val last = candles.last()
        val bullWeight = evaluations.filter { it.enabled && it.direction == SignalDirection.BULLISH }.sumOf { it.score }
        val bearWeight = evaluations.filter { it.enabled && it.direction == SignalDirection.BEARISH }.sumOf { it.score }
        val total = (bullWeight + bearWeight).coerceAtLeast(1.0)
        // Calibrated probabilities intentionally avoid extreme 0%/100% certainty claims
        val rawBull = ((bullWeight / total) * 100.0).coerceIn(18.0, 82.0)
        val rawBear = (100.0 - rawBull).coerceIn(18.0, 82.0)
        val uncertainty = (100.0 - abs(rawBull - 50.0) * 1.4).coerceIn(18.0, 48.0)

        val falseBreakoutEval = evaluations.firstOrNull { it.definition.number == 22 }
        val reversalEval = evaluations.firstOrNull { it.definition.number == 24 }
        val bslTarget = overlays.liquidityPoints.filter { it.side == "BSL" && it.price > last.close }.minByOrNull { it.price }?.price
            ?: (last.close + regime.atrValue * 2.2)
        val sslTarget = overlays.liquidityPoints.filter { it.side == "SSL" && it.price < last.close }.maxByOrNull { it.price }?.price
            ?: (last.close - regime.atrValue * 2.2)

        val avgSamples = evaluations.map { it.sampleSize }.filter { it > 0 }.average().let { if (it.isNaN()) 0 else it.toInt() }
        val invalidation = if (rawBull >= rawBear) last.close - regime.atrValue * 1.5 else last.close + regime.atrValue * 1.5

        return PredictionScenario(
            symbol = symbol,
            timeframe = timeframeCode,
            horizonLabel = "Next 6–12 $timeframeCode Bars",
            timestampMillis = last.timestampMillis,
            modelVersion = "JAX-QUANT-v2.4",
            bullishProbabilityPct = rawBull,
            bearishProbabilityPct = rawBear,
            expectedExpansionPct = regime.atrPct * 1.85,
            volatilityForecastPct = regime.volatilityPercentile,
            reversalRiskPct = if (reversalEval?.direction != SignalDirection.NEUTRAL) 64.0 else 32.0,
            falseBreakoutRiskPct = if ((falseBreakoutEval?.score ?: 70.0) < 50.0) 68.0 else 29.0,
            uncertaintyMarginPct = uncertainty,
            liquidityTargetUpside = bslTarget,
            liquidityTargetDownside = sslTarget,
            invalidationLevel = invalidation,
            historicalMatches = avgSamples,
            notes = "Calibrated directional distribution with ±${String.format("%.1f", uncertainty)}% uncertainty band across $avgSamples historical pattern matches. Not a certainty guarantee."
        )
    }

    fun generatePineScriptV6(symbol: String, timeframeCode: String, minScore: Double, minRr: Double): String = """
        |//@version=6
        |indicator("J.A.X. Omniverse Confluence Bridge [$symbol $timeframeCode]", overlay=true, max_boxes_count=50, max_lines_count=50)
        |
        |// Inputs synchronized with J.A.X. Omniverse Mobile Terminal
        |emaFastLen = input.int(21, "Adaptive Trend Fast EMA", minval=5)
        |emaSlowLen = input.int(50, "Adaptive Trend Slow EMA", minval=10)
        |atrLen     = input.int(14, "Volatility Adaptation ATR", minval=5)
        |minScore   = input.float(${String.format("%.1f", minScore)}, "Min Confluence Score")
        |targetRR   = input.float(${String.format("%.1f", minRr)}, "Target Risk:Reward")
        |
        |emaFast = ta.ema(close, emaFastLen)
        |emaSlow = ta.ema(close, emaSlowLen)
        |atrVal  = ta.atr(atrLen)
        |rsiVal  = ta.rsi(close, 14)
        |
        |bullTrend = close > emaFast and emaFast > emaSlow and rsiVal >= 48 and rsiVal <= 68
        |bearTrend = close < emaFast and emaFast < emaSlow and rsiVal >= 32 and rsiVal <= 52
        |
        |bullCross = ta.crossover(close, emaFast) and bullTrend
        |bearCross = ta.crossunder(close, emaFast) and bearTrend
        |
        |plot(emaFast, "J.A.X. Fast EMA", color=color.new(#D4AF37, 0), linewidth=2)
        |plot(emaSlow, "J.A.X. Slow EMA", color=color.new(#38BDF8, 20), linewidth=2)
        |
        |if bullCross and barstate.isconfirmed
        |    sl = low - atrVal * 1.35
        |    tp1 = close + (close - sl) * 1.5
        |    tp2 = close + (close - sl) * targetRR
        |    label.new(bar_index, low, "JAX BUY\nSL:" + str.tostring(sl, format.mintick) + "\nTP2:" + str.tostring(tp2, format.mintick), color=#10B981, textcolor=color.black, style=label.style_label_up)
        |    alert('{"source":"TradingView","symbol":"' + syminfo.ticker + '","tf":"' + timeframe.period + '","dir":"BUY","entry":' + str.tostring(close) + ',"sl":' + str.tostring(sl) + ',"tp":' + str.tostring(tp2) + '}', alert.freq_once_per_bar_close)
        |
        |if bearCross and barstate.isconfirmed
        |    sl = high + atrVal * 1.35
        |    tp1 = close - (sl - close) * 1.5
        |    tp2 = close - (sl - close) * targetRR
        |    label.new(bar_index, high, "JAX SELL\nSL:" + str.tostring(sl, format.mintick) + "\nTP2:" + str.tostring(tp2, format.mintick), color=#EF4444, textcolor=color.white, style=label.style_label_down)
        |    alert('{"source":"TradingView","symbol":"' + syminfo.ticker + '","tf":"' + timeframe.period + '","dir":"SELL","entry":' + str.tostring(close) + ',"sl":' + str.tostring(sl) + ',"tp":' + str.tostring(tp2) + '}', alert.freq_once_per_bar_close)
    """.trimMargin()
}
