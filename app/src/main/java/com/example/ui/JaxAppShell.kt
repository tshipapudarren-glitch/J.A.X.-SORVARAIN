package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProviderHealthState
import com.example.ui.components.CommandPaletteDialog
import com.example.ui.components.ConnectionDot
import com.example.ui.navigation.JaxSection
import com.example.ui.navigation.JaxSectionGroup
import com.example.ui.screens.AiChartAnalyserScreen
import com.example.ui.screens.AiIndicatorGeneratorScreen
import com.example.ui.screens.AiResearchCentreScreen
import com.example.ui.screens.AlertsCentreScreen
import com.example.ui.screens.BacktestingLabScreen
import com.example.ui.screens.CommandCentreScreen
import com.example.ui.screens.DualAiApiManagementScreen
import com.example.ui.screens.DualPlatformBuilderScreen
import com.example.ui.screens.EconomicNewsScreen
import com.example.ui.screens.ForgeAdminManagementScreen
import com.example.ui.screens.ForgeBacktestAndAnalyticsScreen
import com.example.ui.screens.ForgeCommandCentreScreen
import com.example.ui.screens.ForgeCompilationAndRepairScreen
import com.example.ui.screens.ForgeDownloadsAndLibraryScreen
import com.example.ui.screens.ForgeMarketplaceAndDevScreen
import com.example.ui.screens.ForgeMtfAndPresetsScreen
import com.example.ui.screens.ForgePreviewAndDrawingDesignerScreen
import com.example.ui.screens.ForgeVersionsInstallUpdatesScreen
import com.example.ui.screens.ForgeWorkspaceEditorScreen
import com.example.ui.screens.GlobalMarketScannerScreen
import com.example.ui.screens.IndicatorPerformanceScreen
import com.example.ui.screens.JaxCoreCouncilScreen
import com.example.ui.screens.LearningHistoryScreen
import com.example.ui.screens.LiquidityRadarScreen
import com.example.ui.screens.LiveMarketIntelligenceScreen
import com.example.ui.screens.MarketBehaviorDnaScreen
import com.example.ui.screens.Mt5IntegrationScreen
import com.example.ui.screens.MultiTimeframeScreen
import com.example.ui.screens.PredictionLabScreen
import com.example.ui.screens.RiskIntelligenceScreen
import com.example.ui.screens.SelfLearningEngineScreen
import com.example.ui.screens.SettingsAndApprovalsScreen
import com.example.ui.screens.SetupGeneratorScreen
import com.example.ui.screens.StrategyOptimisationScreen
import com.example.ui.screens.SystemHealthScreen
import com.example.ui.screens.TradeInvestigatorScreen
import com.example.ui.screens.TradingSessionsScreen
import com.example.ui.screens.TradingViewIntegrationScreen
import com.example.ui.screens.VipIndicatorsScreen
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverText
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.JaxForgeViewModel
import com.example.ui.viewmodel.JaxViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JaxAppShell(
    viewModel: JaxViewModel,
    forgeViewModel: JaxForgeViewModel
) {
    var currentRoute by rememberSaveable { mutableStateOf(JaxSection.COMMAND_CENTRE.route) }
    val currentSection = JaxSection.fromRoute(currentRoute)

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showCommandPalette by remember { mutableStateOf(false) }

    val market by viewModel.marketState.collectAsStateWithLifecycle()
    val forgeUi by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val primaryProviderState = market.providers.firstOrNull()?.state ?: ProviderHealthState.DISCONNECTED

    LaunchedEffect(market.snackbarMessage) {
        val msg = market.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(forgeUi.snackbarMessage) {
        val msg = forgeUi.snackbarMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            forgeViewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = currentSection != JaxSection.COMMAND_CENTRE) {
        currentRoute = JaxSection.COMMAND_CENTRE.route
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CharcoalSurface,
                drawerContentColor = CrispWhite,
                modifier = Modifier.width(300.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                        Text("J.A.X. OMNIVERSE", style = MaterialTheme.typography.titleLarge, color = BrightGold, fontWeight = FontWeight.Bold)
                        Text("${JaxSection.entries.size} Autonomous Modules · Forge & Dual AI", style = MaterialTheme.typography.labelSmall, color = SilverText)
                    }
                    HorizontalDivider(color = SubtleDivider)
                    JaxSectionGroup.entries.forEach { group ->
                        Text(
                            text = group.title.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MetallicGold,
                            modifier = Modifier.padding(start = 18.dp, top = 14.dp, bottom = 4.dp)
                        )
                        JaxSection.entries.filter { it.group == group }.forEach { section ->
                            val selected = section == currentSection
                            NavigationDrawerItem(
                                selected = selected,
                                onClick = {
                                    currentRoute = section.route
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(section.icon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                label = { Text(section.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = GoldContainer,
                                    selectedIconColor = BrightGold,
                                    selectedTextColor = BrightGold,
                                    unselectedContainerColor = CharcoalSurface,
                                    unselectedIconColor = SilverText,
                                    unselectedTextColor = CrispWhite
                                ),
                                modifier = Modifier
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                                    .testTag("drawer_nav_${section.route}")
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().background(ObsidianBlack)) {
            val useRail = maxWidth >= 600.dp
            val liveArmed = profile?.liveTradingEnabled == true

            Scaffold(
                containerColor = ObsidianBlack,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = CharcoalSurface,
                            titleContentColor = CrispWhite
                        ),
                        navigationIcon = {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.size(48.dp).testTag("open_drawer_btn")
                            ) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open 25 module drawer", tint = MetallicGold)
                            }
                        },
                        title = {
                            Column {
                                Text(
                                    text = currentSection.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = CrispWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    ConnectionDot(primaryProviderState)
                                    Text(
                                        text = "${market.selectedInstrument.symbol} · ${market.selectedTimeframe.code}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SilverText,
                                        maxLines = 1
                                    )
                                }
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = { showCommandPalette = true },
                                modifier = Modifier.size(48.dp).testTag("command_palette_btn")
                            ) {
                                Icon(Icons.Filled.Search, contentDescription = "Open Command Palette", tint = BrightGold)
                            }
                            IconButton(
                                onClick = { viewModel.triggerEmergencyStop() },
                                modifier = Modifier.size(48.dp).testTag("topbar_emergency_stop_btn")
                            ) {
                                Icon(
                                    Icons.Filled.Shield,
                                    contentDescription = if (liveArmed) "Trigger Emergency Stop" else "Live execution locked safe",
                                    tint = if (liveArmed) WarningAmber else BullishEmerald
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = CharcoalSurface,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            JaxSection.primaryTabs.forEach { tab ->
                                val selected = currentSection == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentRoute = tab.route },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.shortLabel, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = BrightGold,
                                        selectedTextColor = BrightGold,
                                        indicatorColor = GoldContainer,
                                        unselectedIconColor = SilverText,
                                        unselectedTextColor = SilverText
                                    ),
                                    modifier = Modifier.testTag("bottom_nav_${tab.route}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = CharcoalSurface,
                            modifier = Modifier.fillMaxHeight()
                        ) {
                            Spacer(Modifier.height(8.dp))
                            JaxSection.primaryTabs.forEach { tab ->
                                val selected = currentSection == tab
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { currentRoute = tab.route },
                                    icon = { Icon(tab.icon, contentDescription = tab.title) },
                                    label = { Text(tab.shortLabel, maxLines = 1) },
                                    colors = NavigationRailItemDefaults.colors(
                                        selectedIconColor = BrightGold,
                                        selectedTextColor = BrightGold,
                                        indicatorColor = GoldContainer,
                                        unselectedIconColor = SilverText,
                                        unselectedTextColor = SilverText
                                    )
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        val navigateTo: (JaxSection) -> Unit = { sec -> currentRoute = sec.route }
                        when (currentSection) {
                            JaxSection.COMMAND_CENTRE -> CommandCentreScreen(viewModel, navigateTo)
                            JaxSection.LIVE_MARKET -> LiveMarketIntelligenceScreen(viewModel, navigateTo)
                            JaxSection.AI_CHART_ANALYSER -> AiChartAnalyserScreen(viewModel)
                            JaxSection.VIP_INDICATORS -> VipIndicatorsScreen(viewModel)
                            JaxSection.SELF_LEARNING -> SelfLearningEngineScreen(viewModel)
                            JaxSection.INDICATOR_PERFORMANCE -> IndicatorPerformanceScreen(viewModel)
                            JaxSection.LEARNING_HISTORY -> LearningHistoryScreen(viewModel)
                            JaxSection.SETUP_GENERATOR -> SetupGeneratorScreen(viewModel, navigateTo)
                            JaxSection.MARKET_SCANNER -> GlobalMarketScannerScreen(viewModel, navigateTo)
                            JaxSection.MTF_INTELLIGENCE -> MultiTimeframeScreen(viewModel)
                            JaxSection.LIQUIDITY_RADAR -> LiquidityRadarScreen(viewModel)
                            JaxSection.BEHAVIOR_DNA -> MarketBehaviorDnaScreen(viewModel)
                            JaxSection.PREDICTION_LAB -> PredictionLabScreen(viewModel)
                            JaxSection.BACKTESTING_LAB -> BacktestingLabScreen(viewModel)
                            JaxSection.STRATEGY_OPT -> StrategyOptimisationScreen(viewModel)
                            JaxSection.TRADE_INVESTIGATOR -> TradeInvestigatorScreen(viewModel)
                            JaxSection.FORGE_COMMAND_CENTRE -> ForgeCommandCentreScreen(forgeViewModel, navigateTo)
                            JaxSection.FORGE_AI_GENERATOR -> AiIndicatorGeneratorScreen(forgeViewModel, viewModel, navigateTo)
                            JaxSection.FORGE_WORKSPACE -> ForgeWorkspaceEditorScreen(forgeViewModel, navigateTo, initialTab = 0)
                            JaxSection.FORGE_DUAL_BUILDER -> DualPlatformBuilderScreen(forgeViewModel, navigateTo)
                            JaxSection.FORGE_MARKETPLACE -> ForgeMarketplaceAndDevScreen(forgeViewModel, initialDevStudio = false)
                            JaxSection.FORGE_MY_INDICATORS -> ForgeDownloadsAndLibraryScreen(forgeViewModel, initialTab = 0)
                            JaxSection.FORGE_MY_DOWNLOADS -> ForgeDownloadsAndLibraryScreen(forgeViewModel, initialTab = 1)
                            JaxSection.FORGE_MT5_LIBRARY -> ForgeDownloadsAndLibraryScreen(forgeViewModel, initialTab = 2)
                            JaxSection.FORGE_TV_LIBRARY -> ForgeDownloadsAndLibraryScreen(forgeViewModel, initialTab = 3)
                            JaxSection.FORGE_CODE_EDITOR -> ForgeWorkspaceEditorScreen(forgeViewModel, navigateTo, initialTab = 0)
                            JaxSection.FORGE_COMPILATION -> ForgeCompilationAndRepairScreen(forgeViewModel, viewModel, initialFocusRepair = false)
                            JaxSection.FORGE_CODE_REPAIR -> ForgeCompilationAndRepairScreen(forgeViewModel, viewModel, initialFocusRepair = true)
                            JaxSection.FORGE_BACKTESTING -> ForgeBacktestAndAnalyticsScreen(forgeViewModel, viewModel, initialFocusAnalytics = false)
                            JaxSection.FORGE_CHART_PREVIEW -> ForgePreviewAndDrawingDesignerScreen(forgeViewModel, viewModel, initialFocusDesigner = false)
                            JaxSection.FORGE_SETUP_DRAWING -> ForgePreviewAndDrawingDesignerScreen(forgeViewModel, viewModel, initialFocusDesigner = true)
                            JaxSection.FORGE_MTF_BUILDER -> ForgeMtfAndPresetsScreen(forgeViewModel, viewModel, initialFocusPresets = false)
                            JaxSection.FORGE_PRESET_MANAGER -> ForgeMtfAndPresetsScreen(forgeViewModel, viewModel, initialFocusPresets = true)
                            JaxSection.FORGE_VERSIONS -> ForgeVersionsInstallUpdatesScreen(forgeViewModel, initialTab = 0)
                            JaxSection.FORGE_ANALYTICS -> ForgeBacktestAndAnalyticsScreen(forgeViewModel, viewModel, initialFocusAnalytics = true)
                            JaxSection.FORGE_INSTALLATION -> ForgeVersionsInstallUpdatesScreen(forgeViewModel, initialTab = 1)
                            JaxSection.FORGE_DEV_STUDIO -> ForgeMarketplaceAndDevScreen(forgeViewModel, initialDevStudio = true)
                            JaxSection.FORGE_UPDATES -> ForgeVersionsInstallUpdatesScreen(forgeViewModel, initialTab = 2)
                            JaxSection.FORGE_ADMIN -> ForgeAdminManagementScreen(forgeViewModel)
                            JaxSection.FORGE_SETTINGS -> ForgeVersionsInstallUpdatesScreen(forgeViewModel, initialTab = 3)
                            JaxSection.AI_COUNCIL_ARENA -> JaxCoreCouncilScreen(forgeViewModel, viewModel)
                            JaxSection.AI_API_MANAGEMENT -> DualAiApiManagementScreen(forgeViewModel)
                            JaxSection.RISK_INTELLIGENCE -> RiskIntelligenceScreen(viewModel)
                            JaxSection.TRADING_SESSIONS -> TradingSessionsScreen(viewModel)
                            JaxSection.ECONOMIC_NEWS -> EconomicNewsScreen(viewModel)
                            JaxSection.MT5_INTEGRATION -> Mt5IntegrationScreen(viewModel)
                            JaxSection.TRADINGVIEW_INTEGRATION -> TradingViewIntegrationScreen(viewModel)
                            JaxSection.ALERTS_CENTRE -> AlertsCentreScreen(viewModel)
                            JaxSection.AI_RESEARCH_CENTRE -> AiResearchCentreScreen(viewModel)
                            JaxSection.SYSTEM_HEALTH -> SystemHealthScreen(viewModel)
                            JaxSection.SETTINGS -> SettingsAndApprovalsScreen(viewModel)
                        }
                    }
                }
            }
        }
    }

    if (showCommandPalette) {
        CommandPaletteDialog(
            onDismiss = { showCommandPalette = false },
            onSelectSection = { currentRoute = it.route }
        )
    }
}
