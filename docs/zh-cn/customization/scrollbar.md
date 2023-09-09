# 滚动条

编辑器允许使用自定义的滚动条样式

## 实现方式

您首先需要提供相应的样式文件，例如：

``` xml
<!-- scrollbar_thumb.xml -->
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle"
    android:tint="@color/textActionNameColor">
    <padding android:top="4dp" android:bottom="4dp" />
    <size android:width="8dp" android:height="52dp" />
    <solid android:color="@android:color/white" />
</shape>

<!-- scrollbar_track.xml -->
<shape
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle"
    android:tint="?colorControlNormal">
    <size android:width="8dp" />
    <solid android:color="#39FFFFFF" />
</shape>
```

然后在您的editor中指定滚动条的样式

``` xml
<io.github.rosemoe.sora.widget.CodeEditor
    android:scrollbarThumbHorizontal="@drawable/scrollbar_thumb"
    android:scrollbarThumbVertical="@drawable/scrollbar_thumb"
    android:scrollbarTrackHorizontal="@drawable/scrollbar_track"
    android:scrollbarTrackVertical="@drawable/scrollbar_track" />
```