# textmate

!> **正在开发中**

language-textmate模块是一个动态执行语法高亮显示和其他功能的模块。要使用它，您需要引入其他几个textmate-*
模块。

我们的目标是实现VSCode的效果。然而，由于许多原因，这可能很难在短期内实现。

# 目前可用功能

- 基于语法规则突出显示文件
- 从文件加载颜色主题
- 基于缩进和规则的代码块行

# 如何获取语法和主题文件

如果许多人使用此模块，可能会在以后将可用的配置文件收集到存储库中。

- 您可以从[Textmate](https://github.com/textmate)获取相关文档。
- Eclipse还使用Textmate，您也还可以从其相关的存储库中获取文件
- [vscode](https://github.com/microsoft/vscode/tree/main/extensions)
  中也使用了Textmate，但它的版本领先于本模块中使用的版本。您可以从其源代码中获取配置文件，但并非所有配置文件都可以正常使用