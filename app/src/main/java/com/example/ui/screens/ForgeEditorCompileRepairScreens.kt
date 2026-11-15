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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FindReplace
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ForgePlatform
import com.example.ui.components.JaxGlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
import com.example.ui.navigation.JaxSection
import com.example.ui.theme.BearishContainer
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BullishContainer
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.CyanQuant
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverText
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import com.example.ui.viewmodel.JaxForgeViewModel
import com.example.ui.viewmodel.JaxViewModel

@Composable
fun ForgeWorkspaceEditorScreen(
    forgeViewModel: JaxForgeViewModel,
    onNavigate: (JaxSection) -> Unit,
    initialTab: Int = 0
) {
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val activeProj = projects.firstOrNull { it.id == ui.selectedProjectId }

    var platformTab by remember(initialTab) { mutableIntStateOf(initialTab) } // 0 = MT5, 1 = Pine v6
    var showFindReplace by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var changeSummary by remember { mutableStateOf("") }

    LaunchedEffect(projects) {
        if (ui.selectedProjectId == null && projects.isNotEmpty()) {
            forgeViewModel.selectProject(projects.first().id)
        }
    }

    val currentCode = if (platformTab == 0) ui.editorMql5Code else ui.editorPineCode
    val activeValidation = if (platformTab == 0) ui.lastMql5Validation else ui.lastPineValidation

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (activeProj != null) "Code Editor · ${activeProj.name}" else "Indicator Forge & Editor",
                subtitle = if (activeProj != null) "v${activeProj.currentVersion} · ${activeProj.platform} · ${if (ui.hasUnsavedEditorChanges) "Unsaved Draft" else "Synced"}" else "Select or generate an indicator project"
            )
        }

        if (projects.isNotEmpty()) {
            item {
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    projects.forEach { proj ->
                        FilterChip(
                            selected = proj.id == ui.selectedProjectId,
                            onClick = { forgeViewModel.selectProject(proj.id) },
                            label = { Text("${proj.name} v${proj.currentVersion}") },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = BrightGold)
                        )
                    }
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = platformTab == 0,
                    onClick = { platformTab = 0 },
                    label = { Text("MetaTrader 5 (.mq5)") },
                    modifier = Modifier.testTag("editor_tab_mt5")
                )
                FilterChip(
                    selected = platformTab == 1,
                    onClick = { platformTab = 1 },
                    label = { Text("TradingView (.pine v6)") },
                    modifier = Modifier.testTag("editor_tab_pine")
                )
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = { showFindReplace = !showFindReplace }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.FindReplace, contentDescription = "Find and Replace", modifier = Modifier.size(18.dp))
                }
            }
        }

        if (showFindReplace) {
            item {
                JaxGlassCard {
                    Text("Find & Replace", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = findText, onValueChange = { findText = it }, label = { Text("Find") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = replaceText, onValueChange = { replaceText = it }, label = { Text("Replace") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            if (findText.isNotEmpty()) {
                                val updated = currentCode.replace(findText, replaceText)
                                if (platformTab == 0) forgeViewModel.updateEditorMql5(updated) else forgeViewModel.updateEditorPine(updated)
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Replace All")
                    }
                }
            }
        }

        item {
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(if (platformTab == 0) "MQL5 Source Buffer" else "Pine Script v6 Source Buffer", style = MaterialTheme.typography.titleSmall, color = CyanQuant)
                    if (activeValidation != null) {
                        StatusPill(
                            if (activeValidation.passed) "Syntax OK · Repaint ${activeValidation.repaintRiskLevel}" else "Issues Detected",
                            if (activeValidation.passed) BullishEmerald else BearishCrimson,
                            if (activeValidation.passed) BullishContainer else BearishContainer
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentCode,
                    onValueChange = { code ->
                        if (platformTab == 0) forgeViewModel.updateEditorMql5(code) else forgeViewModel.updateEditorPine(code)
                    },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CrispWhite),
                    minLines = 14,
                    maxLines = 24,
                    placeholder = { Text("No source loaded yet. Generate an indicator or select a project above.") },
                    modifier = Modifier.fillMaxWidth().testTag("forge_source_code_editor")
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = changeSummary,
                    onValueChange = { changeSummary = it },
                    label = { Text("Version Commit Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            forgeViewModel.saveEditorVersion(changeSummary, isStable = true)
                            changeSummary = ""
                        },
                        enabled = activeProj != null && currentCode.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("forge_save_version_btn")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Version", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onNavigate(JaxSection.FORGE_COMPILATION) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Build, contentDescription = null, tint = CyanQuant, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Compile / Check", color = CyanQuant)
                    }
                }
            }
        }
    }
}

@Composable
fun ForgeCompilationAndRepairScreen(
    forgeViewModel: JaxForgeViewModel,
    jaxViewModel: JaxViewModel,
    initialFocusRepair: Boolean = false
) {
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val builds by forgeViewModel.builds.collectAsStateWithLifecycle()
    val integrations by jaxViewModel.integrations.collectAsStateWithLifecycle()

    val savedWorkerUrl = integrations.firstOrNull { it.type == "MT5_COMPILER_WORKER" }?.endpointUrl.orEmpty()
    var workerUrl by remember(savedWorkerUrl) { mutableStateOf(savedWorkerUrl) }
    var repairPlatform by remember { mutableStateOf(ForgePlatform.MT5) }

    val activeProj = projects.firstOrNull { it.id == ui.selectedProjectId }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (initialFocusRepair) "AI Code Repair Centre" else "Compilation & Validation Centre",
                subtitle = if (activeProj != null) "${activeProj.name} v${activeProj.currentVersion} · Genuine MetaEditor Worker + Pine v6 Static Validator" else "Select an indicator project"
            )
        }

        // Compilation Worker
        item {
            JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.45f)) {
                Text("MetaTrader 5 MetaEditor Compiler Worker", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Android devices cannot natively execute Windows MetaEditor64.exe. Configure your authenticated MetaEditor HTTP Worker endpoint below to compile .mq5 -> genuine .ex5 bytes; otherwise J.A.X. truthfully reports COMPILER_UNAVAILABLE and never fabricates .ex5 binaries.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SilverText
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = workerUrl,
                    onValueChange = { workerUrl = it },
                    label = { Text("MetaEditor HTTP Worker URL (e.g. https://mt5-worker.yourdomain/compile)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("forge_compiler_worker_url")
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { forgeViewModel.compileActiveProjectMt5(workerUrl.ifBlank { null }) },
                    enabled = activeProj != null && !ui.isCompiling,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("forge_compile_mt5_btn")
                ) {
                    if (ui.isCompiling) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Compiling via Worker...", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Compile .mq5 -> .ex5 via Worker", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Static Validation & Repaint Audit
        item {
            JaxGlassCard {
                Text("Static Syntax, Lookahead & Repainting Audit", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
                Spacer(Modifier.height(8.dp))
                listOfNotNull(ui.lastMql5Validation, ui.lastPineValidation).forEach { rep ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(rep.platform.label, style = MaterialTheme.typography.titleSmall, color = BrightGold)
                        StatusPill(
                            "Repaint ${rep.repaintRiskLevel}${if (rep.lookaheadDetected) " · LOOKAHEAD" else ""}",
                            if (rep.passed && !rep.lookaheadDetected) BullishEmerald else BearishCrimson,
                            if (rep.passed && !rep.lookaheadDetected) BullishContainer else BearishContainer
                        )
                    }
                    Text(rep.summary, style = MaterialTheme.typography.bodySmall, color = SilverText)
                    rep.findings.forEach { f ->
                        Text(
                            "L${f.lineNumber} [${f.severity}]: ${f.message} -> ${f.suggestion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (f.severity == "ERROR") BearishCrimson else WarningAmber
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // AI Code Repair
        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
                Text("JAX-CORE AI Code Repair", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(4.dp))
                Text("Routes diagnostics to DeepSeek Code Debugger + Gemini Audit to patch syntax, buffer indexing, or repainting bugs and saves a new version.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = repairPlatform == ForgePlatform.MT5, onClick = { repairPlatform = ForgePlatform.MT5 }, label = { Text("Repair MQL5 (.mq5)") })
                    FilterChip(selected = repairPlatform == ForgePlatform.TRADINGVIEW, onClick = { repairPlatform = ForgePlatform.TRADINGVIEW }, label = { Text("Repair Pine v6 (.pine)") })
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { forgeViewModel.repairActiveCodeWithAi(repairPlatform) },
                    enabled = activeProj != null && !ui.isRepairing,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldContainer, contentColor = BrightGold),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("forge_ai_repair_btn")
                ) {
                    if (ui.isRepairing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrightGold, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Repairing Source Code...")
                    } else {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Run AI Code Repair & Save New Version")
                    }
                }
            }
        }

        item {
            Text("Build & Validation History", style = MaterialTheme.typography.titleMedium, color = CrispWhite)
        }

        items(builds.take(10), key = { it.id }) { b ->
            val ok = b.hasCompiledBinary || b.buildStatus == "COMPILED_SUCCESS" || b.buildStatus == "VALIDATED_UNCOMPILED"
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("${b.platform} · v${b.sourceVersion} (${b.compiler})", style = MaterialTheme.typography.titleSmall, color = CrispWhite, modifier = Modifier.weight(1f))
                    StatusPill(b.buildStatus, if (ok) BullishEmerald else BearishCrimson, if (ok) BullishContainer else BearishContainer)
                }
                if (b.errorsSummary.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(b.errorsSummary, style = MaterialTheme.typography.bodySmall, color = BearishCrimson)
                }
                if (b.warningsSummary.isNotBlank()) {
                    Text(b.warningsSummary, style = MaterialTheme.typography.bodySmall, color = WarningAmber)
                }
            }
        }
    }
}
