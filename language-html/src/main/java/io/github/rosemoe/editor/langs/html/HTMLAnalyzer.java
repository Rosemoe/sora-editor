package io.github.rosemoe.editor.langs.html;

import io.github.rosemoe.editor.interfaces.CodeAnalyzer;
import io.github.rosemoe.editor.text.TextAnalyzeResult;
import io.github.rosemoe.editor.text.TextAnalyzer;
import java.io.StringReader;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import java.io.IOException;
import org.antlr.v4.runtime.Token;
import java.util.Stack;
import io.github.rosemoe.editor.struct.BlockLine;
import io.github.rosemoe.editor.widget.EditorColorScheme;

public class HTMLAnalyzer implements CodeAnalyzer
{
	@Override
	public void analyze(CharSequence content, TextAnalyzeResult colors, TextAnalyzer.AnalyzeThread.Delegate delegate)
	{
		try
		{
			CodePointCharStream stream = CharStreams.fromReader(new StringReader(content.toString()));
			HTMLLexer lexer = new HTMLLexer(stream);
			Token token = null;
			boolean first = true;
			int lastLine = 1;
			int line = 0, column = 0;
			while(delegate.shouldAnalyze())
			{
				token = lexer.nextToken();
				if(token == null || token.getType() == HTMLLexer.EOF)
					break;
				line = token.getLine() - 1;
				column = token.getCharPositionInLine();
				lastLine = line;

				switch(token.getType())
				{
					case HTMLLexer.TAG_WHITESPACE  :
						if(first) colors.addNormalIfNull();
						break;	
					case HTMLLexer.TAG_OPEN        :
					case HTMLLexer.TAG_SLASH       :
					case HTMLLexer.TAG_SLASH_CLOSE :
					case HTMLLexer.TAG_CLOSE       :
					case HTMLLexer.TAG_EQUALS      :
						colors.addIfNeeded(line, column, EditorColorScheme.OPERATOR);
						break;
					case HTMLLexer.TAG_NAME        :
					case HTMLLexer.XML             :
						colors.addIfNeeded(line, column, EditorColorScheme.HTML_TAG);
						break;
					case HTMLLexer.CDATA           :
					case HTMLLexer.ATTRIBUTE       :
						colors.addIfNeeded(line, column, EditorColorScheme.ATTRIBUTE_NAME);
						break;
					case HTMLLexer.ATTVALUE_VALUE  :
						colors.addIfNeeded(line, column, EditorColorScheme.ATTRIBUTE_VALUE);
						break;
					case HTMLLexer.HTML_CONDITIONAL_COMMENT :
					case HTMLLexer.HTML_COMMENT    :
						colors.addIfNeeded(line, column, EditorColorScheme.COMMENT);
						break;
					case HTMLLexer.HTML_TEXT       :
					default :
						colors.addIfNeeded(line, column, EditorColorScheme.TEXT_NORMAL);
						break;
				}

				first = false;
			}
			colors.determine(lastLine);
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
