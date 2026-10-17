// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import java.util.concurrent.ConcurrentHashMap

class AppResolver(private val context: Context) {

    private val pm: PackageManager = context.packageManager
    private val iconCache = ConcurrentHashMap<String, Drawable?>()

    data class AppMatch(
        val appName: String,
        val packageName: String?
    )

    fun resolveApp(path: String, bucketName: String): AppMatch {
        val lowerPath = path.lowercase().replace('\\', '/')
        val lowerBucket = bucketName.lowercase()

        return when {
            lowerPath.contains("com.whatsapp.w4b") || lowerBucket.contains("whatsapp business") ->
                AppMatch("WhatsApp Business", "com.whatsapp.w4b")

            lowerPath.contains("com.whatsapp") || lowerPath.contains("whatsapp") || lowerBucket.contains("whatsapp") ->
                AppMatch("WhatsApp", "com.whatsapp")

            lowerPath.contains("org.telegram") || lowerPath.contains("telegram") || lowerBucket.contains("telegram") ->
                AppMatch("Telegram", "org.telegram.messenger")

            lowerPath.contains("com.discord") || lowerPath.contains("discord") || lowerBucket.contains("discord") ->
                AppMatch("Discord", "com.discord")

            lowerPath.contains("com.instagram") || lowerPath.contains("instagram") || lowerBucket.contains("instagram") ->
                AppMatch("Instagram", "com.instagram.android")

            lowerPath.contains("com.twitter") || lowerPath.contains("twitter") || lowerBucket == "x" || lowerBucket.contains("twitter") ->
                AppMatch("X (Twitter)", "com.twitter.android")

            lowerPath.contains("com.zhiliaoapp.musically") || lowerPath.contains("tiktok") || lowerBucket.contains("tiktok") ->
                AppMatch("TikTok", "com.zhiliaoapp.musically")

            lowerPath.contains("com.reddit") || lowerPath.contains("reddit") || lowerBucket.contains("reddit") ->
                AppMatch("Reddit", "com.reddit.frontpage")

            lowerPath.contains("com.facebook.katana") || lowerPath.contains("facebook") || lowerBucket.contains("facebook") ->
                AppMatch("Facebook", "com.facebook.katana")

            lowerPath.contains("com.facebook.orca") || lowerPath.contains("messenger") || lowerBucket.contains("messenger") ->
                AppMatch("Messenger", "com.facebook.orca")

            lowerPath.contains("screenshot") || lowerBucket.contains("screenshot") || lowerBucket.contains("captura") ->
                AppMatch("Capturas de pantalla", null)

            lowerPath.contains("dcim/camera") || lowerBucket.equals("camera", ignoreCase = true) || lowerBucket.equals("cámara", ignoreCase = true) || lowerBucket.equals("100media", ignoreCase = true) ->
                AppMatch("Cámara", resolveCameraPackage())

            lowerPath.contains("snapseed") || lowerBucket.contains("snapseed") ->
                AppMatch("Snapseed", "com.niksoftware.snapseed")

            lowerPath.contains("lightroom") || lowerBucket.contains("lightroom") ->
                AppMatch("Lightroom", "com.adobe.lrm")

            lowerPath.contains("canva") || lowerBucket.contains("canva") ->
                AppMatch("Canva", "com.canva.editor")

            lowerPath.contains("capcut") || lowerBucket.contains("capcut") ->
                AppMatch("CapCut", "com.lemon.lvoverseas")

            lowerPath.contains("/download") || lowerBucket.equals("download", ignoreCase = true) || lowerBucket.equals("descargas", ignoreCase = true) ->
                AppMatch("Descargas", null)

            else -> {
                val cleanBucket = bucketName.ifBlank { "Almacenamiento" }
                AppMatch(cleanBucket, null)
            }
        }
    }

    fun getAppIcon(packageName: String?): Drawable? {
        if (packageName.isNullOrBlank()) return null
        return iconCache.computeIfAbsent(packageName) { pkg ->
            try {
                pm.getApplicationIcon(pkg)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun resolveCameraPackage(): String? {
        return try {
            val intent = android.content.Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            resolveInfo?.activityInfo?.packageName
        } catch (_: Exception) {
            null
        }
    }
}
