package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.IndicatorProjectDoc
import com.example.data.model.MarketplaceListingDoc
import com.example.data.model.SetupDoc
import com.example.data.model.SetupStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JaxRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun ensureUserProfile_andSeedIndicators_succeedsForOwner() = runBlocking {
        val uid = signInTestUser("alice@jax.test")
        val repo = JaxFirestoreRepository(firestore)

        val profile = withTimeout(5000L) {
            repo.ensureUserProfile(uid, "Alice Quant", "alice@jax.test")
        }
        assertEquals(uid, profile.userId)
        assertFalse(profile.liveTradingEnabled)

        withTimeout(5000L) {
            repo.seedDefaultIndicatorConfigsIfEmpty(uid)
        }
        val configs = withTimeout(4000L) {
            repo.observeIndicatorConfigs(uid).first { it.size == 30 }
        }
        assertEquals(30, configs.size)
    }

    @Test
    fun saveSetup_andObserveSetups_emitsRealtimeSetupForOwner() = runBlocking {
        val uid = signInTestUser("alice@jax.test")
        val repo = JaxFirestoreRepository(firestore)
        repo.ensureUserProfile(uid, "Alice Quant", "alice@jax.test")

        val setup = SetupDoc(
            userId = uid,
            symbol = "BTCUSDT",
            timeframe = "H1",
            direction = "BUY",
            status = SetupStatus.CONFIRMED.name.uppercase(),
            entryPrice = 91500.0,
            entryZoneLow = 91300.0,
            entryZoneHigh = 91600.0,
            stopLoss = 90800.0,
            tp1 = 92400.0,
            tp2 = 93200.0,
            tp3 = 94500.0,
            riskReward = 2.4,
            confluenceScore = 78.5,
            regime = "TRENDING_BULLISH",
            sessionName = "New York",
            invalidationLevel = 90750.0,
            explanation = "EMA stack + bullish OB + FVG alignment.",
            dnaFingerprint = "DNA-BTC-H1-78",
            confirmingIndicators = listOf("Adaptive Trend DNA", "Order Block Precision AI"),
            conflictingIndicators = emptyList()
        )
        val setupId = withTimeout(5000L) { repo.saveSetup(uid, setup) }
        assertTrue(setupId.isNotBlank())

        val setups = withTimeout(4000L) {
            repo.observeSetups(uid).first { list -> list.any { it.id == setupId } }
        }
        assertTrue(setups.any { it.id == setupId && it.symbol == "BTCUSDT" })
    }

    @Test
    fun observeSetups_crossUserAccess_denied() = runBlocking {
        val aliceUid = signInTestUser("alice@jax.test")
        val repo = JaxFirestoreRepository(firestore)
        repo.ensureUserProfile(aliceUid, "Alice Quant", "alice@jax.test")

        signInTestUser("bob@jax.test")
        var caught: Throwable? = null
        try {
            withTimeout(4000L) {
                repo.observeSetups(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED for cross-user access", caught)
        val fsEx = caught!!.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    @Test
    fun observeUserProfile_unauthenticated_denied() = runBlocking {
        auth.signOut()
        val repo = JaxFirestoreRepository(firestore)
        var caught: Throwable? = null
        try {
            withTimeout(4000L) {
                repo.observeUserProfile("alice_missing").first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED when unauthenticated", caught)
        val fsEx = caught!!.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx!!.code)
    }

    @Test
    fun forgeProject_ownerLifecycle_andCrossUserDenied() = runBlocking {
        val aliceUid = signInTestUser("alice@jax.test")
        val forgeRepo = JaxForgeRepository(firestore)
        val projectId = withTimeout(5000L) {
            forgeRepo.createProject(
                aliceUid,
                IndicatorProjectDoc(
                    userId = aliceUid,
                    name = "Liquidity Sweep Forge",
                    description = "MQL5 + Pine v6",
                    category = "Smart Money",
                    platform = "DUAL",
                    complexity = "PRO"
                )
            )
        }
        assertTrue(projectId.isNotBlank())
        val projects = withTimeout(4000L) {
            forgeRepo.observeProjects(aliceUid).first { list -> list.any { it.id == projectId } }
        }
        assertTrue(projects.any { it.name == "Liquidity Sweep Forge" })

        signInTestUser("bob@jax.test")
        var caught: Throwable? = null
        try {
            withTimeout(4000L) {
                forgeRepo.observeProjects(aliceUid).first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Expected PERMISSION_DENIED for cross-user Forge project read", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught!!.unwrapFirestoreException()?.code)
    }

    @Test
    fun marketplaceModeration_nonAdminDenied() = runBlocking {
        val aliceUid = signInTestUser("alice@jax.test")
        val forgeRepo = JaxForgeRepository(firestore)
        val listingId = withTimeout(5000L) {
            forgeRepo.submitMarketplaceListing(
                aliceUid,
                "Alice Quant",
                MarketplaceListingDoc(
                    creatorUid = aliceUid,
                    creatorName = "Alice Quant",
                    projectId = "proj_1",
                    name = "Alpha Sweep",
                    description = "Non-repainting liquidity sweep",
                    category = "Smart Money",
                    platform = "DUAL",
                    listingStatus = "PENDING_REVIEW",
                    version = "1.0.0"
                )
            )
        }
        assertTrue(listingId.isNotBlank())

        var caught: Throwable? = null
        try {
            withTimeout(4000L) {
                forgeRepo.moderateMarketplaceListing(listingId, "APPROVED")
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull("Non-admin must be denied Marketplace approval", caught)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, caught!!.unwrapFirestoreException()?.code)
    }
}
