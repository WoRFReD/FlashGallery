// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.flashchip.flashgallery.model.ApkItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class ApkRepository(private val context: Context) {

    private val pm: PackageManager = context.packageManager

    suspend fun scanApks(): List<ApkItem> = withContext(Dispatchers.IO) {
        val apkFiles = mutableListOf<File>()

        // 1. Escaneo mediante MediaStore para APKs indexados por Android
        queryApksFromMediaStore(apkFiles)

        // 2. Escaneo del sistema de archivos en carpetas comunes
        val rootsToScan = mutableListOf<File>()
        val extStorage = Environment.getExternalStorageDirectory()
        if (extStorage != null && extStorage.exists()) {
            rootsToScan.add(File(extStorage, "Download"))
            rootsToScan.add(File(extStorage, "Downloads"))
            rootsToScan.add(File(extStorage, "Documents"))
            rootsToScan.add(File(extStorage, "Bluetooth"))
            rootsToScan.add(File(extStorage, "Telegram"))
            rootsToScan.add(File(extStorage, "WhatsApp"))
            rootsToScan.add(File(extStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents"))
            rootsToScan.add(File(extStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Documents"))
            rootsToScan.add(extStorage)
        }

        val visitedDirs = mutableSetOf<String>()
        for (dir in rootsToScan) {
            if (dir.exists() && dir.isDirectory) {
                scanDirRecursive(dir, apkFiles, visitedDirs, currentDepth = 0, maxDepth = 4)
            }
        }

        // Deduplicar archivos
        val uniqueFiles = apkFiles.distinctBy { it.canonicalPath }

        val apkItems = mutableListOf<ApkItem>()
        for (file in uniqueFiles) {
            try {
                val apkPath = file.absolutePath
                val pkgInfo = pm.getPackageArchiveInfo(apkPath, 0)
                val (appLabel, icon, pkgName, verName, verCode) = if (pkgInfo != null) {
                    val appInfo = pkgInfo.applicationInfo
                    appInfo.sourceDir = apkPath
                    appInfo.publicSourceDir = apkPath
                    val label = try {
                        pm.getApplicationLabel(appInfo).toString()
                    } catch (_: Exception) {
                        file.nameWithoutExtension
                    }
                    val iconDrawable = try {
                        pm.getApplicationIcon(appInfo)
                    } catch (_: Exception) {
                        null
                    }
                    val vName = pkgInfo.versionName ?: "1.0"
                    val vCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        pkgInfo.longVersionCode
                    } else {
                        @Suppress("DEPRECATION")
                        pkgInfo.versionCode.toLong()
                    }
                    Tuple5(label, iconDrawable, pkgInfo.packageName ?: "", vName, vCode)
                } else {
                    Tuple5(file.nameWithoutExtension, null, "apk.desconocido", "1.0", 1L)
                }

                val sizeBytes = file.length()
                val sizeFormatted = formatFileSize(sizeBytes)
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (_: Exception) {
                    android.net.Uri.fromFile(file)
                }

                apkItems.add(
                    ApkItem(
                        file = file,
                        fileName = file.name,
                        appLabel = appLabel,
                        packageName = pkgName,
                        versionName = verName,
                        versionCode = verCode,
                        sizeBytes = sizeBytes,
                        sizeFormatted = sizeFormatted,
                        lastModified = file.lastModified(),
                        icon = icon,
                        uri = uri
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        apkItems.sortedByDescending { it.lastModified }
    }

    private fun queryApksFromMediaStore(results: MutableList<File>) {
        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            val selection = "${MediaStore.Files.FileColumns.DATA} LIKE '%.apk' OR ${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
            val selectionArgs = arrayOf("application/vnd.android.package-archive")
            context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    if (dataCol != -1) {
                        val path = cursor.getString(dataCol)
                        if (!path.isNullOrBlank()) {
                            val file = File(path)
                            if (file.exists() && file.isFile && file.extension.equals("apk", ignoreCase = true)) {
                                results.add(file)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun scanDirRecursive(
        dir: File,
        results: MutableList<File>,
        visitedDirs: MutableSet<String>,
        currentDepth: Int,
        maxDepth: Int
    ) {
        if (currentDepth > maxDepth) return
        val canonical = try { dir.canonicalPath } catch (_: Exception) { dir.absolutePath }
        if (!visitedDirs.add(canonical)) return

        val files = try { dir.listFiles() } catch (_: Exception) { null } ?: return
        for (f in files) {
            val name = f.name
            if (name.startsWith(".")) continue

            if (f.isDirectory) {
                // Evitar carpetas protegidas que causan SecurityException en Android 11+
                if (name.equals("data", ignoreCase = true) || name.equals("obb", ignoreCase = true)) {
                    continue
                }
                if (name.equals("Android", ignoreCase = true)) {
                    val mediaFolder = File(f, "media")
                    if (mediaFolder.exists() && mediaFolder.isDirectory) {
                        scanDirRecursive(mediaFolder, results, visitedDirs, currentDepth + 1, maxDepth)
                    }
                    continue
                }
                scanDirRecursive(f, results, visitedDirs, currentDepth + 1, maxDepth)
            } else if (f.isFile && f.name.endsWith(".apk", ignoreCase = true)) {
                results.add(f)
            }
        }
    }

    fun launchInstaller(apk: ApkItem) {
        try {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk.file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteApk(apk: ApkItem): Boolean = withContext(Dispatchers.IO) {
        try {
            if (apk.file.exists()) {
                apk.file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun formatFileSize(bytes: Long): String {
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1.0) {
            String.format(Locale.US, "%.1f MB", mb)
        } else {
            val kb = bytes / 1024.0
            String.format(Locale.US, "%.0f KB", kb)
        }
    }

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )
}
