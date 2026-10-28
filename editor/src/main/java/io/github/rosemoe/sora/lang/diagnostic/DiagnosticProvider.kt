package io.github.rosemoe.sora.lang.diagnostic

/**
 * Provider for diagnostics
 */
fun interface DiagnosticProvider {
    /**
     * Provide diagnostics to the given container
     */
    fun provideDiagnostics(container: DiagnosticsContainer)
}
