package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiCouncilRunDoc
import com.example.data.model.AiProviderTelemetry
import com.example.data.model.AiProviderType
import com.example.data.model.AiUsageDoc
import com.example.data.model.DualAiCouncilResult
import com.example.data.model.ForgeBuildStatus
import com.example.data.model.ForgeCategory
import com.example.data.model.ForgeComplexity
import com.example.data.model.ForgePlatform
import com.example.data.model.ForgeTemplates
import com.example.data.model.ForgeValidationReport
import com.example.data.model.IndicatorBuildDoc
import com.example.data.model.IndicatorDownloadDoc
import com.example.data.model.IndicatorPresetDoc
import com.example.data.model.IndicatorProjectDoc
import com.example.data.model.IndicatorReviewDoc
import com.example.data.model.IndicatorSourceDoc
import com.example.data.model.JaxCoreRoutingMode
import com.example.data.model.JaxSpecialistAgents
import com.example.data.model.MarketplaceListingDoc
import com.example.data.model.RuleEquivalenceItem
import com.example.data.model.SetupDrawingConfig
import com.example.data.model.SetupDrawingPattern
import com.example.data.model.SpecialistAgentDef
import com.example.data.network.Mt5CompilerClient
import com.example.data.repository.JaxAiRepository
import com.example.data.repository.JaxCoreOrchestrator
import com.example.data.repository.JaxFirestoreRepository
import com.example.data.repository.JaxForgeRepository
import com.example.data.repository.ProviderInvokeRecord
import com.example.data.storage.ForgeFileStorage
import com.example.domain.engine.IndicatorForgeEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class PendingExportRequest(
    val projectId: String,
    val projectName: String,
    val version: String,
    val platform: String,
    val fileType: String,
    val localFile: File,
    val mimeType: String
)

data class ForgeUiState(
    val selectedProjectId: String? = null,
    val editorMql5Code: String = "",
    val editorPineCode: String = "",
    val hasUnsavedEditorChanges: Boolean = false,
    val lastAiSpecSummary: String = "",
    val lastAiAuditNotes: String = "",
    val lastMql5Validation: ForgeValidationReport? = null,
    val lastPineValidation: ForgeValidationReport? = null,
    val equivalenceReport: List<RuleEquivalenceItem> = emptyList(),
    val drawingConfig: SetupDrawingConfig = SetupDrawingConfig(),
    val isGenerating: Boolean = false,
    val isCompiling: Boolean = false,
    val isRepairing: Boolean = false,
    val routingMode: JaxCoreRoutingMode = JaxCoreRoutingMode.AUTO,
    val selectedAgent: SpecialistAgentDef = JaxSpecialistAgents.all12.first(),
    val providerTelemetry: List<AiProviderTelemetry> = emptyList(),
    val isCouncilRunning: Boolean = false,
    val latestCouncilResult: DualAiCouncilResult? = null,
    val autoUpdateNotifications: Boolean = true,
    val compiledBinaryFiles: Map<String, File> = emptyMap(),
    val snackbarMessage: String? = null
)

class JaxForgeViewModel(
    private val forgeRepo: JaxForgeRepository,
    private val coreRepo: JaxFirestoreRepository,
    private val orchestrator: JaxCoreOrchestrator,
    private val geminiRepo: JaxAiRepository,
    private val compilerClient: Mt5CompilerClient,
    val fileStorage: ForgeFileStorage,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String
) : ViewModel() {

    val isAdminUser: Boolean = userEmail.equals("impuriumai@gmail.com", ignoreCase = true)

    private fun <T> kotlinx.coroutines.flow.Flow<T>.safeState(initial: T): StateFlow<T> =
        this.catch { err ->
            val fsErr = generateSequence(err) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && err is CancellationException) throw err
            Log.w(TAG, "Forge Firestore stream error: ${(fsErr ?: err).message}")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initial)

    val projects: StateFlow<List<IndicatorProjectDoc>> = forgeRepo.observeProjects(currentUserId).safeState(emptyList())
    val sources: StateFlow<List<IndicatorSourceDoc>> = forgeRepo.observeSources(currentUserId).safeState(emptyList())
    val builds: StateFlow<List<IndicatorBuildDoc>> = forgeRepo.observeBuilds(currentUserId).safeState(emptyList())
    val downloads: StateFlow<List<IndicatorDownloadDoc>> = forgeRepo.observeDownloads(currentUserId).safeState(emptyList())
    val presets: StateFlow<List<IndicatorPresetDoc>> = forgeRepo.observePresets(currentUserId).safeState(emptyList())
    val aiUsageLogs: StateFlow<List<AiUsageDoc>> = forgeRepo.observeAiUsageLogs(currentUserId).safeState(emptyList())
    val aiCouncilRuns: StateFlow<List<AiCouncilRunDoc>> = forgeRepo.observeAiCouncilRuns(currentUserId).safeState(emptyList())
    val marketplaceListings: StateFlow<List<MarketplaceListingDoc>> = forgeRepo.observeMarketplaceListings().safeState(emptyList())

    private val _selectedListingReviews = MutableStateFlow<List<IndicatorReviewDoc>>(emptyList())
    val selectedListingReviews: StateFlow<List<IndicatorReviewDoc>> = _selectedListingReviews.asStateFlow()

    private val _uiState = MutableStateFlow(
        ForgeUiState(
            providerTelemetry = orchestrator.initialTelemetry(),
            equivalenceReport = IndicatorForgeEngine.buildRuleEquivalenceReport(listOf("H1", "M15", "M5"), true)
        )
    )
    val uiState: StateFlow<ForgeUiState> = _uiState.asStateFlow()

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun selectProject(projectId: String) {
        val proj = projects.value.firstOrNull { it.id == projectId } ?: return
        val projSources = sources.value.filter { it.projectId == projectId }
        val latestMq5 = projSources.firstOrNull { it.platform == "MT5" }?.sourceCode.orEmpty()
        val latestPine = projSources.firstOrNull { it.platform == "TRADINGVIEW" }?.sourceCode.orEmpty()
        val cfg = SetupDrawingConfig(
            entryColorHex = proj.entryColorHex,
            slColorHex = proj.slColorHex,
            tp1ColorHex = proj.tpColorHex,
            closedCandleOnly = proj.closedCandleOnly
        )
        _uiState.update {
            it.copy(
                selectedProjectId = projectId,
                editorMql5Code = latestMq5,
                editorPineCode = latestPine,
                hasUnsavedEditorChanges = false,
                drawingConfig = cfg,
                lastMql5Validation = if (latestMq5.isNotBlank()) IndicatorForgeEngine.validateSource(ForgePlatform.MT5, latestMq5) else null,
                lastPineValidation = if (latestPine.isNotBlank()) IndicatorForgeEngine.validateSource(ForgePlatform.TRADINGVIEW, latestPine) else null,
                equivalenceReport = IndicatorForgeEngine.buildRuleEquivalenceReport(proj.mtfTimeframes, proj.closedCandleOnly)
            )
        }
    }

    fun updateEditorMql5(code: String) {
        _uiState.update {
            it.copy(
                editorMql5Code = code,
                hasUnsavedEditorChanges = true,
                lastMql5Validation = if (code.isNotBlank()) IndicatorForgeEngine.validateSource(ForgePlatform.MT5, code) else null
            )
        }
    }

    fun updateEditorPine(code: String) {
        _uiState.update {
            it.copy(
                editorPineCode = code,
                hasUnsavedEditorChanges = true,
                lastPineValidation = if (code.isNotBlank()) IndicatorForgeEngine.validateSource(ForgePlatform.TRADINGVIEW, code) else null
            )
        }
    }

    fun updateDrawingConfig(cfg: SetupDrawingConfig) {
        _uiState.update { it.copy(drawingConfig = cfg) }
    }

    fun generateIndicatorProject(
        name: String,
        descriptionPrompt: String,
        category: ForgeCategory,
        platform: ForgePlatform,
        complexity: ForgeComplexity,
        pattern: SetupDrawingPattern,
        mtfTimeframes: List<String>,
        fastPeriod: Int,
        slowPeriod: Int,
        atrPeriod: Int,
        rrTarget: Double
    ) {
        val cleanName = name.ifBlank { "J.A.X. ${pattern.label} Forge" }
        val drawing = _uiState.value.drawingConfig
        val dsModel = _uiState.value.providerTelemetry.firstOrNull { it.provider == AiProviderType.DEEPSEEK }?.selectedModel ?: "deepseek-chat"

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val outcome = orchestrator.generateForgeIndicator(
                    name = cleanName,
                    naturalPrompt = descriptionPrompt,
                    category = category,
                    platform = platform,
                    complexity = complexity,
                    pattern = pattern,
                    mtfTimeframes = mtfTimeframes,
                    fastPeriod = fastPeriod,
                    slowPeriod = slowPeriod,
                    atrPeriod = atrPeriod,
                    rrTarget = rrTarget,
                    drawing = drawing,
                    deepseekModel = dsModel
                )

                val mq5Val = if (outcome.mql5Source.isNotBlank()) IndicatorForgeEngine.validateSource(ForgePlatform.MT5, outcome.mql5Source) else null
                val pineVal = if (outcome.pineSource.isNotBlank()) IndicatorForgeEngine.validateSource(ForgePlatform.TRADINGVIEW, outcome.pineSource) else null
                val allValid = (mq5Val?.passed ?: true) && (pineVal?.passed ?: true)
                val eqList = IndicatorForgeEngine.buildRuleEquivalenceReport(mtfTimeframes, drawing.closedCandleOnly)
                val eqText = eqList.joinToString("\n") { "${it.aspect} [${it.parityStatus}]: MT5=${it.mt5Behavior} | TV=${it.pineBehavior}" }

                val projectId = forgeRepo.createProject(
                    currentUserId,
                    IndicatorProjectDoc(
                        userId = currentUserId,
                        name = cleanName,
                        description = descriptionPrompt.ifBlank { "${category.label} indicator (${pattern.label})" },
                        category = category.label,
                        platform = platform.name,
                        complexity = complexity.code,
                        devStatus = if (allValid) "VALIDATED" else "DRAFT",
                        currentVersion = "1.0.0",
                        visibility = "PRIVATE",
                        isFavorite = false,
                        mtfTimeframes = mtfTimeframes,
                        setupPattern = pattern.label,
                        closedCandleOnly = drawing.closedCandleOnly,
                        entryColorHex = drawing.entryColorHex,
                        slColorHex = drawing.slColorHex,
                        tpColorHex = drawing.tp1ColorHex,
                        equivalenceNotes = eqText,
                        downloadCount = 0
                    )
                )

                val stem = IndicatorForgeEngine.sanitizeFileStem(cleanName)
                if (outcome.mql5Source.isNotBlank() && mq5Val != null) {
                    val fileName = "$stem.mq5"
                    fileStorage.writeTextArtifact(projectId, "1.0.0", fileName, outcome.mql5Source)
                    forgeRepo.saveSourceVersion(
                        currentUserId,
                        IndicatorSourceDoc(
                            userId = currentUserId,
                            projectId = projectId,
                            version = "1.0.0",
                            platform = "MT5",
                            fileName = fileName,
                            sourceCode = outcome.mql5Source,
                            changeSummary = "Initial Forge generation",
                            createdByAgent = outcome.createdByAgent,
                            isStable = mq5Val.passed,
                            repaintRiskNotes = "Repaint Risk: ${mq5Val.repaintRiskLevel} | Lookahead=${mq5Val.lookaheadDetected}",
                            validationPassed = mq5Val.passed,
                            validationSummary = mq5Val.summary
                        )
                    )
                }
                if (outcome.pineSource.isNotBlank() && pineVal != null) {
                    val fileName = "$stem.pine"
                    fileStorage.writeTextArtifact(projectId, "1.0.0", fileName, outcome.pineSource)
                    forgeRepo.saveSourceVersion(
                        currentUserId,
                        IndicatorSourceDoc(
                            userId = currentUserId,
                            projectId = projectId,
                            version = "1.0.0",
                            platform = "TRADINGVIEW",
                            fileName = fileName,
                            sourceCode = outcome.pineSource,
                            changeSummary = "Initial Pine v6 generation",
                            createdByAgent = outcome.createdByAgent,
                            isStable = pineVal.passed,
                            repaintRiskNotes = "Repaint Risk: ${pineVal.repaintRiskLevel} | Lookahead=${pineVal.lookaheadDetected}",
                            validationPassed = pineVal.passed,
                            validationSummary = pineVal.summary
                        )
                    )
                    forgeRepo.saveBuildRecord(
                        currentUserId,
                        IndicatorBuildDoc(
                            userId = currentUserId,
                            projectId = projectId,
                            sourceVersion = "1.0.0",
                            platform = "TRADINGVIEW",
                            compiler = "J.A.X. Pine Script v6 Validator",
                            buildStatus = ForgeBuildStatus.VALIDATED_UNCOMPILED.name,
                            errorsSummary = pineVal.findings.filter { it.severity == "ERROR" }.joinToString("\n") { "L${it.lineNumber}: ${it.message}" },
                            warningsSummary = pineVal.findings.filter { it.severity == "WARNING" }.joinToString("\n") { "L${it.lineNumber}: ${it.message}" },
                            outputFileName = fileName,
                            hasCompiledBinary = false
                        )
                    )
                }

                recordProviderTelemetry(outcome.telemetryLogs)
                coreRepo.logAudit(currentUserId, "FORGE_CREATE_INDICATOR", userDisplayName, "Generated '$cleanName' (${platform.name})", "INFO")

                _uiState.update {
                    it.copy(
                        selectedProjectId = projectId,
                        editorMql5Code = outcome.mql5Source,
                        editorPineCode = outcome.pineSource,
                        hasUnsavedEditorChanges = false,
                        lastAiSpecSummary = outcome.specSummary,
                        lastAiAuditNotes = outcome.auditNotes,
                        lastMql5Validation = mq5Val,
                        lastPineValidation = pineVal,
                        equivalenceReport = eqList,
                        isGenerating = false,
                        snackbarMessage = "Indicator '$cleanName' generated & persisted."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, snackbarMessage = "Generation error: ${e.localizedMessage}") }
            }
        }
    }

    fun generateFromChartScreenshot(bitmap: Bitmap, symbol: String, timeframe: String, notes: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val report = geminiRepo.analyzeChartScreenshot(bitmap, symbol, timeframe, "Chart-to-Indicator Forge request: $notes")
                val combinedPrompt = """
                    Reproduce the visual trading setup identified on $symbol ($timeframe):
                    Bias: ${report.bias}
                    Structure: ${report.marketStructure}
                    Key Levels: ${report.keyLevels.joinToString()}
                    Setup Proposal: ${report.setupProposal}
                    Additional trader notes: $notes
                    Include Entry, Stop Loss, TP1, TP2, TP3, invalidation level, and closed-candle confirmation.
                """.trimIndent()
                _uiState.update { it.copy(isGenerating = false) }
                generateIndicatorProject(
                    name = "$symbol Chart-to-Indicator",
                    descriptionPrompt = combinedPrompt,
                    category = ForgeCategory.AI_INTELLIGENCE,
                    platform = ForgePlatform.DUAL,
                    complexity = ForgeComplexity.PRO,
                    pattern = SetupDrawingPattern.LIQUIDITY_SWEEP,
                    mtfTimeframes = listOf("H1", timeframe),
                    fastPeriod = 21,
                    slowPeriod = 50,
                    atrPeriod = 14,
                    rrTarget = 2.4
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, snackbarMessage = "Chart-to-Indicator failed: ${e.localizedMessage}") }
            }
        }
    }

    fun createFromTemplate(templateId: String) {
        val tpl = ForgeTemplates.presets.firstOrNull { it.id == templateId } ?: return
        generateIndicatorProject(
            name = tpl.name,
            descriptionPrompt = "",
            category = tpl.category,
            platform = ForgePlatform.DUAL,
            complexity = ForgeComplexity.PRO,
            pattern = tpl.pattern,
            mtfTimeframes = tpl.defaultTimeframes,
            fastPeriod = tpl.fastPeriod,
            slowPeriod = tpl.slowPeriod,
            atrPeriod = tpl.atrPeriod,
            rrTarget = tpl.rrTarget
        )
    }

    fun saveEditorVersion(changeSummary: String, isStable: Boolean = true) {
        val st = _uiState.value
        val proj = projects.value.firstOrNull { it.id == st.selectedProjectId } ?: return
        val nextVer = bumpPatchVersion(proj.currentVersion)
        val stem = IndicatorForgeEngine.sanitizeFileStem(proj.name)

        viewModelScope.launch {
            try {
                if (st.editorMql5Code.isNotBlank()) {
                    val vMq5 = IndicatorForgeEngine.validateSource(ForgePlatform.MT5, st.editorMql5Code)
                    val fName = "$stem.mq5"
                    fileStorage.writeTextArtifact(proj.id, nextVer, fName, st.editorMql5Code)
                    forgeRepo.saveSourceVersion(
                        currentUserId,
                        IndicatorSourceDoc(
                            userId = currentUserId,
                            projectId = proj.id,
                            version = nextVer,
                            platform = "MT5",
                            fileName = fName,
                            sourceCode = st.editorMql5Code,
                            changeSummary = changeSummary.ifBlank { "Manual editor update" },
                            createdByAgent = userDisplayName,
                            isStable = isStable,
                            repaintRiskNotes = "Repaint Risk: ${vMq5.repaintRiskLevel}",
                            validationPassed = vMq5.passed,
                            validationSummary = vMq5.summary
                        )
                    )
                }
                if (st.editorPineCode.isNotBlank()) {
                    val vPine = IndicatorForgeEngine.validateSource(ForgePlatform.TRADINGVIEW, st.editorPineCode)
                    val fName = "$stem.pine"
                    fileStorage.writeTextArtifact(proj.id, nextVer, fName, st.editorPineCode)
                    forgeRepo.saveSourceVersion(
                        currentUserId,
                        IndicatorSourceDoc(
                            userId = currentUserId,
                            projectId = proj.id,
                            version = nextVer,
                            platform = "TRADINGVIEW",
                            fileName = fName,
                            sourceCode = st.editorPineCode,
                            changeSummary = changeSummary.ifBlank { "Manual Pine v6 update" },
                            createdByAgent = userDisplayName,
                            isStable = isStable,
                            repaintRiskNotes = "Repaint Risk: ${vPine.repaintRiskLevel}",
                            validationPassed = vPine.passed,
                            validationSummary = vPine.summary
                        )
                    )
                }
                forgeRepo.updateProject(currentUserId, proj.id, mapOf("currentVersion" to nextVer))
                _uiState.update { it.copy(hasUnsavedEditorChanges = false, snackbarMessage = "Saved version $nextVer.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Save version failed: ${e.localizedMessage}") }
            }
        }
    }

    fun restoreSourceVersion(src: IndicatorSourceDoc) {
        val proj = projects.value.firstOrNull { it.id == src.projectId } ?: return
        if (src.platform == "MT5") {
            _uiState.update { it.copy(editorMql5Code = src.sourceCode, hasUnsavedEditorChanges = true) }
        } else {
            _uiState.update { it.copy(editorPineCode = src.sourceCode, hasUnsavedEditorChanges = true) }
        }
        saveEditorVersion("Restored from v${src.version} (${src.platform})", isStable = src.isStable)
    }

    fun compileActiveProjectMt5(workerUrl: String?) {
        val st = _uiState.value
        val proj = projects.value.firstOrNull { it.id == st.selectedProjectId } ?: return
        val code = st.editorMql5Code
        if (code.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "No MQL5 source code present in active project.") }
            return
        }
        val fName = "${IndicatorForgeEngine.sanitizeFileStem(proj.name)}.mq5"

        viewModelScope.launch {
            _uiState.update { it.copy(isCompiling = true) }
            try {
                if (!workerUrl.isNullOrBlank()) {
                    coreRepo.saveIntegration(
                        currentUserId,
                        "mt5_compiler_worker",
                        com.example.data.model.IntegrationDoc(
                            userId = currentUserId,
                            type = "MT5_COMPILER_WORKER",
                            name = "MetaEditor Compiler Worker",
                            endpointUrl = workerUrl.trim(),
                            status = "CONFIGURED",
                            liveExecutionAllowed = false,
                            lastSyncSummary = "Compilation requested"
                        )
                    )
                }
                val res = compilerClient.compileMq5(workerUrl, fName, code)
                if (res.compiledBinaryBytes != null && res.outputFileName.isNotBlank()) {
                    val savedEx5 = fileStorage.writeBinaryArtifact(proj.id, proj.currentVersion, res.outputFileName, res.compiledBinaryBytes)
                    _uiState.update {
                        it.copy(compiledBinaryFiles = it.compiledBinaryFiles + ("${proj.id}_${proj.currentVersion}" to savedEx5.file))
                    }
                    forgeRepo.updateProject(currentUserId, proj.id, mapOf("devStatus" to "COMPILED"))
                } else if (res.status == ForgeBuildStatus.COMPILATION_FAILED) {
                    forgeRepo.updateProject(currentUserId, proj.id, mapOf("devStatus" to "FAILED"))
                }
                forgeRepo.saveBuildRecord(
                    currentUserId,
                    IndicatorBuildDoc(
                        userId = currentUserId,
                        projectId = proj.id,
                        sourceVersion = proj.currentVersion,
                        platform = "MT5",
                        compiler = res.compilerName,
                        buildStatus = res.status.name,
                        errorsSummary = res.errorsSummary,
                        warningsSummary = res.warningsSummary,
                        outputFileName = res.outputFileName,
                        hasCompiledBinary = res.compiledBinaryBytes != null
                    )
                )
                _uiState.update {
                    it.copy(isCompiling = false, snackbarMessage = "${res.status.label}: ${res.outputFileName.ifBlank { res.errorsSummary.take(80) }}")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isCompiling = false, snackbarMessage = "Compile error: ${e.localizedMessage}") }
            }
        }
    }

    fun repairActiveCodeWithAi(platform: ForgePlatform) {
        val st = _uiState.value
        val proj = projects.value.firstOrNull { it.id == st.selectedProjectId } ?: return
        val broken = if (platform == ForgePlatform.TRADINGVIEW) st.editorPineCode else st.editorMql5Code
        if (broken.isBlank()) {
            _uiState.update { it.copy(snackbarMessage = "No ${platform.label} source to repair.") }
            return
        }
        val valReport = IndicatorForgeEngine.validateSource(platform, broken)
        val diagText = valReport.findings.joinToString("\n") { "L${it.lineNumber} [${it.severity}]: ${it.message} (${it.suggestion})" }
            .ifBlank { "Audit for repainting, lookahead leakage, and strict syntax compliance." }
        val dsModel = st.providerTelemetry.firstOrNull { it.provider == AiProviderType.DEEPSEEK }?.selectedModel ?: "deepseek-chat"

        viewModelScope.launch {
            _uiState.update { it.copy(isRepairing = true) }
            try {
                val (repaired, logs) = orchestrator.repairSourceWithAi(platform, broken, diagText, dsModel)
                recordProviderTelemetry(logs)
                if (platform == ForgePlatform.TRADINGVIEW) {
                    updateEditorPine(repaired)
                } else {
                    updateEditorMql5(repaired)
                }
                _uiState.update { it.copy(isRepairing = false) }
                saveEditorVersion("AI Code Repair (${platform.name})", isStable = true)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isRepairing = false, snackbarMessage = "AI repair error: ${e.localizedMessage}") }
            }
        }
    }

    fun prepareExportArtifact(fileType: String): PendingExportRequest? {
        val st = _uiState.value
        val proj = projects.value.firstOrNull { it.id == st.selectedProjectId } ?: return null
        val stem = IndicatorForgeEngine.sanitizeFileStem(proj.name)
        val ver = proj.currentVersion

        return when (fileType) {
            "MQ5" -> {
                if (st.editorMql5Code.isBlank()) return null
                val saved = fileStorage.writeTextArtifact(proj.id, ver, "$stem.mq5", st.editorMql5Code)
                PendingExportRequest(proj.id, proj.name, ver, "MT5", "MQ5", saved.file, "text/plain")
            }
            "EX5" -> {
                val bin = st.compiledBinaryFiles["${proj.id}_$ver"] ?: return null
                if (!bin.exists()) return null
                PendingExportRequest(proj.id, proj.name, ver, "MT5", "EX5", bin, "application/octet-stream")
            }
            "PINE" -> {
                if (st.editorPineCode.isBlank()) return null
                val saved = fileStorage.writeTextArtifact(proj.id, ver, "$stem.pine", st.editorPineCode)
                PendingExportRequest(proj.id, proj.name, ver, "TRADINGVIEW", "PINE", saved.file, "text/plain")
            }
            "TXT" -> {
                val content = st.editorPineCode.ifBlank { st.editorMql5Code }
                if (content.isBlank()) return null
                val saved = fileStorage.writeTextArtifact(proj.id, ver, "${stem}_source.txt", content)
                PendingExportRequest(proj.id, proj.name, ver, proj.platform, "TXT", saved.file, "text/plain")
            }
            "ZIP" -> {
                val entries = mutableMapOf<String, ByteArray>()
                if (st.editorMql5Code.isNotBlank()) entries["MT5/$stem.mq5"] = st.editorMql5Code.toByteArray()
                if (st.editorPineCode.isNotBlank()) entries["TradingView/$stem.pine"] = st.editorPineCode.toByteArray()
                entries["README_INSTALL.txt"] = "J.A.X. Indicator Forge Package\nProject: ${proj.name}\nVersion: $ver\n${proj.equivalenceNotes}".toByteArray()
                val bin = st.compiledBinaryFiles["${proj.id}_$ver"]
                if (bin != null && bin.exists()) entries["MT5/${bin.name}"] = bin.readBytes()
                val saved = fileStorage.buildZipBundle(proj.id, ver, "${stem}_v$ver.zip", entries)
                PendingExportRequest(proj.id, proj.name, ver, proj.platform, "ZIP", saved.file, "application/zip")
            }
            else -> null
        }
    }

    fun completeExportToUri(req: PendingExportRequest, targetUri: Uri) {
        viewModelScope.launch {
            try {
                val bytesWritten = fileStorage.exportToUserUri(req.localFile, targetUri)
                forgeRepo.recordDownload(
                    currentUserId,
                    IndicatorDownloadDoc(
                        userId = currentUserId,
                        projectId = req.projectId,
                        projectName = req.projectName,
                        version = req.version,
                        platform = req.platform,
                        fileType = req.fileType,
                        fileName = req.localFile.name,
                        byteSize = bytesWritten
                    )
                )
                val proj = projects.value.firstOrNull { it.id == req.projectId }
                if (proj != null) {
                    forgeRepo.updateProject(currentUserId, proj.id, mapOf("downloadCount" to proj.downloadCount + 1))
                }
                _uiState.update { it.copy(snackbarMessage = "Downloaded ${req.localFile.name} ($bytesWritten bytes).") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Export error: ${e.localizedMessage}") }
            }
        }
    }

    fun recordShareDownload(req: PendingExportRequest) {
        viewModelScope.launch {
            try {
                forgeRepo.recordDownload(
                    currentUserId,
                    IndicatorDownloadDoc(
                        userId = currentUserId,
                        projectId = req.projectId,
                        projectName = req.projectName,
                        version = req.version,
                        platform = req.platform,
                        fileType = req.fileType,
                        fileName = req.localFile.name,
                        byteSize = req.localFile.length().toInt()
                    )
                )
            } catch (_: Exception) {}
        }
    }

    fun toggleFavorite(proj: IndicatorProjectDoc) {
        viewModelScope.launch {
            try {
                forgeRepo.updateProject(currentUserId, proj.id, mapOf("isFavorite" to !proj.isFavorite))
            } catch (_: Exception) {}
        }
    }

    fun savePresetForActive(symbol: String, timeframe: String, presetName: String, settingsSummary: String, riskPct: Double) {
        val proj = projects.value.firstOrNull { it.id == _uiState.value.selectedProjectId } ?: return
        viewModelScope.launch {
            try {
                forgeRepo.savePreset(
                    currentUserId,
                    IndicatorPresetDoc(
                        userId = currentUserId,
                        projectId = proj.id,
                        projectName = proj.name,
                        symbol = symbol,
                        timeframe = timeframe,
                        presetName = presetName.ifBlank { "$symbol $timeframe Preset" },
                        settingsSummary = settingsSummary,
                        riskPerTradePct = riskPct.coerceIn(0.1, 10.0),
                        drawingStyleSummary = "Entry:${proj.entryColorHex} SL:${proj.slColorHex} TP:${proj.tpColorHex} ClosedOnly:${proj.closedCandleOnly}"
                    )
                )
                _uiState.update { it.copy(snackbarMessage = "Preset '$presetName' saved.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Preset error: ${e.localizedMessage}") }
            }
        }
    }

    fun submitActiveToMarketplace(releaseNotes: String) {
        val st = _uiState.value
        val proj = projects.value.firstOrNull { it.id == st.selectedProjectId } ?: return
        viewModelScope.launch {
            try {
                forgeRepo.submitMarketplaceListing(
                    currentUserId,
                    userDisplayName,
                    MarketplaceListingDoc(
                        creatorUid = currentUserId,
                        creatorName = userDisplayName,
                        projectId = proj.id,
                        name = proj.name,
                        description = proj.description,
                        category = proj.category,
                        platform = proj.platform,
                        priceUsd = 0.0,
                        listingStatus = "PENDING_REVIEW",
                        version = proj.currentVersion,
                        mql5Source = st.editorMql5Code,
                        pineSource = st.editorPineCode,
                        releaseNotes = releaseNotes.ifBlank { "Submitted from J.A.X. Developer Studio" }
                    )
                )
                forgeRepo.updateProject(currentUserId, proj.id, mapOf("visibility" to "MARKETPLACE_SUBMITTED"))
                _uiState.update { it.copy(snackbarMessage = "Submitted '${proj.name}' to Marketplace review queue.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Submit failed: ${e.localizedMessage}") }
            }
        }
    }

    fun importListingIntoForge(listing: MarketplaceListingDoc) {
        viewModelScope.launch {
            try {
                val id = forgeRepo.createProject(
                    currentUserId,
                    IndicatorProjectDoc(
                        userId = currentUserId,
                        name = listing.name,
                        description = listing.description,
                        category = listing.category,
                        platform = listing.platform,
                        complexity = "PRO",
                        devStatus = "VALIDATED",
                        currentVersion = listing.version,
                        visibility = "PRIVATE",
                        mtfTimeframes = listOf("H1", "M15"),
                        setupPattern = "Marketplace Import",
                        closedCandleOnly = true,
                        equivalenceNotes = listing.releaseNotes
                    )
                )
                if (listing.mql5Source.isNotBlank()) {
                    forgeRepo.saveSourceVersion(
                        currentUserId,
                        IndicatorSourceDoc(
                            userId = currentUserId,
                            projectId = id,
                            version = listing.version,
                            platform = "MT5",
                            fileName = "${IndicatorForgeEngine.sanitizeFileStem(listing.name)}.mq5",
                            sourceCode = listing.mql5Source,
                            changeSummary = "Imported from Marketplace",
                            createdByAgent = listing.creatorName,
                            isStable = true,
                            validationPassed = true,
                            validationSummary = "Marketplace listing imported"
                        )
                    )
                }
                if (listing.pineSource.isNotBlank()) {
                    forgeRepo.saveSourceVersion(
                        currentUserId,
                        IndicatorSourceDoc(
                            userId = currentUserId,
                            projectId = id,
                            version = listing.version,
                            platform = "TRADINGVIEW",
                            fileName = "${IndicatorForgeEngine.sanitizeFileStem(listing.name)}.pine",
                            sourceCode = listing.pineSource,
                            changeSummary = "Imported Pine v6 from Marketplace",
                            createdByAgent = listing.creatorName,
                            isStable = true,
                            validationPassed = true,
                            validationSummary = "Marketplace listing imported"
                        )
                    )
                }
                forgeRepo.incrementListingDownloadCount(listing.id, listing.downloadCount)
                _uiState.update { it.copy(selectedProjectId = id, editorMql5Code = listing.mql5Source, editorPineCode = listing.pineSource, snackbarMessage = "Imported '${listing.name}' into your Forge Library.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Import error: ${e.localizedMessage}") }
            }
        }
    }

    fun loadListingReviews(listingId: String) {
        viewModelScope.launch {
            forgeRepo.observeListingReviews(listingId)
                .catch {}
                .collect { _selectedListingReviews.value = it }
        }
    }

    fun addReview(listing: MarketplaceListingDoc, rating: Int, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            try {
                forgeRepo.addMarketplaceReview(currentUserId, userDisplayName, listing.id, rating, text.trim(), listing.avgRating, listing.reviewCount)
                _uiState.update { it.copy(snackbarMessage = "Review posted.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Review failed: ${e.localizedMessage}") }
            }
        }
    }

    fun moderateListing(listingId: String, newStatus: String) {
        viewModelScope.launch {
            try {
                forgeRepo.moderateMarketplaceListing(listingId, newStatus)
                _uiState.update { it.copy(snackbarMessage = "Listing marked $newStatus.") }
            } catch (e: Exception) {
                _uiState.update { it.copy(snackbarMessage = "Admin moderation error: ${e.localizedMessage}") }
            }
        }
    }

    fun setRoutingMode(mode: JaxCoreRoutingMode) {
        _uiState.update { it.copy(routingMode = mode) }
    }

    fun selectSpecialistAgent(agent: SpecialistAgentDef) {
        _uiState.update { it.copy(selectedAgent = agent) }
    }

    fun runCouncilPrompt(prompt: String, marketContext: String) {
        if (prompt.isBlank()) return
        val st = _uiState.value
        val gTel = st.providerTelemetry.firstOrNull { it.provider == AiProviderType.GEMINI }
        val dTel = st.providerTelemetry.firstOrNull { it.provider == AiProviderType.DEEPSEEK }

        viewModelScope.launch {
            _uiState.update { it.copy(isCouncilRunning = true) }
            try {
                val (result, logs) = orchestrator.runCouncilOrAgent(
                    prompt = prompt.trim(),
                    marketContext = marketContext,
                    mode = st.routingMode,
                    agent = st.selectedAgent,
                    geminiEnabled = gTel?.enabled ?: true,
                    deepseekEnabled = dTel?.enabled ?: true,
                    deepseekModel = dTel?.selectedModel ?: "deepseek-chat"
                )
                recordProviderTelemetry(logs)
                forgeRepo.saveAiCouncilRun(
                    currentUserId,
                    AiCouncilRunDoc(
                        userId = currentUserId,
                        prompt = result.prompt,
                        mode = result.mode.code,
                        geminiSummary = result.geminiResponse,
                        deepseekSummary = result.deepseekResponse,
                        agreements = result.agreements,
                        disagreements = result.disagreements,
                        verifiedSummary = result.finalRecommendation,
                        usedProviders = result.usedProviders
                    )
                )
                _uiState.update { it.copy(isCouncilRunning = false, latestCouncilResult = result) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(isCouncilRunning = false, snackbarMessage = "JAX-CORE error: ${e.localizedMessage}") }
            }
        }
    }

    fun testAndDiscoverDeepSeek() {
        viewModelScope.launch {
            val res = orchestrator.deepSeekRepo.discoverModels()
            _uiState.update { st ->
                val updated = st.providerTelemetry.map { tel ->
                    if (tel.provider == AiProviderType.DEEPSEEK) {
                        tel.copy(
                            connected = res.connected,
                            statusLabel = res.message,
                            discoveredModels = res.models.ifEmpty { tel.discoveredModels },
                            selectedModel = res.models.firstOrNull() ?: tel.selectedModel,
                            lastLatencyMs = res.latencyMs,
                            rateLimitEvents = tel.rateLimitEvents + if (res.rateLimited) 1 else 0
                        )
                    } else tel
                }
                st.copy(providerTelemetry = updated, snackbarMessage = res.message)
            }
        }
    }

    fun selectDeepSeekModel(modelId: String) {
        _uiState.update { st ->
            st.copy(providerTelemetry = st.providerTelemetry.map {
                if (it.provider == AiProviderType.DEEPSEEK) it.copy(selectedModel = modelId) else it
            })
        }
    }

    fun toggleProviderEnabled(provider: AiProviderType, enabled: Boolean) {
        _uiState.update { st ->
            st.copy(providerTelemetry = st.providerTelemetry.map {
                if (it.provider == provider) it.copy(enabled = enabled) else it
            })
        }
    }

    fun toggleAutoUpdates(enabled: Boolean) {
        _uiState.update { it.copy(autoUpdateNotifications = enabled) }
    }

    private suspend fun recordProviderTelemetry(records: List<ProviderInvokeRecord>) {
        if (records.isEmpty()) return
        _uiState.update { st ->
            var list = st.providerTelemetry
            records.forEach { rec ->
                list = list.map { tel ->
                    if (tel.provider == rec.provider) {
                        val newCount = tel.requestCount + 1
                        val newErr = tel.errorCount + if (rec.success) 0 else 1
                        val newAvg = (((tel.avgLatencyMs ?: rec.latencyMs) * tel.requestCount) + rec.latencyMs) / newCount.coerceAtLeast(1)
                        tel.copy(
                            requestCount = newCount,
                            errorCount = newErr,
                            lastLatencyMs = rec.latencyMs,
                            avgLatencyMs = newAvg,
                            totalTokens = tel.totalTokens + rec.totalTokens,
                            estimatedCostUsd = tel.estimatedCostUsd + rec.estimatedCostUsd,
                            lastError = rec.errorMessage.ifBlank { tel.lastError }
                        )
                    } else tel
                }
            }
            st.copy(providerTelemetry = list)
        }
        records.forEach { rec ->
            try {
                forgeRepo.logAiUsage(
                    currentUserId,
                    AiUsageDoc(
                        userId = currentUserId,
                        provider = rec.provider.name,
                        modelId = rec.modelId,
                        agentName = rec.agentName,
                        mode = rec.mode,
                        latencyMs = rec.latencyMs.toInt(),
                        totalTokens = rec.totalTokens,
                        estimatedCostUsd = rec.estimatedCostUsd,
                        success = rec.success,
                        errorMessage = rec.errorMessage
                    )
                )
            } catch (_: Exception) {}
        }
    }

    private fun bumpPatchVersion(ver: String): String {
        val parts = ver.split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = (parts.getOrNull(2)?.toIntOrNull() ?: 0) + 1
        return "$major.$minor.$patch"
    }

    private companion object {
        const val TAG = "JaxForgeViewModel"
    }
}
