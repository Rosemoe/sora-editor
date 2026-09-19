/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *  Licensed under the MIT License.
 *--------------------------------------------------------------------------------------------*/
package io.github.rosemoe.sora.lsp.editor.semantic

/** Scope fallbacks from VS Code's tokenClassificationRegistry.ts. Specific rules precede general ones. */
internal fun semanticTokenScopes(type: String, modifiers: Set<String>): List<List<String>> {
    val scopes = ArrayList<List<String>>()
    if ("defaultLibrary" in modifiers) {
        if ("readonly" in modifiers) when (type) {
            "variable" -> scopes.add(listOf("support.constant"))
            "property" -> scopes.add(listOf("support.constant.property"))
        }
        scopes.add(when (type) {
            "type" -> listOf("support.type")
            "class", "interface" -> listOf("support.class")
            "variable" -> listOf("support.variable", "support.other.variable")
            "property" -> listOf("support.variable.property")
            "function", "method", "member" -> listOf("support.function")
            else -> emptyList()
        })
    }
    if ("readonly" in modifiers) when (type) {
        "variable" -> scopes.add(listOf("variable.other.constant"))
        "property" -> scopes.add(listOf("variable.other.constant.property"))
    }
    scopes.add(when (type) {
        "comment" -> listOf("comment")
        "string" -> listOf("string")
        "keyword" -> listOf("keyword.control")
        "number" -> listOf("constant.numeric")
        "regexp" -> listOf("constant.regexp")
        "operator" -> listOf("keyword.operator")
        "namespace" -> listOf("entity.name.namespace")
        "type" -> listOf("entity.name.type", "support.type")
        "struct" -> listOf("entity.name.type.struct")
        "class" -> listOf("entity.name.type.class", "support.class")
        "interface" -> listOf("entity.name.type.interface")
        "enum" -> listOf("entity.name.type.enum")
        "typeParameter" -> listOf("entity.name.type.parameter")
        "function" -> listOf("entity.name.function", "support.function")
        "method", "member" -> listOf("entity.name.function.member", "support.function")
        "macro" -> listOf("entity.name.function.preprocessor")
        "variable" -> listOf("variable.other.readwrite", "entity.name.variable")
        "parameter" -> listOf("variable.parameter")
        "property" -> listOf("variable.other.property")
        "enumMember" -> listOf("variable.other.enummember")
        "event" -> listOf("variable.other.event")
        "decorator" -> listOf("entity.name.decorator", "entity.name.function")
        else -> emptyList()
    })
    return scopes
}
