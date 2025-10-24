/*
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 */
package io.github.rosemoe.sora.langs.textmate.registry

import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IThemeSource

class ThemeRegistry {
    private val allListener = arrayListOf<ThemeChangeListener>()

    private val allThemeModel = arrayListOf<ThemeModel>()

    private var _currentThemeModel = ThemeModel.EMPTY
    var currentThemeModel: ThemeModel
        get() = _currentThemeModel
        set(value) {
            _currentThemeModel = value
            if (!allThemeModel.contains(value)) {
                allThemeModel.add(value)
            }
            if (!value.isLoaded) {
                try {
                    value.load()
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            }
            dispatchThemeChange(_currentThemeModel)
        }


    @JvmOverloads
    @Throws(Exception::class)
    fun loadTheme(themeSource: IThemeSource, isCurrentTheme: Boolean = true) {
        loadTheme(ThemeModel(themeSource), isCurrentTheme)
    }

    @JvmOverloads
    @Synchronized
    @Throws(Exception::class)
    fun loadTheme(themeModel: ThemeModel, isCurrentTheme: Boolean = true) {
        if (!themeModel.isLoaded) {
            themeModel.load()
        }
        val theme = findThemeByThemeName(themeModel.name)
        if (theme != null) {
            currentThemeModel = theme
            return
        }
        allThemeModel.add(themeModel)
        if (isCurrentTheme) {
            currentThemeModel = themeModel
        }
    }


    fun findThemeByFileName(name: String): ThemeModel? {
        return allThemeModel.find { it.name == name }
    }

    fun findThemeByThemeName(name: String): ThemeModel? {
        return allThemeModel.find { it.rawTheme?.name == name }
    }

    @Synchronized
    fun setTheme(name: String): Boolean {
        val targetModel = findThemeByFileName(name)
            ?: findThemeByThemeName(name)
        return if (targetModel != null) {
            currentThemeModel = targetModel
            true
        } else {
            false
        }
    }

    private fun dispatchThemeChange(targetThemeModel: ThemeModel) {
        allListener.forEach { it.onChangeTheme(targetThemeModel) }
    }

    @Synchronized
    fun addListener(themeChangeListener: ThemeChangeListener) {
        if (allListener.contains(themeChangeListener)) return
        allListener.add(themeChangeListener)
    }

    @Synchronized
    fun removeListener(themeChangeListener: ThemeChangeListener?) {
        allListener.remove(themeChangeListener)
    }


    fun dispose() {
        allListener.clear()
    }


    fun interface ThemeChangeListener {
        fun onChangeTheme(newTheme: ThemeModel)
    }

    companion object {
        private var instance: ThemeRegistry? = null

        @JvmStatic
        fun getInstance(): ThemeRegistry {
            if (instance == null) {
                instance = ThemeRegistry()
            }
            return instance!!
        }
    }
}
