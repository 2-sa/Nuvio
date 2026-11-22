package com.nuvio.app.core.ui

import coil3.ComponentRegistry
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.nuvio.app.core.storage.DesktopStorage
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okio.Path.Companion.toOkioPath
import java.nio.file.Files
import java.util.concurrent.TimeUnit

private const val ImageMemoryCacheBytes = 48L * 1024 * 1024
private const val ImageDiskCacheBytes = 300L * 1024 * 1024

internal actual val platformProvidesImageLoader: Boolean = false

internal actual fun createPlatformImageHttpClient(): HttpClient? = HttpClient(OkHttp) {
    engine {
        config {
            dispatcher(
                Dispatcher().apply {
                    maxRequests = 32
                    maxRequestsPerHost = 8
                },
            )
            connectionPool(ConnectionPool(8, 60, TimeUnit.SECONDS))
            connectTimeout(10, TimeUnit.SECONDS)
            readTimeout(15, TimeUnit.SECONDS)
            callTimeout(20, TimeUnit.SECONDS)
            retryOnConnectionFailure(true)
        }
    }
}

internal actual fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder {
    return memoryCache {
        MemoryCache.Builder()
            .maxSizeBytes(ImageMemoryCacheBytes)
            .build()
    }.diskCache {
        val cacheDir = DesktopStorage.cacheDir.resolve("image_cache").also {
            runCatching { Files.createDirectories(it) }
        }
        DiskCache.Builder()
            .directory(cacheDir.toOkioPath())
            .maxSizeBytes(ImageDiskCacheBytes)
            .build()
    }
}

internal actual fun ComponentRegistry.Builder.addPlatformDecoders() {
    add(SkiaGifDecoder.Factory())
    add(SvgEmbeddedImageDecoder.Factory())
}
