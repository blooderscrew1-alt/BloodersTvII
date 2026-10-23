package com.example.extensions

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * Reads a Chrome/Edge extension from a .zip or .crx package.
 *
 * It extracts manifest.json, content scripts, CSS and static
 * declarativeNetRequest rules. Binary assets and remote extension APIs are not
 * supported; the goal is filtering the pages opened by the player.
 */
object ZipExtensionParser {

    private const val MAX_PACKAGE_BYTES = 25 * 1024 * 1024
    private const val MAX_FILE_BYTES = 4 * 1024 * 1024
    private const val MAX_RULES = 12_000

    data class ParsedExtension(
        val name: String,
        val description: String,
        val version: String,
        val matchPatterns: List<String>,
        val scriptContent: String,
        val rulesJson: String
    )

    fun parseZip(inputStream: InputStream): Result<ParsedExtension> {
        return try {
            val packageBytes = inputStream.use { it.readBytes() }
            if (packageBytes.isEmpty()) {
                return Result.failure(Exception("El archivo está vacío."))
            }
            if (packageBytes.size > MAX_PACKAGE_BYTES) {
                return Result.failure(Exception("El paquete supera el límite de 25 MB."))
            }

            val zipBytes = stripCrxHeader(packageBytes)
            val fileMap = readZipFiles(zipBytes)
            if (fileMap.isEmpty()) {
                return Result.failure(Exception("El .zip/.crx no contiene archivos legibles."))
            }

            val manifestKey = fileMap.keys.find { it.endsWith("manifest.json") }
                ?: return Result.failure(Exception("No se encontró manifest.json en el paquete."))

            val rootPrefix = if (manifestKey.contains("/")) {
                manifestKey.substringBeforeLast("/") + "/"
            } else {
                ""
            }

            val manifestBytes = fileMap[manifestKey]
                ?: return Result.failure(Exception("No se pudo leer manifest.json."))
            val manifestJson = JSONObject(String(manifestBytes, Charsets.UTF_8))

            val name = manifestJson.optString("name", "Extensión Chrome/Edge")
            val description = manifestJson.optString("description", "Extensión importada")
            val version = manifestJson.optString("version", "1.0")

            val matchPatterns = mutableListOf<String>()
            val combinedScript = StringBuilder()

            val contentScripts = manifestJson.optJSONArray("content_scripts")
            if (contentScripts != null && contentScripts.length() > 0) {
                for (i in 0 until contentScripts.length()) {
                    val csObj = contentScripts.optJSONObject(i) ?: continue

                    csObj.optJSONArray("matches")?.let { matches ->
                        for (m in 0 until matches.length()) {
                            matchPatterns.add(matches.getString(m))
                        }
                    }

                    csObj.optJSONArray("css")?.let { cssFiles ->
                        for (c in 0 until cssFiles.length()) {
                            val cssPath = rootPrefix + cssFiles.getString(c)
                            val cssBytes = fileMap[cssPath] ?: fileMap[cssFiles.getString(c)]
                            if (cssBytes != null) {
                                val cssText = String(cssBytes, Charsets.UTF_8)
                                    .replace("\\", "\\\\")
                                    .replace("`", "\\`")
                                combinedScript.append(
                                    "if (window.GM_addStyle) { window.GM_addStyle(`$cssText`); }\n"
                                )
                            }
                        }
                    }

                    csObj.optJSONArray("js")?.let { jsFiles ->
                        for (j in 0 until jsFiles.length()) {
                            val jsPath = rootPrefix + jsFiles.getString(j)
                            val jsBytes = fileMap[jsPath] ?: fileMap[jsFiles.getString(j)]
                            if (jsBytes != null) {
                                combinedScript.append("// Script: ").append(jsFiles.getString(j)).append("\n")
                                combinedScript.append(String(jsBytes, Charsets.UTF_8)).append("\n")
                            }
                        }
                    }
                }
            } else {
                val background = manifestJson.optJSONObject("background")
                val serviceWorker = background?.optString("service_worker")
                val bgScripts = background?.optJSONArray("scripts")

                if (!serviceWorker.isNullOrBlank()) {
                    val swBytes = fileMap[rootPrefix + serviceWorker] ?: fileMap[serviceWorker]
                    if (swBytes != null) {
                        combinedScript.append(String(swBytes, Charsets.UTF_8)).append("\n")
                    }
                } else if (bgScripts != null) {
                    for (b in 0 until bgScripts.length()) {
                        val bBytes = fileMap[rootPrefix + bgScripts.getString(b)]
                            ?: fileMap[bgScripts.getString(b)]
                        if (bBytes != null) {
                            combinedScript.append(String(bBytes, Charsets.UTF_8)).append("\n")
                        }
                    }
                }
            }

            if (matchPatterns.isEmpty()) {
                matchPatterns.add("<all_urls>")
            }

            val rulesJson = readDeclarativeNetRequestRules(manifestJson, fileMap, rootPrefix)

            if (combinedScript.isBlank() && rulesJson.isBlank()) {
                return Result.failure(
                    Exception("La extensión no tiene content scripts, background ni reglas de red utilizables.")
                )
            }

            Result.success(
                ParsedExtension(
                    name = name,
                    description = description,
                    version = version,
                    matchPatterns = matchPatterns.distinct(),
                    scriptContent = combinedScript.toString().ifBlank {
                        "// Extensión solo de reglas de red (declarativeNetRequest)."
                    },
                    rulesJson = rulesJson
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readZipFiles(bytes: ByteArray): Map<String, ByteArray> {
        val fileMap = mutableMapOf<String, ByteArray>()
        var totalRead = 0L
        var entries = 0

        ZipInputStream(ByteArrayInputStream(bytes)).use { zipIn ->
            var entry: ZipEntry? = zipIn.nextEntry
            while (entry != null && entries < 3000) {
                entries++
                if (!entry.isDirectory) {
                    val name = entry.name.replace('\\', '/')
                    val lower = name.lowercase()
                    val textFile = lower.endsWith(".json") || lower.endsWith(".js") ||
                        lower.endsWith(".mjs") || lower.endsWith(".css") || lower.endsWith(".html")
                    if (textFile) {
                        val buffer = ByteArray(8192)
                        val out = ByteArrayOutputStream()
                        var len: Int
                        var fileSize = 0
                        while (zipIn.read(buffer).also { len = it } > 0) {
                            fileSize += len
                            totalRead += len
                            if (fileSize > MAX_FILE_BYTES || totalRead > MAX_PACKAGE_BYTES) break
                            out.write(buffer, 0, len)
                        }
                        if (fileSize <= MAX_FILE_BYTES && totalRead <= MAX_PACKAGE_BYTES) {
                            fileMap[name] = out.toByteArray()
                        }
                        if (totalRead > MAX_PACKAGE_BYTES) break
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
        return fileMap
    }

    private fun readDeclarativeNetRequestRules(
        manifest: JSONObject,
        fileMap: Map<String, ByteArray>,
        rootPrefix: String
    ): String {
        val dnr = manifest.optJSONObject("declarative_net_request") ?: return ""
        val resources = dnr.optJSONArray("rule_resources") ?: return ""
        val combined = JSONArray()

        for (i in 0 until resources.length()) {
            if (combined.length() >= MAX_RULES) break
            val resource = resources.optJSONObject(i) ?: continue
            if (!resource.optBoolean("enabled", true)) continue

            val path = resource.optString("path")
            if (path.isBlank()) continue
            val bytes = fileMap[rootPrefix + path] ?: fileMap[path] ?: continue

            try {
                val rules = JSONArray(String(bytes, Charsets.UTF_8))
                for (r in 0 until rules.length()) {
                    if (combined.length() >= MAX_RULES) break
                    rules.optJSONObject(r)?.let { combined.put(it) }
                }
            } catch (_: Exception) {
            }
        }

        return if (combined.length() == 0) "" else combined.toString()
    }

    /**
     * CRX2/CRX3 files are a small header followed by a normal zip. Find the
     * first local-file signature and parse from there.
     */
    private fun stripCrxHeader(bytes: ByteArray): ByteArray {
        if (bytes.size < 16 || bytes[0] != 'C'.code.toByte() || bytes[1] != 'r'.code.toByte() ||
            bytes[2] != '2'.code.toByte() || bytes[3] != '4'.code.toByte()
        ) {
            return bytes
        }

        val version = readIntLe(bytes, 4)
        val zipOffset = when (version) {
            3 -> 12 + readIntLe(bytes, 8)
            2 -> 16 + readIntLe(bytes, 8) + readIntLe(bytes, 12)
            else -> -1
        }
        if (zipOffset in 16 until bytes.size) {
            return bytes.copyOfRange(zipOffset, bytes.size)
        }

        // Fallback for unusual headers: find the local file signature.
        for (i in 12 until bytes.size - 4) {
            if (bytes[i] == 0x50.toByte() && bytes[i + 1] == 0x4B.toByte() &&
                bytes[i + 2] == 0x03.toByte() && bytes[i + 3] == 0x04.toByte()
            ) {
                return bytes.copyOfRange(i, bytes.size)
            }
        }
        return bytes
    }

    private fun readIntLe(bytes: ByteArray, offset: Int): Int {
        if (offset + 3 >= bytes.size) return -1
        return (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)
    }
}
