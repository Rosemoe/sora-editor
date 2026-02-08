/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2026  Rosemoe
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

import org.gradle.api.Project
import java.io.File
import java.util.Base64
import java.util.Properties

/**
 * Signing config for application
 */
object AppSigning {

    data class AppSigningConfig(
        val storeFile: File,
        val storePassword: String,
        val keyAlias: String,
        val keyPassword: String
    )

    fun getAppSigningConfigOptional(project: Project): Result<AppSigningConfig> = runCatching {
        getAppSigningConfig(project)
    }.onFailure {
        val message = when (it) {
            is MissingEnvVarException -> "App signing config not correctly configured"
            else -> "Error when generating app signing config"
        }
        project.logger.error(message, it)
    }

    fun getAppSigningConfig(project: Project): AppSigningConfig {
        val properties = Properties().also {
            val file = project.rootProject.file("signing.properties")
            if (file.exists()) {
                file.reader().use { rd ->
                    it.load(rd)
                }
            }
        }
        val storeFile = project.rootProject.file("signing.keystore")

        val storeBin = getEnvOrProp(properties, "SIGNING_STORE_BIN")
        val storePassword = getEnvOrProp(properties, "SIGNING_STORE_PASSWORD")
        val keyAlias = getEnvOrProp(properties, "SIGNING_KEY_ALIAS")
        val keyPassword = getEnvOrProp(properties, "SIGNING_KEY_PASSWORD")

        val keystoreData = Base64.getDecoder().decode(storeBin)
        storeFile.parentFile.mkdirs()
        storeFile.createNewFile()
        storeFile.writeBytes(keystoreData)

        return AppSigningConfig(
            storeFile = storeFile,
            storePassword = storePassword,
            keyAlias = keyAlias,
            keyPassword = keyPassword
        )
    }

    private fun getEnvOrProp(properties: Properties, key: String): String {
        var value: String? = System.getenv(key)
        if (value.isNullOrBlank()) {
            value = properties[key] as? String?
        }

        if (value.isNullOrBlank()) {
            throw MissingEnvVarException("`$key` is not set in environment variables or properties")
        }
        return value
    }

    class MissingEnvVarException(msg: String) : Exception(msg)

}