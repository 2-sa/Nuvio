package com.nuvio.app

import com.nuvio.app.core.deeplink.handleAppUrl
import java.awt.Frame
import java.awt.Window
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.lang.ref.WeakReference
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.Locale
import javax.swing.SwingUtilities

internal class DesktopSingleInstanceGuard(
    private val lockDir: File,
) : AutoCloseable {
    private var fileChannel: FileChannel? = null
    private var fileLock: FileLock? = null
    private var serverSocket: ServerSocket? = null
    private var listenerThread: Thread? = null

    private val lockFile: File get() = lockDir.resolve(LOCK_FILE_NAME)
    private val portFile: File get() = lockDir.resolve(PORT_FILE_NAME)

    fun tryAcquire(onMessage: (String) -> Unit): Boolean {
        lockDir.mkdirs()

        val channel = try {
            FileChannel.open(
                lockFile.toPath(),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
        } catch (_: IOException) {
            return false
        }

        val lock = try {
            channel.tryLock()
        } catch (_: OverlappingFileLockException) {
            null
        } catch (_: IOException) {
            null
        }

        if (lock == null) {
            runCatching { channel.close() }
            return false
        }

        val socket = try {
            ServerSocket(0, 8, InetAddress.getLoopbackAddress())
        } catch (_: IOException) {
            runCatching { lock.release() }
            runCatching { channel.close() }
            return false
        }

        try {
            // Overwrite any stale port file left behind by a crashed primary process.
            portFile.writeText(socket.localPort.toString(), StandardCharsets.UTF_8)
        } catch (_: IOException) {
            runCatching { socket.close() }
            runCatching { lock.release() }
            runCatching { channel.close() }
            return false
        }

        fileChannel = channel
        fileLock = lock
        serverSocket = socket
        listenerThread = Thread(
            { acceptLoop(socket, onMessage) },
            "NuvioSingleInstanceListener",
        ).apply {
            isDaemon = true
            start()
        }
        return true
    }

    fun forwardToPrimary(args: Array<String>): Boolean {
        val port = runCatching {
            if (!portFile.isFile) return@runCatching null
            portFile.readText(StandardCharsets.UTF_8).trim().toIntOrNull()
        }.getOrNull()?.takeIf { it in 1..65535 } ?: return false

        return try {
            Socket().use { socket ->
                socket.connect(
                    InetSocketAddress(InetAddress.getLoopbackAddress(), port),
                    FORWARD_CONNECT_TIMEOUT_MS,
                )
                socket.soTimeout = FORWARD_CONNECT_TIMEOUT_MS
                val writer = BufferedWriter(
                    OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                )
                writer.write(FOCUS_COMMAND)
                writer.newLine()
                args.asSequence()
                    .map(String::trim)
                    .filter(::isSupportedAppUrl)
                    .forEach { url ->
                        writer.write(url)
                        writer.newLine()
                    }
                writer.flush()
            }
            true
        } catch (_: IOException) {
            false
        }
    }

    override fun close() {
        val hadOwnership = serverSocket != null || fileLock != null
        runCatching { serverSocket?.close() }
        serverSocket = null
        listenerThread = null
        runCatching { fileLock?.release() }
        fileLock = null
        runCatching { fileChannel?.close() }
        fileChannel = null
        if (hadOwnership) {
            runCatching { portFile.delete() }
        }
    }

    private fun acceptLoop(
        socket: ServerSocket,
        onMessage: (String) -> Unit,
    ) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (_: IOException) {
                break
            }
            try {
                client.use { connected ->
                    val reader = BufferedReader(
                        InputStreamReader(connected.getInputStream(), StandardCharsets.UTF_8),
                    )
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isNotBlank()) {
                            onMessage(line)
                        }
                    }
                }
            } catch (_: IOException) {
                // Ignore transient client read errors and continue accepting.
            }
        }
    }

    companion object {
        private const val LOCK_FILE_NAME = "nuvio-desktop.lock"
        private const val PORT_FILE_NAME = "nuvio-desktop.port"
        private const val FOCUS_COMMAND = "FOCUS"
        private const val FORWARD_CONNECT_TIMEOUT_MS = 1500
    }
}

internal object DesktopSingleInstance {
    @Volatile
    private var activeGuard: DesktopSingleInstanceGuard? = null

    @Volatile
    private var windowRef: WeakReference<Window>? = null

    fun acquireOrForward(
        args: Array<String>,
        lockDir: File = defaultLockDir(),
    ): Boolean {
        val guard = DesktopSingleInstanceGuard(lockDir)
        if (guard.tryAcquire(::handleMessage)) {
            activeGuard?.let { previous -> runCatching { previous.close() } }
            activeGuard = guard
            return true
        }
        guard.forwardToPrimary(args)
        guard.close()
        return false
    }

    fun attachWindow(window: Window) {
        windowRef = WeakReference(window)
    }

    fun release() {
        runCatching { activeGuard?.close() }
        activeGuard = null
        windowRef = null
    }

    private fun handleMessage(line: String) {
        when {
            line == "FOCUS" -> {
                SwingUtilities.invokeLater {
                    val window = windowRef?.get() ?: return@invokeLater
                    val frame = window as? Frame
                    if (frame != null && (frame.extendedState and Frame.ICONIFIED) != 0) {
                        frame.extendedState = frame.extendedState and Frame.ICONIFIED.inv()
                    }
                    window.toFront()
                    window.requestFocus()
                }
            }
            isSupportedAppUrl(line) -> {
                handleAppUrl(line)
            }
        }
    }

    private fun defaultLockDir(): File {
        val osName = System.getProperty("os.name").orEmpty().lowercase(Locale.ROOT)
        val userHome = Paths.get(System.getProperty("user.home").orEmpty())
        val path = when {
            osName.contains("mac") -> userHome.resolve("Library/Application Support/Nuvio")
            osName.contains("win") -> {
                val localAppData = System.getenv("LOCALAPPDATA")?.takeIf { it.isNotBlank() }
                (localAppData?.let(Paths::get) ?: userHome.resolve("AppData/Local")).resolve("Nuvio")
            }
            else -> {
                val xdgConfig = System.getenv("XDG_CONFIG_HOME")?.takeIf { it.isNotBlank() }
                (xdgConfig?.let(Paths::get) ?: userHome.resolve(".config")).resolve("nuvio")
            }
        }
        return path.toFile()
    }
}

private fun isSupportedAppUrl(value: String): Boolean =
    value.startsWith("nuvio://", ignoreCase = true) ||
        value.startsWith("stremio://", ignoreCase = true)
