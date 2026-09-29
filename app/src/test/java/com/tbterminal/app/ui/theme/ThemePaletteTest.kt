package com.tbterminal.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePaletteTest {
    @Test
    fun `light palette is muted without sacrificing text contrast`() {
        assertTrue(TbBackground != Color.White)
        assertTrue(TbSurface != Color.White)
        assertTrue(TbBackground.luminance() < 0.9f)
        assertTrue(TbSurface.luminance() > TbBackground.luminance())
        assertTrue(contrastRatio(TbText, TbBackground) >= 7.0f)
        assertTrue(contrastRatio(Color.White, TbGreen) >= 4.5f)
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (max(firstLuminance, secondLuminance) + 0.05f) /
            (min(firstLuminance, secondLuminance) + 0.05f)
    }
}
