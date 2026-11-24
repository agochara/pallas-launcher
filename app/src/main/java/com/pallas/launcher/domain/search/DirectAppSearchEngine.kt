package com.pallas.launcher.domain.search

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

data class DirectAppTarget(
    val aliases: List<String>,
    val displayName: String,
    val packages: List<String>,
    val buildIntent: (packageName: String, query: String) -> Intent
)

data class ParsedDirectAppSearch(
    val target: DirectAppTarget,
    val query: String
)

object DirectAppSearchEngine {

    val TARGETS: List<DirectAppTarget> = listOf(
        DirectAppTarget(
            aliases = listOf("yt", "youtube"),
            displayName = "YouTube",
            packages = listOf("com.google.android.youtube"),
            buildIntent = { pkg, q ->
                Intent(Intent.ACTION_SEARCH).apply {
                    setPackage(pkg)
                    putExtra("query", q)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        ),
        DirectAppTarget(
            aliases = listOf("az", "amz", "amazon"),
            displayName = "Amazon",
            packages = listOf("in.amazon.mShop.android.shopping", "com.amazon.mShop.android.shopping"),
            buildIntent = { pkg, q ->
                val host = if (pkg == "in.amazon.mShop.android.shopping") "www.amazon.in" else "www.amazon.com"
                Intent(Intent.ACTION_VIEW, Uri.parse("https://$host/s?k=${Uri.encode(q)}")).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        ),
        DirectAppTarget(
            aliases = listOf("sp", "spotify"),
            displayName = "Spotify",
            packages = listOf("com.spotify.music"),
            buildIntent = { pkg, q ->
                Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:${Uri.encode(q)}")).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        ),
        DirectAppTarget(
            aliases = listOf("gm", "maps"),
            displayName = "Google Maps",
            packages = listOf("com.google.android.apps.maps"),
            buildIntent = { pkg, q ->
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(q)}")).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        ),
        DirectAppTarget(
            aliases = listOf("play", "ps"),
            displayName = "Google Play",
            packages = listOf("com.android.vending"),
            buildIntent = { pkg, q ->
                Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(q)}&c=apps")).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        ),
        DirectAppTarget(
            aliases = listOf("rd", "reddit"),
            displayName = "Reddit",
            packages = listOf("com.reddit.frontpage"),
            buildIntent = { pkg, q ->
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.reddit.com/search/?q=${Uri.encode(q)}")).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        ),
        DirectAppTarget(
            aliases = listOf("gh", "github"),
            displayName = "GitHub",
            packages = listOf("com.github.android"),
            buildIntent = { pkg, q ->
                Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/search?q=${Uri.encode(q)}")).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        ),
        DirectAppTarget(
            aliases = listOf("x", "tw"),
            displayName = "X",
            packages = listOf("com.twitter.android"),
            buildIntent = { pkg, q ->
                Intent(Intent.ACTION_VIEW, Uri.parse("twitter://search?query=${Uri.encode(q)}")).apply {
                    setPackage(pkg)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    )

    private val TARGET_BY_ALIAS: Map<String, DirectAppTarget> = buildMap {
        TARGETS.forEach { target ->
            target.aliases.forEach { alias -> put(alias.lowercase(Locale.ROOT), target) }
        }
    }

    fun parse(arg: String): ParsedDirectAppSearch? {
        val clean = arg.trim()
        if (clean.isEmpty()) return null
        val firstSpace = clean.indexOfFirst { it.isWhitespace() }
        val token = if (firstSpace == -1) clean else clean.substring(0, firstSpace)
        val query = if (firstSpace == -1) "" else clean.substring(firstSpace + 1).trim()
        val target = TARGET_BY_ALIAS[token.lowercase(Locale.ROOT)] ?: return null
        return ParsedDirectAppSearch(target, query)
    }

    fun launch(context: Context, target: DirectAppTarget, query: String) {
        val pm = context.packageManager
        val installedPkg = target.packages.firstOrNull { pkg ->
            pm.getLaunchIntentForPackage(pkg) != null
        }
        if (installedPkg == null) {
            Toast.makeText(context, "${target.displayName} is not installed", Toast.LENGTH_SHORT).show()
            return
        }
        if (query.isBlank()) {
            val launchIntent = pm.getLaunchIntentForPackage(installedPkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (launchIntent != null) {
                try {
                    context.startActivity(launchIntent)
                    return
                } catch (_: Exception) {}
            }
        }
        try {
            context.startActivity(target.buildIntent(installedPkg, query))
        } catch (_: Exception) {
            val fallback = pm.getLaunchIntentForPackage(installedPkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (fallback != null) {
                try {
                    context.startActivity(fallback)
                    return
                } catch (_: Exception) {}
            }
            Toast.makeText(context, "Could not search inside ${target.displayName}", Toast.LENGTH_SHORT).show()
        }
    }
}
