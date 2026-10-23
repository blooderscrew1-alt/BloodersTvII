package com.example.extensions

import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * Applies static declarativeNetRequest rules shipped by installed extensions.
 *
 * It supports the common block/allow/redirect/upgradeScheme conditions:
 * urlFilter, regexFilter, requestDomains, excludedRequestDomains and
 * initiatorDomains (ignored because WebView does not expose an initiator here).
 * Resource-type filters are ignored; a blocked URL is blocked for every
 * subresource, which is the desired behaviour for ad/tracker domains.
 */
class ExtensionNetworkFilter(extensions: List<WebExtension>) {

    private data class Rule(
        val hosts: List<String> = emptyList(),
        val excludedHosts: List<String> = emptyList(),
        val contains: String? = null,
        val prefix: String? = null,
        val regex: Regex? = null,
        val redirectTo: String? = null,
        val upgradeScheme: Boolean = false
    ) {
        fun matches(url: String, host: String): Boolean {
            if (hosts.isNotEmpty() && hosts.none { hostMatches(host, it) }) return false
            if (excludedHosts.any { hostMatches(host, it) }) return false
            if (contains != null && !url.contains(contains, ignoreCase = true)) return false
            if (prefix != null && !url.startsWith(prefix, ignoreCase = true)) return false
            if (regex != null && !regex.containsMatchIn(url)) return false
            return hosts.isNotEmpty() || excludedHosts.isNotEmpty() || contains != null ||
                prefix != null || regex != null
        }
    }

    private val blockHosts = HashSet<String>()
    private val blockRules = ArrayList<Rule>()
    private val allowHosts = HashSet<String>()
    private val allowRules = ArrayList<Rule>()
    private val redirectRules = ArrayList<Rule>()
    private val upgradeRules = ArrayList<Rule>()

    init {
        for (extension in extensions) {
            if (!extension.isEnabled || extension.rulesJson.isBlank()) continue
            try {
                val rules = JSONArray(extension.rulesJson)
                for (i in 0 until rules.length()) {
                    val obj = rules.optJSONObject(i) ?: continue
                    addRule(obj)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun shouldBlock(url: String): Boolean {
        if (url.isBlank()) return false
        val host = hostOf(url)
        if (matchesAllow(url, host)) return false
        if (hostInSet(host, blockHosts)) return true
        return blockRules.any { it.matches(url, host) }
    }

    fun redirectFor(url: String): String? {
        val host = hostOf(url)
        for (rule in redirectRules) {
            if (rule.matches(url, host)) {
                val target = rule.redirectTo ?: continue
                return if (rule.regex != null && target.contains("\\")) {
                    try {
                        rule.regex.replace(url, target.replace("\\", "$"))
                    } catch (_: Exception) {
                        target
                    }
                } else {
                    target
                }
            }
        }
        return null
    }

    fun shouldUpgradeToHttps(url: String): Boolean {
        if (!url.startsWith("http://", ignoreCase = true)) return false
        val host = hostOf(url)
        return upgradeRules.any { it.matches(url, host) }
    }

    private fun matchesAllow(url: String, host: String): Boolean {
        if (hostInSet(host, allowHosts)) return true
        return allowRules.any { it.matches(url, host) }
    }

    private fun addRule(obj: JSONObject) {
        val condition = obj.optJSONObject("condition") ?: return
        val action = obj.optJSONObject("action") ?: return
        val actionType = action.optString("type").lowercase(Locale.ROOT)
        if (actionType !in setOf("block", "allow", "redirect", "upgradescheme")) return

        val hosts = parseStringArray(condition.optJSONArray("requestDomains")) +
            parseStringArray(condition.optJSONArray("domains"))
        val excludedHosts = parseStringArray(condition.optJSONArray("excludedRequestDomains")) +
            parseStringArray(condition.optJSONArray("excludedDomains"))

        var contains: String? = null
        var prefix: String? = null
        var regex: Regex? = null

        val urlFilter = condition.optString("urlFilter").substringBefore('$')
        if (urlFilter.isNotBlank()) {
            when {
                urlFilter.startsWith("||") -> {
                    val rest = urlFilter.removePrefix("||")
                    val domainEnd = rest.indexOfFirst { it == '^' || it == '/' || it == '*' || it == '|' }
                    val domain = (if (domainEnd >= 0) rest.substring(0, domainEnd) else rest)
                        .removePrefix("*.")
                        .lowercase(Locale.ROOT)
                    val tail = if (domainEnd >= 0) rest.substring(domainEnd) else ""
                    val path = tail.trimStart('^', '*').trimEnd('|', '*')

                    if (domain.isNotBlank()) {
                        val rule = Rule(
                            hosts = hosts + domain,
                            excludedHosts = excludedHosts,
                            contains = path.takeIf { it.isNotBlank() }
                        )
                        addToAction(actionType, action, rule, onlyHost = path.isBlank())
                    }
                }

                urlFilter.startsWith("|") -> {
                    prefix = urlFilter.removePrefix("|").trimEnd('|', '*')
                }

                else -> {
                    contains = urlFilter.trim('*').replace("^", "")
                }
            }
        }

        val regexFilter = condition.optString("regexFilter")
        if (regexFilter.isNotBlank() && regex == null) {
            regex = try {
                Regex(regexFilter, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                null
            }
        }

        if (contains.isNullOrBlank() && prefix.isNullOrBlank() && regex == null && hosts.isEmpty()) {
            return
        }

        val rule = Rule(
            hosts = hosts,
            excludedHosts = excludedHosts,
            contains = contains,
            prefix = prefix,
            regex = regex
        )
        addToAction(actionType, action, rule, onlyHost = false)
    }

    private fun addToAction(
        actionType: String,
        action: JSONObject,
        rule: Rule,
        onlyHost: Boolean
    ) {
        when (actionType) {
            "block" -> {
                if (onlyHost && rule.hosts.isNotEmpty() && rule.contains == null &&
                    rule.prefix == null && rule.regex == null && rule.excludedHosts.isEmpty()
                ) {
                    blockHosts.addAll(rule.hosts.map { it.lowercase(Locale.ROOT) })
                } else {
                    blockRules.add(rule)
                }
            }

            "allow" -> {
                if (onlyHost && rule.hosts.isNotEmpty() && rule.contains == null &&
                    rule.prefix == null && rule.regex == null && rule.excludedHosts.isEmpty()
                ) {
                    allowHosts.addAll(rule.hosts.map { it.lowercase(Locale.ROOT) })
                } else {
                    allowRules.add(rule)
                }
            }

            "redirect" -> {
                val redirect = action.optJSONObject("redirect") ?: return
                val target = redirect.optString("url").takeIf { it.isNotBlank() } ?: return
                redirectRules.add(rule.copy(redirectTo = target))
            }

            "upgradescheme" -> upgradeRules.add(rule.copy(upgradeScheme = true))
        }
    }

    private fun parseStringArray(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        val result = ArrayList<String>(array.length())
        for (i in 0 until array.length()) {
            val value = array.optString(i).trim().lowercase(Locale.ROOT)
                .removePrefix("*.")
            if (value.isNotBlank()) result.add(value)
        }
        return result
    }

    companion object {
        private fun hostOf(url: String): String {
            return try {
                Uri.parse(url).host?.lowercase(Locale.ROOT).orEmpty()
            } catch (_: Exception) {
                ""
            }
        }

        private fun hostMatches(host: String, suffix: String): Boolean {
            if (host.isBlank() || suffix.isBlank()) return false
            return host == suffix || host.endsWith(".$suffix")
        }

        private fun hostInSet(host: String, set: Set<String>): Boolean {
            if (host.isBlank() || set.isEmpty()) return false
            var candidate = host
            while (true) {
                if (candidate in set) return true
                val dot = candidate.indexOf('.')
                if (dot < 0) return false
                candidate = candidate.substring(dot + 1)
            }
        }
    }
}
