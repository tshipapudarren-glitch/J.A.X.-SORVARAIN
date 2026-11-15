package com.example.domain.engine

import com.example.data.model.ForgeCategory
import com.example.data.model.ForgeComplexity
import com.example.data.model.ForgePlatform
import com.example.data.model.SetupDrawingConfig
import com.example.data.model.SetupDrawingPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IndicatorForgeEngineTest {

    @Test
    fun generateMql5Source_producesValidNonRepaintingIndicator() {
        val code = IndicatorForgeEngine.generateMql5Source(
            name = "J.A.X. Liquidity Sniper",
            category = ForgeCategory.SNIPER_SETUP,
            complexity = ForgeComplexity.PRO,
            pattern = SetupDrawingPattern.LIQUIDITY_SWEEP,
            mtfTimeframes = listOf("H1", "M15", "M5"),
            fastPeriod = 21,
            slowPeriod = 50,
            atrPeriod = 14,
            rrTarget = 2.4,
            drawing = SetupDrawingConfig(closedCandleOnly = true)
        )
        assertTrue(code.contains("#property strict"))
        assertTrue(code.contains("int OnInit()"))
        assertTrue(code.contains("int OnCalculate("))
        assertTrue(code.contains("InpClosedCandleOnly"))

        val report = IndicatorForgeEngine.validateSource(ForgePlatform.MT5, code)
        assertTrue("Expected MQL5 validation to pass: ${report.summary}", report.passed)
        assertEquals("LOW", report.repaintRiskLevel)
        assertFalse(report.lookaheadDetected)
    }

    @Test
    fun generatePineV6Source_producesValidNonRepaintingScript() {
        val code = IndicatorForgeEngine.generatePineV6Source(
            name = "J.A.X. Liquidity Sniper",
            category = ForgeCategory.SNIPER_SETUP,
            complexity = ForgeComplexity.PRO,
            pattern = SetupDrawingPattern.LIQUIDITY_SWEEP,
            mtfTimeframes = listOf("H1", "M15", "M5"),
            fastPeriod = 21,
            slowPeriod = 50,
            atrPeriod = 14,
            rrTarget = 2.4,
            drawing = SetupDrawingConfig(closedCandleOnly = true)
        )
        assertTrue(code.contains("//@version=6"))
        assertTrue(code.contains("indicator("))
        assertTrue(code.contains("barmerge.lookahead_off"))

        val report = IndicatorForgeEngine.validateSource(ForgePlatform.TRADINGVIEW, code)
        assertTrue("Expected Pine v6 validation to pass: ${report.summary}", report.passed)
        assertEquals("LOW", report.repaintRiskLevel)
        assertFalse(report.lookaheadDetected)
    }

    @Test
    fun validateSource_flagsLookaheadAndMissingOnCalculate() {
        val badPine = """
            //@version=5
            indicator("Bad Script")
            x = request.security(syminfo.tickerid, "60", close, lookahead=barmerge.lookahead_on)
            plot(x)
        """.trimIndent()
        val pineReport = IndicatorForgeEngine.validateSource(ForgePlatform.TRADINGVIEW, badPine)
        assertFalse(pineReport.passed)
        assertTrue(pineReport.lookaheadDetected)
        assertEquals("HIGH", pineReport.repaintRiskLevel)

        val badMq5 = """
            #property indicator_chart_window
            void Foo() { }
        """.trimIndent()
        val mq5Report = IndicatorForgeEngine.validateSource(ForgePlatform.MT5, badMq5)
        assertFalse(mq5Report.passed)
    }
}
