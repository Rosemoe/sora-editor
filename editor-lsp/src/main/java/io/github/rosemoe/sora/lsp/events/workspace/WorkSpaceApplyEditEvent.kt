/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2023  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

package io.github.rosemoe.sora.lsp.events.workspace

import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventListener
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.document.applyEdits
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.utils.FileUri
import io.github.rosemoe.sora.lsp.utils.LSPException
import io.github.rosemoe.sora.lsp.utils.toFileUri
import io.github.rosemoe.sora.lsp.utils.toURI
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.ResourceOperation
import org.eclipse.lsp4j.TextDocumentEdit
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.jsonrpc.messages.Either


class WorkSpaceApplyEditEvent : EventListener {
    override val eventName: String = EventType.workSpaceApplyEdit


    override fun handle(context: EventContext) {
        val workspaceEditParams = context.getByClass<ApplyWorkspaceEditParams>() ?: return

        val documentChanges = workspaceEditParams.edit.documentChanges

        if (documentChanges != null) {
            applyDocumentChanges(context, documentChanges)
        } else {
            applyChanges(context, workspaceEditParams.edit.changes)
        }
    }

    private fun applyDocumentChanges(
        context: EventContext,
        documentChanges: List<Either<TextDocumentEdit, ResourceOperation>>
    ) {
        val project = context.getByClass<LspProject>() ?: return

        documentChanges.forEach {
            if (it.isRight) {
                applyResourceOperation(it.right)
            } else {
                applyTextDocumentEdit(project, it.left)
            }
        }
    }

    private fun applyResourceOperation(operation: ResourceOperation) {
        throw LSPException("ResourceOperation is not supported now $operation")
    }

    private fun applyTextDocumentEdit(
        project: LspProject,
        textDocumentEdit: TextDocumentEdit
    ) {
        val textDocument = textDocumentEdit.textDocument
        val uri = textDocument.uri.toURI().toFileUri()
        val editor = project.getEditor(uri)
            ?: throw LSPException("The url ${textDocument.uri} is not opened.")

        applySingleChange(editor, uri, textDocumentEdit.edits)

    }

    private fun applyChanges(context: EventContext, changes: Map<String, List<TextEdit>>) {
        val project = context.getByClass<LspProject>() ?: return


        changes.forEach { (uri, textEdits) ->
            val fileUri = uri.toURI().toFileUri()
            val editor =
                project.getEditor(uri) ?: throw LSPException("The url $uri is not opened.")


            applySingleChange(editor, fileUri, textEdits)
        }
    }

    private fun applySingleChange(editor: LspEditor, uri: FileUri, textEdits: List<TextEdit>) {
        editor.eventManager.emit(EventType.applyEdits) {
            put("edits", textEdits)
            put(
                "content",
                editor.editor?.text ?: throw LSPException("The editor content $uri is null.")
            )
        }
    }

}

val EventType.workSpaceApplyEdit: String
    get() = "workspace/applyEdit"