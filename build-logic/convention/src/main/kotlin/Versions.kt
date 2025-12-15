/*******************************************************************************
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
 ******************************************************************************/
@SuppressWarnings("unused")
object Versions {
    // Project versions
    private const val version = "0.24.3"
    const val versionCode = 92

    val versionName by lazy {
        if (CI.isCiBuild) {
            "$version-SNAPSHOT"
        } else version
    }

    // Platform & Tool versions
    const val buildToolsVersion = "36.0.0"
    const val compileSdkVersion = 36
    const val minSdkVersion = 21
    const val minSdkVersionHighApi = 26
    const val targetSdkVersion = 36
}