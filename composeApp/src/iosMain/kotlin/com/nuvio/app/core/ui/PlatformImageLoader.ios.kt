package com.nuvio.app.core.ui

import coil3.ComponentRegistry
import coil3.ImageLoader
import io.ktor.client.HttpClient

internal actual fun ImageLoader.Builder.configurePlatformImageLoader(): ImageLoader.Builder = this

internal actual fun ComponentRegistry.Builder.addPlatformDecoders() = Unit

internal actual fun createPlatformImageHttpClient(): HttpClient? = null

internal actual val platformProvidesImageLoader: Boolean = false