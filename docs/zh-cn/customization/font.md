# 字体

您可以自定义编辑器的字体样式

## 实现方式

首先，您需要在assets中放置好字体文件，然后在您的代码中添加以下代码即可实现自定义字体的功能。

``` java
Typeface typeface = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf");
editor.setTypefaceText(typeface);
```