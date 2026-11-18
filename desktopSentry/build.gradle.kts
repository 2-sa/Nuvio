plugins {
    java
    alias(libs.plugins.sentry.jvm.gradle)
}

val sentryAuthToken = providers.environmentVariable("SENTRY_AUTH_TOKEN")
    .orNull
    ?.trim()
    ?.takeIf { it.isNotBlank() }

val sentryOrg = providers.environmentVariable("SENTRY_ORG")
    .orNull
    ?.trim()
    ?.takeIf { it.isNotBlank() }
    ?: "nuviomedia"

val sentryProject = providers.environmentVariable("SENTRY_PROJECT")
    .orNull
    ?.trim()
    ?.takeIf { it.isNotBlank() }
    ?: "nuvio-desktop"

sentry {
    includeSourceContext.set(true)
    autoUploadSourceContext.set(sentryAuthToken != null)
    additionalSourceDirsForSourceContext.set(
        setOf(
            "../composeApp/src/commonMain/kotlin",
            "../composeApp/src/desktopMain/kotlin",
            "../composeApp/src/fullCommonMain/kotlin",
        ),
    )
    includeDependenciesReport.set(false)
    telemetry.set(false)
    org.set(sentryOrg)
    projectName.set(sentryProject)
    sentryAuthToken?.let(authToken::set)
    autoInstallation {
        enabled.set(false)
    }
}
