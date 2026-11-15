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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.Switch
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
import com.example.data.model.AiProviderType
import com.example.data.model.JaxCoreRoutingMode
import com.example.data.model.JaxSpecialistAgents
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
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverText
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.JaxForgeViewModel
import com.example.ui.viewmodel.JaxViewModel

@Composable
fun JaxCoreCouncilScreen(
    forgeViewModel: JaxForgeViewModel,
    jaxViewModel: JaxViewModel
) {
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val councilHistory by forgeViewModel.aiCouncilRuns.collectAsStateWithLifecycle()
    val market by jaxViewModel.marketState.collectAsStateWithLifecycle()

    var prompt by remember {
        mutableStateOf("Debate current ${market.selectedInstrument.symbol} ${market.selectedTimeframe.code} market structure, setup invalidation, and MQL5 vs Pine v6 indicator risk.")
    }

    val liveMarketContext = remember(market.selectedInstrument, market.selectedTimeframe, market.regime, market.generatedSetups) {
        val top = market.generatedSetups.firstOrNull()
        "Instrument=${market.selectedInstrument.symbol}, Timeframe=${market.selectedTimeframe.code}, " +
            "Regime=${market.regime.regime.name} (${market.regime.bias}), ATR=${String.format("%.4f", market.regime.atrValue)}, " +
            "TopSetup=${top?.direction ?: "NONE"} Entry=${top?.entryPrice ?: 0.0} SL=${top?.stopLoss ?: 0.0} TP2=${top?.tp2 ?: 0.0}"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "JAX-CORE & AI Council",
                subtitle = "12 Specialist Agents · Auto Router / Gemini / DeepSeek / Dual-AI Debate & Verification"
            )
        }

        // 4 Routing Mode Chips
        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text("JAX-CORE Routing Mode", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JaxCoreRoutingMode.entries.forEach { mode ->
                        FilterChip(
                            selected = ui.routingMode == mode,
                            onClick = { forgeViewModel.setRoutingMode(mode) },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = BrightGold),
                            modifier = Modifier.testTag("jax_core_mode_${mode.code}")
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Active Specialist Agent (12 Agents)", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    JaxSpecialistAgents.all12.forEach { agent ->
                        FilterChip(
                            selected = ui.selectedAgent.id == agent.id,
                            onClick = { forgeViewModel.selectSpecialistAgent(agent) },
                            label = { Text(agent.name) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanContainer, selectedLabelColor = CyanQuant)
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${ui.selectedAgent.name} (${ui.selectedAgent.primaryProvider}): ${ui.selectedAgent.roleSummary} -> ${ui.selectedAgent.outputFormat}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverText
                )
            }
        }

        // Prompt Input & Invoke
        item {
            JaxGlassCard {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Council / Specialist Prompt") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth().testTag("jax_core_prompt_input")
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { forgeViewModel.runCouncilPrompt(prompt, liveMarketContext) },
                    enabled = !ui.isCouncilRunning && prompt.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("jax_core_run_btn")
                ) {
                    if (ui.isCouncilRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Convening JAX-CORE Council...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Execute ${ui.routingMode.label}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Latest Council Debate & Verification Verdict
        ui.latestCouncilResult?.let { res ->
            item {
                JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.5f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Council Verdict · ${res.agentName}", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                        StatusPill(res.verificationStatus, BullishEmerald, BullishContainer)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Gemini (Firebase AI Logic):", style = MaterialTheme.typography.labelMedium, color = CyanQuant)
                    Text(res.geminiResponse, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = SubtleDivider)
                    Spacer(Modifier.height(8.dp))
                    Text("DeepSeek API:", style = MaterialTheme.typography.labelMedium, color = BrightGold)
                    Text(res.deepseekResponse, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = SubtleDivider)
                    Spacer(Modifier.height(8.dp))
                    Text("Agreements: ${res.agreements}", style = MaterialTheme.typography.bodySmall, color = BullishEmerald)
                    Text("Disagreements: ${res.disagreements}", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                    Text("Identified Errors: ${res.identifiedErrors}", style = MaterialTheme.typography.bodySmall, color = BearishCrimson)
                    Spacer(Modifier.height(6.dp))
                    Text("Synthesized Recommendation:", style = MaterialTheme.typography.labelMedium, color = BrightGold)
                    Text(res.finalRecommendation, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                }
            }
        }

        if (councilHistory.isNotEmpty()) {
            item {
                Text("Saved AI Council Sessions (${councilHistory.size})", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
            }
            items(councilHistory.take(8), key = { it.id }) { run ->
                JaxGlassCard {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${run.mode} · ${run.usedProviders.joinToString(" + ")}", style = MaterialTheme.typography.labelMedium, color = BrightGold)
                    }
                    Text(run.prompt, style = MaterialTheme.typography.bodySmall, color = CrispWhite, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    Text(run.verifiedSummary.take(240), style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        }
    }
}

@Composable
fun DualAiApiManagementScreen(
    forgeViewModel: JaxForgeViewModel
) {
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val usageLogs by forgeViewModel.aiUsageLogs.collectAsStateWithLifecycle()

    val geminiTel = ui.providerTelemetry.firstOrNull { it.provider == AiProviderType.GEMINI }
    val deepseekTel = ui.providerTelemetry.firstOrNull { it.provider == AiProviderType.DEEPSEEK }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Gemini + DeepSeek API Management",
                subtitle = "Live connection health, model discovery, latency, token telemetry, rate limits & spend guardrails"
            )
        }

        // Gemini Card (Firebase AI Logic + App Check)
        if (geminiTel != null) {
            item {
                JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.5f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(geminiTel.provider.label, style = MaterialTheme.typography.titleMedium, color = CyanQuant)
                            Text(geminiTel.statusLabel, style = MaterialTheme.typography.bodySmall, color = SilverText)
                        }
                        Switch(
                            checked = geminiTel.enabled,
                            onCheckedChange = { forgeViewModel.toggleProviderEnabled(AiProviderType.GEMINI, it) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Managed securely via Firebase AI Logic & App Check (no Gemini API key needed in APK).", style = MaterialTheme.typography.bodySmall, color = BullishEmerald)
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Requests", "${geminiTel.requestCount}", CyanQuant, Modifier.weight(1f))
                        MetricBadge("Latency", "${geminiTel.lastLatencyMs ?: 0} ms", CrispWhite, Modifier.weight(1f))
                        MetricBadge("Errors", "${geminiTel.errorCount}", if (geminiTel.errorCount > 0) BearishCrimson else BullishEmerald, Modifier.weight(1f))
                    }
                }
            }
        }

        // DeepSeek Card (Secrets panel -> BuildConfig.DEEPSEEK_API_KEY)
        if (deepseekTel != null) {
            item {
                JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(deepseekTel.provider.label, style = MaterialTheme.typography.titleMedium, color = BrightGold)
                            Text(deepseekTel.statusLabel, style = MaterialTheme.typography.bodySmall, color = SilverText)
                        }
                        Switch(
                            checked = deepseekTel.enabled,
                            onCheckedChange = { forgeViewModel.toggleProviderEnabled(AiProviderType.DEEPSEEK, it) }
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    StatusPill(
                        if (deepseekTel.connected) "CONNECTED" else "CONFIGURE IN AI STUDIO SECRETS",
                        if (deepseekTel.connected) BullishEmerald else WarningAmber,
                        if (deepseekTel.connected) BullishContainer else WarningContainer
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Security Policy: Set DEEPSEEK_API_KEY in the AI Studio Secrets panel (injected cleanly via BuildConfig). Never paste secret keys inside app forms.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverText
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { forgeViewModel.testAndDiscoverDeepSeek() },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("deepseek_discover_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = BrightGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Test Connection & Discover Models", color = BrightGold)
                    }
                    if (deepseekTel.discoveredModels.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            deepseekTel.discoveredModels.forEach { m ->
                                FilterChip(
                                    selected = deepseekTel.selectedModel == m,
                                    onClick = { forgeViewModel.selectDeepSeekModel(m) },
                                    label = { Text(m) }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Requests", "${deepseekTel.requestCount}", BrightGold, Modifier.weight(1f))
                        MetricBadge("Tokens", "${deepseekTel.totalTokens}", CyanQuant, Modifier.weight(1f))
                        MetricBadge("Est. Cost", "$${String.format("%.4f", deepseekTel.estimatedCostUsd)}", BullishEmerald, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Latency", "${deepseekTel.lastLatencyMs ?: 0} ms", CrispWhite, Modifier.weight(1f))
                        MetricBadge("Rate Limits", "${deepseekTel.rateLimitEvents}", if (deepseekTel.rateLimitEvents > 0) WarningAmber else SilverText, Modifier.weight(1f))
                        MetricBadge("Errors", "${deepseekTel.errorCount}", if (deepseekTel.errorCount > 0) BearishCrimson else BullishEmerald, Modifier.weight(1f))
                    }
                }
            }
        }

        item {
            Text("Recent AI Invocation Audit Logs (${usageLogs.size})", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        }

        items(usageLogs.take(15), key = { it.id }) { log ->
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Hub, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(16.dp))
                        Text("${log.provider} · ${log.modelId} (${log.agentName})", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                    }
                    StatusPill(
                        if (log.success) "${log.latencyMs}ms" else "ERR",
                        if (log.success) BullishEmerald else BearishCrimson,
                        if (log.success) BullishContainer else BearishContainer
                    )
                }
                Text("Mode: ${log.mode} · Tokens: ${log.totalTokens} · Est: $${String.format("%.5f", log.estimatedCostUsd)}", style = MaterialTheme.typography.bodySmall, color = SilverText)
                if (!log.success && log.errorMessage.isNotBlank()) {
                    Text(log.errorMessage, style = MaterialTheme.typography.labelSmall, color = BearishCrimson)
                }
            }
        }
    }
}
