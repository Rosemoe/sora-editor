# Font

You can customize the font style of the editor

## Implementation <!-- {docsify-ignore} -->

Firstly, you need to place the font file in assets, and then add the following code to your code to
achieve the custom font function.

``` java
Typeface typeface = Typeface.createFromAsset(assets, "JetBrainsMono-Regular.ttf");
editor.setTypefaceText(typeface);
```