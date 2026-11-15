package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.engine.SetupGeneratorEngine
import com.example.ui.components.ConnectionDot
import com.example.ui.components.JaxGlassCard
import com.example.ui.components.MetricBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.providerColor
import com.example.ui.theme.BearishContainer
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BullishContainer
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.CyanContainer
import com.example.ui.theme.CyanQuant
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverText
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.JaxViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

@Composable
fun RiskIntelligenceScreen(viewModel: JaxViewModel) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val market by viewModel.marketState.collectAsStateWithLifecycle()

    var balanceStr by remember(profile?.accountBalance) { mutableStateOf((profile?.accountBalance ?: 100000.0).toString()) }
    var riskPctStr by remember(profile?.riskPerTradePct) { mutableStateOf((profile?.riskPerTradePct ?: 1.0).toString()) }
    var dailyLossStr by remember(profile?.dailyLossLimitPct) { mutableStateOf((profile?.dailyLossLimitPct ?: 3.0).toString()) }
    var weeklyLossStr by remember(profile?.weeklyLossLimitPct) { mutableStateOf((profile?.weeklyLossLimitPct ?: 6.0).toString()) }
    var maxDdStr by remember(profile?.maxDrawdownPct) { mutableStateOf((profile?.maxDrawdownPct ?: 12.0).toString()) }

    val topSetup = market.generatedSetups.firstOrNull()
    var calcEntryStr by remember(topSetup?.entryPrice) { mutableStateOf(topSetup?.entryPrice?.let { String.format("%.4f", it) } ?: "100.0") }
    var calcSlStr by remember(topSetup?.stopLoss) { mutableStateOf(topSetup?.stopLoss?.let { String.format("%.4f", it) } ?: "98.5") }

    val bal = balanceStr.toDoubleOrNull() ?: 100000.0
    val riskPct = riskPctStr.toDoubleOrNull() ?: 1.0
    val riskAmountUsd = bal * (riskPct / 100.0)
    val entryVal = calcEntryStr.toDoubleOrNull() ?: 100.0
    val slVal = calcSlStr.toDoubleOrNull() ?: 98.5
    val stopDist = abs(entryVal - slVal).coerceAtLeast(1e-6)
    val unitsRecommended = riskAmountUsd / stopDist
    val liveEnabled = profile?.liveTradingEnabled == true

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Risk & Capital Intelligence",
                subtitle = "Position sizing, drawdown circuit breakers & mandatory approval-gated live execution"
            )
        }

        item {
            JaxGlassCard(borderColor = if (liveEnabled) WarningAmber else BearishCrimson.copy(alpha = 0.55f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Live Execution Gate", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                        Text(
                            if (liveEnabled) "LIVE DISPATCH UNLOCKED (Explicit Approval Active)" else "LOCKED OFF (Safe Paper/Signal Mode)",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (liveEnabled) WarningAmber else BullishEmerald
                        )
                    }
                    StatusPill(
                        if (liveEnabled) "ARMED" else "SAFE LOCK",
                        if (liveEnabled) WarningAmber else BullishEmerald,
                        ObsidianBlack
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { viewModel.triggerEmergencyStop() },
                        colors = ButtonDefaults.buttonColors(containerColor = BearishCrimson, contentColor = CrispWhite),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("emergency_stop_btn")
                    ) {
                        Icon(Icons.Filled.Dangerous, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("EMERGENCY STOP", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { viewModel.requestLiveTradingActivation() },
                        modifier = Modifier.weight(1f).height(48.dp).testTag("request_live_trading_btn")
                    ) {
                        Text("Request Live Approval", color = MetallicGold)
                    }
                }
            }
        }

        item {
            JaxGlassCard {
                Text("Live Position Size Calculator", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = calcEntryStr,
                        onValueChange = { calcEntryStr = it },
                        label = { Text("Entry Price") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = calcSlStr,
                        onValueChange = { calcSlStr = it },
                        label = { Text("Stop Loss") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge("Risk Amount", "$${String.format("%.2f", riskAmountUsd)}", WarningAmber, Modifier.weight(1f))
                    MetricBadge("Position Size", String.format("%.3f units", unitsRecommended), BullishEmerald, Modifier.weight(1f))
                    MetricBadge("Stop Distance", String.format("%.4f", stopDist), CyanQuant, Modifier.weight(1f))
                }
            }
        }

        item {
            JaxGlassCard {
                Text("Capital Guardrails (Synced & Audited)", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = balanceStr,
                        onValueChange = { balanceStr = it },
                        label = { Text("Account ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = riskPctStr,
                        onValueChange = { riskPctStr = it },
                        label = { Text("Risk/Trade %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dailyLossStr,
                        onValueChange = { dailyLossStr = it },
                        label = { Text("Daily Max %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = weeklyLossStr,
                        onValueChange = { weeklyLossStr = it },
                        label = { Text("Weekly Max %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxDdStr,
                        onValueChange = { maxDdStr = it },
                        label = { Text("Max DD %") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        viewModel.updateRiskLimits(
                            balance = bal.coerceAtLeast(100.0),
                            riskPct = riskPct.coerceIn(0.1, 10.0),
                            dailyPct = (dailyLossStr.toDoubleOrNull() ?: 3.0).coerceIn(0.5, 25.0),
                            weeklyPct = (weeklyLossStr.toDoubleOrNull() ?: 6.0).coerceIn(1.0, 40.0),
                            maxDdPct = (maxDdStr.toDoubleOrNull() ?: 12.0).coerceIn(2.0, 50.0)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("save_risk_limits_btn")
                ) {
                    Text("Save Risk Guardrails", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun TradingSessionsScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val dec = market.selectedInstrument.priceDecimals

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Trading Sessions & Killzones",
                subtitle = "${market.selectedInstrument.symbol} · Asian, London & New York UTC liquidity cycles"
            )
        }

        items(market.sessions, key = { it.name }) { sess ->
            JaxGlassCard(borderColor = if (sess.isActiveNow) BullishEmerald.copy(alpha = 0.55f) else GlassBorder) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(sess.name, style = MaterialTheme.typography.titleMedium, color = BrightGold)
                        Text(sess.utcWindow, style = MaterialTheme.typography.labelSmall, color = SilverText)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (sess.sweepDetected) StatusPill("Sweep Detected", WarningAmber, WarningContainer)
                        StatusPill(
                            if (sess.isActiveNow) "ACTIVE NOW" else "CLOSED",
                            if (sess.isActiveNow) BullishEmerald else SilverText,
                            ObsidianBlack
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge("Session High", String.format("%.${dec}f", sess.sessionHigh), BullishEmerald, Modifier.weight(1f))
                    MetricBadge("Session Low", String.format("%.${dec}f", sess.sessionLow), BearishCrimson, Modifier.weight(1f))
                    MetricBadge("Bias", sess.directionBias, MetallicGold, Modifier.weight(1f))
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Opening Range: ${String.format("%.${dec}f", sess.openingRangeLow)} – ${String.format("%.${dec}f", sess.openingRangeHigh)} · Avg Range ${String.format("%.2f", sess.avgRangePct)}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = SilverText
                )
            }
        }
    }
}

@Composable
fun EconomicNewsScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Economic News Intelligence",
                subtitle = "Macro event awareness & high-impact No-Trade Window circuit breaker"
            )
        }

        item {
            JaxGlassCard(borderColor = if (market.noTradeNewsWindowActive) WarningAmber else GlassBorder) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("No-Trade Macro Guard", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                        Text("Suppresses automated setup confirmations during high-impact macro releases.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                    }
                    Switch(
                        checked = market.noTradeNewsWindowActive,
                        onCheckedChange = { viewModel.toggleNoTradeNewsWindow(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianBlack, checkedTrackColor = WarningAmber),
                        modifier = Modifier.testTag("no_trade_news_switch")
                    )
                }
            }
        }

        items(viewModel.economicEvents, key = { it.id }) { ev ->
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${ev.currency} · ${ev.title}", style = MaterialTheme.typography.titleSmall, color = CrispWhite, modifier = Modifier.weight(1f))
                    StatusPill(ev.impact, if (ev.impact == "HIGH") BearishCrimson else WarningAmber, ObsidianBlack)
                }
                Spacer(Modifier.height(4.dp))
                Text(ev.scheduledUtc, style = MaterialTheme.typography.labelSmall, color = MetallicGold)
                Text(ev.expectedVolatilityNote, style = MaterialTheme.typography.bodySmall, color = SilverText)
                Text("Feed Status: ${ev.sourceStatus}", style = MaterialTheme.typography.labelSmall, color = WarningAmber)
            }
        }
    }
}

@Composable
fun Mt5IntegrationScreen(viewModel: JaxViewModel) {
    val integrations by viewModel.integrations.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val mt5Doc = integrations.firstOrNull { it.type == "MT5_BRIDGE" }
    var endpointUrl by remember(mt5Doc?.endpointUrl) { mutableStateOf(mt5Doc?.endpointUrl ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "MetaTrader 5 EA Bridge",
                subtitle = "Connect an external MT5 Expert Advisor HTTP bridge for XAUUSD, US30, NAS100 & FX ticks"
            )
        }

        item {
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Bridge Status", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                    StatusPill(mt5Doc?.status ?: "DISCONNECTED", if (mt5Doc?.status == "CONNECTED") BullishEmerald else WarningAmber, ObsidianBlack)
                }
                Spacer(Modifier.height(6.dp))
                Text(mt5Doc?.lastSyncSummary ?: "No EA bridge configured. Enter your HTTPS bridge URL (e.g. https://your-ea-host.example/api).", style = MaterialTheme.typography.bodySmall, color = SilverText)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it },
                    label = { Text("MT5 EA Bridge Endpoint URL") },
                    placeholder = { Text("https://ea-bridge.yourdomain.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("mt5_url_input")
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.testAndSaveIntegration("mt5_bridge", "MT5_BRIDGE", "MetaTrader 5 EA Bridge", endpointUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("mt5_test_save_btn")
                ) {
                    Text("Test & Save MT5 Bridge", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            JaxGlassCard {
                Text("EA Protocol Contract", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "• GET /health -> HTTP 200 OK\n" +
                        "• GET /candles?symbol=XAUUSD&tf=H1&limit=200 -> JSON [{time,open,high,low,close,volume}]\n" +
                        "• POST /orders -> Disabled unless Risk Intelligence Live Gate is explicitly approved (currently ${if (profile?.liveTradingEnabled == true) "ARMED" else "LOCKED OFF"}).",
                    style = MaterialTheme.typography.bodySmall,
                    color = CrispWhite
                )
            }
        }
    }
}

@Composable
fun TradingViewIntegrationScreen(viewModel: JaxViewModel) {
    val context = LocalContext.current
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val integrations by viewModel.integrations.collectAsStateWithLifecycle()
    val tvDoc = integrations.firstOrNull { it.type == "TRADINGVIEW_WEBHOOK" }
    var webhookUrl by remember(tvDoc?.endpointUrl) { mutableStateOf(tvDoc?.endpointUrl ?: "") }

    val pineScript = remember(market.selectedInstrument.symbol, market.selectedTimeframe.code) {
        SetupGeneratorEngine.generatePineScriptV6(market.selectedInstrument.symbol, market.selectedTimeframe.code, 65.0, 2.0)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "TradingView & Pine Script v6",
                subtitle = "Export synchronized Pine v6 alert indicator & manage TradingView webhook relay"
            )
        }

        item {
            JaxGlassCard {
                Text("Webhook Relay Endpoint", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = webhookUrl,
                    onValueChange = { webhookUrl = it },
                    label = { Text("TradingView Webhook Relay URL") },
                    placeholder = { Text("https://webhook-relay.yourdomain.com/tv") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("tv_webhook_input")
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { viewModel.testAndSaveIntegration("tv_webhook", "TRADINGVIEW_WEBHOOK", "TradingView Webhook Relay", webhookUrl) },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("tv_save_btn")
                ) {
                    Text("Verify & Save Webhook", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.45f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Pine Script v6 (${market.selectedInstrument.symbol})", style = MaterialTheme.typography.titleMedium, color = CyanQuant)
                    OutlinedButton(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cb?.setPrimaryClip(ClipData.newPlainText("JAX Pine Script v6", pineScript))
                        },
                        modifier = Modifier.height(48.dp).testTag("copy_pine_script_btn")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy Pine Script", tint = MetallicGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy v6", color = MetallicGold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(color = ObsidianBlack, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = pineScript,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = CrispWhite,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AlertsCentreScreen(viewModel: JaxViewModel) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Alerts & Signal Feed",
                subtitle = "Real-time setup confirmations, decay warnings & risk circuit events"
            )
        }

        if (alerts.isEmpty()) {
            item {
                JaxGlassCard {
                    Text("No alerts logged yet. Saving a setup or triggering a learning/risk action records alerts here.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        } else {
            items(alerts, key = { it.id }) { alert ->
                JaxGlassCard(borderColor = if (alert.acknowledged) SubtleDivider else MetallicGold.copy(alpha = 0.55f)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(alert.title, style = MaterialTheme.typography.titleSmall, color = CrispWhite, modifier = Modifier.weight(1f))
                        StatusPill(alert.severity, if (alert.severity == "SIGNAL") BullishEmerald else WarningAmber, ObsidianBlack)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(alert.message, style = MaterialTheme.typography.bodySmall, color = SilverText)
                    if (!alert.acknowledged) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { viewModel.acknowledgeAlert(alert.id) },
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = BullishEmerald, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Acknowledge", color = CrispWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AiResearchCentreScreen(viewModel: JaxViewModel) {
    val messages by viewModel.copilotMessages.collectAsStateWithLifecycle()
    val streaming by viewModel.copilotStreaming.collectAsStateWithLifecycle()
    val copilotError by viewModel.copilotError.collectAsStateWithLifecycle()
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    var input by remember { mutableStateOf("") }

    val quickPrompts = listOf(
        "Explain top setup for ${market.selectedInstrument.symbol}",
        "Which 30 VIP indicators are dissenting?",
        "Check indicator decay & market regime",
        "Summarize backtest & OOS quality"
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader(
            title = "AI Research & Copilot",
            subtitle = "Grounded in live ${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}) telemetry · Gemini 3.8 Flash"
        )
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickPrompts.forEach { qp ->
                SuggestionChip(
                    onClick = { viewModel.askCopilot(qp) },
                    label = { Text(qp, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                Surface(
                    color = if (msg.isUser) CharcoalSurface else CyanContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (msg.isUser) MetallicGold.copy(alpha = 0.45f) else CyanQuant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            if (msg.isUser) "TRADER" else "J.A.X. COPILOT",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.isUser) MetallicGold else CyanQuant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(msg.text, style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                    }
                }
            }

            if (streaming != null) {
                item {
                    Surface(
                        color = CyanContainer.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, CyanQuant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = CyanQuant, strokeWidth = 2.dp)
                                Text("J.A.X. COPILOT (Streaming...)", style = MaterialTheme.typography.labelSmall, color = CyanQuant)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(streaming ?: "", style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                        }
                    }
                }
            }
        }

        if (copilotError != null) {
            Spacer(Modifier.height(6.dp))
            Text(copilotError!!, style = MaterialTheme.typography.bodySmall, color = BearishCrimson)
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Ask about setups, R:R, decay, or regime...") },
                modifier = Modifier.weight(1f).testTag("copilot_input")
            )
            IconButton(
                onClick = {
                    val q = input.trim()
                    if (q.isNotEmpty()) {
                        input = ""
                        viewModel.askCopilot(q)
                    }
                },
                enabled = streaming == null,
                modifier = Modifier.size(48.dp).testTag("copilot_send_btn")
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send prompt", tint = MetallicGold)
            }
        }
    }
}

@Composable
fun SystemHealthScreen(viewModel: JaxViewModel) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val logs by viewModel.auditLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "System Health & Telemetry",
                subtitle = "Honest provider diagnostics — no synthetic or fabricated connection badges"
            )
        }

        items(market.providers, key = { it.id }) { p ->
            val (fg, bg) = providerColor(p.state)
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ConnectionDot(p.state)
                        Column {
                            Text(p.name, style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                            Text(p.feedScope, style = MaterialTheme.typography.labelSmall, color = SilverText)
                        }
                    }
                    StatusPill(p.state.name, fg, bg)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "${p.statusDetail}${p.latencyMs?.let { " · ${it}ms" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverText
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Recent Audit Trail (${logs.size})", style = MaterialTheme.typography.titleMedium, color = BrightGold)
        }
        items(logs.take(15), key = { it.id }) { log ->
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(log.action, style = MaterialTheme.typography.labelLarge, color = MetallicGold)
                    Text(log.severity, style = MaterialTheme.typography.labelSmall, color = SilverText)
                }
                Text(log.details, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
            }
        }
    }
}

@Composable
fun SettingsAndApprovalsScreen(viewModel: JaxViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val approvals by viewModel.approvalRequests.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val logs by viewModel.auditLogs.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Settings & Governance Approvals",
                subtitle = "Explicit human sign-off for model promotions, live trading & strategy deploys"
            )
        }

        item {
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(profile?.displayName ?: viewModel.userDisplayName, style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                        Text(profile?.email?.ifBlank { viewModel.userEmail } ?: viewModel.userEmail, style = MaterialTheme.typography.bodySmall, color = SilverText)
                        Text("Preferred: ${profile?.preferredSymbol ?: "BTCUSDT"} (${profile?.preferredTimeframe ?: "H1"})", style = MaterialTheme.typography.labelSmall, color = MetallicGold)
                    }
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                try {
                                    withContext(NonCancellable) {
                                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                                    }
                                } catch (e: CancellationException) {
                                    throw e
                                } catch (e: Exception) {
                                    Log.w("JaxSignOut", "clearCredentialState skipped: ${e.localizedMessage}")
                                } finally {
                                    Firebase.auth.signOut()
                                }
                            }
                        },
                        modifier = Modifier.height(48.dp).testTag("sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = BearishCrimson, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Sign Out", color = BearishCrimson)
                    }
                }
            }
        }

        item {
            Text("Approval Queue (${approvals.count { it.status == "PENDING" }} Pending)", style = MaterialTheme.typography.titleMedium, color = BrightGold)
        }

        if (approvals.isEmpty()) {
            item {
                JaxGlassCard {
                    Text("No approval requests recorded yet.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        } else {
            items(approvals, key = { it.id }) { req ->
                val pending = req.status == "PENDING"
                JaxGlassCard(borderColor = if (pending) WarningAmber.copy(alpha = 0.55f) else GlassBorder) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(req.actionType, style = MaterialTheme.typography.titleSmall, color = BrightGold)
                        StatusPill(
                            req.status,
                            when (req.status) {
                                "APPROVED" -> BullishEmerald
                                "REJECTED" -> BearishCrimson
                                else -> WarningAmber
                            },
                            ObsidianBlack
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(req.summary, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                    Text("Requested by ${req.requestedBy}", style = MaterialTheme.typography.labelSmall, color = SilverText)
                    if (pending) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { viewModel.resolveApproval(req, true) },
                                colors = ButtonDefaults.buttonColors(containerColor = BullishEmerald, contentColor = ObsidianBlack),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("approve_req_${req.id}")
                            ) {
                                Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Approve", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.resolveApproval(req, false) },
                                modifier = Modifier.weight(1f).height(48.dp).testTag("reject_req_${req.id}")
                            ) {
                                Text("Reject", color = BearishCrimson)
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Immutable Audit Trail (${logs.size})", style = MaterialTheme.typography.titleMedium, color = BrightGold)
        }
        items(logs.take(15), key = { it.id }) { log ->
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(log.action, style = MaterialTheme.typography.labelLarge, color = CyanQuant)
                    Text(log.actor, style = MaterialTheme.typography.labelSmall, color = SilverText)
                }
                Text(log.details, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
            }
        }
    }
}
