<div align="center">

![Banner](/images/editor_banner.jpg)
----
[![CI](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml/badge.svg?event=push)](https://github.com/Rosemoe/CodeEditor/actions/workflows/gradle.yml)
[![GitHub license](https://img.shields.io/github/license/Rosemoe/CodeEditor)](https://github.com/Rosemoe/CodeEditor/blob/main/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.Rosemoe.sora-editor/editor.svg?label=Maven%20Central)]((https://search.maven.org/search?q=io.github.Rosemoe.sora-editor%20editor))   
[![Telegram](https://img.shields.io/badge/Join-Telegram-blue)](https://t.me/rosemoe_code_editor)
[![QQ](https://img.shields.io/badge/Join-QQ_Group-ff69b4)](https://jq.qq.com/?_wv=1027&k=n68uxQws)

sora-editor es un increíble editor de código optimizado para Android

</div>

Lea esto en otros idiomas: [English](README.md), [简体中文](README.zh-cn.md), [Español](README.es.md).

***Trabajo en Progreso***

Este proyecto todavía se está desarrollando lentamente.
Descarga las versiones más recientes desde [Releases](https://github.com/Rosemoe/CodeEditor/releases)
en lugar de clonar este repositorio directamente.
**Los problemas y las solicitudes de extracción son bienvenidos.**

## Características

- [x] Resaltado de sintaxis
- [x] Autocompletar código (con [code snippets](https://macromates.com/manual/en/snippets))
- [x] Sangría automática
- [x] Líneas de bloque de código
- [x] Texto escalable
- [x] Deshacer/rehacer
- [x] Buscar y reemplazar
- [x] Ajuste automático de palabras
- [x] Mostrar caracteres no imprimibles
- [x] Indicadores de error/advertencia/tipografía/obsoletos
- [x] Lupa de texto
- [x] Análisis de resaltado incremental
- [x] Resaltar pares de corchetes
- [x] Sistema de eventos

## Atajos de teclado

Cuando trabaje con un teclado físico, puede usar combinaciones de teclas para realizar varias funciones de texto.
comportamiento.
El editor proporciona compatibilidad con algunas combinaciones de teclas de forma predeterminada.
Sin embargo, puedes echarle un vistazo
a [`KeyBindingEvent`](https://github.com/Rosemoe/sora-editor/blob/main/editor/src/main/java/io/github/rosemoe/sora/event/KeyBindingEvent.java)
y agregar tus propias combinaciones de teclas. Incluso puede anular los enlaces de teclas predeterminados y realizar acciones personalizadas que usted necesite.

Las combinaciones de teclas admitidas actualmente son en su mayoría similares a Android Studio/Intellij IDEA.
Consulte las [combinaciones de teclas admitidas](./keybindings.md).

## Capturas de pantalla

<div style="overflow: hidden">
<img src="/images/general.jpg" alt="GeneralAppearance" width="40%" align="bottom" />
<img src="/images/problem_indicators.jpg" alt="ProblemIndicator" width="40%" align="bottom" />
</div>

## Para empezar

Agregue las dependencias en su aplicación:

```Gradle
dependencies {
    implementation(platform("io.github.Rosemoe.sora-editor:bom:<versionName>"))
    implementation("io.github.Rosemoe.sora-editor:<moduleName>")
}
```

Modulos disponibles:

**- editor**
Biblioteca de widgets que contiene todas las cosas básicas del marco

**- editor-lsp**
Una biblioteca conveniente para crear idiomas usando el Protocolo de Servidor de Lenguajes (también conocido como LSP)

**- language-java**
Una implementación simple para el resaltado de Java y el autocompletado de identificadores

**- language-textmate**
Un resaltador avanzado para el editor. Puede encontrar paquetes y temas de idiomas para compañeros de texto y cargar ellos usando este módulo. La implementación interna de textmate es de [tm4e](https://github.com/eclipse/tm4e).

Compruebe la versión más reciente ingresando a [Releases](https://github.com/Rosemoe/CodeEditor/releases).

## Comunidad

* [Grupo Oficial de QQ](https://jq.qq.com/?_wv=1027&k=n68uxQws)
* [Grupo Oficial de Telegram](https://t.me/rosemoe_code_editor)

## Colaboradores

<a href="https://github.com/Rosemoe/sora-editor/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Rosemoe/sora-editor" />
</a>

## Licencias

```
sora-editor - the awesome code editor for Android
https://github.com/Rosemoe/sora-editor
Copyright (C) 2020-2022  Rosemoe

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
USA

Please contact Rosemoe by email 2073412493@qq.com if you need
additional information or have any questions
```

## Agradecimientos

Gracias a [JetBrains](https://www.jetbrains.com/?from=CodeEditor) por brindar licencia de código abierto gratuito
 para IDE como [IntelliJ IDEA](https://www.jetbrains.com/idea/?from=CodeEditor).   
[<img src=".github/jetbrains-variant-3.png" width="200"/>](https://www.jetbrains.com/?from=CodeEditor)
