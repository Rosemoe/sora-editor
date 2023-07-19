# textmate

!> **Work In Progress**

language-textmate module is a module that performs syntax highlighting and other functions
dynamically. To use it, you need to introduce several other textmate-* modules.

Our goal is to achieve the effect of VSCode. However, for many reasons, this may be difficult to
achieve in the short term.

# Features(already available)

- Highlighting of files based on syntax rules
- Load color theme from file
- Code block line based on indent and rule

# How to get syntax and theme files

Code block line based on indent and rule

- How to get syntax and theme files [Textmate](https://github.com/textmate)
- Eclipse also uses Textmate, and you can also get files from its related repository。
- Textmate is also used in [vscode](https://github.com/microsoft/vscode/tree/main/extensions), but
  its version is ahead of the version used in this module. You can get the configuration file from
  its source code, but not all of them can be used normally