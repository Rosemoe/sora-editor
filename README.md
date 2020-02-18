# RoseCodeEditor / CodeEditor  
A professional code editor on Android device with highlight and auto completion.  
## Strong Abilities  
* Highligh yours code
* A strong auto complete window
* Auto indent your code
* Spot code blocks with vertical lines
* Format your code
* Scale by thumb
* Select texts with two handles
## Language Supported  
* S5droid
* Java(Building)
## Extra Module Inside
* A man-made lexer for S5droid (Quite fast than JFLex)
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
