package io.github.rosemoe.sora.lsp.client.languageserver.requestmanager

import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse
import org.eclipse.lsp4j.CodeAction
import org.eclipse.lsp4j.CodeActionParams
import org.eclipse.lsp4j.CodeLens
import org.eclipse.lsp4j.CodeLensParams
import org.eclipse.lsp4j.ColorInformation
import org.eclipse.lsp4j.ColorPresentation
import org.eclipse.lsp4j.ColorPresentationParams
import org.eclipse.lsp4j.Command
import org.eclipse.lsp4j.CompletionItem
import org.eclipse.lsp4j.CompletionList
import org.eclipse.lsp4j.CompletionParams
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DidChangeConfigurationParams
import org.eclipse.lsp4j.DidChangeTextDocumentParams
import org.eclipse.lsp4j.DidChangeWatchedFilesParams
import org.eclipse.lsp4j.DidCloseTextDocumentParams
import org.eclipse.lsp4j.DidOpenTextDocumentParams
import org.eclipse.lsp4j.DidSaveTextDocumentParams
import org.eclipse.lsp4j.DocumentColorParams
import org.eclipse.lsp4j.DocumentFormattingParams
import org.eclipse.lsp4j.DocumentHighlight
import org.eclipse.lsp4j.DocumentHighlightParams
import org.eclipse.lsp4j.DocumentLink
import org.eclipse.lsp4j.DocumentLinkParams
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams
import org.eclipse.lsp4j.DocumentRangeFormattingParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.ExecuteCommandParams
import org.eclipse.lsp4j.FoldingRange
import org.eclipse.lsp4j.FoldingRangeRequestParams
import org.eclipse.lsp4j.Hover
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.ImplementationParams
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InitializeResult
import org.eclipse.lsp4j.InitializedParams
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.LocationLink
import org.eclipse.lsp4j.MessageActionItem
import org.eclipse.lsp4j.MessageParams
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior
import org.eclipse.lsp4j.PrepareRenameParams
import org.eclipse.lsp4j.PrepareRenameResult
import org.eclipse.lsp4j.PublishDiagnosticsParams
import org.eclipse.lsp4j.Range
import org.eclipse.lsp4j.ReferenceParams
import org.eclipse.lsp4j.RegistrationParams
import org.eclipse.lsp4j.RenameParams
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.ShowMessageRequestParams
import org.eclipse.lsp4j.SignatureHelp
import org.eclipse.lsp4j.SignatureHelpParams
import org.eclipse.lsp4j.SymbolInformation
import org.eclipse.lsp4j.TextDocumentPositionParams
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.TypeDefinitionParams
import org.eclipse.lsp4j.UnregistrationParams
import org.eclipse.lsp4j.WillSaveTextDocumentParams
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.eclipse.lsp4j.jsonrpc.messages.Either3
import org.eclipse.lsp4j.services.TextDocumentService
import org.eclipse.lsp4j.services.WorkspaceService
import java.lang.reflect.Modifier
import java.util.concurrent.CompletableFuture

class AggregatedRequestManager(
    sessions: List<SessionEntry>
) : RequestManager() {

    data class SessionEntry(
        val serverName: String,
        val requestManagerProvider: () -> RequestManager?,
        val capabilitiesProvider: () -> ServerCapabilities?
    )

    private var sessionEntries = sessions.toList()

    fun updateSessions(newSessions: List<SessionEntry>) {
        sessionEntries = newSessions.toList()
    }

    override val capabilities: ServerCapabilities?
        get() = mergeCapabilities()

    /** Combine capabilities from every active session, preferring the first non-null value per field. */
    private fun mergeCapabilities(): ServerCapabilities? {
        val all = sessionEntries.mapNotNull { it.capabilitiesProvider() }
        if (all.isEmpty()) {
            return null
        }
        val merged = ServerCapabilities()
        ServerCapabilities::class.java.declaredFields.forEach { field ->
            if (Modifier.isStatic(field.modifiers)) {
                return@forEach
            }
            field.isAccessible = true
            for (cap in all) {
                val value = field.get(cap)
                if (value != null) {
                    field.set(merged, value)
                    break
                }
            }
        }
        return merged
    }

    private val activeManagers: List<RequestManager>
        get() = sessionEntries.mapNotNull { it.requestManagerProvider() }

    private inline fun fanOut(action: RequestManager.() -> Unit) {
        activeManagers.forEach { it.action() }
    }

    private inline fun <T> firstFuture(crossinline call: RequestManager.() -> CompletableFuture<T>?): CompletableFuture<T>? {
        return activeManagers.asSequence().mapNotNull { it.call() }.firstOrNull()
    }

    fun getActiveManagers(): List<RequestManager> = activeManagers

    private fun <T> collectFutures(futures: List<CompletableFuture<T>>): CompletableFuture<List<T>>? {
        if (futures.isEmpty()) {
            return null
        }
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
            futures.mapNotNull { future ->
                runCatching { future.join() }.getOrNull()
            }
        }
    }

    /** Merge completion lists from multiple servers, deduplicating by label+insertText. */
    private fun aggregateCompletion(futures: List<CompletableFuture<Either<List<CompletionItem>, CompletionList>>>): CompletableFuture<Either<List<CompletionItem>, CompletionList>>? {
        if (futures.isEmpty()) {
            return null
        }
        return CompletableFuture.allOf(*futures.toTypedArray()).thenApply {
            val aggregated = mutableListOf<CompletionItem>()
            for (future in futures) {
                val either = runCatching { future.join() }.getOrNull() ?: continue
                val list = when {
                    either.isLeft -> either.left
                    either.isRight -> either.right.items ?: emptyList()
                    else -> emptyList()
                }
                aggregated.addAll(list)
            }
            Either.forLeft(aggregated)
        }
    }

    /** Helper to flatten list-based responses such as code actions or highlights. */
    private fun <T> aggregateLists(
        futures: List<CompletableFuture<List<T>>>
    ): CompletableFuture<List<T>>? {
        return collectFutures(futures)?.thenApply { lists ->
            lists.flatMap { it }
        }
    }

    override fun showMessage(messageParams: MessageParams) {
        fanOut { showMessage(messageParams) }
    }

    override fun showMessageRequest(showMessageRequestParams: ShowMessageRequestParams): CompletableFuture<MessageActionItem> {
        return firstFuture { showMessageRequest(showMessageRequestParams) }
            ?: CompletableFuture.completedFuture(MessageActionItem())
    }

    override fun logMessage(messageParams: MessageParams) {
        fanOut { logMessage(messageParams) }
    }

    override fun telemetryEvent(o: Any) {
        fanOut { telemetryEvent(o) }
    }

    override fun registerCapability(params: RegistrationParams): CompletableFuture<Void> {
        return firstFuture { registerCapability(params) }
            ?: CompletableFuture.completedFuture(null)
    }

    override fun unregisterCapability(params: UnregistrationParams): CompletableFuture<Void> {
        return firstFuture { unregisterCapability(params) }
            ?: CompletableFuture.completedFuture(null)
    }

    override fun applyEdit(params: ApplyWorkspaceEditParams): CompletableFuture<ApplyWorkspaceEditResponse> {
        return firstFuture { applyEdit(params) }
            ?: CompletableFuture.completedFuture(ApplyWorkspaceEditResponse())
    }

    override fun publishDiagnostics(publishDiagnosticsParams: PublishDiagnosticsParams) {
        fanOut { publishDiagnostics(publishDiagnosticsParams) }
    }

    override fun initialize(params: InitializeParams): CompletableFuture<InitializeResult>? {
        return firstFuture { initialize(params) }
    }

    override fun initialized(params: InitializedParams) {
        fanOut { initialized(params) }
    }

    override fun shutdown(): CompletableFuture<Any>? {
        return firstFuture { shutdown() }
    }

    override fun exit() {
        fanOut { exit() }
    }

    override fun didChangeConfiguration(params: DidChangeConfigurationParams) {
        fanOut { didChangeConfiguration(params) }
    }

    override fun didChangeWatchedFiles(params: DidChangeWatchedFilesParams) {
        fanOut { didChangeWatchedFiles(params) }
    }

    override fun executeCommand(params: ExecuteCommandParams): CompletableFuture<Any>? {
        return firstFuture { executeCommand(params) }
    }

    override fun didOpen(params: DidOpenTextDocumentParams) {
        fanOut { didOpen(params) }
    }

    override fun didChange(params: DidChangeTextDocumentParams) {
        fanOut { didChange(params) }
    }

    override fun willSave(params: WillSaveTextDocumentParams) {
        fanOut { willSave(params) }
    }

    override fun willSaveWaitUntil(params: WillSaveTextDocumentParams): CompletableFuture<List<TextEdit>>? {
        return firstFuture { willSaveWaitUntil(params) }
    }

    override fun didSave(params: DidSaveTextDocumentParams) {
        fanOut { didSave(params) }
    }

    override fun didClose(params: DidCloseTextDocumentParams) {
        fanOut { didClose(params) }
    }

    override fun completion(params: CompletionParams): CompletableFuture<Either<List<CompletionItem>, CompletionList>>? {
        val futures = activeManagers.mapNotNull { it.completion(params) }
        return aggregateCompletion(futures)
    }

    override fun resolveCompletionItem(unresolved: CompletionItem): CompletableFuture<CompletionItem>? {
        return firstFuture { resolveCompletionItem(unresolved) }
    }

    override fun hover(params: HoverParams): CompletableFuture<Hover>? {
        return firstFuture { hover(params) }
    }

    override fun hover(params: TextDocumentPositionParams): CompletableFuture<Hover>? {
        return firstFuture { hover(params) }
    }

    override fun signatureHelp(params: TextDocumentPositionParams): CompletableFuture<SignatureHelp>? {
        return firstFuture { signatureHelp(params) }
    }

    override fun signatureHelp(params: SignatureHelpParams): CompletableFuture<SignatureHelp>? {
        return firstFuture { signatureHelp(params) }
    }

    override fun references(params: ReferenceParams): CompletableFuture<List<Location?>>? {
        return firstFuture { references(params) }
    }

    override fun documentHighlight(params: TextDocumentPositionParams): CompletableFuture<List<DocumentHighlight>>? {
        val futures = activeManagers.mapNotNull { it.documentHighlight(params) }
        return aggregateLists(futures)
    }

    override fun documentHighlight(params: DocumentHighlightParams): CompletableFuture<List<DocumentHighlight>>? {
        val futures = activeManagers.mapNotNull { it.documentHighlight(params) }
        return aggregateLists(futures)
    }

    override fun documentSymbol(params: DocumentSymbolParams): CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>>? {
        return firstFuture { documentSymbol(params) }
    }

    override fun formatting(params: DocumentFormattingParams): CompletableFuture<List<TextEdit>>? {
        return firstFuture { formatting(params) }
    }

    override fun rangeFormatting(params: DocumentRangeFormattingParams): CompletableFuture<List<TextEdit>>? {
        return firstFuture { rangeFormatting(params) }
    }

    override fun onTypeFormatting(params: DocumentOnTypeFormattingParams): CompletableFuture<List<TextEdit>>? {
        return firstFuture { onTypeFormatting(params) }
    }

    override fun definition(params: TextDocumentPositionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>>? {
        return firstFuture { definition(params) }
    }

    override fun definition(params: DefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>>? {
        return firstFuture { definition(params) }
    }

    override fun codeAction(params: CodeActionParams): CompletableFuture<List<Either<Command, CodeAction>>>? {
        val futures = activeManagers.mapNotNull { it.codeAction(params) }
        return aggregateLists(futures)
    }

    override fun codeLens(params: CodeLensParams): CompletableFuture<List<CodeLens>>? {
        return firstFuture { codeLens(params) }
    }

    override fun resolveCodeLens(unresolved: CodeLens): CompletableFuture<CodeLens>? {
        return firstFuture { resolveCodeLens(unresolved) }
    }

    override fun documentLink(params: DocumentLinkParams): CompletableFuture<List<DocumentLink>>? {
        return firstFuture { documentLink(params) }
    }

    override fun documentLinkResolve(unresolved: DocumentLink): CompletableFuture<DocumentLink>? {
        return firstFuture { documentLinkResolve(unresolved) }
    }

    override fun prepareRename(params: PrepareRenameParams?): CompletableFuture<Either3<Range?, PrepareRenameResult?, PrepareRenameDefaultBehavior?>?>? {
        return firstFuture { prepareRename(params) }
    }

    override fun rename(params: RenameParams): CompletableFuture<WorkspaceEdit>? {
        return firstFuture { rename(params) }
    }

    override fun implementation(params: ImplementationParams): CompletableFuture<Either<List<Location>, List<LocationLink>>>? {
        return firstFuture { implementation(params) }
    }

    override fun typeDefinition(params: TypeDefinitionParams): CompletableFuture<Either<List<Location>, List<LocationLink>>>? {
        return firstFuture { typeDefinition(params) }
    }

    override fun documentColor(params: DocumentColorParams): CompletableFuture<List<ColorInformation>>? {
        val futures = activeManagers.mapNotNull { it.documentColor(params) }
        return aggregateLists(futures)
    }

    override fun colorPresentation(params: ColorPresentationParams): CompletableFuture<List<ColorPresentation>>? {
        val futures = activeManagers.mapNotNull { it.colorPresentation(params) }
        return aggregateLists(futures)
    }

    override fun foldingRange(params: FoldingRangeRequestParams): CompletableFuture<List<FoldingRange>>? {
        val futures = activeManagers.mapNotNull { it.foldingRange(params) }
        return aggregateLists(futures)
    }

    override fun getTextDocumentService(): TextDocumentService? {
        // Don't support text document service.
        return null
    }

    override fun getWorkspaceService(): WorkspaceService? {
        return null
    }
}
