package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Candle
import com.example.data.model.ChartOverlayBundle
import com.example.data.model.ProviderHealthState
import com.example.data.model.SetupPlan
import com.example.data.model.SetupStatus
import com.example.ui.navigation.JaxSection
import com.example.ui.theme.BearishContainer
import com.example.ui.theme.BearishCrimson
import com.example.ui.theme.BrightGold
import com.example.ui.theme.BullishContainer
import com.example.ui.theme.BullishEmerald
import com.example.ui.theme.CharcoalSurface
import com.example.ui.theme.CrispWhite
import com.example.ui.theme.CyanQuant
import com.example.ui.theme.ElevatedGraphite
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GoldContainer
import com.example.ui.theme.GraphiteCard
import com.example.ui.theme.MetallicGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.SilverText
import com.example.ui.theme.SubtleDivider
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningContainer
import kotlin.math.max
import kotlin.math.min

@Composable
fun JaxGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = GlassBorder,
    containerColor: Color = GraphiteCard,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(ElevatedGraphite.copy(alpha = 0.25f), Color.Transparent)
                    )
                )
                .padding(14.dp),
            content = content
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = BrightGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = SilverText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun MetricBadge(
    label: String,
    value: String,
    accent: Color = MetallicGold,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = CharcoalSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = SilverText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    containerColor: Color
) {
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
        )
    }
}

fun providerColor(state: ProviderHealthState): Pair<Color, Color> = when (state) {
    ProviderHealthState.LIVE_CONNECTED -> BullishEmerald to BullishContainer
    ProviderHealthState.CONFIGURED_STANDBY -> WarningAmber to WarningContainer
    ProviderHealthState.DISCONNECTED -> SilverText to CharcoalSurface
    ProviderHealthState.DEGRADED_ERROR -> BearishCrimson to BearishContainer
}

@Composable
fun CandlestickSetupChart(
    candles: List<Candle>,
    overlays: ChartOverlayBundle,
    activeSetup: SetupPlan?,
    showEmas: Boolean,
    showOrderBlocks: Boolean,
    showFvg: Boolean,
    showLiquidity: Boolean,
    showFib: Boolean,
    showSetupDrawings: Boolean,
    decimals: Int,
    modifier: Modifier = Modifier
) {
    val visibleCount = min(75, candles.size)
    val visibleCandles = remember(candles) { candles.takeLast(visibleCount) }
    val startOffset = (candles.size - visibleCandles.size).coerceAtLeast(0)
    var selectedIdx by remember(visibleCandles) { mutableStateOf<Int?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (visibleCandles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianBlack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No market candles loaded for current provider.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SilverText
                )
            }
            return
        }

        val inspected = selectedIdx?.let { visibleCandles.getOrNull(it) } ?: visibleCandles.last()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "O:${String.format("%.${decimals}f", inspected.open)}  H:${String.format("%.${decimals}f", inspected.high)}  L:${String.format("%.${decimals}f", inspected.low)}  C:${String.format("%.${decimals}f", inspected.close)}",
                style = MaterialTheme.typography.labelSmall,
                color = if (inspected.close >= inspected.open) BullishEmerald else BearishCrimson
            )
            if (selectedIdx != null) {
                Text(
                    text = "Bar #${selectedIdx!! + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MetallicGold
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianBlack)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(visibleCandles) {
                        detectTapGestures { offset ->
                            val step = size.width / visibleCandles.size.coerceAtLeast(1).toFloat()
                            val idx = (offset.x / step).toInt().coerceIn(0, visibleCandles.lastIndex)
                            selectedIdx = idx
                        }
                    }
            ) {
                val w = size.width
                val h = size.height
                val padTop = 18f
                val padBottom = 18f
                val usableH = (h - padTop - padBottom).coerceAtLeast(10f)

                var minPrice = visibleCandles.minOf { it.low }
                var maxPrice = visibleCandles.maxOf { it.high }
                if (showSetupDrawings && activeSetup != null) {
                    minPrice = min(minPrice, min(activeSetup.stopLoss, activeSetup.tp3))
                    maxPrice = max(maxPrice, max(activeSetup.stopLoss, activeSetup.tp3))
                }
                val range = (maxPrice - minPrice).coerceAtLeast(1e-6)
                val paddedMin = minPrice - range * 0.06
                val paddedMax = maxPrice + range * 0.06
                val fullRange = (paddedMax - paddedMin).coerceAtLeast(1e-6)

                fun yFor(price: Double): Float {
                    val norm = ((price - paddedMin) / fullRange).toFloat()
                    return h - padBottom - norm * usableH
                }

                // Grid lines
                for (g in 0..4) {
                    val gy = padTop + (usableH / 4f) * g
                    drawLine(
                        color = SubtleDivider.copy(alpha = 0.7f),
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = 1f
                    )
                }

                val stepX = w / visibleCandles.size.toFloat()

                // Order Blocks
                if (showOrderBlocks) {
                    overlays.orderBlocks.filter { !it.mitigated }.forEach { ob ->
                        val localIdx = ob.index - startOffset
                        val startX = if (localIdx >= 0) localIdx * stepX else 0f
                        val topY = yFor(ob.top)
                        val botY = yFor(ob.bottom)
                        val obColor = if (ob.bullish) BullishEmerald else BearishCrimson
                        drawRect(
                            color = obColor.copy(alpha = 0.14f),
                            topLeft = Offset(startX, topY),
                            size = Size((w - startX).coerceAtLeast(10f), (botY - topY).coerceAtLeast(4f))
                        )
                    }
                }

                // Fair Value Gaps
                if (showFvg) {
                    overlays.fvgZones.filter { !it.mitigated }.forEach { fvg ->
                        val localIdx = fvg.startIndex - startOffset
                        val startX = if (localIdx >= 0) localIdx * stepX else 0f
                        val topY = yFor(fvg.top)
                        val botY = yFor(fvg.bottom)
                        val fvgColor = if (fvg.bullish) CyanQuant else WarningAmber
                        drawRect(
                            color = fvgColor.copy(alpha = 0.15f),
                            topLeft = Offset(startX, topY),
                            size = Size((w - startX).coerceAtLeast(10f), (botY - topY).coerceAtLeast(3f))
                        )
                    }
                }

                // Golden Zone Fib
                if (showFib && overlays.fibLevels != null) {
                    val fib = overlays.fibLevels
                    val y618 = yFor(fib.level618)
                    val y786 = yFor(fib.level786)
                    drawRect(
                        color = MetallicGold.copy(alpha = 0.10f),
                        topLeft = Offset(0f, min(y618, y786)),
                        size = Size(w, kotlin.math.abs(y786 - y618).coerceAtLeast(2f))
                    )
                    drawLine(
                        color = MetallicGold.copy(alpha = 0.55f),
                        start = Offset(0f, y618),
                        end = Offset(w, y618),
                        strokeWidth = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                    )
                }

                // Liquidity levels
                if (showLiquidity) {
                    overlays.liquidityPoints.filter { it.status == "ACTIVE" }.take(6).forEach { lp ->
                        val ly = yFor(lp.price)
                        val lc = if (lp.side == "BSL") BullishEmerald else BearishCrimson
                        drawLine(
                            color = lc.copy(alpha = 0.5f),
                            start = Offset(w * 0.35f, ly),
                            end = Offset(w, ly),
                            strokeWidth = 1.4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                        )
                    }
                }

                // Automatic Setup Drawings (Entry, SL, TP1-TP3, R:R boxes)
                if (showSetupDrawings && activeSetup != null) {
                    val entryY = yFor(activeSetup.entryPrice)
                    val slY = yFor(activeSetup.stopLoss)
                    val tp1Y = yFor(activeSetup.tp1)
                    val tp2Y = yFor(activeSetup.tp2)
                    val tp3Y = yFor(activeSetup.tp3)

                    val boxLeft = w * 0.62f
                    val boxW = w - boxLeft

                    // Risk box
                    drawRect(
                        color = BearishCrimson.copy(alpha = 0.18f),
                        topLeft = Offset(boxLeft, min(entryY, slY)),
                        size = Size(boxW, kotlin.math.abs(slY - entryY).coerceAtLeast(2f))
                    )
                    // Reward box
                    drawRect(
                        color = BullishEmerald.copy(alpha = 0.16f),
                        topLeft = Offset(boxLeft, min(entryY, tp2Y)),
                        size = Size(boxW, kotlin.math.abs(tp2Y - entryY).coerceAtLeast(2f))
                    )

                    val dash = if (activeSetup.status == SetupStatus.CONFIRMED) null else PathEffect.dashPathEffect(floatArrayOf(10f, 6f))
                    drawLine(MetallicGold, Offset(boxLeft, entryY), Offset(w, entryY), strokeWidth = 2.5f, pathEffect = dash)
                    drawLine(BearishCrimson, Offset(boxLeft, slY), Offset(w, slY), strokeWidth = 2.5f)
                    drawLine(BullishEmerald.copy(alpha = 0.7f), Offset(boxLeft, tp1Y), Offset(w, tp1Y), strokeWidth = 1.6f)
                    drawLine(BullishEmerald, Offset(boxLeft, tp2Y), Offset(w, tp2Y), strokeWidth = 2.2f)
                    drawLine(CyanQuant, Offset(boxLeft, tp3Y), Offset(w, tp3Y), strokeWidth = 1.6f)
                }

                // Candles
                val candleWidth = (stepX * 0.62f).coerceIn(2f, 16f)
                visibleCandles.forEachIndexed { idx, c ->
                    val cx = idx * stepX + stepX / 2f
                    val color = if (c.close >= c.open) BullishEmerald else BearishCrimson
                    val highY = yFor(c.high)
                    val lowY = yFor(c.low)
                    val openY = yFor(c.open)
                    val closeY = yFor(c.close)

                    drawLine(color = color, start = Offset(cx, highY), end = Offset(cx, lowY), strokeWidth = 1.8f)
                    val bodyTop = min(openY, closeY)
                    val bodyH = kotlin.math.abs(closeY - openY).coerceAtLeast(2f)
                    drawRect(
                        color = color,
                        topLeft = Offset(cx - candleWidth / 2f, bodyTop),
                        size = Size(candleWidth, bodyH)
                    )
                }

                // EMAs
                if (showEmas && overlays.emaFast.size == candles.size && overlays.emaSlow.size == candles.size) {
                    val fastSlice = overlays.emaFast.takeLast(visibleCount)
                    val slowSlice = overlays.emaSlow.takeLast(visibleCount)
                    val fastPath = Path()
                    val slowPath = Path()
                    fastSlice.forEachIndexed { i, v ->
                        val x = i * stepX + stepX / 2f
                        val y = yFor(v)
                        if (i == 0) fastPath.moveTo(x, y) else fastPath.lineTo(x, y)
                    }
                    slowSlice.forEachIndexed { i, v ->
                        val x = i * stepX + stepX / 2f
                        val y = yFor(v)
                        if (i == 0) slowPath.moveTo(x, y) else slowPath.lineTo(x, y)
                    }
                    drawPath(fastPath, color = BrightGold, style = Stroke(width = 2.2f))
                    drawPath(slowPath, color = CyanQuant, style = Stroke(width = 2.0f))
                }

                // Crosshair
                selectedIdx?.let { idx ->
                    val cx = idx * stepX + stepX / 2f
                    val cy = yFor(visibleCandles[idx].close)
                    drawLine(SilverText.copy(alpha = 0.6f), Offset(cx, 0f), Offset(cx, h), strokeWidth = 1.2f)
                    drawLine(SilverText.copy(alpha = 0.6f), Offset(0f, cy), Offset(w, cy), strokeWidth = 1.2f)
                }
            }
        }
    }
}

@Composable
fun EquityCurveChart(
    equityCurve: List<Double>,
    modifier: Modifier = Modifier
) {
    if (equityCurve.size < 2) return
    val isNetPositive = equityCurve.last() >= equityCurve.first()
    val lineColor = if (isNetPositive) BullishEmerald else BearishCrimson

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianBlack)
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val minV = equityCurve.minOrNull() ?: 0.0
            val maxV = equityCurve.maxOrNull() ?: 1.0
            val range = (maxV - minV).coerceAtLeast(1.0)
            val stepX = w / (equityCurve.size - 1).coerceAtLeast(1)

            val path = Path()
            val fillPath = Path()
            equityCurve.forEachIndexed { idx, value ->
                val x = idx * stepX
                val y = h - (((value - minV) / range).toFloat() * (h - 16f) + 8f)
                if (idx == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, h)
                    fillPath.lineTo(x, y)
                } else {
                    path.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (idx == equityCurve.lastIndex) {
                    fillPath.lineTo(x, h)
                    fillPath.close()
                }
            }
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.30f), Color.Transparent))
            )
            drawPath(path, color = lineColor, style = Stroke(width = 3f))
        }
    }
}

@Composable
fun CommandPaletteDialog(
    onDismiss: () -> Unit,
    onSelectSection: (JaxSection) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) JaxSection.entries
        else JaxSection.entries.filter {
            it.title.contains(query, ignoreCase = true) || it.group.title.contains(query, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = GraphiteCard,
            border = BorderStroke(1.dp, MetallicGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("J.A.X. Command Palette", style = MaterialTheme.typography.titleLarge, color = BrightGold)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Jump to module (e.g. Backtest, MT5, DNA, Pine)") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search modules") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("command_palette_search")
                )
                Spacer(Modifier.height(10.dp))
                LazyColumn(modifier = Modifier.height(320.dp)) {
                    items(filtered, key = { it.route }) { section ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectSection(section)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(section.icon, contentDescription = null, tint = MetallicGold, modifier = Modifier.size(20.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(section.title, style = MaterialTheme.typography.titleSmall, color = CrispWhite)
                                Text(section.group.title, style = MaterialTheme.typography.bodySmall, color = SilverText)
                            }
                        }
                        HorizontalDivider(color = SubtleDivider)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = MetallicGold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionDot(state: ProviderHealthState) {
    val color = when (state) {
        ProviderHealthState.LIVE_CONNECTED -> BullishEmerald
        ProviderHealthState.CONFIGURED_STANDBY -> WarningAmber
        ProviderHealthState.DISCONNECTED -> SilverText
        ProviderHealthState.DEGRADED_ERROR -> BearishCrimson
    }
    Box(
        modifier = Modifier
            .size(9.dp)
            .clip(CircleShape)
            .background(color)
    )
}
