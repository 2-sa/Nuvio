package com.nuvio.app.core.diagnostics

internal actual fun capturePlatformMemoryDiagnostics(): MemoryDiagnosticsSnapshot? = null

internal actual fun requestPlatformMemoryTrim(level: MemoryTrimLevel) {
    MemoryTrimCoordinator.trim(level)
}
