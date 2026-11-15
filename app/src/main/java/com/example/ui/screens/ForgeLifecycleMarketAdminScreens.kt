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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.MarketplaceListingDoc
import com.example.ui.components.EquityCurveChart
import com.example.ui.components.JaxGlassCard
import com.example.ui.components.MetricBadge
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusPill
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
fun ForgeVersionsInstallUpdatesScreen(
    forgeViewModel: JaxForgeViewModel,
    initialTab: Int = 0 // 0=Versions, 1=Installation, 2=Updates, 3=Settings
) {
    val sources by forgeViewModel.sources.collectAsStateWithLifecycle()
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember(initialTab) { mutableIntStateOf(initialTab) }

    val projSources = remember(sources, ui.selectedProjectId) {
        val pid = ui.selectedProjectId
        if (pid == null) sources else sources.filter { it.projectId == pid }
    }

    val tabLabels = listOf("Version Control", "Installation", "Update Centre", "Forge Settings")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = tabLabels.getOrElse(activeTab) { "Forge Lifecycle" },
                subtitle = "Source snapshots, step-by-step MT5 / TradingView installation guides, updates & safety defaults"
            )
        }

        item {
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                tabLabels.forEachIndexed { idx, label ->
                    FilterChip(
                        selected = activeTab == idx,
                        onClick = { activeTab = idx },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GoldContainer, selectedLabelColor = BrightGold)
                    )
                }
            }
        }

        when (activeTab) {
            0 -> {
                items(projSources, key = { it.id }) { src ->
                    JaxGlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("v${src.version} · ${src.fileName} (${src.platform})", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                                Text("${src.changeSummary} · By ${src.createdByAgent}", style = MaterialTheme.typography.bodySmall, color = SilverText)
                            }
                            StatusPill(
                                if (src.isStable) "STABLE" else "EXPERIMENTAL",
                                if (src.isStable) BullishEmerald else WarningAmber,
                                if (src.isStable) BullishContainer else WarningContainer
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(src.repaintRiskNotes, style = MaterialTheme.typography.labelSmall, color = CyanQuant)
                        Spacer(Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = { forgeViewModel.restoreSourceVersion(src) },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("Restore Version to New Patch", color = BrightGold)
                        }
                    }
                }
            }
            1 -> {
                item {
                    JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                        Text("MetaTrader 5 Installation Guide (.mq5 / .ex5)", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                        Spacer(Modifier.height(6.dp))
                        Text("1. Download the .mq5 (or worker-compiled .ex5) file from My Downloads.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Text("2. On desktop MetaTrader 5, open File -> Open Data Folder -> MQL5 -> Indicators.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Text("3. Paste the downloaded file into MQL5/Indicators.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Text("4. If using .mq5 source, open MetaEditor (F4) and press F7 (Compile) to generate your local .ex5.", style = MaterialTheme.typography.bodySmall, color = CyanQuant)
                        Text("5. In MT5 Navigator -> Indicators -> Right-click Refresh -> Drag onto chart.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                    }
                }
                item {
                    JaxGlassCard(borderColor = CyanQuant.copy(alpha = 0.45f)) {
                        Text("TradingView Pine Script v6 Guide (.pine / .txt)", style = MaterialTheme.typography.titleMedium, color = CyanQuant)
                        Spacer(Modifier.height(6.dp))
                        Text("1. Tap Copy Pine v6 or Save .pine in My Downloads.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Text("2. Open TradingView -> Pine Editor tab at the bottom of your chart.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Text("3. Clear the default template, paste the //@version=6 script, and tap Save.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Text("4. Tap Add to chart and configure Entry/SL/TP colors & closed-candle inputs.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                    }
                }
            }
            2 -> {
                item {
                    JaxGlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Indicator Update Alerts", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                                Text("Notify when a newer Forge or Marketplace revision ships", style = MaterialTheme.typography.bodySmall, color = SilverText)
                            }
                            Switch(checked = ui.autoUpdateNotifications, onCheckedChange = { forgeViewModel.toggleAutoUpdates(it) })
                        }
                    }
                }
                items(projects, key = { it.id }) { proj ->
                    JaxGlassCard {
                        Text("${proj.name} · Current v${proj.currentVersion}", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                        Text("Status: ${proj.devStatus} · Platform: ${proj.platform}", style = MaterialTheme.typography.bodySmall, color = SilverText)
                    }
                }
            }
            3 -> {
                item {
                    JaxGlassCard {
                        Text("Forge Generation & Export Safety Settings", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                        Spacer(Modifier.height(6.dp))
                        Text("• Default Non-Repainting Guard: Closed-candle evaluation enabled.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Text("• Lookahead Policy: request.security lookahead_off enforced on Pine v6.", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                        Text("• Binary Integrity: .ex5 exports strictly require verified HTTP MetaEditor worker output.", style = MaterialTheme.typography.bodySmall, color = CyanQuant)
                        Text("• Active AI Router: ${ui.routingMode.label}", style = MaterialTheme.typography.bodySmall, color = BullishEmerald)
                    }
                }
            }
        }
    }
}

@Composable
fun ForgeBacktestAndAnalyticsScreen(
    forgeViewModel: JaxForgeViewModel,
    jaxViewModel: JaxViewModel,
    initialFocusAnalytics: Boolean = false
) {
    val market by jaxViewModel.marketState.collectAsStateWithLifecycle()
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()
    val activeProj = projects.firstOrNull { it.id == ui.selectedProjectId }
    val bt = market.activeBacktest

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (initialFocusAnalytics) "Indicator Performance Analytics" else "Forge Backtesting Centre",
                subtitle = "Historical signal evaluation on ${market.selectedInstrument.symbol} (${market.selectedTimeframe.code}). Past performance does not guarantee future results."
            )
        }

        item {
            JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.45f)) {
                Text(activeProj?.name ?: "Active Forge Indicator", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = {
                        jaxViewModel.runBacktest(
                            strategyName = activeProj?.name ?: "J.A.X. Forge Indicator",
                            spreadPips = market.selectedInstrument.defaultSpreadPips,
                            commissionPct = 0.04,
                            slippagePips = 0.5,
                            targetR = 2.2,
                            emaFast = 21,
                            emaSlow = 50
                        )
                    },
                    enabled = !market.isBacktesting,
                    colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("forge_run_backtest_btn")
                ) {
                    Text(if (market.isBacktesting) "Running Historical Simulation..." else "Run Candle-by-Candle Backtest", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (bt != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Win Rate", "${String.format("%.1f", bt.winRate)}%", BrightGold, Modifier.weight(1f))
                        MetricBadge("Profit Factor", String.format("%.2f", bt.profitFactor), BullishEmerald, Modifier.weight(1f))
                        MetricBadge("Max DD", "${String.format("%.1f", bt.maxDrawdownPct)}%", BearishCrimson, Modifier.weight(1f))
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricBadge("Trades", "${bt.totalTrades}", CyanQuant, Modifier.weight(1f))
                        MetricBadge("Avg R", String.format("%.2fR", bt.avgR), MetallicGold, Modifier.weight(1f))
                        MetricBadge("Sharpe", String.format("%.2f", bt.sharpeRatio), CyanQuant, Modifier.weight(1f))
                    }
                    EquityCurveChart(equityCurve = bt.equityCurve)
                }
            }
        }
    }
}

@Composable
fun ForgeMarketplaceAndDevScreen(
    forgeViewModel: JaxForgeViewModel,
    initialDevStudio: Boolean = false
) {
    val listings by forgeViewModel.marketplaceListings.collectAsStateWithLifecycle()
    val reviews by forgeViewModel.selectedListingReviews.collectAsStateWithLifecycle()
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val ui by forgeViewModel.uiState.collectAsStateWithLifecycle()

    var devMode by remember(initialDevStudio) { mutableStateOf(initialDevStudio) }
    var search by remember { mutableStateOf("") }
    var releaseNotes by remember { mutableStateOf("Non-repainting Dual-Platform build with Entry/SL/TP1-TP3 drawings.") }
    var reviewingListing by remember { mutableStateOf<MarketplaceListingDoc?>(null) }
    var reviewText by remember { mutableStateOf("") }
    var reviewRating by remember { mutableIntStateOf(5) }

    val activeProj = projects.firstOrNull { it.id == ui.selectedProjectId }
    val visibleListings = remember(listings, search) {
        listings.filter { it.listingStatus == "APPROVED" && (search.isBlank() || it.name.contains(search, true) || it.category.contains(search, true)) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = if (devMode) "Developer Studio" else "Custom Indicator Marketplace",
                subtitle = "Publish reviewed indicators or import community MT5 & TradingView tools into your Forge library"
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !devMode, onClick = { devMode = false }, label = { Text("Marketplace Catalog") })
                FilterChip(selected = devMode, onClick = { devMode = true }, label = { Text("Developer Studio") })
            }
        }

        if (devMode) {
            item {
                JaxGlassCard(borderColor = MetallicGold.copy(alpha = 0.5f)) {
                    Text("Submit Active Indicator to Marketplace", style = MaterialTheme.typography.titleMedium, color = BrightGold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (activeProj != null) "Project: ${activeProj.name} v${activeProj.currentVersion} (${activeProj.platform})" else "Select a project in Indicator Forge first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = SilverText
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = releaseNotes, onValueChange = { releaseNotes = it }, label = { Text("Release Notes & Compatibility") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { forgeViewModel.submitActiveToMarketplace(releaseNotes) },
                        enabled = activeProj != null,
                        colors = ButtonDefaults.buttonColors(containerColor = MetallicGold, contentColor = ObsidianBlack),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("forge_submit_marketplace_btn")
                    ) {
                        Text("Submit for Admin Moderation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            item {
                OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search Marketplace") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (visibleListings.isEmpty()) {
                item {
                    JaxGlassCard {
                        Text("No approved Marketplace listings yet. Use Developer Studio to submit an indicator for review.", style = MaterialTheme.typography.bodySmall, color = SilverText)
                    }
                }
            } else {
                items(visibleListings, key = { it.id }) { item ->
                    JaxGlassCard {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${item.name} v${item.version}", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                                Text("By ${item.creatorName} · ${item.category} · ${item.platform}", style = MaterialTheme.typography.labelSmall, color = CyanQuant)
                            }
                            StatusPill("${String.format("%.1f", item.avgRating)} ★ (${item.reviewCount})", BullishEmerald, BullishContainer)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = SilverText)
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { forgeViewModel.importListingIntoForge(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldContainer, contentColor = BrightGold),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Import to Forge")
                            }
                            OutlinedButton(
                                onClick = {
                                    reviewingListing = item
                                    forgeViewModel.loadListingReviews(item.id)
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text("Reviews (${item.reviewCount})", color = CrispWhite)
                            }
                        }
                    }
                }
            }
        }
    }

    reviewingListing?.let { target ->
        AlertDialog(
            onDismissRequest = { reviewingListing = null },
            title = { Text("Reviews · ${target.name}", color = BrightGold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        (1..5).forEach { stars ->
                            FilterChip(selected = reviewRating == stars, onClick = { reviewRating = stars }, label = { Text("${stars}★") })
                        }
                    }
                    OutlinedTextField(value = reviewText, onValueChange = { reviewText = it }, label = { Text("Write a review") }, modifier = Modifier.fillMaxWidth())
                    HorizontalDivider(color = SubtleDivider)
                    reviews.take(5).forEach { rev ->
                        Text("${rev.reviewerName} (${rev.rating}★): ${rev.reviewText}", style = MaterialTheme.typography.bodySmall, color = CrispWhite)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    forgeViewModel.addReview(target, reviewRating, reviewText)
                    reviewText = ""
                }) { Text("Post Review") }
            },
            dismissButton = {
                TextButton(onClick = { reviewingListing = null }) { Text("Close") }
            }
        )
    }
}

@Composable
fun ForgeAdminManagementScreen(
    forgeViewModel: JaxForgeViewModel
) {
    val listings by forgeViewModel.marketplaceListings.collectAsStateWithLifecycle()
    val projects by forgeViewModel.projects.collectAsStateWithLifecycle()
    val builds by forgeViewModel.builds.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Admin Indicator Management",
                subtitle = "Server-enforced moderation of Marketplace submissions and compilation telemetry"
            )
        }

        item {
            JaxGlassCard(borderColor = if (forgeViewModel.isAdminUser) BullishEmerald else WarningAmber) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Operator: ${forgeViewModel.userEmail.ifBlank { forgeViewModel.userDisplayName }}", style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                        Text(
                            if (forgeViewModel.isAdminUser) "Authorized Marketplace Moderator" else "Standard User (Approve/Reject enforced by Firestore security rules)",
                            style = MaterialTheme.typography.bodySmall,
                            color = SilverText
                        )
                    }
                    StatusPill(
                        if (forgeViewModel.isAdminUser) "ADMIN" else "USER",
                        if (forgeViewModel.isAdminUser) BullishEmerald else WarningAmber,
                        if (forgeViewModel.isAdminUser) BullishContainer else WarningContainer
                    )
                }
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricBadge("Listings", "${listings.size}", BrightGold, Modifier.weight(1f))
                MetricBadge("Pending", "${listings.count { it.listingStatus == "PENDING_REVIEW" }}", WarningAmber, Modifier.weight(1f))
                MetricBadge("User Projects", "${projects.size}", CyanQuant, Modifier.weight(1f))
                MetricBadge("Builds", "${builds.size}", BullishEmerald, Modifier.weight(1f))
            }
        }

        items(listings, key = { it.id }) { listing ->
            JaxGlassCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${listing.name} v${listing.version}", style = MaterialTheme.typography.titleSmall, color = BrightGold)
                        Text("${listing.creatorName} · ${listing.platform}", style = MaterialTheme.typography.bodySmall, color = SilverText)
                    }
                    StatusPill(
                        listing.listingStatus,
                        when (listing.listingStatus) {
                            "APPROVED" -> BullishEmerald
                            "REJECTED" -> BearishCrimson
                            else -> WarningAmber
                        },
                        when (listing.listingStatus) {
                            "APPROVED" -> BullishContainer
                            "REJECTED" -> BearishContainer
                            else -> WarningContainer
                        }
                    )
                }
                if (forgeViewModel.isAdminUser) {
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { forgeViewModel.moderateListing(listing.id, "APPROVED") },
                            colors = ButtonDefaults.buttonColors(containerColor = BullishContainer, contentColor = BullishEmerald),
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Approve") }
                        OutlinedButton(
                            onClick = { forgeViewModel.moderateListing(listing.id, "REJECTED") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Reject", color = BearishCrimson) }
                        OutlinedButton(
                            onClick = { forgeViewModel.moderateListing(listing.id, "ARCHIVED") },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) { Text("Archive", color = SilverText) }
                    }
                }
            }
        }
    }
}
