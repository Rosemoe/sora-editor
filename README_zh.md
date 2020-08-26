# CodeEditor
[![](https://jitpack.io/v/Rosemoe/CodeEditor.svg)](https://jitpack.io/#Rosemoe/CodeEditor)   
CodeEditor是一个运行于Android的，具有良好性能和众多特性的的代码编辑器。   
**由于学业原因，本项目暂停开发。下一次重大更新可能会被推迟到2021年1月**   
   
***工作中*** 本项目正在缓慢开发，其中可能包含一些问题。   
将本项目用于生产开发依赖是**不**推荐的。   
***提示：***
`dev`分支具有最新的特性和错误修复，并且正在开发测验中。   
任何方法或字段在目前开发阶段都有可能被重命名，移动甚至删除。     
如果您在本项目中发现了任何漏洞或您有些许建议，都可以通过Issues或者其他方式告知我。    
   
查看[项目](https://github.com/Rosemoe/CodeEditor/projects/)页面获得更多关于我目前正在解决的问题/开发的特性的信息    
**我们感谢您的任何Issue和Pull Request**   
## 特性
- [x] 平滑的语法高亮
- [x] 自动补全
- [x] 自动插入空格
- [x] 代码区域划线
- [x] 格式化 (需要改进)
- [x] 手势缩放
- [x] 选择文本
- [x] 滚动和边缘效果
- [x] 撤销和重做
- [x] 搜索和替换
- [x] 快捷键
- [x] 文本自动换行
- [x] 显示不可打印字符
- [ ] 增量词法分析
## 目前支持的语言
* Java,JavaScript,C,C++ (仅基础支持:高亮, 代码区划线,标识符和关键字自动完成)
* S5droid 2.x(基于上下文的自动补全, 高亮, 代码区划线, 导航)(将被弃用)   
## 截图预览  
![整体外观](/images/outline.png)
![自动补全](/images/auto-completion.png)
![选择文本](/images/select-text.png)
![搜索替换](/images/search-replace.png)
![自动换行](/images/wordwrap.png)
## 如何使用 
* 首先，把JitPack添加到你的构建文件：
把它添加到您的根项目的build.gradle的repositories的末尾:
```Gradle
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}
 ```
* 然后，将依赖添加到您的应用
```Gradle
dependencies {
  implementation 'com.github.Rosemoe.CodeEditor:editor:<versionName>'
}
```
可用的模块:     
editor,language-base,language-java,language-universal,language-s5d    
## 更多信息
请转到[维基](https://github.com/Rosemoe/CodeEditor/wiki)页面。
