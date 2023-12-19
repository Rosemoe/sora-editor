# Initialization
To integrate **Sora-Editor** into your project, follow these simple steps. 
First, add it to your layout :
```xml
    <io.github.rosemoe.sora.widget.CodeEditor
         android:id="@+id/editor"
         android:layout_height="match_parent"
         android:layout_width="match_parent"/>
```
and initialize it in your Activity or Fragment :
```java
private CodeEditor editor;
...
editor=findViewById(R.id.editor);
```
now you can build and test the editor.
