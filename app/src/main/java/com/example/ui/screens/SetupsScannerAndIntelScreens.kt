package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AssetClass
import com.example.data.model.SetupStatus
import com.example.data.model.SupportedInstruments
import com.example.ui.components.JaxGlassCard
import com.example.ui.components.MetricBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.navigation.JaxSection
import com.example.ui.theme.BearishContainer
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BullishContainer
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanQuant
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverText
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.JaxViewModel

@Composable
fun SetupGeneratorScreen(
    viewModel: JaxViewModel,
    onNavigate: (JaxSection) -> Unit
) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val savedSetups by viewModel.savedSetups.collectAsStateWithLifecycle()
    val dec = market.selectedInstrument.priceDecimals

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Automatic Setup Generator",
                subtitle = "${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}) · Entry, SL, TP1–TP3 & Indicator Council"
            )
        }

        if (market.generatedSetups.isEmpty()) {
            item {
                JaxGlassCard {
                    Text("No Setups Meet Current Confluence Threshold", style = MaterialTheme.typography.titleMedium, color = WarningAmber)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Current regime (${market.regime.regime.label}) has fewer than 3 independent aligned indicators on closed bars, or insufficient candle history (${market.candles.size} bars).",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverText
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onNavigate(JaxSection.AI_CHART_ANALYSER) },
                        modifier = Modifier.testTag("open_chart_analyser_btn")
                    ) {
                        Text("Inspect Chart Overlays", color = MetallicGold)
                    }
                }
            }
        } else {
            items(market.generatedSetups, key = { it.id }) { setup ->
                val isBuy = setup.direction == "BUY"
                val dirColor = if (isBuy) BullishEmerald else BearishCrimson
                val dirBg = if (isBuy) BullishContainer else BearishContainer
                val statusColor = when (setup.status) {
                    SetupStatus.CONFIRMED -> BullishEmerald
                    SetupStatus.DEVELOPING -> WarningAmber
                    SetupStatus.INVALIDATED -> BearishCrimson
                    SetupStatus.COMPLETED -> CyanQuant
                }
                val statusBg = when (setup.status) {
                    SetupStatus.CONFIRMED -> BullishContainer
                    SetupStatus.DEVELOPING -> WarningContainer
                    SetupStatus.INVALIDATED -> BearishContainer
                    SetupStatus.COMPLETED -> CyanContainer
                }

                JaxGlassCard(borderColor = dirColor.copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            StatusPill("${setup.direction} ${setup.symbol}", dirColor, dirBg)
                            StatusPill(setup.status.label, statusColor, statusBg)
                        }
                        Text(
                            "Score ${setup.confluenceScore.toInt()}/100",
                            style = MaterialTheme.typography.labelLarge,
                            color = BrightGold,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Entry", String.format("%.${dec}f", setup.entryPrice), MetallicGold, Modifier.weight(1f))
                        MetricBadge("Stop Loss", String.format("%.${dec}f", setup.stopLoss), BearishCrimson, Modifier.weight(1f))
                        MetricBadge("R:R", "1:${String.format("%.2f", setup.riskReward)}", BullishEmerald, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("TP1", String.format("%.${dec}f", setup.tp1), BullishEmerald, Modifier.weight(1f))
                        MetricBadge("TP2", String.format("%.${dec}f", setup.tp2), BullishEmerald, Modifier.weight(1f))
                        MetricBadge("TP3", String.format("%.${dec}f", setup.tp3), CyanQuant, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Entry Zone: ${String.format("%.${dec}f", setup.entryZoneLow)} – ${String.format("%.${dec}f", setup.entryZoneHigh)} | Session: ${setup.sessionName} | DNA: ${setup.dnaFingerprint}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(setup.explanation, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Confirming (${setup.confirmingIndicators.size}): ${setup.confirmingIndicators.take(5).joinToString()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = BullishEmerald
                    )
                    if (setup.conflictingIndicators.isNotEmpty()) {
                        Text(
                            "Dissenting (${setup.conflictingIndicators.size}): ${setup.conflictingIndicators.take(4).joinToString()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = WarningAmber
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.persistSetup(setup) },
                            colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("save_setup_btn_${setup.direction.lowercase()}")
                        ) {
                            Icon(Icons.Filled.BookmarkAdded, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save & Alert", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { onNavigate(JaxSection.AI_CHART_ANALYSER) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Plot on Chart", color = CrispWhite)
                        }
                    }
                }
            }
        }

        if (savedSetups.isNotEmpty()) {
            item {
                Spacer(Modifier.height(4.dp))
                Text("Synced Cloud Setup Archive (${savedSetups.size})", style = MaterialTheme.typography.titleMedium, color = BrightGold)
            }
            items(savedSetups.take(8), key = { it.id }) { doc ->
                JaxGlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${doc.direction} ${doc.symbol} (${doc.timeframe})", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                        StatusPill(doc.status, MetallicGold, ObsidianBlack)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Entry ${String.format("%.4f", doc.entryPrice)} | SL ${String.format("%.4f", doc.stopLoss)} | TP2 ${String.format("%.4f", doc.tp2)} | Score ${doc.confluenceScore.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText
                    )
                }
            }
        }
    }
}

@Composable
fun GlobalMarketScannerScreen(
    viewModel: JaxViewModel,
    onNavigate: (JaxSection) -> Unit
) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    var assetFilter by remember { mutableStateOf<AssetClass?>(null) }
    var dirFilter by remember { mutableStateOf<String?>(null) }

    val filtered = remember(market.scannerOpportunities, assetFilter, dirFilter) {
        market.scannerOpportunities.filter { op ->
            (assetFilter == null || op.assetClass == assetFilter) &&
                (dirFilter == null || op.direction.contains(dirFilter!!, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Global Market Scanner",
                subtitle = "Ranks live multi-asset feeds by independent confluence score & R:R"
            )
        }

        item {
            Button(
                onClick = { viewModel.runGlobalScanner() },
                enabled = !market.isScanning,
                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("run_global_scanner_btn")
            ) {
                if (market.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Scanning Live Binance & ECB Feeds...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Scan All Live Markets (${market.selectedTimeframe.code})", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = assetFilter == null,
                    onClick = { assetFilter = null },
                    label = { Text("All Assets") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MetallicGold, selectedLabelColor = ObsidianBlack)
                )
                listOf(AssetClass.CRYPTO, AssetClass.FOREX, AssetClass.METALS).forEach { ac ->
                    FilterChip(
                        selected = assetFilter == ac,
                        onClick = { assetFilter = if (assetFilter == ac) null else ac },
                        label = { Text(ac.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MetallicGold, selectedLabelColor = ObsidianBlack)
                    )
                }
                listOf("BUY", "SELL").forEach { d ->
                    FilterChip(
                        selected = dirFilter == d,
                        onClick = { dirFilter = if (dirFilter == d) null else d },
                        label = { Text(d) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanQuant, selectedLabelColor = ObsidianBlack)
                    )
                }
            }
        }

        if (filtered.isEmpty() && !market.isScanning) {
            item {
                JaxGlassCard {
                    Text("Tap 'Scan All Live Markets' to evaluate Crypto, Gold Proxy & ECB FX instruments in parallel.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        } else {
            items(filtered, key = { it.symbol }) { op ->
                val bull = op.direction.contains("BUY", true) || op.direction.contains("BULL", true)
                val c = if (bull) BullishEmerald else BearishCrimson
                val bg = if (bull) BullishContainer else BearishContainer
                JaxGlassCard(
                    modifier = Modifier.clickable {
                        viewModel.selectInstrument(SupportedInstruments.find(op.symbol))
                        onNavigate(JaxSection.AI_CHART_ANALYSER)
                    }
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("${op.symbol} · ${op.timeframe}", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                            Text("${op.assetClass.label} · ${op.setupType}", style = MaterialTheme.typography.bodySmall, color = SilverText)
                        }
                        StatusPill("${op.direction} ${op.confluenceScore.toInt()}%", c, bg)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Price: ${String.format("%.4f", op.lastPrice)}", style = MaterialTheme.typography.labelMedium, color = MetallicGold)
                        Text("R:R 1:${String.format("%.2f", op.riskReward)} · ${op.regime}", style = MaterialTheme.typography.labelSmall, color = SilverText)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(op.evidenceSummary, style = MaterialTheme.typography.bodySmall, color = CrispWhite.copy(alpha = 0.85f), maxLines = 2)
                }
            }
        }
    }
}

@Composable
fun MultiTimeframeScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Multi-Timeframe Prediction Matrix",
                subtitle = "${market.selectedInstrument.symbol} · D1 → H4 → H1 → M30 → M15 → M5 → M1 alignment"
            )
        }

        if (market.mtfMatrix.isEmpty()) {
            item {
                JaxGlassCard {
                    Text("MTF matrix refreshes automatically for live Binance feeds (or tap Refresh in Command Centre).", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        } else {
            items(market.mtfMatrix, key = { it.timeframe.code }) { row ->
                val biasColor = when {
                    row.trendBias.contains("BULL", true) -> BullishEmerald
                    row.trendBias.contains("BEAR", true) -> BearishCrimson
                    else -> WarningAmber
                }
                JaxGlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(row.timeframe.code, style = MaterialTheme.typography.titleLarge, color = BrightGold, fontWeight = FontWeight.Bold)
                        StatusPill(
                            text = if (row.dataAvailable) "${row.trendBias} (${row.confluenceScore.toInt()}%)" else "N/A",
                            color = if (row.dataAvailable) biasColor else SilverText,
                            containerColor = ObsidianBlack
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Structure", row.structureState, CrispWhite, Modifier.weight(1f))
                        MetricBadge("Momentum", row.momentumState, CyanQuant, Modifier.weight(1f))
                        MetricBadge("Volatility", row.volatilityState, WarningAmber, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun LiquidityRadarScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val dec = market.selectedInstrument.priceDecimals
    val pools = market.overlays.liquidityPoints

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Liquidity Radar",
                subtitle = "${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}) · BSL / SSL Pools & Sweep Lifecycle"
            )
        }

        if (pools.isEmpty()) {
            item {
                JaxGlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Radar, contentDescription = null, tint = MetallicGold)
                        Text("Load >= 20 candles to map swing & equal-high/low liquidity pools.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                    }
                }
            }
        } else {
            items(pools, key = { it.id }) { lp ->
                val isBsl = lp.side == "BSL"
                val sideColor = if (isBsl) BullishEmerald else BearishCrimson
                val statusColor = when (lp.status) {
                    "ACTIVE" -> CyanQuant
                    "SWEPT" -> WarningAmber
                    else -> SilverText
                }
                JaxGlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(lp.label, style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                            Text("Price: ${String.format("%.${dec}f", lp.price)} · TF ${lp.timeframe}", style = MaterialTheme.typography.labelMedium, color = MetallicGold)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(lp.side, sideColor, ObsidianBlack)
                            StatusPill(lp.status, statusColor, ObsidianBlack)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarketBehaviorDnaScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val dna = market.behaviorDna
    val dec = market.selectedInstrument.priceDecimals

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Market Behaviour DNA",
                subtitle = "${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}) · Instrument-Specific Structural Fingerprint"
            )
        }

        if (dna == null) {
            item {
                JaxGlassCard {
                    Text("Load at least 20 candles to compute Market Behaviour DNA.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        } else {
            item {
                JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sample Size: ${dna.sampleCandles} candles", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                        StatusPill(
                            text = if (dna.sufficientData) "Sufficient Sample" else "Low Sample (<60)",
                            color = if (dna.sufficientData) BullishEmerald else WarningAmber,
                            containerColor = ObsidianBlack
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("ATR(14)", String.format("%.${dec}f", dna.avgTrueRange), MetallicGold, Modifier.weight(1f))
                        MetricBadge("Avg Bar Move", "${String.format("%.2f", dna.avgSessionMovePct)}%", CyanQuant, Modifier.weight(1f))
                        MetricBadge("Avg Pullback", "${String.format("%.2f", dna.avgPullbackPct)}%", WarningAmber, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Breakout Dist", "${String.format("%.2f", dna.avgBreakoutDistancePct)}%", BullishEmerald, Modifier.weight(1f))
                        MetricBadge("Sweep Tendency", "${dna.liquiditySweepTendencyPct.toInt()}%", BearishCrimson, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = SubtleDivider)
                    Spacer(Modifier.height(8.dp))
                    Text("Dominant Regime: ${dna.dominantRegime.label}", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                    Text(dna.bestTimeframeNote, style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        }
    }
}

@Composable
fun PredictionLabScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val pred = market.predictionScenario
    val dec = market.selectedInstrument.priceDecimals

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Prediction Laboratory",
                subtitle = "${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}) · Calibrated Probabilistic Scenarios"
            )
        }

        if (pred == null) {
            item {
                JaxGlassCard {
                    Text("Load at least 30 candles to generate calibrated probabilistic forecasts.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        } else {
            item {
                JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.45f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(pred.horizonLabel, style = MaterialTheme.typography.titleMedium, color = BrightGold)
                        StatusPill(pred.modelVersion, CyanQuant, CyanContainer)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Bullish Scenario: ${pred.bullishProbabilityPct.toInt()}%  |  Bearish Scenario: ${pred.bearishProbabilityPct.toInt()}%", style = MaterialTheme.typography.labelLarge, color = CrispWhite)
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { (pred.bullishProbabilityPct / 100.0).toFloat() },
                        color = BullishEmerald,
                        trackColor = BearishCrimson,
                        modifier = Modifier.fillMaxWidth().height(10.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Expected Exp", "${String.format("%.2f", pred.expectedExpansionPct)}%", MetallicGold, Modifier.weight(1f))
                        MetricBadge("Uncertainty ±", "${String.format("%.1f", pred.uncertaintyMarginPct)}%", WarningAmber, Modifier.weight(1f))
                        MetricBadge("Hist Matches", "n=${pred.historicalMatches}", CyanQuant, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Reversal Risk", "${pred.reversalRiskPct.toInt()}%", BearishCrimson, Modifier.weight(1f))
                        MetricBadge("False Break Risk", "${pred.falseBreakoutRiskPct.toInt()}%", WarningAmber, Modifier.weight(1f))
                        MetricBadge("Vol Forecast", "${pred.volatilityForecastPct.toInt()}th pct", CrispWhite, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Upside Liq", String.format("%.${dec}f", pred.liquidityTargetUpside), BullishEmerald, Modifier.weight(1f))
                        MetricBadge("Downside Liq", String.format("%.${dec}f", pred.liquidityTargetDownside), BearishCrimson, Modifier.weight(1f))
                        MetricBadge("Invalidation", String.format("%.${dec}f", pred.invalidationLevel), WarningAmber, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(pred.notes, style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        }
    }
}
