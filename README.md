# RoseCodeEditor / CodeEditor  
A professional code editor on Android device with highlight and auto completion.  
不想看英文请:[中文版](/README_zh.md).   
总之项目里面全都是英文啦(除了少部分扯淡的话)...
## Strong Abilities  
* Highlight yours code
* A strong automatically complete window
* Automatically indent your code
* Show region of code blocks with vertical lines
* Format your code
* Scale text size by gesture
* Select text
* Scroll freely
## Language Supported  
* S5droid(context sensitive auto completion,highlight,code block line,navigation)
* Java(Basic Support:highlight,code block line,identifier and keyword auto completion)
## Extra Module Inside
* A man-made lexer(Quite fast than JFLex)
Language:Java,S5droid
## How to use this Editor  
Gradle not supported because of my poor knowledge about it.  
To include this project into your project:  
* Copy files in src/assets/ to your project(If you do not want to use S5droid language,please go on to next step)  
* Copy files in src/res/layout to your project(Except src/res/layout/activity_main.xml)   
* Copy files in src/java/ to your project(Except src/com/rose/editor/android/MainActivity.java)    
* Change these files "import com.rose.editor.android.R" to "import yourPackageName.R" :  
**  src/java/com/rose/editor/android/AutoCompletePanel.java  
**  src/java/com/rose/editor/android/TextComposePanel.java  
* Now you have finished all the steps!
