package com.example.ui.screens

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Timeframe
import com.example.data.model.VipIndicatorCatalog
import com.example.ui.components.EquityCurveChart
import com.example.ui.components.JaxGlassCard
import com.example.ui.components.MetricBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.BearishContainer
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BullishContainer
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.CyanQuant
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverText
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.JaxViewModel

@Composable
fun BacktestingLabScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val savedBacktests by viewModel.backtests.collectAsStateWithLifecycle()

    var strategyName by remember { mutableStateOf("J.A.X. Adaptive Confluence") }
    var spreadInput by remember { mutableStateOf(market.selectedInstrument.defaultSpreadPips.toString()) }
    var commissionInput by remember { mutableStateOf("0.04") }
    var slippageInput by remember { mutableStateOf("0.5") }
    var targetRInput by remember { mutableStateOf("2.2") }
    var emaFastInput by remember { mutableStateOf("21") }
    var emaSlowInput by remember { mutableStateOf("50") }

    val bt = market.activeBacktest

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Backtesting Laboratory",
                subtitle = "${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}) · Causal Candle-by-Candle + Monte Carlo & Walk-Forward"
            )
        }

        item {
            JaxGlassCard {
                Text("Execution Friction & Model Parameters", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = strategyName,
                    onValueChange = { strategyName = it },
                    label = { Text("Strategy Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bt_strategy_name")
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = spreadInput,
                        onValueChange = { spreadInput = it },
                        label = { Text("Spread (pips)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = commissionInput,
                        onValueChange = { commissionInput = it },
                        label = { Text("Comm (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = slippageInput,
                        onValueChange = { slippageInput = it },
                        label = { Text("Slip (pips)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetRInput,
                        onValueChange = { targetRInput = it },
                        label = { Text("Target R") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = emaFastInput,
                        onValueChange = { emaFastInput = it },
                        label = { Text("Fast EMA") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = emaSlowInput,
                        onValueChange = { emaSlowInput = it },
                        label = { Text("Slow EMA") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.runBacktest(
                            strategyName = strategyName,
                            spreadPips = spreadInput.toDoubleOrNull() ?: 1.2,
                            commissionPct = commissionInput.toDoubleOrNull() ?: 0.04,
                            slippagePips = slippageInput.toDoubleOrNull() ?: 0.5,
                            targetR = (targetRInput.toDoubleOrNull() ?: 2.2).coerceIn(1.0, 6.0),
                            emaFast = (emaFastInput.toIntOrNull() ?: 21).coerceIn(5, 50),
                            emaSlow = (emaSlowInput.toIntOrNull() ?: 50).coerceIn(15, 200)
                        )
                    },
                    enabled = !market.isBacktesting,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("run_backtest_button")
                ) {
                    if (market.isBacktesting) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Running Causal Simulation...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Run Backtest (${market.candles.size} Bars)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (bt != null) {
            item {
                JaxGlassCard(borderColor = if (bt.netProfit >= 0) BullishEmerald.copy(alpha = 0.45f) else BearishCrimson.copy(alpha = 0.45f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${bt.strategyName} · ${bt.symbol}", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                        StatusPill(
                            text = if (bt.walkForwardPassed) "Walk-Forward PASS" else "Walk-Forward FAIL",
                            color = if (bt.walkForwardPassed) BullishEmerald else BearishCrimson,
                            containerColor = ObsidianBlack
                        )
                    }
                    bt.sampleSufficiencyWarning?.let { warn ->
                        Spacer(Modifier.height(6.dp))
                        Text(warn, style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                    }
                    Spacer(Modifier.height(10.dp))
                    EquityCurveChart(equityCurve = bt.equityCurve)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Trades", "${bt.totalTrades} (${bt.winningTrades}W/${bt.losingTrades}L)", CrispWhite, Modifier.weight(1f))
                        MetricBadge("Win Rate", "${bt.winRate.toInt()}%", BullishEmerald, Modifier.weight(1f))
                        MetricBadge("Profit Factor", String.format("%.2f", bt.profitFactor), MetallicGold, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Net Profit", "$${String.format("%.0f", bt.netProfit)}", if (bt.netProfit >= 0) BullishEmerald else BearishCrimson, Modifier.weight(1f))
                        MetricBadge("Max DD", "${String.format("%.2f", bt.maxDrawdownPct)}%", BearishCrimson, Modifier.weight(1f))
                        MetricBadge("Expectancy", "${String.format("%.2f", bt.expectancy)}R", CyanQuant, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Sharpe", String.format("%.2f", bt.sharpeRatio), CrispWhite, Modifier.weight(1f))
                        MetricBadge("MC P95 DD", "${String.format("%.2f", bt.monteCarloP5DrawdownPct)}%", WarningAmber, Modifier.weight(1f))
                        MetricBadge("OOS Score", "${bt.outOfSampleScore.toInt()}/100", MetallicGold, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Train WR: ${bt.walkForwardTrainWinRate.toInt()}% vs OOS Test WR: ${bt.walkForwardTestWinRate.toInt()}% · Recovery Factor: ${String.format("%.2f", bt.recoveryFactor)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText
                    )
                }
            }

            if (bt.trades.isNotEmpty()) {
                item {
                    Text("Trade-by-Trade Audit (${bt.trades.size})", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                }
                items(bt.trades.take(25), key = { it.index }) { tr ->
                    val win = tr.pnlR >= 0
                    JaxGlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("#${tr.index} ${tr.direction} · ${tr.exitReason}", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                            StatusPill(
                                "${if (win) "+" else ""}${String.format("%.2f", tr.pnlR)}R",
                                if (win) BullishEmerald else BearishCrimson,
                                if (win) BullishContainer else BearishContainer
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Entry ${String.format("%.4f", tr.entryPrice)} -> Exit ${String.format("%.4f", tr.exitPrice)} | MFE +${String.format("%.2f", tr.mfeR)}R | MAE ${String.format("%.2f", tr.maeR)}R | ${tr.regime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SilverText
                        )
                    }
                }
            }
        }

        if (savedBacktests.isNotEmpty()) {
            item {
                Text("Saved Cloud Backtest Archive (${savedBacktests.size})", style = MaterialTheme.typography.titleMedium, color = BrightGold)
            }
            items(savedBacktests.take(8), key = { it.id }) { doc ->
                JaxGlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${doc.strategyName} (${doc.symbol} ${doc.timeframe})", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                        StatusPill(if (doc.walkForwardPassed) "OOS PASS" else "OOS FAIL", if (doc.walkForwardPassed) BullishEmerald else BearishCrimson, ObsidianBlack)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${doc.totalTrades} trades | WR ${doc.winRate.toInt()}% | PF ${String.format("%.2f", doc.profitFactor)} | DD ${String.format("%.2f", doc.maxDrawdownPct)}% | Net $${String.format("%.0f", doc.netProfit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText
                    )
                }
            }
        }
    }
}

@Composable
fun StrategyOptimisationScreen(viewModel: JaxViewModel) {
    val strategies by viewModel.strategies.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("Sniper MTF Confluence") }
    var description by remember { mutableStateOf("Combines Market Structure, FVG, Order Block Precision, and Liquidity Sweep.") }
    var biasTf by remember { mutableStateOf("H4") }
    var entryTf by remember { mutableStateOf("M15") }
    var minScore by remember { mutableStateOf("65") }
    var minRr by remember { mutableStateOf("2.0") }
    val selectedIndicators = remember { mutableStateListOf("ind_01", "ind_03", "ind_08", "ind_11", "ind_17", "ind_20") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Strategy Optimisation & Builder",
                subtitle = "Compose versioned multi-indicator strategies with explicit production promotion"
            )
        }

        item {
            JaxGlassCard {
                Text("Visual Strategy Builder", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Strategy Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("strategy_name_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Execution Logic Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("Bias Timeframe", style = MaterialTheme.typography.labelMedium, color = SilverText)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("D1", "H4", "H1").forEach { tf ->
                        FilterChip(
                            selected = biasTf == tf,
                            onClick = { biasTf = tf },
                            label = { Text(tf) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = MetallicGold, selectedLabelColor = ObsidianBlack)
                        )
                    }
                }
                Text("Entry Timeframe", style = MaterialTheme.typography.labelMedium, color = SilverText)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Timeframe.entries.forEach { tf ->
                        FilterChip(
                            selected = entryTf == tf.code,
                            onClick = { entryTf = tf.code },
                            label = { Text(tf.code) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanQuant, selectedLabelColor = ObsidianBlack)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = minScore,
                        onValueChange = { minScore = it },
                        label = { Text("Min Score") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minRr,
                        onValueChange = { minRr = it },
                        label = { Text("Min R:R") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text("Select VIP Indicators (${selectedIndicators.size}/30 selected)", style = MaterialTheme.typography.labelLarge, color = BrightGold)
                Spacer(Modifier.height(4.dp))
                VipIndicatorCatalog.all30.take(12).forEach { def ->
                    val checked = selectedIndicators.contains(def.id)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked && !checked) selectedIndicators.add(def.id)
                                else if (!isChecked) selectedIndicators.remove(def.id)
                            }
                        )
                        Text("#${def.number} ${def.name}", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.saveStrategy(
                            name = name,
                            description = description,
                            indicatorIds = selectedIndicators.toList(),
                            entryTf = entryTf,
                            biasTf = biasTf,
                            minScore = (minScore.toDoubleOrNull() ?: 65.0).coerceIn(30.0, 95.0),
                            minRr = (minRr.toDoubleOrNull() ?: 2.0).coerceIn(1.0, 8.0)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_strategy_button")
                ) {
                    Text("Save Draft Strategy Version", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("Saved Versioned Strategies (${strategies.size})", style = MaterialTheme.typography.titleMedium, color = BrightGold)
        }

        items(strategies, key = { it.id }) { strat ->
            JaxGlassCard(borderColor = if (strat.isProduction) BullishEmerald.copy(alpha = 0.5f) else SubtleDivider) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${strat.name} (v${strat.version})", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                        Text("${strat.biasTimeframe} -> ${strat.entryTimeframe} · ${strat.indicatorIds.size} indicators · Min ${strat.minScore.toInt()} / ${strat.minRiskReward}R", style = MaterialTheme.typography.labelSmall, color = SilverText)
                    }
                    StatusPill(
                        text = strat.status,
                        color = if (strat.isProduction) BullishEmerald else WarningAmber,
                        containerColor = if (strat.isProduction) BullishContainer else WarningContainer
                    )
                }
                if (strat.description.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(strat.description, style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
                if (!strat.isProduction) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { viewModel.requestStrategyPromotion(strat) },
                        modifier = Modifier.height(48.dp).testTag("promote_strategy_${strat.id}")
                    ) {
                        Icon(Icons.Filled.RocketLaunch, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Request Production Promotion", color = MetallicGold)
                    }
                }
            }
        }
    }
}

@Composable
fun TradeInvestigatorScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val journal by viewModel.tradeJournal.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    val wins = journal.count { it.pnlR > 0 }
    val avgR = if (journal.isNotEmpty()) journal.map { it.pnlR }.average() else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Trade Journal & Investigator",
                subtitle = "Forensic post-trade review: MFE/MAE in R, regime tagging & execution notes",
                trailing = {
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                        modifier = Modifier.height(48.dp).testTag("add_trade_journal_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Log trade")
                        Spacer(Modifier.width(4.dp))
                        Text("Log", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBadge("Logged Trades", "${journal.size}", CrispWhite, Modifier.weight(1f))
                MetricBadge("Win Rate", if (journal.isEmpty()) "N/A" else "${(wins * 100 / journal.size)}%", BullishEmerald, Modifier.weight(1f))
                MetricBadge("Avg P&L (R)", if (journal.isEmpty()) "0.00R" else "${String.format("%.2f", avgR)}R", MetallicGold, Modifier.weight(1f))
            }
        }

        if (journal.isEmpty()) {
            item {
                JaxGlassCard {
                    Text("No forensic trade records logged yet. Tap 'Log' to audit an executed setup or signal.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        } else {
            items(journal, key = { it.id }) { tr ->
                val win = tr.pnlR >= 0
                JaxGlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${tr.direction} ${tr.symbol} (${tr.timeframe})", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                        StatusPill(
                            "${if (win) "+" else ""}${String.format("%.2f", tr.pnlR)}R",
                            if (win) BullishEmerald else BearishCrimson,
                            if (win) BullishContainer else BearishContainer
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Entry ${String.format("%.4f", tr.entryPrice)} | Exit ${String.format("%.4f", tr.exitPrice)} | SL ${String.format("%.4f", tr.stopLoss)} | TP1 ${String.format("%.4f", tr.tp1)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MetallicGold
                    )
                    Text(
                        "MFE +${String.format("%.2f", tr.mfeR)}R · MAE ${String.format("%.2f", tr.maeR)}R · Regime: ${tr.regime}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText
                    )
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(color = SubtleDivider)
                    Spacer(Modifier.height(6.dp))
                    Text(tr.notes, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                }
            }
        }
    }

    if (showAddDialog) {
        var dir by remember { mutableStateOf("BUY") }
        val lastPrice = market.candles.lastOrNull()?.close ?: 100.0
        var entryTxt by remember { mutableStateOf(String.format("%.4f", lastPrice)) }
        var exitTxt by remember { mutableStateOf(String.format("%.4f", lastPrice * 1.015)) }
        var slTxt by remember { mutableStateOf(String.format("%.4f", lastPrice * 0.99)) }
        var tp1Txt by remember { mutableStateOf(String.format("%.4f", lastPrice * 1.02)) }
        var pnlRTxt by remember { mutableStateOf("1.50") }
        var notesTxt by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Log Forensic Trade Review", color = BrightGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("BUY", "SELL").forEach { d ->
                            FilterChip(
                                selected = dir == d,
                                onClick = { dir = d },
                                label = { Text(d) }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = entryTxt, onValueChange = { entryTxt = it }, label = { Text("Entry") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = exitTxt, onValueChange = { exitTxt = it }, label = { Text("Exit") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = slTxt, onValueChange = { slTxt = it }, label = { Text("Stop Loss") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = pnlRTxt, onValueChange = { pnlRTxt = it }, label = { Text("P&L (R)") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(
                        value = notesTxt,
                        onValueChange = { notesTxt = it },
                        label = { Text("Forensic Notes (Mistake / Confluence)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addTradeJournalEntry(
                            symbol = market.selectedInstrument.symbol,
                            tf = market.selectedTimeframe.code,
                            direction = dir,
                            entry = entryTxt.toDoubleOrNull() ?: lastPrice,
                            exit = exitTxt.toDoubleOrNull() ?: lastPrice,
                            sl = slTxt.toDoubleOrNull() ?: lastPrice * 0.99,
                            tp1 = tp1Txt.toDoubleOrNull() ?: lastPrice * 1.02,
                            pnlR = pnlRTxt.toDoubleOrNull() ?: 0.0,
                            notes = notesTxt
                        )
                        showAddDialog = false
                    },
                    modifier = Modifier.testTag("confirm_log_trade_btn")
                ) {
                    Text("Save Record", color = BullishEmerald)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancel", color = SilverText) }
            }
        )
    }
}
