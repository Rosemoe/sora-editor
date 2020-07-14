# CodeEditor
A professional code editor on Android device with highlight and auto completion.  
***Work In Progress*** This project is still developing slowly because of school.
***Note:*** This project is moving fast.
Any method or field can be changed and moved or even deleted.
And it also have to do more tests on different devices.
If you find any exception please send it to me.
See Projects/CodeEditor to get more information about what I am working on.    
Issues and pull requests are welcome.
## Features Implemented Currently
* Syntax highlight
* Automatic completion
* Automatic Indent
* Code block lines
* Format code
* Scale text
* Select text
* Scroll freely, Scrollbars (with EdgeEffect)
* Undo/redo actions
* Search and replace text
* Common Shortcuts
## Language Supported  
* Java,JavaScript,C,C++(Basic Support:highlight,code block line,identifier and keyword auto completion)
* S5droid(context sensitive auto completion,highlight,code block line,navigation)   
## Screenshots  
![View Outline](/images/outline.jpg)
![Auto Complete](/images/auto-completion.jpg)   
## Extra Module Inside
* A man-made lexer for Java,S5droid   
* A simple style-able syntax lexer (Including comments, keywords and operators)
## How to use this Editor  
This project hasn't deploy to any place such as jitpack.    
To include this project into your project:  
* Copy files in src/assets/ to your project(If you do not want to use S5droid language,please go on to the next step)
* Copy files in src/res/layout to your project(Except src/res/layout/activity_main.xml)   
* Copy files in src/java/ to your project(Except src/com/rose/editor/android/MainActivity.java)    
* Change these files "import com.rose.editor.android.R" to "import yourPackageName.R" :  
**  EditorAutoCompleteWindow.java
**  EditorTextActionWindow.java
* Now you have finished all the steps!
## How to customize your language for editor
### A Simple Method   
You just have to implement a `LanguageDescription`!   
See the interface in `/app/src/main/java/com/rose/editor/langs/universal/LanguageDescription` to get more information about it.    
There is also some implementations in `/app/src/main/java/com/rose/editor/langs/desc/`. You can view them and learn how to implement one by yourself.   
This way supports: Highlight(Keywords,Comments,Operators,Literals), Code block lines   
This method is quite simple for everyone, but it will have some limits that are annoying.   
If you want to get advanced features of CodeEditor, you'd better use the followig method.   
### Advanced Method - For experts   
* Make a lexer for the language (Use JFlex or ANTLR to analyze code and highlight. JFlex is recommended)   
[ANTLR Website](https://www.antlr.org/)   
[ANTLR4 Repository](https://github.com/antlr/antlr4)   
[Grammars for ANTLR4](https://github.com/antlr/grammars-v4)   
[JFLex Website](https://jflex.de/)   
[JFLex Repository](https://github.com/jflex-de/jflex)   
You may obtain a example flex for Java in ThisRepo/jflex/
* Implement a CodeAnalyzer:
```Java
public class XxxCodeAnalyzer {

	public void analyze(CharSequence content, TextAnalyzer.TextColors colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
		//Analyze code here(Use your Lexer)
		//Use colors.addIfNeeded() to add a new span if required
		//Use colors.add() to add a new span forcely
		//Use colors.addBlockLine() to add a new code block line
		//Use colors.setNavigation() to specify navigation list
		//Assign colors.mExtra to pass your information to AutoCompleteProvider module
		
		//Note that you should stop when delegate.shouldAnalyze() returns false.
		//...
	}

}
```
* Create an AutoCompleteProvider:
You have two choices:   
A.Implement a AutoCompleteProvider by your self   
```Java
public class XxxAutoComplete {

	public List<ResultItem> getAutoCompleteItems(String prefix, boolean isInCodeBlock, TextColorProvider.TextColors colors, int line) {
		//Match items here
		//How to match is up to you (contains/startsWith/toLowerCase...)
		//You can get information from last analysis by colors.mExtra
		//This value can be null
		//...
		
		//You are unexpected to return null(May cause crash)
		return ...;
	}

}
```
B.Use a simple AutoCompleteProvider implementation -- IdentifierAutoComplete   
This can only match your given word case insensitively by using startsWith().   
And you should use IdentifierAutoComplete.Identifiers as extra data from CodeAnalyzer or there will not be any effect.   
Sample:   
```Java
IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
String[] keywords = new String[]{"public","private","protected"};
//True if your all of keywords are in small case
autoComplete.setKeywords(keywords,true);
```
And in your CodeAnalyzer:   
```Java
public class XxxCodeAnalyzer {

	public void analyze(CharSequence content, TextAnalyzer.TextColors colors, TextAnalyzer.AnalyzeThread.Delegate delegate) {
		XxxTokenizer tokenizer = ...;
		...
		IdentifierAutoComplete.Identifiers identifiers = new IdentifierAutoComplete.Identifiers();
		identifiers.begin();
		while(...) {
			switch(...) {
				...
				case IDENTIFIER:
					identifiers.addIdentifier(tokenizer.getTokenText());
					...
					break;
				...
			}
		}
		identifiers.finish();
		colors.mExtra = identifiers;
		...
	}

}
```
Note that repeat identifier will not be added.    
And you must call begin() before you call addIdentifier().   
Remember to call finish() after all addIdentifier() actions done.(This will free some space)   
* Implement your formatter (Optional)    
Use lexer/tokenizer to create a formatter for your language.   
* Implement EditorLanguage:   
```Java
public class MyLanguage implements EditorLanguage {

	public CodeAnalyzer getAnalyzer() {
		//NonNull or crash
		return new XxxCodeAnalyzer();
	}
	
	public AutoCompleteProvider getAutoCompleteProvider() {
		//NonNull or crash
		return new XxxAutoComplete();
		//Or
		IdentifierAutoComplete autoComplete = new IdentifierAutoComplete();
		String[] keywords = new String[]{"public","private","protected"};
		//True if your all of keywords are in small case
		autoComplete.setKeywords(keywords,true);
		return autoComplete;
	}
	
	public boolean isAutoCompleteChar(char ch) {
		//This is up to your.
		//We will search character from the tail of insert position
		//The default implementation is Character#isJavaIdentifierPart(char)
	}
	
	public int getIndentAdvance(String content) {
		//This is will be called when a newline is at start of input
		//And you will receive text on current line before cursor
		//The return value is counted as space
	}
	
	public boolean useTab() {
		//Whether we use tab to indent
		//And how many spaces can tab replace is related to editor's settings
	}
	
	public CharSequence format(CharSequence text) {
		//Format code
		//You should return formatted code
		//If you can not,just return the given text
	}
}
```
* Apply your language   
It's very simple:   
```Java
editor.setEditorLanguage(yourLanguage);
```
All things after will be done in a minute by editor   
