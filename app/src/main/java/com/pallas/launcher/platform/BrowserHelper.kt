package com.pallas.launcher.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

data class BrowserInfo(val packageName: String, val label: String)

object BrowserHelper {

    private fun probeIntent() = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com"))
        .addCategory(Intent.CATEGORY_BROWSABLE)

    /** Every installed app that can open an https link, A-Z. Requires the https <queries> entry in the manifest. */
    fun listBrowsers(context: Context): List<BrowserInfo> {
        val pm = context.packageManager
        val infos = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(probeIntent(), PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(probeIntent(), PackageManager.MATCH_ALL)
            }
        } catch (_: Exception) {
            emptyList()
        }
        return infos
            .mapNotNull { ri ->
                val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                BrowserInfo(pkg, ri.loadLabel(pm)?.toString() ?: pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** Human label for a stored package, or null if it is no longer installed. */
    fun labelFor(context: Context, packageName: String?): String? {
        if (packageName.isNullOrBlank()) return null
        return try {
            val pm = context.packageManager
            pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString()
        } catch (_: Exception) {
            null
        }
    }
}
