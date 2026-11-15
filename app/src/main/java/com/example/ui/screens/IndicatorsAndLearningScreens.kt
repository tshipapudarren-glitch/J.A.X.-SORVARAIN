package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.IndicatorCategory
import com.example.data.model.IndicatorConfigDoc
import com.example.data.model.IndicatorEvaluationResult
import com.example.data.model.SignalDirection
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
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanQuant
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverText
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.JaxViewModel

@Composable
fun VipIndicatorsScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val configs by viewModel.indicatorConfigs.collectAsStateWithLifecycle()
    var selectedCategory by remember { mutableStateOf<IndicatorCategory?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var editingPair by remember { mutableStateOf<Pair<IndicatorEvaluationResult, IndicatorConfigDoc?>?>(null) }

    val configByNum = remember(configs) { configs.associateBy { it.indicatorNumber } }
    val filtered = remember(market.evaluations, selectedCategory, searchQuery) {
        market.evaluations.filter { eval ->
            (selectedCategory == null || eval.definition.category == selectedCategory) &&
                (searchQuery.isBlank() || eval.definition.name.contains(searchQuery, ignoreCase = true) ||
                    eval.definition.capability.contains(searchQuery, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "30 Self-Learning VIP Indicators",
                subtitle = "Independently configurable · ${market.evaluations.count { it.enabled }}/${market.evaluations.size} active on ${market.selectedInstrument.symbol} ${market.selectedTimeframe.code}"
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search 30 indicators (Trend, Liquidity, FVG, Order Block...)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All 30") }
                )
                IndicatorCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat.title.substringAfter("— ")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldContainer,
                            selectedLabelColor = BrightGold
                        )
                    )
                }
            }
        }

        items(filtered, key = { it.definition.id }) { eval ->
            val cfg = configByNum[eval.definition.number]
            val dirColor = when (eval.direction) {
                SignalDirection.BULLISH -> BullishEmerald
                SignalDirection.BEARISH -> BearishCrimson
                SignalDirection.NEUTRAL -> SilverText
            }
            val dirBg = when (eval.direction) {
                SignalDirection.BULLISH -> BullishContainer
                SignalDirection.BEARISH -> BearishContainer
                SignalDirection.NEUTRAL -> GlassBorder.copy(alpha = 0.25f)
            }
            JaxGlassCard(
                borderColor = if (eval.decayDetected) WarningAmber else GlassBorder,
                modifier = Modifier
                    .clickable { editingPair = eval to cfg }
                    .testTag("indicator_card_${eval.definition.number}")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "#${"%02d".format(eval.definition.number)} ${eval.definition.name}",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispWhite
                        )
                        Text(
                            eval.definition.capability,
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverText
                        )
                    }
                    StatusPill(
                        text = if (eval.enabled) "${eval.direction.label} ${eval.score.toInt()}" else "DISABLED",
                        color = if (eval.enabled) dirColor else SilverText,
                        containerColor = dirBg
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(eval.rationale, style = MaterialTheme.typography.bodySmall, color = CyanQuant)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Ver ${eval.activeVersion} · Samples: ${eval.sampleSize}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SilverText
                    )
                    Text(
                        eval.historicalWinRate?.let { "Hist WR ${it.toInt()}% · PF ${String.format("%.2f", eval.profitFactor ?: 1.0)}" } ?: "Insufficient history",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrightGold
                    )
                }
                if (eval.decayDetected) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "DECAY ALARM: Recent win rate dropped below historical baseline — inspect in Self-Learning Engine.",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                }
            }
        }
    }

    editingPair?.let { (eval, cfg) ->
        IndicatorConfigDialog(
            eval = eval,
            config = cfg,
            onDismiss = { editingPair = null },
            onSave = { enabled, sens, lookback, conf, overlay, alerts ->
                if (cfg != null) {
                    viewModel.updateIndicatorConfig(cfg, enabled, sens, lookback, conf, overlay, alerts)
                }
                editingPair = null
            },
            onRunLearning = {
                viewModel.runSelfLearningCycle(eval.definition.number)
                editingPair = null
            }
        )
    }
}

@Composable
private fun IndicatorConfigDialog(
    eval: IndicatorEvaluationResult,
    config: IndicatorConfigDoc?,
    onDismiss: () -> Unit,
    onSave: (Boolean, Double, Int, Double, Boolean, Boolean) -> Unit,
    onRunLearning: () -> Unit
) {
    var enabled by remember { mutableStateOf(config?.enabled ?: eval.enabled) }
    var sensitivity by remember { mutableFloatStateOf((config?.sensitivity ?: 1.0).toFloat()) }
    var lookback by remember { mutableFloatStateOf((config?.lookbackPeriod ?: eval.definition.defaultLookback).toFloat()) }
    var minConf by remember { mutableFloatStateOf((config?.minConfidence ?: eval.definition.defaultMinConfidence).toFloat()) }
    var overlay by remember { mutableStateOf(config?.showChartOverlay ?: true) }
    var alerts by remember { mutableStateOf(config?.alertsEnabled ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("#${eval.definition.number} ${eval.definition.name}", color = BrightGold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(eval.definition.capability, style = MaterialTheme.typography.bodySmall, color = SilverText)
                Text("Invalidation: ${eval.invalidationHint}", style = MaterialTheme.typography.labelSmall, color = CyanQuant)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Enabled", color = CrispWhite)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Chart Overlay", color = CrispWhite)
                    Switch(checked = overlay, onCheckedChange = { overlay = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Signal Alerts", color = CrispWhite)
                    Switch(checked = alerts, onCheckedChange = { alerts = it })
                }
                Text("Sensitivity: ${String.format("%.2f", sensitivity)}x", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                Slider(
                    value = sensitivity,
                    onValueChange = { sensitivity = it },
                    valueRange = 0.5f..2.5f,
                    colors = SliderDefaults.colors(thumbColor = MetallicGold, activeTrackColor = MetallicGold)
                )
                Text("Lookback Period: ${lookback.toInt()} bars", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                Slider(
                    value = lookback,
                    onValueChange = { lookback = it },
                    valueRange = 8f..100f,
                    colors = SliderDefaults.colors(thumbColor = MetallicGold, activeTrackColor = MetallicGold)
                )
                Text("Min Confidence: ${minConf.toInt()}%", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                Slider(
                    value = minConf,
                    onValueChange = { minConf = it },
                    valueRange = 40f..90f,
                    colors = SliderDefaults.colors(thumbColor = MetallicGold, activeTrackColor = MetallicGold)
                )
                OutlinedButton(onClick = onRunLearning, modifier = Modifier.fillMaxWidth()) {
                    Text("Run Walk-Forward Optimization", color = CyanQuant)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(enabled, sensitivity.toDouble(), lookback.toInt(), minConf.toDouble(), overlay, alerts) },
                colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack)
            ) { Text("Save Config") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun SelfLearningEngineScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val jobs by viewModel.learningJobs.collectAsStateWithLifecycle()
    var selectedInd by remember { mutableStateOf(1) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Self-Learning Engine",
                subtitle = "Shared causal walk-forward pipeline · Never uses future candles · Approval-gated deployment"
            )
        }

        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
                Text("Trigger Walk-Forward Experiment", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Splits loaded ${market.candles.size} bars for ${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}) into 70% training and 30% unseen out-of-sample validation.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverText
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    market.evaluations.take(12).forEach { eval ->
                        FilterChip(
                            selected = selectedInd == eval.definition.number,
                            onClick = { selectedInd = eval.definition.number },
                            label = { Text("#${eval.definition.number} ${eval.definition.name.take(14)}") }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.runSelfLearningCycle(selectedInd) },
                    enabled = !market.isLearningRunning,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("run_learning_job_btn")
                ) {
                    if (market.isLearningRunning) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    }
                    Text("Execute Walk-Forward Learning Job", fontWeight = FontWeight.Bold)
                }
            }
        }

        items(jobs, key = { it.id }) { job ->
            JaxGlassCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${job.indicatorName} (${job.modelVersion})", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                    StatusPill(
                        text = job.approvalStatus,
                        color = if (job.approvalStatus == "APPROVED") BullishEmerald else if (job.approvalStatus == "PENDING_APPROVAL") WarningAmber else SilverText,
                        containerColor = if (job.approvalStatus == "APPROVED") BullishContainer else WarningContainer
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text("${job.symbol} · ${job.timeframe} · Train ${job.trainingSamples} / Val ${job.validationSamples} bars", style = MaterialTheme.typography.labelSmall, color = CyanQuant)
                Text(job.proposedChangeSummary, style = MaterialTheme.typography.bodySmall, color = SilverText)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Base WR: ${job.previousWinRate.toInt()}% (${String.format("%.2f", job.previousExpectancy)}R)", style = MaterialTheme.typography.labelSmall, color = SilverText)
                    Text("Cand WR: ${job.candidateWinRate.toInt()}% (${String.format("%.2f", job.candidateExpectancy)}R)", style = MaterialTheme.typography.labelSmall, color = BullishEmerald)
                }
                if (job.approvalStatus == "PENDING_APPROVAL") {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { viewModel.approveOrRejectLearningJob(job, false) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Reject", color = BearishCrimson) }
                        Button(
                            onClick = { viewModel.approveOrRejectLearningJob(job, true) },
                            colors = ButtonDefaults.buttonColors(containerColor = BullishEmerald, contentColor = ObsidianBlack),
                            modifier = Modifier.weight(1f)
                        ) { Text("Approve Promotion", fontWeight = FontWeight.Bold) }
                    }
                } else if (job.approvalStatus == "APPROVED") {
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { viewModel.rollbackIndicatorVersion(job.indicatorNumber) },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Rollback to v1.0.0", color = WarningAmber) }
                }
            }
        }
    }
}

@Composable
fun IndicatorPerformanceScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val sorted = remember(market.evaluations) {
        market.evaluations.sortedByDescending { it.historicalWinRate ?: 0.0 }
    }
    var compareA by remember { mutableStateOf(1) }
    var compareB by remember { mutableStateOf(11) }

    val evalA = market.evaluations.firstOrNull { it.definition.number == compareA }
    val evalB = market.evaluations.firstOrNull { it.definition.number == compareB }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Indicator Performance Centre",
                subtitle = "Head-to-head causal statistics & sample size transparency"
            )
        }
        if (evalA != null && evalB != null) {
            item {
                JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.45f)) {
                    Text("Side-by-Side Comparison", style = MaterialTheme.typography.titleMedium, color = CyanQuant)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge(
                            label = "#${evalA.definition.number} ${evalA.definition.name.take(12)}",
                            value = evalA.historicalWinRate?.let { "${it.toInt()}% (n=${evalA.sampleSize})" } ?: "N/A",
                            accent = BrightGold,
                            modifier = Modifier.weight(1f)
                        )
                        MetricBadge(
                            label = "#${evalB.definition.number} ${evalB.definition.name.take(12)}",
                            value = evalB.historicalWinRate?.let { "${it.toInt()}% (n=${evalB.sampleSize})" } ?: "N/A",
                            accent = BullishEmerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Tap any two rows below to compare them.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        }
        items(sorted, key = { it.definition.id }) { e ->
            JaxGlassCard(
                modifier = Modifier.clickable {
                    compareB = compareA
                    compareA = e.definition.number
                }
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("#${e.definition.number} ${e.definition.name}", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                    Text(
                        e.historicalWinRate?.let { "${it.toInt()}% WR" } ?: "—",
                        style = MaterialTheme.typography.labelLarge,
                        color = if ((e.historicalWinRate ?: 0.0) >= 55.0) BullishEmerald else WarningAmber
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Samples: ${e.sampleSize} ${if (e.sampleSize < 15) "(Low sample)" else ""}", style = MaterialTheme.typography.labelSmall, color = SilverText)
                    Text("PF: ${String.format("%.2f", e.profitFactor ?: 0.0)} · Avg R: ${String.format("%.2f", e.avgR ?: 0.0)}", style = MaterialTheme.typography.labelSmall, color = CyanQuant)
                }
            }
        }
    }
}

@Composable
fun LearningHistoryScreen(viewModel: JaxViewModel) {
    val jobs by viewModel.learningJobs.collectAsStateWithLifecycle()
    val configs by viewModel.indicatorConfigs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Learning History & Model Versions",
                subtitle = "Traceable indicator parameter versions & instant rollback"
            )
        }
        item {
            JaxGlassCard {
                Text("Production Indicator Versions (${configs.count { it.activeVersion != "v1.0.0" }} customized)", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(6.dp))
                configs.filter { it.activeVersion != "v1.0.0" || it.learningStatus != "IDLE" }.forEach { cfg ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("#${cfg.indicatorNumber} ${cfg.name}", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                            Text("Active: ${cfg.activeVersion} · Status: ${cfg.learningStatus}", style = MaterialTheme.typography.labelSmall, color = SilverText)
                        }
                        TextButton(onClick = { viewModel.rollbackIndicatorVersion(cfg.indicatorNumber) }) {
                            Text("Rollback", color = WarningAmber)
                        }
                    }
                }
                if (configs.none { it.activeVersion != "v1.0.0" || it.learningStatus != "IDLE" }) {
                    Text("All 30 indicators are currently on validated baseline v1.0.0.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        }
        items(jobs, key = { it.id }) { job ->
            JaxGlassCard {
                Text("${job.indicatorName} · ${job.modelVersion} (${job.approvalStatus})", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                Text("${job.symbol} ${job.timeframe} · ${job.proposedChangeSummary}", style = MaterialTheme.typography.bodySmall, color = SilverText)
            }
        }
    }
}
