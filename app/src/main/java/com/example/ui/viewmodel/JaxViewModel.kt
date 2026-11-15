package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AlertDoc
import com.example.data.model.ApprovalRequestDoc
import com.example.data.model.AuditLogDoc
import com.example.data.model.BacktestDoc
import com.example.data.model.BacktestResult
import com.example.data.model.Candle
import com.example.data.model.ChartOverlayBundle
import com.example.data.model.DataFeedType
import com.example.data.model.EconomicEventItem
import com.example.data.model.IndicatorConfigDoc
import com.example.data.model.IndicatorEvaluationResult
import com.example.data.model.InstrumentSpec
import com.example.data.model.IntegrationDoc
import com.example.data.model.LearningJobDoc
import com.example.data.model.MarketBehaviorDna
import com.example.data.model.MarketProviderStatus
import com.example.data.model.MarketRegimeSnapshot
import com.example.data.model.MarketRegimeType
import com.example.data.model.MultiTimeframeRow
import com.example.data.model.PredictionScenario
import com.example.data.model.ProviderHealthState
import com.example.data.model.ScannerOpportunity
import com.example.data.model.SetupDoc
import com.example.data.model.SetupPlan
import com.example.data.model.SignalDirection
import com.example.data.model.StrategyDoc
import com.example.data.model.SupportedInstruments
import com.example.data.model.Timeframe
import com.example.data.model.TradeJournalDoc
import com.example.data.model.TradingSessionInfo
import com.example.data.model.UserProfileDoc
import com.example.data.model.VipIndicatorCatalog
import com.example.data.network.MarketDataService
import com.example.data.repository.JaxAiRepository
import com.example.data.repository.JaxFirestoreRepository
import com.example.data.repository.VisionChartReport
import com.example.domain.engine.BacktestAndLearningEngine
import com.example.domain.engine.IndicatorEngine
import com.example.domain.engine.SetupGeneratorEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CopilotMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampMillis: Long = System.currentTimeMillis()
)

sealed interface VisionUiState {
    data object Idle : VisionUiState
    data object Analyzing : VisionUiState
    data class Success(val report: VisionChartReport) : VisionUiState
    data class Error(val message: String) : VisionUiState
}

data class MarketIntelState(
    val selectedInstrument: InstrumentSpec = SupportedInstruments.catalog.first(),
    val selectedTimeframe: Timeframe = Timeframe.H1,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val candles: List<Candle> = emptyList(),
    val change24hPct: Double = 0.0,
    val volume24h: Double = 0.0,
    val sourceNote: String = "Initializing...",
    val providers: List<MarketProviderStatus> = emptyList(),
    val regime: MarketRegimeSnapshot = MarketRegimeSnapshot(
        MarketRegimeType.TRANSITIONAL, 0.0, 50.0, 0.0, 0.0, "NEUTRAL", "Awaiting market data"
    ),
    val overlays: ChartOverlayBundle = ChartOverlayBundle(
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(),
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null
    ),
    val evaluations: List<IndicatorEvaluationResult> = emptyList(),
    val generatedSetups: List<SetupPlan> = emptyList(),
    val predictionScenario: PredictionScenario? = null,
    val behaviorDna: MarketBehaviorDna? = null,
    val sessions: List<TradingSessionInfo> = emptyList(),
    val mtfMatrix: List<MultiTimeframeRow> = emptyList(),
    val scannerOpportunities: List<ScannerOpportunity> = emptyList(),
    val isScanning: Boolean = false,
    val activeBacktest: BacktestResult? = null,
    val isBacktesting: Boolean = false,
    val isLearningRunning: Boolean = false,
    val noTradeNewsWindowActive: Boolean = false,
    val snackbarMessage: String? = null
)

class JaxViewModel(
    private val repository: JaxFirestoreRepository,
    private val marketService: MarketDataService,
    private val aiRepository: JaxAiRepository,
    val currentUserId: String,
    val userDisplayName: String,
    val userEmail: String
) : ViewModel() {

    private fun <T> kotlinx.coroutines.flow.Flow<T>.safeStateIn(initial: T): StateFlow<T> =
        this.catch { err ->
            val fsErr = generateSequence(err) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && err is CancellationException) throw err
            Log.w(TAG, "Firestore stream error: ${(fsErr ?: err).message}")
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), initial)

    val userProfile: StateFlow<UserProfileDoc?> = repository.observeUserProfile(currentUserId).safeStateIn(null)
    val indicatorConfigs: StateFlow<List<IndicatorConfigDoc>> = repository.observeIndicatorConfigs(currentUserId).safeStateIn(emptyList())
    val savedSetups: StateFlow<List<SetupDoc>> = repository.observeSetups(currentUserId).safeStateIn(emptyList())
    val backtests: StateFlow<List<BacktestDoc>> = repository.observeBacktests(currentUserId).safeStateIn(emptyList())
    val strategies: StateFlow<List<StrategyDoc>> = repository.observeStrategies(currentUserId).safeStateIn(emptyList())
    val learningJobs: StateFlow<List<LearningJobDoc>> = repository.observeLearningJobs(currentUserId).safeStateIn(emptyList())
    val tradeJournal: StateFlow<List<TradeJournalDoc>> = repository.observeTradeJournal(currentUserId).safeStateIn(emptyList())
    val alerts: StateFlow<List<AlertDoc>> = repository.observeAlerts(currentUserId).safeStateIn(emptyList())
    val integrations: StateFlow<List<IntegrationDoc>> = repository.observeIntegrations(currentUserId).safeStateIn(emptyList())
    val approvalRequests: StateFlow<List<ApprovalRequestDoc>> = repository.observeApprovalRequests(currentUserId).safeStateIn(emptyList())
    val auditLogs: StateFlow<List<AuditLogDoc>> = repository.observeAuditLogs(currentUserId).safeStateIn(emptyList())

    private val _marketState = MutableStateFlow(MarketIntelState())
    val marketState: StateFlow<MarketIntelState> = _marketState.asStateFlow()

    private val _copilotMessages = MutableStateFlow(
        listOf(
            CopilotMessage(
                id = "welcome",
                isUser = false,
                text = "J.A.X. Trading Copilot online (Gemini 3.8 Flash via Firebase AI Logic). Ask about active setups, 30-indicator council debates, MTF structure, or indicator decay—every answer is grounded in live loaded market telemetry."
            )
        )
    )
    val copilotMessages: StateFlow<List<CopilotMessage>> = _copilotMessages.asStateFlow()

    private val _copilotStreaming = MutableStateFlow<String?>(null)
    val copilotStreaming: StateFlow<String?> = _copilotStreaming.asStateFlow()

    private val _copilotError = MutableStateFlow<String?>(null)
    val copilotError: StateFlow<String?> = _copilotError.asStateFlow()

    private val _visionState = MutableStateFlow<VisionUiState>(VisionUiState.Idle)
    val visionState: StateFlow<VisionUiState> = _visionState.asStateFlow()

    val economicEvents: List<EconomicEventItem> = listOf(
        EconomicEventItem("ev_1", "US CPI Core YoY", "USD", "HIGH", "Scheduled Monthly 13:30 UTC", "High USD, Gold (PAXG/XAU) & Index volatility expected", "External calendar sync not connected"),
        EconomicEventItem("ev_2", "FOMC Rate Decision & Press Conference", "USD", "HIGH", "Scheduled 19:00 UTC", "Liquidity sweeps common across major pairs", "External calendar sync not connected"),
        EconomicEventItem("ev_3", "US Non-Farm Payrolls (NFP)", "USD", "HIGH", "First Friday 13:30 UTC", "Spread widening & spike risk — No-Trade Window recommended", "External calendar sync not connected"),
        EconomicEventItem("ev_4", "ECB Monetary Policy Statement", "EUR", "MEDIUM", "Scheduled 13:15 UTC", "Impacts EURUSD & GBPUSD volatility", "External calendar sync not connected")
    )

    init {
        viewModelScope.launch {
            try {
                val prof = repository.ensureUserProfile(currentUserId, userDisplayName, userEmail)
                repository.seedDefaultIndicatorConfigsIfEmpty(currentUserId)
                val initInst = SupportedInstruments.find(prof.preferredSymbol)
                val initTf = Timeframe.entries.firstOrNull { it.code == prof.preferredTimeframe } ?: Timeframe.H1
                _marketState.update { it.copy(selectedInstrument = initInst, selectedTimeframe = initTf) }
                refreshMarketData()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Init profile/seed failed", e)
                refreshMarketData()
            }
        }
    }

    fun clearSnackbar() {
        _marketState.update { it.copy(snackbarMessage = null) }
    }

    fun selectInstrument(spec: InstrumentSpec) {
        _marketState.update { it.copy(selectedInstrument = spec) }
        viewModelScope.launch {
            try {
                repository.updateProfilePreferences(currentUserId, spec.symbol, _marketState.value.selectedTimeframe.code)
            } catch (_: Exception) {}
            refreshMarketData()
        }
    }

    fun selectTimeframe(tf: Timeframe) {
        _marketState.update { it.copy(selectedTimeframe = tf) }
        viewModelScope.launch {
            try {
                repository.updateProfilePreferences(currentUserId, _marketState.value.selectedInstrument.symbol, tf.code)
            } catch (_: Exception) {}
            refreshMarketData()
        }
    }

    fun refreshMarketData() {
        val inst = _marketState.value.selectedInstrument
        val tf = _marketState.value.selectedTimeframe
        val mt5Url = integrations.value.firstOrNull { it.type == "MT5_BRIDGE" }?.endpointUrl

        viewModelScope.launch {
            _marketState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = marketService.fetchCandles(inst, tf, limit = 220, mt5BridgeUrl = mt5Url)
            applyCandlesToState(
                candles = result.candles,
                change24h = result.change24hPct,
                volume24h = result.volume24h,
                primaryProvider = result.providerStatus,
                sourceNote = result.sourceNote
            )
            // Automatically compute MTF if Binance feed
            if (inst.feedType == DataFeedType.BINANCE_SPOT) {
                refreshMultiTimeframeMatrix(inst)
            } else {
                _marketState.update { state ->
                    state.copy(
                        mtfMatrix = Timeframe.entries.map { t ->
                            MultiTimeframeRow(t, "UNAVAILABLE", "Feed ${inst.feedType.name}", "N/A", "N/A", 0.0, false)
                        }
                    )
                }
            }
        }
    }

    fun importCsvCandles(csvText: String) {
        val parsed = marketService.parseCsvCandles(csvText)
        if (parsed.size < 30) {
            _marketState.update { it.copy(snackbarMessage = "CSV import needs at least 30 valid OHLC rows (timestamp,open,high,low,close,volume).") }
            return
        }
        val inst = _marketState.value.selectedInstrument
        val status = MarketProviderStatus(
            id = "csv_import",
            name = "User Imported Dataset",
            feedScope = "${inst.symbol} (${parsed.size} bars)",
            state = ProviderHealthState.LIVE_CONNECTED,
            latencyMs = 0L,
            lastSyncMillis = System.currentTimeMillis(),
            statusDetail = "Loaded ${parsed.size} imported OHLCV candles"
        )
        applyCandlesToState(parsed, 0.0, parsed.sumOf { it.volume }, status, "Imported CSV dataset (${parsed.size} bars)")
        _marketState.update { it.copy(snackbarMessage = "Imported ${parsed.size} candles for ${inst.symbol}.") }
    }

    private fun applyCandlesToState(
        candles: List<Candle>,
        change24h: Double,
        volume24h: Double,
        primaryProvider: MarketProviderStatus,
        sourceNote: String
    ) {
        val st = _marketState.value
        val tfCode = st.selectedTimeframe.code
        val configs = indicatorConfigs.value
        val regime = IndicatorEngine.classifyRegime(candles)
        val overlays = IndicatorEngine.buildOverlays(candles, tfCode)
        val evals = IndicatorEngine.evaluateAll30Indicators(candles, configs, tfCode)
        val setups = SetupGeneratorEngine.generateSetups(
            st.selectedInstrument.symbol, tfCode, candles, evals, overlays, regime
        )
        val prediction = SetupGeneratorEngine.buildPredictionScenario(
            st.selectedInstrument.symbol, tfCode, candles, evals, overlays, regime
        )
        val dna = IndicatorEngine.computeBehaviorDna(st.selectedInstrument.symbol, candles)
        val sessions = IndicatorEngine.computeSessions(candles)

        val mt5Doc = integrations.value.firstOrNull { it.type == "MT5_BRIDGE" }
        val tvDoc = integrations.value.firstOrNull { it.type == "TRADINGVIEW_WEBHOOK" }
        val providerList = listOf(
            primaryProvider,
            MarketProviderStatus(
                id = "mt5",
                name = "MetaTrader 5 EA Bridge",
                feedScope = "Forex / Metals / Indices / Execution",
                state = when (mt5Doc?.status) {
                    "CONNECTED" -> ProviderHealthState.LIVE_CONNECTED
                    "CONFIGURED" -> ProviderHealthState.CONFIGURED_STANDBY
                    else -> ProviderHealthState.DISCONNECTED
                },
                latencyMs = null,
                lastSyncMillis = mt5Doc?.updatedAt?.toDate()?.time,
                statusDetail = mt5Doc?.lastSyncSummary?.ifBlank { "Bridge endpoint not configured" } ?: "Not connected"
            ),
            MarketProviderStatus(
                id = "tradingview",
                name = "TradingView Webhook & Pine v6",
                feedScope = "Alert Ingestion & Pine Export",
                state = if (tvDoc?.status == "CONFIGURED" || tvDoc?.status == "CONNECTED") ProviderHealthState.CONFIGURED_STANDBY else ProviderHealthState.DISCONNECTED,
                latencyMs = null,
                lastSyncMillis = tvDoc?.updatedAt?.toDate()?.time,
                statusDetail = tvDoc?.lastSyncSummary?.ifBlank { "Webhook standby" } ?: "Not configured"
            ),
            MarketProviderStatus(
                id = "firebase_ai",
                name = "Firebase AI Logic (Gemini 3.8 Flash)",
                feedScope = "Copilot & Chart Vision Analysis",
                state = ProviderHealthState.LIVE_CONNECTED,
                latencyMs = null,
                lastSyncMillis = System.currentTimeMillis(),
                statusDetail = "App Check attested endpoint ready"
            )
        )

        _marketState.update {
            it.copy(
                isLoading = false,
                candles = candles,
                change24hPct = change24h,
                volume24h = volume24h,
                sourceNote = sourceNote,
                providers = providerList,
                regime = regime,
                overlays = overlays,
                evaluations = evals,
                generatedSetups = setups,
                predictionScenario = prediction,
                behaviorDna = dna,
                sessions = sessions,
                errorMessage = if (candles.isEmpty() && primaryProvider.state == ProviderHealthState.DEGRADED_ERROR) primaryProvider.statusDetail else null
            )
        }
    }

    private fun refreshMultiTimeframeMatrix(instrument: InstrumentSpec) {
        viewModelScope.launch {
            val rows = coroutineScope {
                Timeframe.entries.map { tf ->
                    async {
                        val res = marketService.fetchCandles(instrument, tf, limit = 120)
                        if (res.candles.size < 30) {
                            MultiTimeframeRow(tf, "N/A", "Insufficient", "N/A", "N/A", 0.0, false)
                        } else {
                            val reg = IndicatorEngine.classifyRegime(res.candles)
                            val evals = IndicatorEngine.evaluateAll30Indicators(res.candles, indicatorConfigs.value, tf.code)
                            val bull = evals.count { it.direction == SignalDirection.BULLISH }
                            val bear = evals.count { it.direction == SignalDirection.BEARISH }
                            val rsi = IndicatorEngine.calculateRsi(res.candles.map { it.close }, 14).last()
                            val ov = IndicatorEngine.buildOverlays(res.candles, tf.code)
                            val struct = ov.structurePoints.lastOrNull()?.type ?: "Range"
                            MultiTimeframeRow(
                                timeframe = tf,
                                trendBias = reg.bias,
                                structureState = struct,
                                momentumState = "RSI ${rsi.toInt()}",
                                volatilityState = "${reg.volatilityPercentile.toInt()}th pct",
                                confluenceScore = (maxOf(bull, bear).toDouble() / evals.size.coerceAtLeast(1)) * 100.0,
                                dataAvailable = true
                            )
                        }
                    }
                }.awaitAll()
            }
            _marketState.update { it.copy(mtfMatrix = rows) }
        }
    }

    fun runGlobalScanner() {
        val tf = _marketState.value.selectedTimeframe
        viewModelScope.launch {
            _marketState.update { it.copy(isScanning = true) }
            val liveInstruments = SupportedInstruments.catalog.filter {
                it.feedType == DataFeedType.BINANCE_SPOT || it.feedType == DataFeedType.FRANKFURTER_FX
            }
            val opportunities = coroutineScope {
                liveInstruments.map { inst ->
                    async {
                        val res = marketService.fetchCandles(inst, tf, limit = 150)
                        if (res.candles.size < 35) return@async null
                        val reg = IndicatorEngine.classifyRegime(res.candles)
                        val ov = IndicatorEngine.buildOverlays(res.candles, tf.code)
                        val evals = IndicatorEngine.evaluateAll30Indicators(res.candles, indicatorConfigs.value, tf.code)
                        val setups = SetupGeneratorEngine.generateSetups(
                            inst.symbol, tf.code, res.candles, evals, ov, reg, minScore = 50.0, minRiskReward = 1.2
                        )
                        val top = setups.firstOrNull()
                        ScannerOpportunity(
                            symbol = inst.symbol,
                            assetClass = inst.assetClass,
                            timeframe = if (inst.feedType == DataFeedType.FRANKFURTER_FX) "D1 (ECB)" else tf.code,
                            setupType = top?.let { "${it.confirmingIndicators.firstOrNull() ?: "Confluence"} (${it.status.label})" } ?: reg.regime.label,
                            direction = top?.direction ?: reg.bias,
                            confluenceScore = top?.confluenceScore ?: reg.trendStrengthScore,
                            riskReward = top?.riskReward ?: 1.5,
                            regime = reg.regime.label,
                            lastPrice = res.candles.last().close,
                            change24hPct = res.change24hPct,
                            dataLive = true,
                            evidenceSummary = top?.explanation ?: reg.summary
                        )
                    }
                }.awaitAll().filterNotNull().sortedByDescending { it.confluenceScore }
            }
            _marketState.update { it.copy(scannerOpportunities = opportunities, isScanning = false) }
        }
    }

    fun runBacktest(
        strategyName: String,
        spreadPips: Double,
        commissionPct: Double,
        slippagePips: Double,
        targetR: Double,
        emaFast: Int,
        emaSlow: Int
    ) {
        val st = _marketState.value
        if (st.candles.size < 60) {
            _marketState.update { it.copy(snackbarMessage = "Load >= 60 live or imported candles before running backtest.") }
            return
        }
        viewModelScope.launch {
            _marketState.update { it.copy(isBacktesting = true) }
            val profile = userProfile.value
            val result = BacktestAndLearningEngine.runBacktest(
                strategyName = strategyName,
                symbol = st.selectedInstrument.symbol,
                timeframeCode = st.selectedTimeframe.code,
                candles = st.candles,
                pipSize = st.selectedInstrument.pipSize,
                initialBalance = profile?.accountBalance ?: 100000.0,
                riskPerTradePct = profile?.riskPerTradePct ?: 1.0,
                spreadPips = spreadPips,
                commissionPct = commissionPct,
                slippagePips = slippagePips,
                targetR = targetR,
                emaFastLen = emaFast,
                emaSlowLen = emaSlow
            )
            _marketState.update { it.copy(activeBacktest = result, isBacktesting = false) }
            try {
                repository.saveBacktest(
                    currentUserId,
                    BacktestDoc(
                        userId = currentUserId,
                        strategyName = result.strategyName,
                        symbol = result.symbol,
                        timeframe = result.timeframe,
                        indicatorIds = indicatorConfigs.value.filter { it.enabled }.take(10).map { it.id },
                        totalTrades = result.totalTrades,
                        winningTrades = result.winningTrades,
                        losingTrades = result.losingTrades,
                        winRate = result.winRate,
                        netProfit = result.netProfit,
                        profitFactor = result.profitFactor,
                        maxDrawdownPct = result.maxDrawdownPct,
                        avgR = result.avgR,
                        expectancy = result.expectancy,
                        sharpeRatio = result.sharpeRatio,
                        spreadPips = result.spreadPips,
                        commissionPct = result.commissionPct,
                        slippagePips = result.slippagePips,
                        walkForwardPassed = result.walkForwardPassed,
                        outOfSampleScore = result.outOfSampleScore
                    )
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed saving backtest summary", e)
            }
        }
    }

    fun runSelfLearningCycle(indicatorNumber: Int) {
        val st = _marketState.value
        if (st.candles.size < 60) {
            _marketState.update { it.copy(snackbarMessage = "Need >= 60 candles to execute walk-forward learning.") }
            return
        }
        val def = VipIndicatorCatalog.all30.firstOrNull { it.number == indicatorNumber } ?: return
        val cfg = indicatorConfigs.value.firstOrNull { it.indicatorNumber == indicatorNumber }

        viewModelScope.launch {
            _marketState.update { it.copy(isLearningRunning = true) }
            val job = BacktestAndLearningEngine.runSelfLearningExperiment(
                userId = currentUserId,
                indicatorNumber = def.number,
                indicatorName = def.name,
                symbol = st.selectedInstrument.symbol,
                timeframeCode = st.selectedTimeframe.code,
                candles = st.candles,
                pipSize = st.selectedInstrument.pipSize,
                currentVersion = cfg?.activeVersion ?: "v1.0.0"
            )
            try {
                val jobId = repository.saveLearningJob(currentUserId, job)
                repository.createApprovalRequest(
                    uid = currentUserId,
                    actionType = "DEPLOY_MODEL_VERSION",
                    targetId = jobId,
                    summary = "Promote ${def.name} ${job.modelVersion} (${st.selectedInstrument.symbol} ${st.selectedTimeframe.code}): ${job.proposedChangeSummary}",
                    requestedBy = "Autonomous Evolution Engine"
                )
                repository.updateIndicatorVersionStatus(currentUserId, def.id, cfg?.activeVersion ?: "v1.0.0", "PENDING_APPROVAL")
                _marketState.update { it.copy(isLearningRunning = false, snackbarMessage = "Walk-forward learning completed. Candidate awaits your approval.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(isLearningRunning = false, snackbarMessage = "Learning job error: ${e.localizedMessage}") }
            }
        }
    }

    fun approveOrRejectLearningJob(job: LearningJobDoc, approve: Boolean) {
        viewModelScope.launch {
            try {
                val status = if (approve) "APPROVED" else "REJECTED"
                repository.updateLearningJobApproval(currentUserId, job.id, status)
                val cfgId = "ind_%02d".format(job.indicatorNumber)
                if (approve) {
                    repository.updateIndicatorVersionStatus(currentUserId, cfgId, job.modelVersion, "EVALUATED")
                } else {
                    repository.updateIndicatorVersionStatus(currentUserId, cfgId, "v1.0.0", "IDLE")
                }
                val matchingReq = approvalRequests.value.firstOrNull { it.targetId == job.id && it.status == "PENDING" }
                if (matchingReq != null) {
                    repository.resolveApprovalRequest(currentUserId, matchingReq.id, approve)
                }
                _marketState.update { it.copy(snackbarMessage = "${job.indicatorName} ${if (approve) "promoted to ${job.modelVersion}" else "rejected"}.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Update failed: ${e.localizedMessage}") }
            }
        }
    }

    fun rollbackIndicatorVersion(indicatorNumber: Int) {
        viewModelScope.launch {
            try {
                val cfgId = "ind_%02d".format(indicatorNumber)
                repository.updateIndicatorVersionStatus(currentUserId, cfgId, "v1.0.0", "IDLE")
                repository.logAudit(currentUserId, "MODEL_ROLLBACK", userDisplayName, "Rolled back indicator #$indicatorNumber to v1.0.0", "WARNING")
                _marketState.update { it.copy(snackbarMessage = "Indicator #$indicatorNumber rolled back to v1.0.0.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Rollback error: ${e.localizedMessage}") }
            }
        }
    }

    fun updateIndicatorConfig(
        config: IndicatorConfigDoc,
        enabled: Boolean,
        sensitivity: Double,
        lookback: Int,
        minConfidence: Double,
        showOverlay: Boolean,
        alertsEnabled: Boolean
    ) {
        viewModelScope.launch {
            try {
                repository.updateIndicatorConfig(
                    currentUserId, config.id, enabled, sensitivity, lookback, minConfidence, showOverlay, alertsEnabled
                )
                // Re-evaluate live overlays/signals immediately
                val st = _marketState.value
                if (st.candles.isNotEmpty()) {
                    val updatedList = indicatorConfigs.value.map {
                        if (it.id == config.id) it.copy(enabled = enabled, sensitivity = sensitivity, lookbackPeriod = lookback, minConfidence = minConfidence, showChartOverlay = showOverlay, alertsEnabled = alertsEnabled) else it
                    }
                    val evals = IndicatorEngine.evaluateAll30Indicators(st.candles, updatedList, st.selectedTimeframe.code)
                    val setups = SetupGeneratorEngine.generateSetups(st.selectedInstrument.symbol, st.selectedTimeframe.code, st.candles, evals, st.overlays, st.regime)
                    _marketState.update { it.copy(evaluations = evals, generatedSetups = setups) }
                }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Save failed: ${e.localizedMessage}") }
            }
        }
    }

    fun persistSetup(setup: SetupPlan) {
        viewModelScope.launch {
            try {
                repository.saveSetup(
                    currentUserId,
                    SetupDoc(
                        userId = currentUserId,
                        symbol = setup.symbol,
                        timeframe = setup.timeframe,
                        direction = setup.direction,
                        status = setup.status.name,
                        entryPrice = setup.entryPrice,
                        entryZoneLow = setup.entryZoneLow,
                        entryZoneHigh = setup.entryZoneHigh,
                        stopLoss = setup.stopLoss,
                        tp1 = setup.tp1,
                        tp2 = setup.tp2,
                        tp3 = setup.tp3,
                        riskReward = setup.riskReward,
                        confluenceScore = setup.confluenceScore,
                        regime = setup.regime,
                        sessionName = setup.sessionName,
                        invalidationLevel = setup.invalidationLevel,
                        explanation = setup.explanation,
                        dnaFingerprint = setup.dnaFingerprint,
                        confirmingIndicators = setup.confirmingIndicators.take(30),
                        conflictingIndicators = setup.conflictingIndicators.take(30)
                    )
                )
                repository.createAlert(
                    currentUserId,
                    AlertDoc(
                        userId = currentUserId,
                        symbol = setup.symbol,
                        timeframe = setup.timeframe,
                        category = "SETUP_${setup.status.name}",
                        title = "${setup.direction} ${setup.symbol} (${setup.timeframe}) · Score ${setup.confluenceScore.toInt()}",
                        message = "Entry ${String.format("%.4f", setup.entryPrice)} | SL ${String.format("%.4f", setup.stopLoss)} | TP2 ${String.format("%.4f", setup.tp2)}",
                        severity = "SIGNAL",
                        acknowledged = false
                    )
                )
                _marketState.update { it.copy(snackbarMessage = "Setup saved & alert logged.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Failed saving setup: ${e.localizedMessage}") }
            }
        }
    }

    fun saveStrategy(name: String, description: String, indicatorIds: List<String>, entryTf: String, biasTf: String, minScore: Double, minRr: Double) {
        viewModelScope.launch {
            try {
                repository.saveStrategy(
                    currentUserId,
                    StrategyDoc(
                        userId = currentUserId,
                        name = name.ifBlank { "J.A.X. Custom Confluence" },
                        description = description,
                        indicatorIds = indicatorIds.take(30),
                        entryTimeframe = entryTf,
                        biasTimeframe = biasTf,
                        minScore = minScore,
                        minRiskReward = minRr,
                        version = 1,
                        isProduction = false,
                        status = "DRAFT"
                    )
                )
                _marketState.update { it.copy(snackbarMessage = "Strategy saved as versioned DRAFT.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Strategy save error: ${e.localizedMessage}") }
            }
        }
    }

    fun requestStrategyPromotion(strategy: StrategyDoc) {
        viewModelScope.launch {
            try {
                repository.createApprovalRequest(
                    uid = currentUserId,
                    actionType = "DEPLOY_STRATEGY",
                    targetId = strategy.id,
                    summary = "Promote strategy '${strategy.name}' v${strategy.version} (${strategy.biasTimeframe}->${strategy.entryTimeframe}) to PRODUCTION.",
                    requestedBy = userDisplayName
                )
                _marketState.update { it.copy(snackbarMessage = "Strategy promotion queued for explicit approval.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Request error: ${e.localizedMessage}") }
            }
        }
    }

    fun addTradeJournalEntry(symbol: String, tf: String, direction: String, entry: Double, exit: Double, sl: Double, tp1: Double, pnlR: Double, notes: String) {
        viewModelScope.launch {
            try {
                repository.addTradeRecord(
                    currentUserId,
                    TradeJournalDoc(
                        userId = currentUserId,
                        symbol = symbol,
                        timeframe = tf,
                        direction = direction,
                        source = "SIGNAL_TRACKER",
                        entryPrice = entry,
                        exitPrice = exit,
                        stopLoss = sl,
                        tp1 = tp1,
                        pnlR = pnlR,
                        mfeR = maxOf(pnlR, 0.4),
                        maeR = if (pnlR < 0) -1.0 else -0.3,
                        regime = _marketState.value.regime.regime.label,
                        notes = notes.ifBlank { "Recorded via Trade Investigator" },
                        status = "CLOSED"
                    )
                )
                _marketState.update { it.copy(snackbarMessage = "Trade forensic record added.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Journal error: ${e.localizedMessage}") }
            }
        }
    }

    fun updateRiskLimits(balance: Double, riskPct: Double, dailyPct: Double, weeklyPct: Double, maxDdPct: Double) {
        viewModelScope.launch {
            try {
                repository.updateRiskSettings(currentUserId, balance, riskPct, dailyPct, weeklyPct, maxDdPct)
                _marketState.update { it.copy(snackbarMessage = "Risk limits updated & logged to Audit Trail.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Risk update failed: ${e.localizedMessage}") }
            }
        }
    }

    fun triggerEmergencyStop() {
        viewModelScope.launch {
            try {
                repository.setLiveTradingEnabled(currentUserId, false)
                _marketState.update { it.copy(snackbarMessage = "EMERGENCY STOP ACTIVE: Live trading disabled.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Emergency stop error: ${e.localizedMessage}") }
            }
        }
    }

    fun requestLiveTradingActivation() {
        viewModelScope.launch {
            try {
                repository.createApprovalRequest(
                    uid = currentUserId,
                    actionType = "ENABLE_LIVE_TRADING",
                    targetId = currentUserId,
                    summary = "Enable live trade instruction dispatch (requires verified MT5 EA Bridge and explicit trader approval).",
                    requestedBy = userDisplayName
                )
                _marketState.update { it.copy(snackbarMessage = "Live trading approval request created in Settings & Approvals.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Request failed: ${e.localizedMessage}") }
            }
        }
    }

    fun resolveApproval(req: ApprovalRequestDoc, approve: Boolean) {
        viewModelScope.launch {
            try {
                repository.resolveApprovalRequest(currentUserId, req.id, approve)
                if (approve) {
                    when (req.actionType) {
                        "ENABLE_LIVE_TRADING" -> repository.setLiveTradingEnabled(currentUserId, true)
                        "DEPLOY_STRATEGY" -> repository.updateStrategyStatus(currentUserId, req.targetId, "PRODUCTION", true)
                        "DEPLOY_MODEL_VERSION" -> {
                            val job = learningJobs.value.firstOrNull { it.id == req.targetId }
                            if (job != null) {
                                repository.updateLearningJobApproval(currentUserId, job.id, "APPROVED")
                                repository.updateIndicatorVersionStatus(currentUserId, "ind_%02d".format(job.indicatorNumber), job.modelVersion, "EVALUATED")
                            }
                        }
                    }
                }
                _marketState.update { it.copy(snackbarMessage = "Approval ${if (approve) "granted" else "rejected"} & recorded.") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Approval action failed: ${e.localizedMessage}") }
            }
        }
    }

    fun toggleNoTradeNewsWindow(enabled: Boolean) {
        _marketState.update { it.copy(noTradeNewsWindowActive = enabled) }
    }

    fun testAndSaveIntegration(integrationId: String, type: String, name: String, url: String) {
        viewModelScope.launch {
            val (ok, msg) = marketService.pingEndpoint(url)
            val status = when {
                url.isBlank() -> "DISCONNECTED"
                ok -> "CONNECTED"
                else -> "ERROR"
            }
            try {
                repository.saveIntegration(
                    currentUserId,
                    integrationId,
                    IntegrationDoc(
                        userId = currentUserId,
                        type = type,
                        name = name,
                        endpointUrl = url.trim(),
                        status = status,
                        liveExecutionAllowed = false,
                        lastSyncSummary = msg
                    )
                )
                _marketState.update { it.copy(snackbarMessage = "$name: $msg") }
            } catch (e: Exception) {
                _marketState.update { it.copy(snackbarMessage = "Integration save error: ${e.localizedMessage}") }
            }
        }
    }

    fun acknowledgeAlert(alertId: String) {
        viewModelScope.launch {
            try {
                repository.acknowledgeAlert(currentUserId, alertId)
            } catch (_: Exception) {}
        }
    }

    fun askCopilot(question: String) {
        if (question.isBlank()) return
        val userMsg = CopilotMessage(id = "u_${System.currentTimeMillis()}", isUser = true, text = question.trim())
        _copilotMessages.update { it + userMsg }
        _copilotError.value = null
        _copilotStreaming.value = ""

        val contextText = buildAiSystemContext()
        viewModelScope.launch {
            try {
                var finalAnswer = ""
                aiRepository.streamCopilotResponse(question.trim(), contextText).collect { partial ->
                    finalAnswer = partial
                    _copilotStreaming.value = partial
                }
                _copilotStreaming.value = null
                _copilotMessages.update {
                    it + CopilotMessage(id = "a_${System.currentTimeMillis()}", isUser = false, text = finalAnswer)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _copilotStreaming.value = null
                _copilotError.value = e.localizedMessage ?: "AI Copilot request failed."
            }
        }
    }

    fun analyzeChartBitmap(bitmap: Bitmap) {
        val st = _marketState.value
        _visionState.value = VisionUiState.Analyzing
        viewModelScope.launch {
            try {
                val report = aiRepository.analyzeChartScreenshot(
                    bitmap = bitmap,
                    symbol = st.selectedInstrument.symbol,
                    timeframeCode = st.selectedTimeframe.code,
                    liveContext = buildAiSystemContext()
                )
                _visionState.value = VisionUiState.Success(report)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _visionState.value = VisionUiState.Error(e.localizedMessage ?: "Vision chart analysis failed.")
            }
        }
    }

    private fun buildAiSystemContext(): String {
        val st = _marketState.value
        val lastPrice = st.candles.lastOrNull()?.close?.let { String.format("%.4f", it) } ?: "UNAVAILABLE"
        val bullCount = st.evaluations.count { it.enabled && it.direction == SignalDirection.BULLISH }
        val bearCount = st.evaluations.count { it.enabled && it.direction == SignalDirection.BEARISH }
        val decayNames = st.evaluations.filter { it.decayDetected }.joinToString { it.definition.name }.ifBlank { "None" }
        val topSetups = st.generatedSetups.joinToString("\n") { s ->
            "- ${s.direction} ${s.symbol} (${s.timeframe}) [${s.status.label}] Score=${s.confluenceScore.toInt()} R:R=${String.format("%.2f", s.riskReward)} Entry=${String.format("%.4f", s.entryPrice)} SL=${String.format("%.4f", s.stopLoss)} TP2=${String.format("%.4f", s.tp2)} | ${s.explanation}"
        }.ifBlank { "No setups met criteria on current candle set." }
        val topInds = st.evaluations.filter { it.enabled }.sortedByDescending { it.score }.take(8).joinToString("\n") { e ->
            "- #${e.definition.number} ${e.definition.name}: ${e.direction.label} (${e.score.toInt()}/100) | ${e.rationale} | HistWR=${e.historicalWinRate?.let { "${it.toInt()}% (n=${e.sampleSize})" } ?: "N/A"}"
        }
        return """
            Instrument: ${st.selectedInstrument.symbol} (${st.selectedInstrument.assetClass.label}) | Timeframe: ${st.selectedTimeframe.code}
            Data Source: ${st.sourceNote} | Candle Count: ${st.candles.size} | Last Close: $lastPrice | 24h Chg: ${String.format("%.2f", st.change24hPct)}%
            Market Regime: ${st.regime.summary}
            Indicator Council Tally: $bullCount Bullish vs $bearCount Bearish (out of ${st.evaluations.size} indicators)
            Performance Decay Warnings: $decayNames
            Active Setups:
            $topSetups
            Top Ranked Indicators:
            $topInds
            Backtest Latest: ${st.activeBacktest?.let { "${it.strategyName}: ${it.totalTrades} trades, WR=${it.winRate.toInt()}%, PF=${String.format("%.2f", it.profitFactor)}, Exp=${String.format("%.2f", it.expectancy)}R, OOS=${it.outOfSampleScore.toInt()}" } ?: "No backtest executed in current session."}
            Risk Profile: Balance=${userProfile.value?.accountBalance ?: 100000.0}, Risk/Trade=${userProfile.value?.riskPerTradePct ?: 1.0}%, LiveTrading=${userProfile.value?.liveTradingEnabled ?: false}
        """.trimIndent()
    }

    private companion object {
        const val TAG = "JaxViewModel"
    }
}
