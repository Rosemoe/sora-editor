# CodeEditor
[![](https://jitpack.io/v/Rosemoe/CodeEditor.svg)](https://jitpack.io/#Rosemoe/CodeEditor)   
A professional code editor on Android device with good performance and nice features.   
***Work In Progress*** This project is still developing slowly because of school.Bugs may be inside.   
***Note:***
Code in branch `master` will be a stabler version which can run without a lot problems (but sometimes there might be some important problems that has been solved in `dev`).   
Branch `dev` has newest features in editor and is developing.   
Any non-public method or field can be changed and moved or even deleted at current period of developing.     
If you find any exception please send it to me.   
   
See [Projects/CodeEditor](https://github.com/Rosemoe/CodeEditor/projects/1) to get more information about what I am working on.    
***Issues and pull requests are welcome.***   
## Features Implemented Currently
* Syntax highlight
* Automatic completion
* Automatic Indent
* Code block lines
* Format code
* Scale text
* Select text
* Scroll freely, Scrollbars, EdgeEffect, OverScroll(Optional)
* Undo/redo actions
* Search and replace text
* Common Shortcuts
## Language Supported  
* Java,JavaScript,C,C++(Basic Support:highlight,code block line,identifier and keyword auto completion)
* S5droid(context sensitive auto completion,highlight,code block line,navigation)   
## Screenshots  
![View Outline](/images/outline.png)
![Auto Complete](/images/auto-completion.png)   
![Select Text](/images/select-text.png)   
![Search and Replace](/images/search-replace.png)   
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
  implementation 'com.github.Rosemoe.CodeEditor:editor:0.1.0-beta'
}
```
Available modules:     
editor,language-base,language-java,language-universal,language-s5d    
## More information
Turn to [wiki](https://github.com/Rosemoe/CodeEditor/wiki) page.
