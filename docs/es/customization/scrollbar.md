# Scrollbar

The editor allows using custom scrollbar styles.

## Implementation <!-- {docsify-ignore} -->

First you need to provide the corresponding style files, for example:

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

Then specify the scrollbar styles in your editor:

``` xml
<io.github.rosemoe.sora.widget.CodeEditor
    android:scrollbarThumbHorizontal="@drawable/scrollbar_thumb"
    android:scrollbarThumbVertical="@drawable/scrollbar_thumb"
    android:scrollbarTrackHorizontal="@drawable/scrollbar_track"
    android:scrollbarTrackVertical="@drawable/scrollbar_track" />
```