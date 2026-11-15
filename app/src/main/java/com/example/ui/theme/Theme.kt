package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val JaxDarkColorScheme = darkColorScheme(
    primary = MetallicGold,
    onPrimary = ObsidianBlack,
    primaryContainer = GoldContainer,
    onPrimaryContainer = BrightGold,
    secondary = BullishEmerald,
    onSecondary = ObsidianBlack,
    secondaryContainer = BullishContainer,
    onSecondaryContainer = BullishEmerald,
    tertiary = CyanQuant,
    onTertiary = ObsidianBlack,
    tertiaryContainer = CyanContainer,
    onTertiaryContainer = CyanQuant,
    error = BearishCrimson,
    onError = CrispWhite,
    errorContainer = BearishContainer,
    onErrorContainer = BearishCrimson,
    background = ObsidianBlack,
    onBackground = CrispWhite,
    surface = CharcoalSurface,
    onSurface = CrispWhite,
    surfaceVariant = GraphiteCard,
    onSurfaceVariant = SilverText,
    surfaceContainerLowest = ObsidianBlack,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = GraphiteCard,
    surfaceContainerHigh = ElevatedGraphite,
    outline = GlassBorder,
    outlineVariant = SubtleDivider
)

val JaxShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun JaxOmniverseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JaxDarkColorScheme,
        typography = JaxTypography,
        shapes = JaxShapes,
        content = content
    )
}
