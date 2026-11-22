package com.nuvio.app.features.player.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NativePlayerBridgeWarmupPolicyTest {

    @Test
    fun skipsStartupWebViewWarmupOnWindowsMacosAndUnknown() {
        assertFalse(shouldWarmupWebViewOnStartup(DesktopHostOs.WINDOWS))
        assertFalse(shouldWarmupWebViewOnStartup(DesktopHostOs.MACOS))
        assertFalse(shouldWarmupWebViewOnStartup(DesktopHostOs.UNKNOWN))
    }

    @Test
    fun keepsStartupWebKitGtkWarmupOnLinux() {
        assertTrue(shouldWarmupWebViewOnStartup(DesktopHostOs.LINUX))
    }
}
