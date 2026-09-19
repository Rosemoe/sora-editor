# Keep these for LSP & JsonRPC working properly
-keep class org.eclipse.lsp4j.* { *; }
-keep class org.eclipse.lsp4j.services.* { *; }
-keep class org.eclipse.lsp4j.jsonrpc.messages.* { *; }

-keepclassmembers enum org.eclipse.lsp4j.** {
   public static **[] values();
   public static ** valueOf(java.lang.String);
}

# Optional language theme adapters (compileOnly dependencies).
-dontwarn io.github.rosemoe.sora.editor.ts.TsLanguage
-dontwarn io.github.rosemoe.sora.editor.ts.TsTheme
-dontwarn io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry**
-dontwarn io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
-dontwarn org.eclipse.tm4e.core.internal.grammar.ScopeStack
-dontwarn org.eclipse.tm4e.core.internal.theme.**
