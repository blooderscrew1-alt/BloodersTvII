package com.example.extensions

import java.util.UUID

data class WebExtension(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val author: String = "BloodersTv",
    val version: String = "1.0",
    val iconEmoji: String = "🧩",
    val isEnabled: Boolean = true,
    val matchPatterns: List<String> = listOf("<all_urls>"),
    val scriptContent: String,
    /** Raw declarativeNetRequest static rules, if the extension ships them. */
    val rulesJson: String = "",
    val isBuiltIn: Boolean = false
) {
    fun matchesUrl(url: String): Boolean {
        if (!isEnabled) return false
        if (matchPatterns.contains("<all_urls>") || matchPatterns.contains("*://*/*")) {
            return true
        }

        for (pattern in matchPatterns) {
            val cleanPattern = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
            val regex = Regex("^$cleanPattern")
            if (regex.containsMatchIn(url)) return true
        }
        return false
    }
}
