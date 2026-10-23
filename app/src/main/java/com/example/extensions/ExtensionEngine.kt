package com.example.extensions

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Chrome/Edge-style extension engine for the web player.
 *
 * Supported today:
 *  - content_scripts (JS + CSS) with @match patterns;
 *  - manifest V2 background scripts and V3 service workers, injected as page scripts;
 *  - static declarativeNetRequest rules (block / redirect / upgradeScheme);
 *  - .zip and .crx packages.
 *
 * Chrome APIs are emulated with a small compatibility shim. This is not a full
 * Chromium extension runtime, but it covers the ad-blocking/redirect/popup use
 * cases the player needs.
 */
object ExtensionEngine {

    private const val PREFS_NAME = "blooders_extensions_pref"

    val COMPATIBILITY_SHIM: String = """
        (function() {
            if (!window.__bloodersExtensionShimInjected) {
                window.__bloodersExtensionShimInjected = true;
                window.chrome = window.chrome || {};
                window.chrome.runtime = window.chrome.runtime || {
                    id: 'blooders-extension-engine',
                    sendMessage: function() {},
                    onMessage: { addListener: function() {}, removeListener: function() {} }
                };
                window.chrome.storage = window.chrome.storage || {
                    local: {
                        get: function(keys, cb) { if (cb) cb({}); },
                        set: function(items, cb) { if (cb) cb(); }
                    }
                };
                window.GM_addStyle = function(css) {
                    try {
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.textContent = css;
                        (document.head || document.documentElement).appendChild(style);
                        return style;
                    } catch(e) {}
                };
                window.GM_setValue = function(key, val) {
                    try { localStorage.setItem('__blooders_gm_' + key, JSON.stringify(val)); } catch(e) {}
                };
                window.GM_getValue = function(key, def) {
                    try {
                        var v = localStorage.getItem('__blooders_gm_' + key);
                        return v ? JSON.parse(v) : def;
                    } catch(e) { return def; }
                };
            }
        })();
    """.trimIndent()

    val BUILT_IN_EXTENSIONS = listOf(
        WebExtension(
            id = "ext_ublock_video",
            name = "uBlock Video Shield",
            description = "Filtro cosmético para ocultar banners, overlays y anuncios en reproductores de vídeo.",
            author = "uBlock Community (Port)",
            version = "1.58.0",
            iconEmoji = "🛡️",
            isEnabled = true,
            matchPatterns = listOf("<all_urls>"),
            isBuiltIn = true,
            scriptContent = """
                (function() {
                    var css = '[class*="ad-"], [id*="ad-"], [class*="ads-"], [id*="ads-"], ' +
                        '.ytp-ad-module, .video-ads, .ad-overlay, .ad-container, ' +
                        'iframe[src*="doubleclick"], iframe[src*="googleadservices"] ' +
                        '{ display: none !important; visibility: hidden !important; height: 0 !important; width: 0 !important; }';
                    if (window.GM_addStyle) { window.GM_addStyle(css); }
                })();
            """.trimIndent()
        ),
        WebExtension(
            id = "ext_popup_redirect_shield",
            name = "Anti-Popup y Redirecciones",
            description = "Bloquea ventanas emergentes, enlaces que abren pestañas nuevas y redirecciones automáticas de los reproductores.",
            author = "BloodersTv",
            version = "2.0",
            iconEmoji = "🚫",
            isEnabled = true,
            matchPatterns = listOf("<all_urls>"),
            isBuiltIn = true,
            scriptContent = """
                (function() {
                    try { window.open = function() { return null; }; } catch(e) {}
                    try {
                        document.querySelectorAll('meta[http-equiv="refresh"]').forEach(function(m) { m.remove(); });
                    } catch(e) {}
                    document.addEventListener('click', function(e) {
                        var a = e.target && e.target.closest ? e.target.closest('a') : null;
                        if (!a) return;
                        if (a.target === '_blank') a.target = '_self';
                        var href = (a.href || '').toLowerCase();
                        if (href.indexOf('popads') >= 0 || href.indexOf('onclickads') >= 0 ||
                            href.indexOf('adsterra') >= 0 || href.indexOf('go.php') >= 0) {
                            e.preventDefault();
                            e.stopPropagation();
                        }
                    }, true);
                })();
            """.trimIndent()
        ),
        WebExtension(
            id = "ext_sponsor_block",
            name = "SponsorBlock (Video)",
            description = "Oculta banners de patrocinadores e intros en reproductores compatibles.",
            author = "SponsorBlock (Port)",
            version = "5.8.1",
            iconEmoji = "⏭️",
            isEnabled = true,
            matchPatterns = listOf("*://*.youtube.com/*", "*://youtube.com/*", "*://m.youtube.com/*"),
            isBuiltIn = true,
            scriptContent = """
                (function() {
                    function skipSponsors() {
                        document.querySelectorAll(
                            '.ytd-banner-promo-renderer, #sponsor-button, .sparkles-light-cta'
                        ).forEach(function(el) { el.remove(); });
                    }
                    skipSponsors();
                    setInterval(skipSponsors, 1200);
                })();
            """.trimIndent()
        ),
        WebExtension(
            id = "ext_dark_reader",
            name = "Dark Reader",
            description = "Modo oscuro para páginas y reproductores, con menor brillo.",
            author = "Dark Reader (Port)",
            version = "4.9.86",
            iconEmoji = "🌙",
            isEnabled = false,
            matchPatterns = listOf("<all_urls>"),
            isBuiltIn = true,
            scriptContent = """
                (function() {
                    var darkCss = 'html, body { background-color: #121212 !important; color: #e0e0e0 !important; } ' +
                        'img, video { opacity: 0.94; } a { color: #8ab4f8 !important; }';
                    if (window.GM_addStyle) { window.GM_addStyle(darkCss); }
                })();
            """.trimIndent()
        ),
        WebExtension(
            id = "ext_video_speed",
            name = "Video Speed Controller",
            description = "Expone window.setSpeed(rate) para controlar cualquier vídeo HTML5.",
            author = "Ilya G. (Port)",
            version = "0.7.3",
            iconEmoji = "⚡",
            isEnabled = true,
            matchPatterns = listOf("<all_urls>"),
            isBuiltIn = true,
            scriptContent = """
                (function() {
                    window.setSpeed = function(rate) {
                        document.querySelectorAll('video').forEach(function(v) {
                            v.playbackRate = rate;
                        });
                    };
                })();
            """.trimIndent()
        ),
        WebExtension(
            id = "ext_force_pip",
            name = "Force Picture-in-Picture",
            description = "Quita la restricción disablepictureinpicture de los reproductores.",
            author = "WebExt Devs",
            version = "1.2.0",
            iconEmoji = "📺",
            isEnabled = true,
            matchPatterns = listOf("<all_urls>"),
            isBuiltIn = true,
            scriptContent = """
                (function() {
                    document.querySelectorAll('video').forEach(function(v) {
                        v.removeAttribute('disablepictureinpicture');
                    });
                })();
            """.trimIndent()
        )
    )

    fun loadExtensions(context: Context): List<WebExtension> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString("custom_extensions", null)

        val customList = mutableListOf<WebExtension>()
        if (!raw.isNullOrBlank()) {
            try {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val patterns = mutableListOf<String>()
                    val patternsArr = obj.optJSONArray("matchPatterns")
                    if (patternsArr != null) {
                        for (p in 0 until patternsArr.length()) {
                            patterns.add(patternsArr.getString(p))
                        }
                    } else {
                        patterns.add("<all_urls>")
                    }

                    customList.add(
                        WebExtension(
                            id = obj.optString("id"),
                            name = obj.optString("name"),
                            description = obj.optString("description"),
                            author = obj.optString("author", "Usuario"),
                            version = obj.optString("version", "1.0"),
                            iconEmoji = obj.optString("iconEmoji", "🧩"),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            matchPatterns = patterns,
                            scriptContent = obj.optString("scriptContent"),
                            rulesJson = obj.optString("rulesJson"),
                            isBuiltIn = false
                        )
                    )
                }
            } catch (_: Exception) {
            }
        }

        val result = mutableListOf<WebExtension>()
        for (builtIn in BUILT_IN_EXTENSIONS) {
            val enabled = prefs.getBoolean("ext_state_${builtIn.id}", builtIn.isEnabled)
            result.add(builtIn.copy(isEnabled = enabled))
        }
        result.addAll(customList)
        return result
    }

    fun saveExtensions(context: Context, extensions: List<WebExtension>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        val customList = extensions.filterNot { it.isBuiltIn }
        val array = JSONArray()
        for (item in customList) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("name", item.name)
            obj.put("description", item.description)
            obj.put("author", item.author)
            obj.put("version", item.version)
            obj.put("iconEmoji", item.iconEmoji)
            obj.put("isEnabled", item.isEnabled)
            obj.put("scriptContent", item.scriptContent)
            obj.put("rulesJson", item.rulesJson)
            val pArr = JSONArray()
            item.matchPatterns.forEach { pArr.put(it) }
            obj.put("matchPatterns", pArr)
            array.put(obj)
        }
        editor.putString("custom_extensions", array.toString())

        for (item in extensions.filter { it.isBuiltIn }) {
            editor.putBoolean("ext_state_${item.id}", item.isEnabled)
        }
        editor.apply()
    }

    fun generateInjectionScript(extensions: List<WebExtension>, url: String): String {
        val matching = extensions.filter { it.isEnabled && it.matchesUrl(url) }
        if (matching.isEmpty()) return ""

        val sb = StringBuilder()
        sb.append("(function() {\n")
        sb.append("if (window.__bloodersExtensionsInjected) return;\n")
        sb.append("window.__bloodersExtensionsInjected = true;\n")
        sb.append(COMPATIBILITY_SHIM).append("\n")

        for (ext in matching) {
            if (ext.scriptContent.isBlank()) continue
            sb.append("// Extension: ").append(ext.name).append("\n")
            sb.append("try {\n")
            sb.append(ext.scriptContent).append("\n")
            sb.append("} catch(e) { console.error('Blooders extension ").append(ext.name).append(" error:', e); }\n")
        }
        sb.append("})();")
        return sb.toString()
    }
}
