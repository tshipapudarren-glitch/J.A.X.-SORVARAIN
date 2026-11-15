package com.example.ui.screens

import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ImageSearch
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.ForgeCategory
import com.example.data.model.ForgeComplexity
import com.example.data.model.ForgePlatform
import com.example.data.model.ForgeTemplates
import com.example.data.model.SetupDrawingPattern
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
fun ForgeCommandCentreScreen(
    forgeViewModel: JaxForgeViewModel,
    onNavigate: (JaxSection) -> Unit
) {
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val builds by forgeViewModel.builds.collectAsStateWithLifecycle()
    val downloads by forgeViewModel.downloads.collectAsStateWithLifecycle()
    val listings by forgeViewModel.marketplaceListings.collectAsStateWithLifecycle()
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()

    val mt5Count = projects.count { it.platform == "MT5" || it.platform == "DUAL" }
    val tvCount = projects.count { it.platform == "TRADINGVIEW" || it.platform == "DUAL" }
    val dualCount = projects.count { it.platform == "DUAL" }
    val compiledCount = builds.count { it.hasCompiledBinary }
    val failedCount = builds.count { it.buildStatus == "COMPILATION_FAILED" }
    val favCount = projects.count { it.isFavorite }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "J.A.X. Indicator Forge",
                subtitle = "Genuine MT5 (.mq5 / .ex5) & TradingView Pine v6 Source Factory · Routing: ${ui.routingMode.label}",
                trailing = {
                    Button(
                        onClick = { onNavigate(JaxSection.FORGE_AI_GENERATOR) },
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("forge_cmd_new_btn")
                    ) {
                        Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge("Projects", "${projects.size}", BrightGold, Modifier.weight(1f))
                    MetricBadge("MT5 / Dual", "$mt5Count / $dualCount", CyanQuant, Modifier.weight(1f))
                    MetricBadge("Pine v6", "$tvCount", BullishEmerald, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge("Compiled .ex5", "$compiledCount", BullishEmerald, Modifier.weight(1f))
                    MetricBadge("Build Fails", "$failedCount", if (failedCount > 0) BearishCrimson else SilverText, Modifier.weight(1f))
                    MetricBadge("Downloads", "${downloads.size}", MetallicGold, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricBadge("Marketplace", "${listings.count { it.listingStatus == "APPROVED" }}", CyanQuant, Modifier.weight(1f))
                    MetricBadge("Favorites", "$favCount", BrightGold, Modifier.weight(1f))
                    MetricBadge("JAX-CORE", ui.routingMode.code, MetallicGold, Modifier.weight(1f))
                }
            }
        }

        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text("Quick Launch Modules", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onNavigate(JaxSection.FORGE_AI_GENERATOR) }, modifier = Modifier.heightIn(min = 48.dp).testTag("forge_quick_ai_gen")) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrightGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Generator", color = CrispWhite)
                    }
                    OutlinedButton(onClick = { onNavigate(JaxSection.FORGE_DUAL_BUILDER) }, modifier = Modifier.heightIn(min = 48.dp).testTag("forge_quick_dual")) {
                        Icon(Icons.Filled.CompareArrows, contentDescription = null, tint = CyanQuant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Dual Builder", color = CrispWhite)
                    }
                    OutlinedButton(onClick = { onNavigate(JaxSection.FORGE_CODE_EDITOR) }, modifier = Modifier.heightIn(min = 48.dp).testTag("forge_quick_editor")) {
                        Icon(Icons.Filled.Code, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Code Editor", color = CrispWhite)
                    }
                    OutlinedButton(onClick = { onNavigate(JaxSection.FORGE_MY_DOWNLOADS) }, modifier = Modifier.heightIn(min = 48.dp).testTag("forge_quick_downloads")) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = BullishEmerald, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Downloads", color = CrispWhite)
                    }
                    OutlinedButton(onClick = { onNavigate(JaxSection.AI_COUNCIL_ARENA) }, modifier = Modifier.heightIn(min = 48.dp).testTag("forge_quick_council")) {
                        Icon(Icons.Filled.Build, contentDescription = null, tint = CyanQuant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("AI Council", color = CrispWhite)
                    }
                }
            }
        }

        item {
            Text("Starter Forge Blueprints (1-Tap Generate)", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        }

        items(ForgeTemplates.presets, key = { it.id }) { tpl ->
            JaxGlassCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tpl.name, style = MaterialTheme.typography.titleSmall, color = BrightGold)
                        Text("${tpl.category.label} · ${tpl.defaultTimeframes.joinToString("/")} · R:R ${tpl.rrTarget}", style = MaterialTheme.typography.labelSmall, color = CyanQuant)
                    }
                    Button(
                        onClick = {
                            forgeViewModel.createFromTemplate(tpl.id)
                            onNavigate(JaxSection.FORGE_CODE_EDITOR)
                        },
                        enabled = !ui.isGenerating,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldContainer, contentColor = BrightGold),
                        modifier = Modifier.heightIn(min = 48.dp).testTag("forge_tpl_${tpl.id}")
                    ) {
                        Text("Forge")
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(tpl.description, style = MaterialTheme.typography.bodySmall, color = SilverText)
            }
        }

        item {
            Text("Active Projects & Versions", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        }

        if (projects.isEmpty()) {
            item {
                JaxGlassCard {
                    Text("No indicators forged yet. Tap Create or pick a starter blueprint above to generate downloadable .mq5 and .pine source files.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                }
            }
        } else {
            items(projects.take(6), key = { it.id }) { proj ->
                val isSel = proj.id == ui.selectedProjectId
                JaxGlassCard(borderColor = if (isSel) MetallicGold else SubtlerBorderColor()) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${proj.name} (v${proj.currentVersion})", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                            Text("${proj.platform} · ${proj.category} · ${proj.setupPattern} · ${proj.downloadCount} downloads", style = MaterialTheme.typography.labelSmall, color = SilverText)
                        }
                        IconButton(onClick = { forgeViewModel.toggleFavorite(proj) }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                if (proj.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Toggle Favorite",
                                tint = if (proj.isFavorite) BrightGold else SilverText
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusPill(
                            proj.devStatus,
                            if (proj.devStatus == "COMPILED" || proj.devStatus == "VALIDATED") BullishEmerald else WarningAmber,
                            if (proj.devStatus == "COMPILED" || proj.devStatus == "VALIDATED") BullishContainer else WarningContainer
                        )
                        OutlinedButton(
                            onClick = {
                                forgeViewModel.selectProject(proj.id)
                                onNavigate(JaxSection.FORGE_CODE_EDITOR)
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Open Code", color = BrightGold)
                        }
                        OutlinedButton(
                            onClick = {
                                forgeViewModel.selectProject(proj.id)
                                onNavigate(JaxSection.FORGE_MY_DOWNLOADS)
                            },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Export", color = CyanQuant)
                        }
                    }
                }
            }
        }

        if (downloads.isNotEmpty()) {
            item {
                JaxGlassCard {
                    Text("Recent Export Activity", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                    Spacer(Modifier.height(6.dp))
                    downloads.take(5).forEach { dl ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${dl.fileName} (${dl.platform})", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                            Text("${dl.byteSize} B · v${dl.version}", style = MaterialTheme.typography.labelSmall, color = BullishEmerald)
                        }
                        HorizontalDivider(color = SubtleDivider)
                    }
                }
            }
        }
    }
}

private fun SubtlerBorderColor() = SubtleDivider

@Composable
fun AiIndicatorGeneratorScreen(
    forgeViewModel: JaxForgeViewModel,
    jaxViewModel: JaxViewModel,
    onNavigate: (JaxSection) -> Unit
) {
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val market by jaxViewModel.marketState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var name by remember { mutableStateOf("J.A.X. Institutional Sweep AI") }
    var prompt by remember {
        mutableStateOf(
            "Build an indicator detecting H1 bias, M15 liquidity sweeps above/below prior swing highs/lows, displacement FVG confirmation, and closed-candle M5 entries with Entry, SL, TP1, TP2, TP3 and invalidation level."
        )
    }
    var selectedCategory by remember { mutableStateOf(ForgeCategory.SMART_MONEY) }
    var selectedPlatform by remember { mutableStateOf(ForgePlatform.DUAL) }
    var selectedComplexity by remember { mutableStateOf(ForgeComplexity.PRO) }
    var selectedPattern by remember { mutableStateOf(SetupDrawingPattern.LIQUIDITY_SWEEP) }
    var fastInput by remember { mutableStateOf("21") }
    var slowInput by remember { mutableStateOf("50") }
    var atrInput by remember { mutableStateOf("14") }
    var rrInput by remember { mutableStateOf("2.4") }

    val chartToIndicatorPicker = rememberLauncherForActivityResult(
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
                forgeViewModel.generateFromChartScreenshot(
                    bitmap = bmp,
                    symbol = market.selectedInstrument.symbol,
                    timeframe = market.selectedTimeframe.code,
                    notes = prompt
                )
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "AI Indicator Generator",
                subtitle = "Describe a strategy in plain English or upload a chart screenshot -> Dual-AI generates MQL5 & Pine v6"
            )
        }

        item {
            JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.45f)) {
                Text("Chart-to-Indicator Multimodal Pipeline", style = MaterialTheme.typography.titleMedium, color = CyanQuant)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pick any TradingView/MT5 chart screenshot: Gemini Vision extracts market structure & levels, then JAX-CORE builds downloadable .mq5 & .pine code.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverText
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { chartToIndicatorPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !ui.isGenerating,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("forge_chart_to_indicator_btn")
                ) {
                    Icon(Icons.Filled.ImageSearch, contentDescription = null, tint = CyanQuant)
                    Spacer(Modifier.width(8.dp))
                    Text("Upload Chart Screenshot -> Build Indicator", color = CyanQuant)
                }
            }
        }

        item {
            JaxGlassCard {
                Text("Natural Language Specification", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Indicator Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("forge_gen_name_input")
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("Trading Rules, Conditions & Setup Drawing Prompt") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("forge_gen_prompt_input")
                )
                Spacer(Modifier.height(10.dp))
                Text("Target Platform", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForgePlatform.entries.forEach { p ->
                        FilterChip(
                            selected = p == selectedPlatform,
                            onClick = { selectedPlatform = p },
                            label = { Text(p.name) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = BrightGold)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Category", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForgeCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = cat == selectedCategory,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.label) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = CyanContainer, selectedLabelColor = CyanQuant)
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("Complexity & Setup Pattern", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ForgeComplexity.entries.forEach { c ->
                        FilterChip(
                            selected = c == selectedComplexity,
                            onClick = { selectedComplexity = c },
                            label = { Text(c.label) }
                        )
                    }
                }
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SetupDrawingPattern.entries.forEach { pat ->
                        FilterChip(
                            selected = pat == selectedPattern,
                            onClick = { selectedPattern = pat },
                            label = { Text(pat.label) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fastInput, onValueChange = { fastInput = it }, label = { Text("Fast EMA") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = slowInput, onValueChange = { slowInput = it }, label = { Text("Slow EMA") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = atrInput, onValueChange = { atrInput = it }, label = { Text("ATR") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = rrInput, onValueChange = { rrInput = it }, label = { Text("Target R") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        forgeViewModel.generateIndicatorProject(
                            name = name,
                            descriptionPrompt = prompt,
                            category = selectedCategory,
                            platform = selectedPlatform,
                            complexity = selectedComplexity,
                            pattern = selectedPattern,
                            mtfTimeframes = listOf("H1", "M15", "M5"),
                            fastPeriod = fastInput.toIntOrNull()?.coerceIn(3, 200) ?: 21,
                            slowPeriod = slowInput.toIntOrNull()?.coerceIn(8, 400) ?: 50,
                            atrPeriod = atrInput.toIntOrNull()?.coerceIn(5, 100) ?: 14,
                            rrTarget = rrInput.toDoubleOrNull()?.coerceIn(0.5, 10.0) ?: 2.4
                        )
                    },
                    enabled = !ui.isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp).testTag("forge_generate_submit_btn")
                ) {
                    if (ui.isGenerating) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("JAX-CORE Forging Source...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate Indicator Source", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (ui.lastAiSpecSummary.isNotBlank() || ui.lastAiAuditNotes.isNotBlank()) {
            item {
                JaxGlassCard(borderColor = BullishEmerald.copy(alpha = 0.45f)) {
                    Text("JAX-CORE Specification & Dual-AI Audit", style = MaterialTheme.typography.titleMedium, color = BullishEmerald)
                    Spacer(Modifier.height(6.dp))
                    if (ui.lastAiSpecSummary.isNotBlank()) {
                        Text(ui.lastAiSpecSummary, style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Spacer(Modifier.height(6.dp))
                    }
                    if (ui.lastAiAuditNotes.isNotBlank()) {
                        Text(ui.lastAiAuditNotes, style = MaterialTheme.typography.bodySmall, color = SilverText)
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onNavigate(JaxSection.FORGE_CODE_EDITOR) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Inspect Code", color = BrightGold)
                        }
                        OutlinedButton(onClick = { onNavigate(JaxSection.FORGE_MY_DOWNLOADS) }, modifier = Modifier.heightIn(min = 48.dp)) {
                            Text("Download Files", color = CyanQuant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DualPlatformBuilderScreen(
    forgeViewModel: JaxForgeViewModel,
    onNavigate: (JaxSection) -> Unit
) {
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf("J.A.X. Dual Parity Sniper") }
    var selectedPattern by remember { mutableStateOf(SetupDrawingPattern.BREAKOUT_RETEST) }
    var fastEma by remember { mutableStateOf("21") }
    var slowEma by remember { mutableStateOf("50") }
    var rrTarget by remember { mutableStateOf("2.2") }
    val selectedTfs = remember { mutableStateListOf("H1", "M15", "M5") }
    val allTfs = listOf("D1", "H4", "H1", "M30", "M15", "M5")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Dual Platform Builder",
                subtitle = "Synthesizes parity-aligned MetaTrader 5 (.mq5) and TradingView Pine Script v6 (.pine) from one specification"
            )
        }

        item {
            JaxGlassCard {
                Text("Shared Rule Architecture", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dual Indicator Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("Higher & Execution Timeframes", style = MaterialTheme.typography.labelMedium, color = CrispWhite)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    allTfs.forEach { tf ->
                        val active = tf in selectedTfs
                        FilterChip(
                            selected = active,
                            onClick = {
                                if (active && selectedTfs.size > 1) selectedTfs.remove(tf)
                                else if (!active) selectedTfs.add(tf)
                            },
                            label = { Text(tf) }
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SetupDrawingPattern.entries.take(8).forEach { pat ->
                        FilterChip(selected = pat == selectedPattern, onClick = { selectedPattern = pat }, label = { Text(pat.label) })
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = fastEma, onValueChange = { fastEma = it }, label = { Text("Fast EMA") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = slowEma, onValueChange = { slowEma = it }, label = { Text("Slow EMA") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = rrTarget, onValueChange = { rrTarget = it }, label = { Text("Target R:R") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        forgeViewModel.generateIndicatorProject(
                            name = name,
                            descriptionPrompt = "Synchronized Dual-Platform build for ${selectedPattern.label} with closed-candle parity and automatic Entry/SL/TP1-TP3 drawings.",
                            category = ForgeCategory.MULTI_TIMEFRAME,
                            platform = ForgePlatform.DUAL,
                            complexity = ForgeComplexity.PRO,
                            pattern = selectedPattern,
                            mtfTimeframes = selectedTfs.toList(),
                            fastPeriod = fastEma.toIntOrNull() ?: 21,
                            slowPeriod = slowEma.toIntOrNull() ?: 50,
                            atrPeriod = 14,
                            rrTarget = rrTarget.toDoubleOrNull() ?: 2.2
                        )
                    },
                    enabled = !ui.isGenerating,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("dual_builder_generate_btn")
                ) {
                    Text(if (ui.isGenerating) "Building Dual Package..." else "Build MQL5 + Pine Script v6 Pair", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Text("Rule Equivalence Matrix (MT5 vs TradingView Pine v6)", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        }

        items(ui.equivalenceReport, key = { it.aspect }) { eq ->
            val (col, bg) = when (eq.parityStatus) {
                "EQUIVALENT" -> BullishEmerald to BullishContainer
                "ADAPTED" -> WarningAmber to WarningContainer
                else -> CyanQuant to CyanContainer
            }
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(eq.aspect, style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                    StatusPill(eq.parityStatus, col, bg)
                }
                Spacer(Modifier.height(6.dp))
                Text("MT5: ${eq.mt5Behavior}", style = MaterialTheme.typography.bodySmall, color = SilverText)
                Text("Pine v6: ${eq.pineBehavior}", style = MaterialTheme.typography.bodySmall, color = CyanQuant)
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onNavigate(JaxSection.FORGE_CODE_EDITOR) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("Compare Source", color = BrightGold)
                }
                OutlinedButton(onClick = { onNavigate(JaxSection.FORGE_MY_DOWNLOADS) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) {
                    Text("Download ZIP Bundle", color = BullishEmerald)
                }
            }
        }
    }
}
