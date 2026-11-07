package com.xalane.vpn

import com.xalane.vpn.data.AUTO_SERVER_ID
import com.xalane.vpn.ui.components.flagEmoji
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LogicTest {
    @Test fun flagEmojiBuildsRegionalIndicators() {
        assertEquals("🇩🇪", flagEmoji("de"))
        assertEquals("", flagEmoji(""))
    }

    @Test fun autoPicksFastestWhenNothingIsLive() {
        val s = SampleData.state(selected = AUTO_SERVER_ID)
        assertTrue(s.isAuto)
        assertEquals("de-fra", s.target?.id)
    }

    @Test fun selectedServerIsTarget() {
        val s = SampleData.state(selected = "jp-tyo")
        assertFalse(s.isAuto)
        assertEquals("jp-tyo", s.target?.id)
    }

    @Test fun demoServersCannotConnect() {
        assertTrue(SampleData.servers.all { it.isDemo && !it.canConnect })
    }
}
