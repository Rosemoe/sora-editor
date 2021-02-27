# CodeEditor
[![](https://jitpack.io/v/Rosemoe/CodeEditor.svg)](https://jitpack.io/#Rosemoe/CodeEditor)   
A cool and optimized code editor on Android platform with good performance and nice features.

***Work In Progress*** This project is still developing slowly. Bugs may be inside.
It is **not** recommended to use this project for production use.   
***Note:***
Any method or field can be changed, moved or even deleted at current period.     
If you find any bug or require any enhancement, please send it to me by issues or other ways.
Download newest sources from [Releases](https://github.com/Rosemoe/CodeEditor/releases) instead of clone this repo directly.

**Issues and pull requests are welcome.**   
## Features
- [x] Smooth syntax highlight
- [x] Auto completion
- [x] Auto indent
- [x] Code block lines
- [x] Format code (Requires improvement)
- [x] Scale text
- [x] Select text
- [x] Scroll, Scrollbars, EdgeEffect, OverScroll
- [x] Undo/redo
- [x] Search and replace
- [x] Shortcuts
- [x] Auto wordwrap
- [x] Show non-printable characters
- [ ] Incremental highlight Analysis
## Language Supported  
* Java, JavaScript, C, C++, HTML (Basic Support:highlight, code block line,identifier and keyword auto-completion). Code block line isn't available for HTML Language
* S5droid 2.x(context sensitive auto completion, highlight, code block line, navigation)(going to be deprecated)   
## Screenshots  
![View Outline](/images/outline.png)
![Auto Complete](/images/auto-completion.png)
![Select Text](/images/select-text.png)
![Search and Replace](/images/search-replace.png)
![Wordwrap](/images/wordwrap.png)
## How to use this editor  
* Step 1.Add the JitPack repository to your build file
Add it in your root build.gradle at the end of repositories:
```Gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
 ```
* Step 2. Add the dependency to your app
```Gradle
dependencies {
  implementation 'com.github.Rosemoe.CodeEditor:editor:<versionName>'
}
```
* Step 3 (Optional) : HTMLLexer is generated using ANTLR 4.9.1. You may need to add this dependency 
```Gradle
  implementation 'org.antlr:antlr4:4.9.1'
```
Available modules:     
* editor 
* language-base
* language-java
* language-html
* language-universal
* language-s5d    
