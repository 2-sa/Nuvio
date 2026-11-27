package com.nuvio.app.features.updater

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WindowsInstallerCommandTest {
    @Test
    fun `windowsInstallerCommand waits for current pid and releases locks before running msiexec`() {
        val msi = File("C:/Users/Test's User/AppData/Roaming/Nuvio/updates/Nuvio-Windows-x64-0.1.58-alpha.msi")
        val cmd = windowsInstallerCommand(updateFile = msi, currentPid = 4242L)

        assertEquals("powershell.exe", cmd.first())
        assertTrue(cmd.contains("-WindowStyle"))
        assertTrue(cmd.contains("Hidden"))
        val script = cmd.last()
        assertTrue(script.contains("Wait-Process -Id 4242 -Timeout 15"))
        assertTrue(script.contains("Stop-Process -Name 'Nuvio' -Force"))
        assertTrue(script.contains("Start-Sleep -Milliseconds 1500"))
        assertTrue(script.contains("Start-Process -FilePath 'msiexec.exe'"))
        assertTrue(script.contains("Test''s User"))
    }

    @Test
    fun `windowsInstallerCommand launches non-msi installers directly after waiting for exit`() {
        val exe = File("C:/Temp/NuvioSetup.exe")
        val cmd = windowsInstallerCommand(updateFile = exe, currentPid = 99L)

        val script = cmd.last()
        assertTrue(script.contains("Wait-Process -Id 99 -Timeout 15"))
        assertTrue(script.contains("Start-Process -FilePath"))
        assertTrue(!script.contains("msiexec.exe"))
    }
}
