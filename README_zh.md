## RoseCodeEditor (别名CodeEditor)   
一个运行于Android设备的专业而高效的代码编辑器。    
## 强大的功能   
* 高亮您的代码
* 一个智能的自动补全窗口
* 自动缩进代码
* 使用纵向竖线指出您的代码区域
* 格式化您的代码
* 手势放大缩小
* 自由选择文本
* 自由滚动
## 目前支持的语言   
* S5droid（结绳）(全能的上下文敏感自动补全,代码区划线,高亮)
* Java (基础支持:高亮,代码区划线,标识符和关键字补全)
## 本项目中含有的其他模块   
* 一个手写的词法分析器（速度比JFLex快）
语言包括:Java,S5droid
## 如何使用这个编辑器
由于我不熟悉Gradle，所以你不得不手工将代码复制到项目。
具体的操作有：
* 把` src/assets/ ` 下的文件拷贝到你的项目(如果你不想使用S5droid语言,可以直接去下一个步骤)
* 把` src/res/layout `下的文件拷贝到你的项目(除了src/res/layout/activity_main.xml)
* 把` src/java/ `下的文件拷贝到你的项目(除了src/com/rose/editor/android/MainActivity.java)   
* 修改这些文件的:"import com.rose.editor.android.R" 为 "import yourPackageName.R" :   
** src/java/com/rose/editor/android/AutoCompletePanel.java   
** src/java/com/rose/editor/android/TextComposePanel.java   
* 现在,你已完成所有步骤!赶紧试试吧!
