package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.SupportedInstruments
import com.example.ui.components.CandlestickSetupChart
import com.example.ui.components.JaxGlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
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
import com.example.ui.viewmodel.PendingExportRequest

@Composable
fun ForgeDownloadsAndLibraryScreen(
    forgeViewModel: JaxForgeViewModel,
    initialTab: Int = 0 // 0=My Indicators, 1=My Downloads, 2=MT5 Library, 3=TV Library
) {
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val downloads by forgeViewModel.downloads.collectAsStateWithLifecycle()
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var searchQuery by remember { mutableStateOf("") }
    var pendingExport by remember { mutableStateOf<PendingExportRequest?>(null) }

    LaunchedEffect(projects) {
        if (ui.selectedProjectId == null && projects.isNotEmpty()) {
            forgeViewModel.selectProject(projects.first().id)
        }
    }

    val createDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val req = pendingExport
        if (uri != null && req != null) {
            forgeViewModel.completeExportToUri(req, uri)
        }
        pendingExport = null
    }

    val triggerSafExport: (String) -> Unit = { fileType ->
        val req = forgeViewModel.prepareExportArtifact(fileType)
        if (req != null) {
            pendingExport = req
            createDocLauncher.launch(req.localFile.name)
        }
    }

    val triggerShare: (String) -> Unit = { fileType ->
        val req = forgeViewModel.prepareExportArtifact(fileType)
        if (req != null) {
            val intent = forgeViewModel.fileStorage.createShareIntent(req.localFile, req.mimeType)
            context.startActivity(Intent.createChooser(intent, "Share ${req.localFile.name}"))
            forgeViewModel.recordShareDownload(req)
        }
    }

    val activeProj = projects.firstOrNull { it.id == ui.selectedProjectId }
    val hasCompiledEx5 = activeProj != null && ui.compiledBinaryFiles["${activeProj.id}_${activeProj.currentVersion}"] != null

    val filteredProjects = remember(projects, activeTab, searchQuery) {
        projects.filter { p ->
            val tabOk = when (activeTab) {
                2 -> p.platform == "MT5" || p.platform == "DUAL"
                3 -> p.platform == "TRADINGVIEW" || p.platform == "DUAL"
                else -> true
            }
            val qOk = searchQuery.isBlank() || p.name.contains(searchQuery, true) || p.category.contains(searchQuery, true)
            tabOk && qOk
        }
    }

    val tabTitles = listOf("My Indicators", "My Downloads", "MT5 Library", "TV Pine v6 Library")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = tabTitles.getOrElse(activeTab) { "Indicator Downloads" },
                subtitle = "Download real .mq5, genuine .ex5 (when compiled), .pine, .txt, and .zip packages to device storage"
            )
        }

        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabTitles.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = BrightGold)
                    )
                }
            }
        }

        // Export Deck for Active Project
        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
                Text(
                    if (activeProj != null) "Export Active Project: ${activeProj.name} v${activeProj.currentVersion}" else "No Project Selected",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrightGold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (hasCompiledEx5) "Compiled .ex5 binary verified by MetaEditor Worker and ready to export."
                    else ".ex5 download remains disabled until compiled by a connected MetaEditor Worker (no fake binaries).",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasCompiledEx5) BullishEmerald else SilverText
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { triggerSafExport("MQ5") },
                        enabled = ui.editorMql5Code.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("export_mq5_btn")
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save .mq5", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { triggerSafExport("EX5") },
                        enabled = hasCompiledEx5,
                        colors = ButtonDefaults.buttonColors(containerColor = BullishContainer, contentColor = BullishEmerald),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("export_ex5_btn")
                    ) {
                        Text("Save .ex5")
                    }
                    Button(
                        onClick = { triggerSafExport("PINE") },
                        enabled = ui.editorPineCode.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanContainer, contentColor = CyanQuant),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("export_pine_btn")
                    ) {
                        Text("Save .pine")
                    }
                    OutlinedButton(
                        onClick = { triggerSafExport("TXT") },
                        enabled = ui.editorPineCode.isNotBlank() || ui.editorMql5Code.isNotBlank(),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("export_txt_btn")
                    ) {
                        Text("Save .txt", color = CrispWhite)
                    }
                    OutlinedButton(
                        onClick = { triggerSafExport("ZIP") },
                        enabled = ui.editorPineCode.isNotBlank() || ui.editorMql5Code.isNotBlank(),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("export_zip_btn")
                    ) {
                        Icon(Icons.Filled.FolderZip, contentDescription = null, tint = BrightGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save .zip", color = BrightGold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            val pine = ui.editorPineCode
                            if (pine.isNotBlank()) {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("Pine Script v6", pine))
                            }
                        },
                        enabled = ui.editorPineCode.isNotBlank(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("copy_pine_clipboard_btn")
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, tint = CyanQuant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy Pine v6", color = CyanQuant)
                    }
                    OutlinedButton(
                        onClick = { triggerShare("ZIP") },
                        enabled = ui.editorPineCode.isNotBlank() || ui.editorMql5Code.isNotBlank(),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("share_zip_intent_btn")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share Bundle", color = MetallicGold)
                    }
                }
            }
        }

        if (activeTab == 1) {
            item {
                Text("Download Ledger (${downloads.size} verified exports)", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
            }
            if (downloads.isEmpty()) {
                item {
                    JaxGlassCard {
                        Text("No exports recorded yet. Tap Save .mq5, Save .pine, or Save .zip above.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                    }
                }
            } else {
                items(downloads, key = { it.id }) { dl ->
                    JaxGlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dl.fileName, style = MaterialTheme.typography.titleSmall, color = BrightGold)
                                Text("${dl.projectName} · v${dl.version} · ${dl.platform}", style = MaterialTheme.typography.bodySmall, color = SilverText)
                            }
                            StatusPill("${dl.fileType} · ${dl.byteSize} B", BullishEmerald, BullishContainer)
                        }
                    }
                }
            }
        } else {
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Filter indicators by name or category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            items(filteredProjects, key = { it.id }) { proj ->
                val selected = proj.id == ui.selectedProjectId
                JaxGlassCard(borderColor = if (selected) MetallicGold else SubtleDivider) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${proj.name} v${proj.currentVersion}", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                            Text("${proj.platform} · ${proj.category} · ${proj.setupPattern}", style = MaterialTheme.typography.labelSmall, color = SilverText)
                        }
                        StatusPill(proj.devStatus, if (proj.devStatus == "COMPILED" || proj.devStatus == "VALIDATED") BullishEmerald else WarningAmber, if (proj.devStatus == "COMPILED" || proj.devStatus == "VALIDATED") BullishContainer else WarningContainer)
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { forgeViewModel.selectProject(proj.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = if (selected) BullishContainer else GoldContainer, contentColor = if (selected) BullishEmerald else BrightGold),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(if (selected) "Active in Export Deck" else "Select for Export")
                    }
                }
            }
        }
    }
}

@Composable
fun ForgePreviewAndDrawingDesignerScreen(
    forgeViewModel: JaxForgeViewModel,
    jaxViewModel: JaxViewModel,
    initialFocusDesigner: Boolean = false
) {
    val market by jaxViewModel.marketState.collectAsStateWithLifecycle()
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val cfg = ui.drawingConfig

    var lineStyle by remember(cfg.lineStyle) { mutableStateOf(cfg.lineStyle) }
    var closedCandleOnly by remember(cfg.closedCandleOnly) { mutableStateOf(cfg.closedCandleOnly) }
    var showArrows by remember(cfg.showSignalArrows) { mutableStateOf(cfg.showSignalArrows) }
    var showBoxes by remember(cfg.showRiskRewardBoxes) { mutableStateOf(cfg.showRiskRewardBoxes) }
    var entryHex by remember(cfg.entryColorHex) { mutableStateOf(cfg.entryColorHex) }
    var slHex by remember(cfg.slColorHex) { mutableStateOf(cfg.slColorHex) }
    var tp1Hex by remember(cfg.tp1ColorHex) { mutableStateOf(cfg.tp1ColorHex) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (initialFocusDesigner) "Setup Drawing Designer" else "Chart Preview Studio",
                subtitle = "Live Entry, SL, TP1-TP3, Invalidation & Closed-Candle preview before downloading .mq5 / .pine"
            )
        }

        item {
            JaxGlassCard {
                CandlestickSetupChart(
                    candles = market.candles,
                    overlays = market.overlays,
                    activeSetup = market.generatedSetups.firstOrNull(),
                    showEmas = true,
                    showOrderBlocks = true,
                    showFvg = true,
                    showLiquidity = true,
                    showFib = false,
                    showSetupDrawings = showBoxes,
                    decimals = market.selectedInstrument.priceDecimals
                )
            }
        }

        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text("Drawing Styling & Non-Repainting Rules", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Closed-Candle Confirmation Only", style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                    Switch(checked = closedCandleOnly, onCheckedChange = { closedCandleOnly = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Signal Arrows & Entry Labels", style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                    Switch(checked = showArrows, onCheckedChange = { showArrows = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("R:R Risk/Reward Shaded Zones", style = MaterialTheme.typography.bodyMedium, color = CrispWhite)
                    Switch(checked = showBoxes, onCheckedChange = { showBoxes = it })
                }
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("SOLID", "DASHED", "DOTTED").forEach { st ->
                        FilterChip(selected = lineStyle == st, onClick = { lineStyle = st }, label = { Text(st) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = entryHex, onValueChange = { entryHex = it }, label = { Text("Entry Hex") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = slHex, onValueChange = { slHex = it }, label = { Text("SL Hex") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = tp1Hex, onValueChange = { tp1Hex = it }, label = { Text("TP Hex") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        forgeViewModel.updateDrawingConfig(
                            cfg.copy(
                                entryColorHex = entryHex,
                                slColorHex = slHex,
                                tp1ColorHex = tp1Hex,
                                lineStyle = lineStyle,
                                showSignalArrows = showArrows,
                                showRiskRewardBoxes = showBoxes,
                                closedCandleOnly = closedCandleOnly
                            )
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("forge_apply_drawing_cfg")
                ) {
                    Text("Apply Drawing Spec to Next Forge Build", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ForgeMtfAndPresetsScreen(
    forgeViewModel: JaxForgeViewModel,
    jaxViewModel: JaxViewModel,
    initialFocusPresets: Boolean = false
) {
    val market by jaxViewModel.marketState.collectAsStateWithLifecycle()
    val presets by forgeViewModel.presets.collectAsStateWithLifecycle()
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()

    val selectedTfs = remember { mutableStateListOf("H4", "H1", "M15", "M5") }
    var presetSymbol by remember { mutableStateOf(market.selectedInstrument.symbol) }
    var presetTf by remember { mutableStateOf("M15") }
    var presetName by remember { mutableStateOf("${market.selectedInstrument.symbol} London Sweep") }
    var presetSummary by remember { mutableStateOf("EMA 21/50, ATR(14), R:R 2.4, Closed-Candle Only") }
    var riskInput by remember { mutableStateOf("1.0") }

    val activeProj = projects.firstOrNull { it.id == ui.selectedProjectId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (initialFocusPresets) "Instrument Preset Manager" else "Multi-Timeframe Indicator Builder",
                subtitle = "Configure M1..MN1 confirmation stacks & instrument-specific presets (XAUUSD, BTCUSDT, FX, Indices)"
            )
        }

        item {
            JaxGlassCard {
                Text("Multi-Timeframe Hierarchy Stack", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("MN1", "W1", "D1", "H4", "H1", "M30", "M15", "M5", "M1").forEach { tf ->
                        val active = tf in selectedTfs
                        FilterChip(
                            selected = active,
                            onClick = { if (active && selectedTfs.size > 1) selectedTfs.remove(tf) else if (!active) selectedTfs.add(tf) },
                            label = { Text(tf) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Active Confirmation Stack: ${selectedTfs.joinToString(" -> ")}", style = MaterialTheme.typography.bodySmall, color = CyanQuant)
                if (market.mtfMatrix.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    market.mtfMatrix.take(5).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${row.timeframe.code} · ${row.structureState}", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                            Text(row.trendBias, style = MaterialTheme.typography.labelSmall, color = if (row.trendBias == "BULLISH") BullishEmerald else WarningAmber)
                        }
                        HorizontalDivider(color = SubtleDivider)
                    }
                }
            }
        }

        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text("Save Instrument Preset${if (activeProj != null) " (${activeProj.name})" else ""}", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SupportedInstruments.catalog.forEach { inst ->
                        FilterChip(selected = presetSymbol == inst.symbol, onClick = { presetSymbol = inst.symbol }, label = { Text(inst.symbol) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = presetName, onValueChange = { presetName = it }, label = { Text("Preset Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = presetTf, onValueChange = { presetTf = it }, label = { Text("Timeframe") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = riskInput, onValueChange = { riskInput = it }, label = { Text("Risk %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = presetSummary, onValueChange = { presetSummary = it }, label = { Text("Parameter Summary") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        forgeViewModel.savePresetForActive(
                            symbol = presetSymbol,
                            timeframe = presetTf,
                            presetName = presetName,
                            settingsSummary = presetSummary,
                            riskPct = riskInput.toDoubleOrNull() ?: 1.0
                        )
                    },
                    enabled = activeProj != null,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("forge_save_preset_btn")
                ) {
                    Text("Save Instrument Preset", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("Saved Presets (${presets.size})", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        }

        items(presets, key = { it.id }) { p ->
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${p.presetName} (${p.symbol} ${p.timeframe})", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                    Text("Risk ${p.riskPerTradePct}%", style = MaterialTheme.typography.labelSmall, color = BullishEmerald)
                }
                Text("${p.projectName} · ${p.settingsSummary}", style = MaterialTheme.typography.bodySmall, color = SilverText)
            }
        }
    }
}
