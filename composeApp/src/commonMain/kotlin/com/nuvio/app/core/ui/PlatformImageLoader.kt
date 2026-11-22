package com.nuvio.app.core.ui

import coil3.ComponentRegistry
import coil3.ImageLoader
import io.ktor.client.HttpClient

internal expect fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder

internal expect fun ComponentRegistry.Builder.addPlatformDecoders()

internal expect fun createPlatformImageHttpClient(): HttpClient?

/**
 * Returns `true` when the platform already provides a singleton [ImageLoader]
 * (e.g. via `Application` implementing `SingletonImageLoader.Factory` on Android).
 * When `true`, the composable [setSingletonImageLoaderFactory] must be skipped
 * so it doesn't overwrite the platform-provided loader.
 */
internal expect val platformProvidesImageLoader: Boolean