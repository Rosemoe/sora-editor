# Keep these for LSP & JsonRPC working properly
-keep class org.eclipse.lsp4j.* { *; }
-keep class org.eclipse.lsp4j.services.* { *; }
-keep class org.eclipse.lsp4j.jsonrpc.messages.* { *; }

-keepclassmembers enum org.eclipse.lsp4j.** {
   public static **[] values();
   public static ** valueOf(java.lang.String);
}
