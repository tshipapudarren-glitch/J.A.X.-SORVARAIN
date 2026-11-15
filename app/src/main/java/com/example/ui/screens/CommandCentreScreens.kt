package com.example.ui.screens

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DataFeedType
import com.example.data.model.ProviderHealthState
import com.example.data.model.SetupStatus
import com.example.data.model.SignalDirection
import com.example.data.model.SupportedInstruments
import com.example.data.model.Timeframe
import com.example.ui.components.CandlestickSetupChart
import com.example.ui.components.ConnectionDot
import com.example.ui.components.JaxGlassCard
import com.example.ui.components.MetricBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.components.providerColor
import com.example.ui.navigation.JaxSection
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
import com.example.ui.viewmodel.MarketIntelState
import com.example.ui.viewmodel.VisionUiState
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun CommandCentreScreen(
    viewModel: JaxViewModel,
    onNavigate: (JaxSection) -> Unit
) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val jobs by viewModel.learningJobs.collectAsStateWithLifecycle()
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val approvals by viewModel.approvalRequests.collectAsStateWithLifecycle()

    val bullCount = market.evaluations.count { it.enabled && it.direction == SignalDirection.BULLISH }
    val bearCount = market.evaluations.count { it.enabled && it.direction == SignalDirection.BEARISH }
    val decayCount = market.evaluations.count { it.decayDetected }
    val dec = market.selectedInstrument.priceDecimals
    val lastPrice = market.candles.lastOrNull()?.close

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Command Centre",
                subtitle = "${market.selectedInstrument.symbol} · ${market.selectedTimeframe.code} · ${market.sourceNote}",
                trailing = {
                    IconButton(
                        onClick = { viewModel.refreshMarketData() },
                        modifier = Modifier.testTag("refresh_market_btn")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh live market data", tint = MetallicGold)
                    }
                }
            )
        }

        // Instrument + Timeframe quick bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SupportedInstruments.catalog.take(10).forEach { inst ->
                    val selected = inst.symbol == market.selectedInstrument.symbol
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectInstrument(inst) },
                        label = { Text(inst.symbol, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldContainer,
                            selectedLabelColor = BrightGold
                        )
                    )
                }
            }
        }

        // Top KPI Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBadge(
                    label = "Price",
                    value = lastPrice?.let { String.format("%.${dec}f", it) } ?: "N/A",
                    accent = CrispWhite,
                    modifier = Modifier.weight(1f)
                )
                MetricBadge(
                    label = "24h Chg",
                    value = if (market.candles.isEmpty()) "—" else "${if (market.change24hPct >= 0) "+" else ""}${String.format("%.2f", market.change24hPct)}%",
                    accent = if (market.change24hPct >= 0) BullishEmerald else BearishCrimson,
                    modifier = Modifier.weight(1f)
                )
                MetricBadge(
                    label = "Council",
                    value = "${bullCount}B / ${bearCount}S",
                    accent = if (bullCount >= bearCount) BullishEmerald else BearishCrimson,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Global AI Market Summary grounded in real fetched candles
        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Market Intelligence Summary", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                    StatusPill(
                        text = market.regime.regime.label,
                        color = CyanQuant,
                        containerColor = CyanContainer
                    )
                }
                Spacer(Modifier.height(6.dp))
                if (market.isLoading) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MetallicGold)
                        Text("Synchronizing real market candles & computing 30 VIP indicators...", style = MaterialTheme.typography.bodySmall, color = SilverText)
                    }
                } else if (market.candles.isEmpty()) {
                    Text(
                        text = market.errorMessage ?: "Feed disconnected for ${market.selectedInstrument.symbol}. Configure MT5 EA Bridge or import OHLCV CSV in AI Chart Analyser.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarningAmber
                    )
                } else {
                    Text(
                        text = "${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}) regime: ${market.regime.summary}. $bullCount indicators align Bullish vs $bearCount Bearish. ${if (decayCount > 0) "$decayCount indicator(s) show recent performance decay." else "All active indicators within historical variance."}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrispWhite
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onNavigate(JaxSection.AI_CHART_ANALYSER) },
                        border = BorderStroke(1.dp, MetallicGold),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Open Chart", color = BrightGold)
                    }
                    Button(
                        onClick = { onNavigate(JaxSection.AI_RESEARCH_CENTRE) },
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ask Copilot", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // VIP Opportunity Board
        item {
            JaxGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("VIP Opportunity Board", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                    TextButton(onClick = { onNavigate(JaxSection.SETUP_GENERATOR) }) {
                        Text("All Setups", color = MetallicGold)
                    }
                }
                if (market.generatedSetups.isEmpty()) {
                    Text(
                        text = if (market.candles.isEmpty()) "Awaiting live or imported market data."
                        else "No candidate setups meet current confluence thresholds on ${market.selectedInstrument.symbol} ${market.selectedTimeframe.code}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverText
                    )
                } else {
                    market.generatedSetups.forEach { setup ->
                        Spacer(Modifier.height(8.dp))
                        val isBuy = setup.direction == "BUY"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigate(JaxSection.SETUP_GENERATOR) }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${setup.direction} ${setup.symbol} · ${setup.timeframe}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (isBuy) BullishEmerald else BearishCrimson
                                )
                                Text(
                                    "Entry ${String.format("%.${dec}f", setup.entryPrice)} | SL ${String.format("%.${dec}f", setup.stopLoss)} | TP2 ${String.format("%.${dec}f", setup.tp2)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SilverText
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusPill(
                                    text = "${setup.status.label} · ${setup.confluenceScore.toInt()}%",
                                    color = if (setup.status == SetupStatus.CONFIRMED) BullishEmerald else WarningAmber,
                                    containerColor = if (setup.status == SetupStatus.CONFIRMED) BullishContainer else WarningContainer
                                )
                                Text(
                                    "R:R ${String.format("%.2f", setup.riskReward)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrightGold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Honest Provider Connection Matrix
        item {
            JaxGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Data Providers & Bridges", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                    TextButton(onClick = { onNavigate(JaxSection.SYSTEM_HEALTH) }) {
                        Text("Health", color = MetallicGold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                market.providers.forEach { p ->
                    val (fg, bg) = providerColor(p.state)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ConnectionDot(p.state)
                            Column {
                                Text(p.name, style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                                Text(p.statusDetail, style = MaterialTheme.typography.bodySmall, color = SilverText)
                            }
                        }
                        StatusPill(
                            text = p.state.name.replace("_", " "),
                            color = fg,
                            containerColor = bg
                        )
                    }
                }
            }
        }

        // Governance, Risk & Learning snapshot
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricBadge(
                    label = "Live Exec",
                    value = if (profile?.liveTradingEnabled == true) "ACTIVE" else "LOCKED OFF",
                    accent = if (profile?.liveTradingEnabled == true) BearishCrimson else BullishEmerald,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(JaxSection.RISK_INTELLIGENCE) }
                )
                MetricBadge(
                    label = "Pending Approvals",
                    value = "${approvals.count { it.status == "PENDING" }}",
                    accent = WarningAmber,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(JaxSection.SETTINGS) }
                )
                MetricBadge(
                    label = "Alerts / Jobs",
                    value = "${alerts.count { !it.acknowledged }} / ${jobs.size}",
                    accent = CyanQuant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(JaxSection.ALERTS_CENTRE) }
                )
            }
        }
    }
}

@Composable
fun LiveMarketIntelligenceScreen(
    viewModel: JaxViewModel,
    onNavigate: (JaxSection) -> Unit
) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    val filtered = remember(search) {
        if (search.isBlank()) SupportedInstruments.catalog
        else SupportedInstruments.catalog.filter {
            it.symbol.contains(search, ignoreCase = true) ||
                it.displayName.contains(search, ignoreCase = true) ||
                it.assetClass.label.contains(search, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "Live Market Intelligence",
                subtitle = "Real public exchange & FX feeds + MT5 EA Bridge instruments"
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Filter Forex, Gold, Crypto, Indices, Stocks...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        items(filtered, key = { it.symbol }) { inst ->
            val isSelected = inst.symbol == market.selectedInstrument.symbol
            JaxGlassCard(
                borderColor = if (isSelected) MetallicGold else GlassBorder,
                modifier = Modifier.clickable {
                    viewModel.selectInstrument(inst)
                    onNavigate(JaxSection.AI_CHART_ANALYSER)
                }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${inst.symbol} · ${inst.assetClass.label}", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                        Text(inst.displayName, style = MaterialTheme.typography.bodySmall, color = SilverText)
                    }
                    val (label, col, bg) = when (inst.feedType) {
                        DataFeedType.BINANCE_SPOT -> Triple("LIVE SPOT API", BullishEmerald, BullishContainer)
                        DataFeedType.FRANKFURTER_FX -> Triple("ECB FX D1 FEED", CyanQuant, CyanContainer)
                        DataFeedType.MT5_BRIDGE_REQUIRED -> Triple("MT5 / CSV REQ", WarningAmber, WarningContainer)
                        DataFeedType.CSV_IMPORTED -> Triple("IMPORTED", MetallicGold, GoldContainer)
                    }
                    StatusPill(label, col, bg)
                }
            }
        }
    }
}

@Composable
fun AiChartAnalyserScreen(
    viewModel: JaxViewModel
) {
    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val visionState by viewModel.visionState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showEmas by remember { mutableStateOf(true) }
    var showOb by remember { mutableStateOf(true) }
    var showFvg by remember { mutableStateOf(true) }
    var showLiq by remember { mutableStateOf(true) }
    var showFib by remember { mutableStateOf(false) }
    var showSetupDrawings by remember { mutableStateOf(true) }
    var showCsvDialog by remember { mutableStateOf(false) }
    var csvInput by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val bmp = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
            } catch (_: Exception) {
                null
            }
            if (bmp != null) {
                viewModel.analyzeChartBitmap(bmp)
            }
        }
    }

    val topSetup = market.generatedSetups.firstOrNull()
    val dec = market.selectedInstrument.priceDecimals

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "AI Chart Analyser",
                subtitle = "${market.selectedInstrument.symbol} · ${market.selectedTimeframe.code} · ${market.candles.size} bars (${market.sourceNote})",
                trailing = {
                    Row {
                        IconButton(onClick = { showCsvDialog = true }) {
                            Icon(Icons.Filled.UploadFile, contentDescription = "Import CSV Candles", tint = CyanQuant)
                        }
                        IconButton(onClick = { viewModel.refreshMarketData() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh chart", tint = MetallicGold)
                        }
                    }
                }
            )
        }

        // Timeframe selector
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Timeframe.entries.forEach { tf ->
                    FilterChip(
                        selected = tf == market.selectedTimeframe,
                        onClick = { viewModel.selectTimeframe(tf) },
                        label = { Text(tf.code, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldContainer,
                            selectedLabelColor = BrightGold
                        )
                    )
                }
            }
        }

        // Interactive Candlestick & Setup Canvas
        item {
            JaxGlassCard {
                CandlestickSetupChart(
                    candles = market.candles,
                    overlays = market.overlays,
                    activeSetup = topSetup,
                    showEmas = showEmas,
                    showOrderBlocks = showOb,
                    showFvg = showFvg,
                    showLiquidity = showLiq,
                    showFib = showFib,
                    showSetupDrawings = showSetupDrawings,
                    decimals = dec
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OverlayToggleChip("EMAs", showEmas) { showEmas = !showEmas }
                    OverlayToggleChip("Setup Box", showSetupDrawings) { showSetupDrawings = !showSetupDrawings }
                    OverlayToggleChip("Order Blocks", showOb) { showOb = !showOb }
                    OverlayToggleChip("FVG", showFvg) { showFvg = !showFvg }
                    OverlayToggleChip("Liquidity", showLiq) { showLiq = !showLiq }
                    OverlayToggleChip("Fib Golden", showFib) { showFib = !showFib }
                }
            }
        }

        // Active Setup Drawing Legend
        if (topSetup != null) {
            item {
                JaxGlassCard(borderColor = if (topSetup.direction == "BUY") BullishEmerald.copy(alpha = 0.5f) else BearishCrimson.copy(alpha = 0.5f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Auto-Drawn ${topSetup.direction} Setup (${topSetup.status.label})",
                            style = MaterialTheme.typography.titleMedium,
                            color = CrispWhite
                        )
                        Text("Score ${topSetup.confluenceScore.toInt()}%", style = MaterialTheme.typography.labelLarge, color = BrightGold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Entry: ${String.format("%.${dec}f", topSetup.entryPrice)}  |  SL: ${String.format("%.${dec}f", topSetup.stopLoss)}  |  Inv: ${String.format("%.${dec}f", topSetup.invalidationLevel)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = CrispWhite
                    )
                    Text(
                        "TP1: ${String.format("%.${dec}f", topSetup.tp1)}  |  TP2: ${String.format("%.${dec}f", topSetup.tp2)}  |  TP3: ${String.format("%.${dec}f", topSetup.tp3)}  |  R:R ${String.format("%.2f", topSetup.riskReward)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = BullishEmerald
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(topSetup.explanation, style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        }

        // Multimodal Chart Screenshot AI Vision Analyzer (Photo Picker + Firebase AI Logic)
        item {
            JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.45f)) {
                Text("AI Chart Screenshot Vision Analyzer", style = MaterialTheme.typography.titleMedium, color = CyanQuant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Upload any TradingView, MT5, or broker chart screenshot via Android Photo Picker for Gemini 3.8 Flash multimodal structure & setup analysis.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverText
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_chart_screenshot_btn")
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Chart Screenshot for AI Report", fontWeight = FontWeight.Bold)
                }

                when (val vs = visionState) {
                    VisionUiState.Idle -> {}
                    VisionUiState.Analyzing -> {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = CyanQuant)
                            Text("Analyzing chart geometry with Gemini 3.8 Flash...", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        }
                    }
                    is VisionUiState.Error -> {
                        Spacer(Modifier.height(10.dp))
                        Text("Vision Error: ${vs.message}", style = MaterialTheme.typography.bodySmall, color = BearishCrimson)
                    }
                    is VisionUiState.Success -> {
                        Spacer(Modifier.height(10.dp))
                        Text("Bias: ${vs.report.bias}", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                        Text("Structure: ${vs.report.marketStructure}", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        if (vs.report.keyLevels.isNotEmpty()) {
                            Text("Key Levels: ${vs.report.keyLevels.joinToString(" · ")}", style = MaterialTheme.typography.labelSmall, color = CyanQuant)
                        }
                        Text("Setup Proposal: ${vs.report.setupProposal}", style = MaterialTheme.typography.bodySmall, color = BullishEmerald)
                        Text("Risk & Uncertainty: ${vs.report.riskWarning} (${vs.report.confidenceNote})", style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                    }
                }
            }
        }
    }

    if (showCsvDialog) {
        AlertDialog(
            onDismissRequest = { showCsvDialog = false },
            title = { Text("Import OHLCV CSV Candles") },
            text = {
                Column {
                    Text(
                        "Paste CSV lines (timestamp,open,high,low,close,volume) for ${market.selectedInstrument.symbol} (>=30 rows):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = csvInput,
                        onValueChange = { csvInput = it },
                        minLines = 6,
                        maxLines = 10,
                        placeholder = { Text("1731628800,2650.1,2655.4,2648.0,2653.2,1400") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.importCsvCandles(csvInput)
                    showCsvDialog = false
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showCsvDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun OverlayToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = CyanContainer,
            selectedLabelColor = CyanQuant
        )
    )
}
