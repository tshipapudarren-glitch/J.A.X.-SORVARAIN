package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.network.MarketDataService
import com.example.data.network.Mt5CompilerClient
import com.example.data.repository.DeepSeekRepository
import com.example.data.repository.JaxAiRepository
import com.example.data.repository.JaxCoreOrchestrator
import com.example.data.repository.JaxFirestoreRepository
import com.example.data.repository.JaxForgeRepository
import com.example.data.storage.ForgeFileStorage
import com.example.security.configureAppCheck
import com.example.ui.JaxAppShell
import com.example.ui.screens.AuthScreen
import com.example.ui.theme.JaxOmniverseTheme
import com.example.ui.theme.ObsidianBlack
import com.example.ui.viewmodel.JaxForgeViewModel
import com.example.ui.viewmodel.JaxViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        setContent {
            JaxOmniverseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianBlack
                ) {
                    JaxRootGate()
                }
            }
        }
    }

    @Composable
    private fun JaxRootGate() {
        val user by Firebase.auth.authStateFlow()
            .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

        val currentUser = user
        if (currentUser == null) {
            AuthScreen(onAuthSuccess = { /* AuthStateListener updates automatically */ })
        } else {
            key(currentUser.uid) {
                val coreRepo = JaxFirestoreRepository(applicationContext)
                val geminiRepo = JaxAiRepository()
                val jaxViewModel: JaxViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return JaxViewModel(
                                repository = coreRepo,
                                marketService = MarketDataService(),
                                aiRepository = geminiRepo,
                                currentUserId = currentUser.uid,
                                userDisplayName = currentUser.displayName ?: "Trader",
                                userEmail = currentUser.email ?: ""
                            ) as T
                        }
                    }
                )
                val forgeViewModel: JaxForgeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return JaxForgeViewModel(
                                forgeRepo = JaxForgeRepository(applicationContext),
                                coreRepo = coreRepo,
                                orchestrator = JaxCoreOrchestrator(geminiRepo, DeepSeekRepository()),
                                geminiRepo = geminiRepo,
                                compilerClient = Mt5CompilerClient(),
                                fileStorage = ForgeFileStorage(applicationContext),
                                currentUserId = currentUser.uid,
                                userDisplayName = currentUser.displayName ?: "Trader",
                                userEmail = currentUser.email ?: ""
                            ) as T
                        }
                    }
                )
                JaxAppShell(viewModel = jaxViewModel, forgeViewModel = forgeViewModel)
            }
        }
    }
}

private fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}
