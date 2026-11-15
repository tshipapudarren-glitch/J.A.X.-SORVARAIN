package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.ui.graphics.vector.ImageVector

enum class JaxSectionGroup(val title: String) {
    CORE("Core Terminal"),
    INTELLIGENCE("30 VIP & AI Learning"),
    EXECUTION_RESEARCH("Setups & Quant Labs"),
    INDICATOR_FORGE("J.A.X. Indicator Forge"),
    DUAL_AI("Gemini + DeepSeek Dual AI"),
    GOVERNANCE("Risk, Bridges & System")
}

enum class JaxSection(
    val route: String,
    val title: String,
    val shortLabel: String,
    val icon: ImageVector,
    val group: JaxSectionGroup
) {
    COMMAND_CENTRE("command_centre", "Command Centre", "Command", Icons.Filled.Dashboard, JaxSectionGroup.CORE),
    LIVE_MARKET("live_market", "Live Market Intelligence", "Markets", Icons.AutoMirrored.Filled.ShowChart, JaxSectionGroup.CORE),
    AI_CHART_ANALYSER("ai_chart_analyser", "AI Chart Analyser", "Chart", Icons.Filled.CandlestickChart, JaxSectionGroup.CORE),
    VIP_INDICATORS("vip_indicators", "30 VIP Indicators", "30 VIP", Icons.Filled.Layers, JaxSectionGroup.INTELLIGENCE),
    SELF_LEARNING("self_learning", "Self-Learning Engine", "Learning", Icons.Filled.Psychology, JaxSectionGroup.INTELLIGENCE),
    INDICATOR_PERFORMANCE("indicator_performance", "Indicator Performance", "Perf", Icons.Filled.Analytics, JaxSectionGroup.INTELLIGENCE),
    LEARNING_HISTORY("learning_history", "Learning History", "Versions", Icons.Filled.History, JaxSectionGroup.INTELLIGENCE),
    SETUP_GENERATOR("setup_generator", "Setup Generator", "Setups", Icons.Filled.RocketLaunch, JaxSectionGroup.EXECUTION_RESEARCH),
    MARKET_SCANNER("market_scanner", "Global Market Scanner", "Scanner", Icons.Filled.Search, JaxSectionGroup.EXECUTION_RESEARCH),
    MTF_INTELLIGENCE("mtf_intelligence", "Multi-Timeframe Matrix", "MTF", Icons.Filled.AccountTree, JaxSectionGroup.EXECUTION_RESEARCH),
    LIQUIDITY_RADAR("liquidity_radar", "Liquidity Radar", "Liquidity", Icons.Filled.Radar, JaxSectionGroup.EXECUTION_RESEARCH),
    BEHAVIOR_DNA("behavior_dna", "Market Behaviour DNA", "DNA", Icons.Filled.Fingerprint, JaxSectionGroup.EXECUTION_RESEARCH),
    PREDICTION_LAB("prediction_lab", "Prediction Laboratory", "Predict", Icons.Filled.Biotech, JaxSectionGroup.EXECUTION_RESEARCH),
    BACKTESTING_LAB("backtesting_lab", "Backtesting Laboratory", "Backtest", Icons.Filled.Science, JaxSectionGroup.EXECUTION_RESEARCH),
    STRATEGY_OPT("strategy_opt", "Strategy Optimisation", "Strategy", Icons.Filled.Build, JaxSectionGroup.EXECUTION_RESEARCH),
    TRADE_INVESTIGATOR("trade_investigator", "Trade Investigator", "Journal", Icons.Filled.Insights, JaxSectionGroup.EXECUTION_RESEARCH),

    // J.A.X. INDICATOR FORGE (24 Modules)
    FORGE_COMMAND_CENTRE("forge_command_centre", "Indicator Command Centre", "Forge", Icons.Filled.Build, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_AI_GENERATOR("forge_ai_generator", "AI Indicator Generator", "AI Forge", Icons.Filled.AutoAwesome, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_WORKSPACE("forge_workspace", "Indicator Forge", "Workspace", Icons.Filled.Code, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_DUAL_BUILDER("forge_dual_builder", "Dual Platform Builder", "Dual", Icons.Filled.CompareArrows, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_MARKETPLACE("forge_marketplace", "Indicator Marketplace", "Market", Icons.Filled.Storefront, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_MY_INDICATORS("forge_my_indicators", "My Indicators", "Library", Icons.Filled.Folder, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_MY_DOWNLOADS("forge_my_downloads", "My Downloads", "Downloads", Icons.Filled.Download, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_MT5_LIBRARY("forge_mt5_library", "MT5 Indicator Library", "MT5 Lib", Icons.Filled.Terminal, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_TV_LIBRARY("forge_tv_library", "TradingView Indicator Library", "Pine Lib", Icons.Filled.Dns, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_CODE_EDITOR("forge_code_editor", "Code Editor", "Editor", Icons.Filled.Code, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_COMPILATION("forge_compilation", "Compilation Centre", "Compile", Icons.Filled.Memory, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_CODE_REPAIR("forge_code_repair", "Code Repair Centre", "Repair", Icons.Filled.AutoFixHigh, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_BACKTESTING("forge_backtesting", "Backtesting Centre", "Forge BT", Icons.Filled.Science, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_CHART_PREVIEW("forge_chart_preview", "Chart Preview Studio", "Preview", Icons.Filled.CandlestickChart, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_SETUP_DRAWING("forge_setup_drawing", "Setup Drawing Designer", "Drawings", Icons.Filled.Palette, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_MTF_BUILDER("forge_mtf_builder", "Multi-Timeframe Builder", "MTF Build", Icons.Filled.AccountTree, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_PRESET_MANAGER("forge_preset_manager", "Preset Manager", "Presets", Icons.Filled.Tune, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_VERSIONS("forge_versions", "Indicator Version Control", "Versions", Icons.Filled.History, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_ANALYTICS("forge_analytics", "Performance Analytics", "Analytics", Icons.Filled.Analytics, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_INSTALLATION("forge_installation", "Installation Centre", "Install", Icons.Filled.InstallMobile, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_DEV_STUDIO("forge_dev_studio", "Developer Studio", "Dev Studio", Icons.Filled.DeveloperMode, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_UPDATES("forge_updates", "Indicator Update Centre", "Updates", Icons.Filled.SystemUpdate, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_ADMIN("forge_admin", "Admin Indicator Management", "Admin", Icons.Filled.AdminPanelSettings, JaxSectionGroup.INDICATOR_FORGE),
    FORGE_SETTINGS("forge_settings", "Indicator Settings", "Forge Cfg", Icons.Filled.Settings, JaxSectionGroup.INDICATOR_FORGE),

    // GEMINI + DEEPSEEK DUAL AI SYSTEM
    AI_COUNCIL_ARENA("ai_council_arena", "JAX-CORE & AI Council", "Council", Icons.Filled.Hub, JaxSectionGroup.DUAL_AI),
    AI_API_MANAGEMENT("ai_api_management", "Gemini + DeepSeek APIs", "AI APIs", Icons.Filled.Api, JaxSectionGroup.DUAL_AI),

    RISK_INTELLIGENCE("risk_intelligence", "Risk Intelligence", "Risk", Icons.Filled.Shield, JaxSectionGroup.GOVERNANCE),
    TRADING_SESSIONS("trading_sessions", "Trading Sessions", "Sessions", Icons.Filled.Schedule, JaxSectionGroup.GOVERNANCE),
    ECONOMIC_NEWS("economic_news", "Economic News Intelligence", "News", Icons.Filled.Newspaper, JaxSectionGroup.GOVERNANCE),
    MT5_INTEGRATION("mt5_integration", "MT5 Integration", "MT5 EA", Icons.Filled.Hub, JaxSectionGroup.GOVERNANCE),
    TRADINGVIEW_INTEGRATION("tradingview_integration", "TradingView Integration", "Pine v6", Icons.Filled.Dns, JaxSectionGroup.GOVERNANCE),
    ALERTS_CENTRE("alerts_centre", "Alerts Centre", "Alerts", Icons.Filled.NotificationsActive, JaxSectionGroup.GOVERNANCE),
    AI_RESEARCH_CENTRE("ai_research_centre", "AI Research Centre", "Copilot", Icons.Filled.AutoAwesome, JaxSectionGroup.GOVERNANCE),
    SYSTEM_HEALTH("system_health", "System Health", "Health", Icons.Filled.MonitorHeart, JaxSectionGroup.GOVERNANCE),
    SETTINGS("settings", "Settings & Approvals", "Settings", Icons.Filled.Settings, JaxSectionGroup.GOVERNANCE);

    companion object {
        val primaryTabs = listOf(COMMAND_CENTRE, AI_CHART_ANALYSER, VIP_INDICATORS, FORGE_COMMAND_CENTRE, AI_COUNCIL_ARENA)
        fun fromRoute(route: String?): JaxSection =
            entries.firstOrNull { it.route == route } ?: COMMAND_CENTRE
    }
}
