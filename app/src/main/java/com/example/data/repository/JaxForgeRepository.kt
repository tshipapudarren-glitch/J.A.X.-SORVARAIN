package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AiCouncilRunDoc
import com.example.data.model.AiUsageDoc
import com.example.data.model.IndicatorBuildDoc
import com.example.data.model.IndicatorDownloadDoc
import com.example.data.model.IndicatorPresetDoc
import com.example.data.model.IndicatorProjectDoc
import com.example.data.model.IndicatorReviewDoc
import com.example.data.model.IndicatorSourceDoc
import com.example.data.model.MarketplaceListingDoc
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

class JaxForgeRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun userDoc(uid: String) = db.collection("users").document(uid)

    private fun <T> Flow<T>.wrapErrors(operationType: OperationType, path: String): Flow<T> =
        catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            handleFirestoreError(fsErr ?: error, operationType, path)
            throw error
        }

    fun observeProjects(uid: String): Flow<List<IndicatorProjectDoc>> {
        val col = userDoc(uid).collection("indicatorProjects")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(IndicatorProjectDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    fun observeSources(uid: String): Flow<List<IndicatorSourceDoc>> {
        val col = userDoc(uid).collection("indicatorSources")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(IndicatorSourceDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    fun observeBuilds(uid: String): Flow<List<IndicatorBuildDoc>> {
        val col = userDoc(uid).collection("indicatorBuilds")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(IndicatorBuildDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    fun observeDownloads(uid: String): Flow<List<IndicatorDownloadDoc>> {
        val col = userDoc(uid).collection("indicatorDownloads")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(IndicatorDownloadDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    fun observePresets(uid: String): Flow<List<IndicatorPresetDoc>> {
        val col = userDoc(uid).collection("indicatorPresets")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(IndicatorPresetDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    fun observeAiUsageLogs(uid: String): Flow<List<AiUsageDoc>> {
        val col = userDoc(uid).collection("aiUsageLogs")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(AiUsageDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    fun observeAiCouncilRuns(uid: String): Flow<List<AiCouncilRunDoc>> {
        val col = userDoc(uid).collection("aiCouncilRuns")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(AiCouncilRunDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    fun observeMarketplaceListings(): Flow<List<MarketplaceListingDoc>> {
        val col = db.collection("marketplaceListings")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(MarketplaceListingDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    fun observeListingReviews(listingId: String): Flow<List<IndicatorReviewDoc>> {
        val col = db.collection("marketplaceListings").document(listingId).collection("reviews")
        return col.snapshots()
            .map { snap ->
                snap.toObjects(IndicatorReviewDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .wrapErrors(OperationType.LIST, col.path)
    }

    suspend fun createProject(uid: String, doc: IndicatorProjectDoc): String {
        val docRef = userDoc(uid).collection("indicatorProjects").document()
        val payload = mapOf(
            "userId" to uid,
            "name" to doc.name.take(100),
            "description" to doc.description.take(1000),
            "category" to doc.category.take(50),
            "platform" to doc.platform,
            "complexity" to doc.complexity,
            "devStatus" to doc.devStatus,
            "currentVersion" to doc.currentVersion.take(24),
            "visibility" to doc.visibility,
            "isFavorite" to doc.isFavorite,
            "mtfTimeframes" to doc.mtfTimeframes.take(12),
            "setupPattern" to doc.setupPattern.take(60),
            "closedCandleOnly" to doc.closedCandleOnly,
            "entryColorHex" to doc.entryColorHex.take(16),
            "slColorHex" to doc.slColorHex.take(16),
            "tpColorHex" to doc.tpColorHex.take(16),
            "equivalenceNotes" to doc.equivalenceNotes.take(3000),
            "downloadCount" to doc.downloadCount.coerceAtLeast(0),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun updateProject(uid: String, projectId: String, updates: Map<String, Any>) {
        val docRef = userDoc(uid).collection("indicatorProjects").document(projectId)
        val merged = updates.toMutableMap().apply {
            put("updatedAt", FieldValue.serverTimestamp())
        }
        docRef.update(merged).await()
    }

    suspend fun deleteProject(uid: String, projectId: String) {
        userDoc(uid).collection("indicatorProjects").document(projectId).delete().await()
    }

    suspend fun saveSourceVersion(uid: String, doc: IndicatorSourceDoc): String {
        val docRef = userDoc(uid).collection("indicatorSources").document()
        val payload = mapOf(
            "userId" to uid,
            "projectId" to doc.projectId,
            "version" to doc.version.take(24),
            "platform" to doc.platform,
            "fileName" to doc.fileName.take(120),
            "sourceCode" to doc.sourceCode.take(200000),
            "changeSummary" to doc.changeSummary.take(400),
            "createdByAgent" to doc.createdByAgent.take(80),
            "isStable" to doc.isStable,
            "repaintRiskNotes" to doc.repaintRiskNotes.take(1000),
            "validationPassed" to doc.validationPassed,
            "validationSummary" to doc.validationSummary.take(1500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun updateSourceStability(uid: String, sourceId: String, isStable: Boolean) {
        userDoc(uid).collection("indicatorSources").document(sourceId).update(
            mapOf("isStable" to isStable, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
    }

    suspend fun saveBuildRecord(uid: String, doc: IndicatorBuildDoc): String {
        val docRef = userDoc(uid).collection("indicatorBuilds").document()
        val payload = mapOf(
            "userId" to uid,
            "projectId" to doc.projectId,
            "sourceVersion" to doc.sourceVersion.take(24),
            "platform" to doc.platform,
            "compiler" to doc.compiler.take(80),
            "buildStatus" to doc.buildStatus,
            "errorsSummary" to doc.errorsSummary.take(4000),
            "warningsSummary" to doc.warningsSummary.take(4000),
            "outputFileName" to doc.outputFileName.take(120),
            "hasCompiledBinary" to doc.hasCompiledBinary,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun recordDownload(uid: String, doc: IndicatorDownloadDoc): String {
        val docRef = userDoc(uid).collection("indicatorDownloads").document()
        val payload = mapOf(
            "userId" to uid,
            "projectId" to doc.projectId,
            "projectName" to doc.projectName.take(100),
            "version" to doc.version.take(24),
            "platform" to doc.platform.take(24),
            "fileType" to doc.fileType,
            "fileName" to doc.fileName.take(120),
            "byteSize" to doc.byteSize.coerceAtLeast(0),
            "createdAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun savePreset(uid: String, doc: IndicatorPresetDoc): String {
        val docRef = userDoc(uid).collection("indicatorPresets").document()
        val payload = mapOf(
            "userId" to uid,
            "projectId" to doc.projectId,
            "projectName" to doc.projectName.take(100),
            "symbol" to doc.symbol.take(24),
            "timeframe" to doc.timeframe.take(10),
            "presetName" to doc.presetName.take(80),
            "settingsSummary" to doc.settingsSummary.take(2000),
            "riskPerTradePct" to doc.riskPerTradePct.coerceIn(0.1, 10.0),
            "drawingStyleSummary" to doc.drawingStyleSummary.take(500),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun deletePreset(uid: String, presetId: String) {
        userDoc(uid).collection("indicatorPresets").document(presetId).delete().await()
    }

    suspend fun logAiUsage(uid: String, doc: AiUsageDoc) {
        val docRef = userDoc(uid).collection("aiUsageLogs").document()
        docRef.set(
            mapOf(
                "userId" to uid,
                "provider" to doc.provider,
                "modelId" to doc.modelId.take(80),
                "agentName" to doc.agentName.take(80),
                "mode" to doc.mode.take(30),
                "latencyMs" to doc.latencyMs.coerceAtLeast(0),
                "totalTokens" to doc.totalTokens.coerceAtLeast(0),
                "estimatedCostUsd" to doc.estimatedCostUsd.coerceAtLeast(0.0),
                "success" to doc.success,
                "errorMessage" to doc.errorMessage.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun saveAiCouncilRun(uid: String, doc: AiCouncilRunDoc) {
        val docRef = userDoc(uid).collection("aiCouncilRuns").document()
        docRef.set(
            mapOf(
                "userId" to uid,
                "prompt" to doc.prompt.take(2000),
                "mode" to doc.mode.take(30),
                "geminiSummary" to doc.geminiSummary.take(8000),
                "deepseekSummary" to doc.deepseekSummary.take(8000),
                "agreements" to doc.agreements.take(4000),
                "disagreements" to doc.disagreements.take(4000),
                "verifiedSummary" to doc.verifiedSummary.take(8000),
                "usedProviders" to doc.usedProviders.take(4),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun submitMarketplaceListing(uid: String, creatorName: String, doc: MarketplaceListingDoc): String {
        val docRef = db.collection("marketplaceListings").document()
        val payload = mapOf(
            "creatorUid" to uid,
            "creatorName" to creatorName.take(80),
            "projectId" to doc.projectId,
            "name" to doc.name.take(100),
            "description" to doc.description.take(2000),
            "category" to doc.category.take(50),
            "platform" to doc.platform,
            "priceUsd" to doc.priceUsd.coerceAtLeast(0.0),
            "listingStatus" to "PENDING_REVIEW",
            "version" to doc.version.take(24),
            "mql5Source" to doc.mql5Source.take(200000),
            "pineSource" to doc.pineSource.take(200000),
            "releaseNotes" to doc.releaseNotes.take(2000),
            "downloadCount" to 0,
            "avgRating" to 0.0,
            "reviewCount" to 0,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(payload).await()
        return docRef.id
    }

    suspend fun moderateMarketplaceListing(listingId: String, newStatus: String) {
        db.collection("marketplaceListings").document(listingId).update(
            mapOf(
                "listingStatus" to newStatus,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun incrementListingDownloadCount(listingId: String, currentCount: Int) {
        db.collection("marketplaceListings").document(listingId).update(
            mapOf(
                "downloadCount" to (currentCount + 1),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    suspend fun addMarketplaceReview(
        uid: String,
        reviewerName: String,
        listingId: String,
        rating: Int,
        reviewText: String,
        currentAvg: Double,
        currentCount: Int
    ) {
        val listingRef = db.collection("marketplaceListings").document(listingId)
        val revRef = listingRef.collection("reviews").document()
        revRef.set(
            mapOf(
                "userId" to uid,
                "reviewerName" to reviewerName.take(80),
                "listingId" to listingId,
                "rating" to rating.coerceIn(1, 5),
                "reviewText" to reviewText.take(1000),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        val newCount = currentCount + 1
        val newAvg = ((currentAvg * currentCount) + rating.coerceIn(1, 5)) / newCount.coerceAtLeast(1)
        listingRef.update(
            mapOf(
                "avgRating" to newAvg,
                "reviewCount" to newCount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }
}
