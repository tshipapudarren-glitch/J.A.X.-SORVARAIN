package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AlertDoc
import com.example.data.model.ApprovalRequestDoc
import com.example.data.model.AuditLogDoc
import com.example.data.model.BacktestDoc
import com.example.data.model.IndicatorConfigDoc
import com.example.data.model.IntegrationDoc
import com.example.data.model.LearningJobDoc
import com.example.data.model.SetupDoc
import com.example.data.model.StrategyDoc
import com.example.data.model.TradeJournalDoc
import com.example.data.model.UserProfileDoc
import com.example.data.model.VipIndicatorCatalog
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class JaxFirestoreRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun <T> Flow<T>.wrapFirestoreErrors(
        operationType: OperationType,
        path: String
    ): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, operationType, path)
        throw error
    }

    fun observeUserProfile(uid: String): Flow<UserProfileDoc?> {
        val docRef = userDoc(uid)
        return docRef.snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    UserProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .wrapFirestoreErrors(OperationType.GET, docRef.path)
    }

    suspend fun ensureUserProfile(
        uid: String,
        displayName: String,
        email: String
    ): UserProfileDoc {
        val docRef = userDoc(uid)
        try {
            val snap = docRef.get().await()
            if (snap.exists()) {
                return snap.toObject(
                    UserProfileDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ) ?: UserProfileDoc(id = uid, userId = uid, displayName = displayName, email = email)
            }
            val safeName = displayName.ifBlank { "Quant Trader" }.take(80)
            val safeEmail = email.ifBlank { "$uid@jax.local" }.take(160)
            val payload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "email" to safeEmail,
                "role" to "trader",
                "accountBalance" to 100000.0,
                "riskPerTradePct" to 1.0,
                "dailyLossLimitPct" to 3.0,
                "weeklyLossLimitPct" to 6.0,
                "maxDrawdownPct" to 12.0,
                "liveTradingEnabled" to false,
                "preferredSymbol" to "BTCUSDT",
                "preferredTimeframe" to "H1",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            logAudit(uid, "PROFILE_INITIALIZED", safeName, "Created trader profile with live trading disabled by default.", "INFO")
            return UserProfileDoc(
                id = uid,
                userId = uid,
                displayName = safeName,
                email = safeEmail
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun updateProfilePreferences(
        uid: String,
        preferredSymbol: String,
        preferredTimeframe: String
    ) {
        val docRef = userDoc(uid)
        try {
            docRef.update(
                mapOf(
                    "preferredSymbol" to preferredSymbol.take(24),
                    "preferredTimeframe" to preferredTimeframe.take(10),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateRiskSettings(
        uid: String,
        accountBalance: Double,
        riskPerTradePct: Double,
        dailyLossLimitPct: Double,
        weeklyLossLimitPct: Double,
        maxDrawdownPct: Double
    ) {
        val docRef = userDoc(uid)
        try {
            docRef.update(
                mapOf(
                    "accountBalance" to accountBalance.coerceAtLeast(0.0),
                    "riskPerTradePct" to riskPerTradePct.coerceIn(0.1, 10.0),
                    "dailyLossLimitPct" to dailyLossLimitPct.coerceIn(0.5, 25.0),
                    "weeklyLossLimitPct" to weeklyLossLimitPct.coerceIn(1.0, 50.0),
                    "maxDrawdownPct" to maxDrawdownPct.coerceIn(1.0, 80.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit(uid, "RISK_LIMITS_UPDATED", "User", "Updated risk per trade=${riskPerTradePct}%, maxDD=${maxDrawdownPct}%", "WARNING")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun setLiveTradingEnabled(uid: String, enabled: Boolean) {
        val docRef = userDoc(uid)
        try {
            docRef.update(
                mapOf(
                    "liveTradingEnabled" to enabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit(
                uid,
                if (enabled) "LIVE_TRADING_ENABLED" else "EMERGENCY_TRADING_STOP",
                "User",
                if (enabled) "Live execution explicitly approved and activated." else "Emergency stop / live execution disabled.",
                "CRITICAL"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeIndicatorConfigs(uid: String): Flow<List<IndicatorConfigDoc>> {
        val col = userDoc(uid).collection("indicatorConfigs")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    IndicatorConfigDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedBy { it.indicatorNumber }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun seedDefaultIndicatorConfigsIfEmpty(uid: String) {
        val col = userDoc(uid).collection("indicatorConfigs")
        try {
            val existing = col.limit(1).get().await()
            if (!existing.isEmpty) return
            val batch = db.batch()
            VipIndicatorCatalog.all30.forEach { def ->
                val ref = col.document(def.id)
                val data = mapOf(
                    "userId" to uid,
                    "indicatorNumber" to def.number,
                    "name" to def.name,
                    "category" to def.category.name,
                    "enabled" to true,
                    "sensitivity" to def.defaultSensitivity,
                    "lookbackPeriod" to def.defaultLookback,
                    "minConfidence" to def.defaultMinConfidence,
                    "showChartOverlay" to (def.number in listOf(1, 2, 3, 4, 8, 11, 14, 17)),
                    "alertsEnabled" to (def.number in listOf(11, 13, 20, 24, 30)),
                    "activeVersion" to "v1.0.0",
                    "learningStatus" to "IDLE",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                batch.set(ref, data)
            }
            batch.commit().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, col.path)
            throw e
        }
    }

    suspend fun updateIndicatorConfig(
        uid: String,
        configId: String,
        enabled: Boolean,
        sensitivity: Double,
        lookbackPeriod: Int,
        minConfidence: Double,
        showChartOverlay: Boolean,
        alertsEnabled: Boolean
    ) {
        val docRef = userDoc(uid).collection("indicatorConfigs").document(configId)
        try {
            docRef.update(
                mapOf(
                    "enabled" to enabled,
                    "sensitivity" to sensitivity.coerceIn(0.1, 5.0),
                    "lookbackPeriod" to lookbackPeriod.coerceIn(5, 500),
                    "minConfidence" to minConfidence.coerceIn(0.0, 100.0),
                    "showChartOverlay" to showChartOverlay,
                    "alertsEnabled" to alertsEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateIndicatorVersionStatus(
        uid: String,
        configId: String,
        activeVersion: String,
        learningStatus: String
    ) {
        val docRef = userDoc(uid).collection("indicatorConfigs").document(configId)
        try {
            docRef.update(
                mapOf(
                    "activeVersion" to activeVersion.take(24),
                    "learningStatus" to learningStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeSetups(uid: String): Flow<List<SetupDoc>> {
        val col = userDoc(uid).collection("setups")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    SetupDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun saveSetup(uid: String, setup: SetupDoc): String {
        val col = userDoc(uid).collection("setups")
        val docRef = col.document()
        try {
            val payload = mapOf(
                "userId" to uid,
                "symbol" to setup.symbol.take(24),
                "timeframe" to setup.timeframe.take(10),
                "direction" to setup.direction,
                "status" to setup.status,
                "entryPrice" to setup.entryPrice,
                "entryZoneLow" to setup.entryZoneLow,
                "entryZoneHigh" to setup.entryZoneHigh,
                "stopLoss" to setup.stopLoss,
                "tp1" to setup.tp1,
                "tp2" to setup.tp2,
                "tp3" to setup.tp3,
                "riskReward" to setup.riskReward,
                "confluenceScore" to setup.confluenceScore.coerceIn(0.0, 100.0),
                "regime" to setup.regime.take(40),
                "sessionName" to setup.sessionName.take(40),
                "invalidationLevel" to setup.invalidationLevel,
                "explanation" to setup.explanation.take(800),
                "dnaFingerprint" to setup.dnaFingerprint.take(64),
                "confirmingIndicators" to setup.confirmingIndicators.take(30),
                "conflictingIndicators" to setup.conflictingIndicators.take(30),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeBacktests(uid: String): Flow<List<BacktestDoc>> {
        val col = userDoc(uid).collection("backtests")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    BacktestDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun saveBacktest(uid: String, backtest: BacktestDoc): String {
        val docRef = userDoc(uid).collection("backtests").document()
        try {
            val payload = mapOf(
                "userId" to uid,
                "strategyName" to backtest.strategyName.take(100),
                "symbol" to backtest.symbol.take(24),
                "timeframe" to backtest.timeframe.take(10),
                "indicatorIds" to backtest.indicatorIds.take(30),
                "totalTrades" to backtest.totalTrades.coerceAtLeast(0),
                "winningTrades" to backtest.winningTrades.coerceAtLeast(0),
                "losingTrades" to backtest.losingTrades.coerceAtLeast(0),
                "winRate" to backtest.winRate.coerceIn(0.0, 100.0),
                "netProfit" to backtest.netProfit,
                "profitFactor" to backtest.profitFactor.coerceAtLeast(0.0),
                "maxDrawdownPct" to backtest.maxDrawdownPct.coerceAtLeast(0.0),
                "avgR" to backtest.avgR,
                "expectancy" to backtest.expectancy,
                "sharpeRatio" to backtest.sharpeRatio,
                "spreadPips" to backtest.spreadPips.coerceAtLeast(0.0),
                "commissionPct" to backtest.commissionPct.coerceAtLeast(0.0),
                "slippagePips" to backtest.slippagePips.coerceAtLeast(0.0),
                "walkForwardPassed" to backtest.walkForwardPassed,
                "outOfSampleScore" to backtest.outOfSampleScore.coerceIn(0.0, 100.0),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeStrategies(uid: String): Flow<List<StrategyDoc>> {
        val col = userDoc(uid).collection("strategies")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StrategyDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun saveStrategy(uid: String, strategy: StrategyDoc): String {
        val docRef = userDoc(uid).collection("strategies").document()
        try {
            val payload = mapOf(
                "userId" to uid,
                "name" to strategy.name.take(100),
                "description" to strategy.description.take(500),
                "indicatorIds" to strategy.indicatorIds.take(30),
                "entryTimeframe" to strategy.entryTimeframe.take(10),
                "biasTimeframe" to strategy.biasTimeframe.take(10),
                "minScore" to strategy.minScore.coerceIn(0.0, 100.0),
                "minRiskReward" to strategy.minRiskReward.coerceIn(0.5, 20.0),
                "version" to strategy.version.coerceAtLeast(1),
                "isProduction" to false,
                "status" to strategy.status,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateStrategyStatus(uid: String, strategyId: String, status: String, isProduction: Boolean) {
        val docRef = userDoc(uid).collection("strategies").document(strategyId)
        try {
            docRef.update(
                mapOf(
                    "status" to status,
                    "isProduction" to isProduction,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeLearningJobs(uid: String): Flow<List<LearningJobDoc>> {
        val col = userDoc(uid).collection("learningJobs")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    LearningJobDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun saveLearningJob(uid: String, job: LearningJobDoc): String {
        val docRef = userDoc(uid).collection("learningJobs").document()
        try {
            val payload = mapOf(
                "userId" to uid,
                "indicatorNumber" to job.indicatorNumber.coerceIn(1, 30),
                "indicatorName" to job.indicatorName.take(80),
                "symbol" to job.symbol.take(24),
                "timeframe" to job.timeframe.take(10),
                "trainingSamples" to job.trainingSamples.coerceAtLeast(0),
                "validationSamples" to job.validationSamples.coerceAtLeast(0),
                "previousWinRate" to job.previousWinRate.coerceIn(0.0, 100.0),
                "candidateWinRate" to job.candidateWinRate.coerceIn(0.0, 100.0),
                "previousExpectancy" to job.previousExpectancy,
                "candidateExpectancy" to job.candidateExpectancy,
                "proposedChangeSummary" to job.proposedChangeSummary.take(500),
                "modelVersion" to job.modelVersion.take(24),
                "approvalStatus" to job.approvalStatus,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun updateLearningJobApproval(uid: String, jobId: String, approvalStatus: String) {
        val docRef = userDoc(uid).collection("learningJobs").document(jobId)
        try {
            docRef.update(
                mapOf(
                    "approvalStatus" to approvalStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeTradeJournal(uid: String): Flow<List<TradeJournalDoc>> {
        val col = userDoc(uid).collection("tradeRecords")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    TradeJournalDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun addTradeRecord(uid: String, record: TradeJournalDoc): String {
        val docRef = userDoc(uid).collection("tradeRecords").document()
        try {
            val payload = mapOf(
                "userId" to uid,
                "symbol" to record.symbol.take(24),
                "timeframe" to record.timeframe.take(10),
                "direction" to record.direction,
                "source" to record.source,
                "entryPrice" to record.entryPrice,
                "exitPrice" to record.exitPrice,
                "stopLoss" to record.stopLoss,
                "tp1" to record.tp1,
                "pnlR" to record.pnlR,
                "mfeR" to record.mfeR,
                "maeR" to record.maeR,
                "regime" to record.regime.take(40),
                "notes" to record.notes.take(1000),
                "status" to record.status,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    fun observeAlerts(uid: String): Flow<List<AlertDoc>> {
        val col = userDoc(uid).collection("alerts")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AlertDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun createAlert(uid: String, alert: AlertDoc): String {
        val docRef = userDoc(uid).collection("alerts").document()
        try {
            val payload = mapOf(
                "userId" to uid,
                "symbol" to alert.symbol.take(24),
                "timeframe" to alert.timeframe.take(10),
                "category" to alert.category.take(40),
                "title" to alert.title.take(120),
                "message" to alert.message.take(500),
                "severity" to alert.severity,
                "acknowledged" to alert.acknowledged,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun acknowledgeAlert(uid: String, alertId: String) {
        val docRef = userDoc(uid).collection("alerts").document(alertId)
        try {
            docRef.update(
                mapOf(
                    "acknowledged" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeIntegrations(uid: String): Flow<List<IntegrationDoc>> {
        val col = userDoc(uid).collection("integrations")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    IntegrationDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun saveIntegration(uid: String, integrationId: String, doc: IntegrationDoc) {
        val docRef = userDoc(uid).collection("integrations").document(integrationId)
        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "name" to doc.name.take(80),
                        "endpointUrl" to doc.endpointUrl.take(300),
                        "status" to doc.status,
                        "liveExecutionAllowed" to doc.liveExecutionAllowed,
                        "lastSyncSummary" to doc.lastSyncSummary.take(300),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "type" to doc.type,
                        "name" to doc.name.take(80),
                        "endpointUrl" to doc.endpointUrl.take(300),
                        "status" to doc.status,
                        "liveExecutionAllowed" to false,
                        "lastSyncSummary" to doc.lastSyncSummary.take(300),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    fun observeApprovalRequests(uid: String): Flow<List<ApprovalRequestDoc>> {
        val col = userDoc(uid).collection("approvalRequests")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ApprovalRequestDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun createApprovalRequest(
        uid: String,
        actionType: String,
        targetId: String,
        summary: String,
        requestedBy: String
    ): String {
        val docRef = userDoc(uid).collection("approvalRequests").document()
        try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "actionType" to actionType,
                    "targetId" to targetId.take(128),
                    "summary" to summary.take(500),
                    "status" to "PENDING",
                    "requestedBy" to requestedBy.take(80),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit(uid, "APPROVAL_REQUESTED", requestedBy, summary, "INFO")
            return docRef.id
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun resolveApprovalRequest(uid: String, requestId: String, approve: Boolean) {
        val docRef = userDoc(uid).collection("approvalRequests").document(requestId)
        try {
            val newStatus = if (approve) "APPROVED" else "REJECTED"
            docRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            logAudit(uid, "APPROVAL_$newStatus", "User", "Request $requestId marked $newStatus", "WARNING")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    fun observeAuditLogs(uid: String): Flow<List<AuditLogDoc>> {
        val col = userDoc(uid).collection("auditLogs")
        return col.snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    AuditLogDoc::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapFirestoreErrors(OperationType.LIST, col.path)
    }

    suspend fun logAudit(
        uid: String,
        action: String,
        actor: String,
        details: String,
        severity: String = "INFO"
    ) {
        val docRef = userDoc(uid).collection("auditLogs").document()
        try {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "action" to action.take(100),
                    "actor" to actor.take(80),
                    "details" to details.take(500),
                    "severity" to severity,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
        }
    }
}
