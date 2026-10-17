; Brackets pattern for editor
; Note that you should match both open node and close node in one pattern
; In sora-editor, capture named 'editor.brackets.open' is regarded as open symbol node, capture named 'editor.brackets.close' is regarded as close symbol node.

("(" @editor.brackets.open ")"  @editor.brackets.close)
("[" @editor.brackets.open "]"  @editor.brackets.close)
("{" @editor.brackets.open "}"  @editor.brackets.close)
("<" @editor.brackets.open ">"  @editor.brackets.close)