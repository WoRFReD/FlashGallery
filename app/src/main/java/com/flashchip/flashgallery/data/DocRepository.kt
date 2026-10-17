// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.flashchip.flashgallery.model.DocCategory
import com.flashchip.flashgallery.model.DocItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

class DocRepository(private val context: Context) {

    private val supportedExtensions = setOf(
        "pdf", "txt", "log", "md", "csv", "ini", "conf",
        "xml", "json", "html", "htm", "css", "js", "yaml", "yml",
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt",
        "epub", "mobi", "fb2", "cbr", "cbz"
    )

    suspend fun scanDocuments(): List<DocItem> = withContext(Dispatchers.IO) {
        val foundFiles = mutableListOf<File>()

        // 1. Escaneo mediante MediaStore para documentos indexados
        queryDocsFromMediaStore(foundFiles)

        // 2. Escaneo del sistema de archivos en directorios comunes
        val rootsToScan = mutableListOf<File>()
        val extStorage = Environment.getExternalStorageDirectory()
        if (extStorage != null && extStorage.exists()) {
            rootsToScan.add(File(extStorage, "Documents"))
            rootsToScan.add(File(extStorage, "Download"))
            rootsToScan.add(File(extStorage, "Downloads"))
            rootsToScan.add(File(extStorage, "Books"))
            rootsToScan.add(File(extStorage, "Telegram"))
            rootsToScan.add(File(extStorage, "WhatsApp"))
            rootsToScan.add(File(extStorage, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents"))
            rootsToScan.add(File(extStorage, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/WhatsApp Business Documents"))
            rootsToScan.add(extStorage)
        }

        val visitedDirs = mutableSetOf<String>()
        for (dir in rootsToScan) {
            if (dir.exists() && dir.isDirectory) {
                scanDirRecursive(dir, foundFiles, visitedDirs, currentDepth = 0, maxDepth = 4)
            }
        }

        val uniqueFiles = foundFiles.distinctBy { it.canonicalPath }
        val docItems = mutableListOf<DocItem>()

        for (file in uniqueFiles) {
            try {
                val ext = file.extension.lowercase()
                val category = resolveCategory(ext)
                val sizeBytes = file.length()
                val sizeFormatted = formatFileSize(sizeBytes)
                val uri = try {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }

                docItems.add(
                    DocItem(
                        file = file,
                        name = file.name,
                        extension = ext,
                        sizeBytes = sizeBytes,
                        sizeFormatted = sizeFormatted,
                        lastModified = file.lastModified(),
                        category = category,
                        uri = uri
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        docItems.sortedByDescending { it.lastModified }
    }

    private fun queryDocsFromMediaStore(results: MutableList<File>) {
        try {
            val uri = MediaStore.Files.getContentUri("external")
            val projection = arrayOf(MediaStore.Files.FileColumns.DATA)
            val selection = "${MediaStore.Files.FileColumns.DATA} LIKE '%.pdf' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.doc' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.docx' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.xls' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.xlsx' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.ppt' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.pptx' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.txt' OR " +
                    "${MediaStore.Files.FileColumns.DATA} LIKE '%.epub' OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'application/%' OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE 'text/%'"

            context.contentResolver.query(uri, projection, selection, null, null)?.use { cursor ->
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)
                while (cursor.moveToNext()) {
                    if (dataCol != -1) {
                        val path = cursor.getString(dataCol)
                        if (!path.isNullOrBlank()) {
                            val file = File(path)
                            if (file.exists() && file.isFile && supportedExtensions.contains(file.extension.lowercase())) {
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
                // Evitar carpetas del sistema que causan fallos en Android 11+
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
            } else if (f.isFile) {
                val ext = f.extension.lowercase()
                if (supportedExtensions.contains(ext)) {
                    results.add(f)
                }
            }
        }
    }

    private fun resolveCategory(extension: String): DocCategory {
        for (cat in DocCategory.values()) {
            if (cat != DocCategory.ALL && cat.extensions.contains(extension)) {
                return cat
            }
        }
        return DocCategory.TXT_LOG
    }

    fun openDocument(doc: DocItem) {
        try {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", doc.file)
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(doc.extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Abrir con...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteDocument(doc: DocItem): Boolean = withContext(Dispatchers.IO) {
        try {
            if (doc.file.exists()) {
                doc.file.delete()
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
}
