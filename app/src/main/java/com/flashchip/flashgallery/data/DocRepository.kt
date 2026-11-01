// Version 1.19 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
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
        "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf",
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

    fun getMimeType(extension: String): String {
        return when (extension.lowercase()) {
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "odt" -> "application/vnd.oasis.opendocument.text"
            "ods" -> "application/vnd.oasis.opendocument.spreadsheet"
            "odp" -> "application/vnd.oasis.opendocument.presentation"
            "rtf" -> "application/rtf"
            "csv" -> "text/csv"
            "txt" -> "text/plain"
            "log" -> "text/plain"
            "xml" -> "text/xml"
            "json" -> "application/json"
            "html", "htm" -> "text/html"
            "epub" -> "application/epub+zip"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "*/*"
        }
    }

    fun openDocument(doc: DocItem) {
        try {
            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", doc.file)
            val mimeType = getMimeType(doc.extension)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, "Abrir con...")
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "No hay ninguna aplicación instalada para abrir archivos .${doc.extension.uppercase()}",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al abrir documento: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
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

    fun createDocItemFromUri(context: Context, uri: Uri, mimeTypeHint: String?): DocItem? {
        return try {
            var displayName = ""
            var sizeBytes: Long = 0L

            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                if (file.exists()) {
                    val ext = file.extension.lowercase()
                    val category = resolveCategory(ext)
                    return DocItem(
                        file = file,
                        name = file.name,
                        extension = ext,
                        sizeBytes = file.length(),
                        sizeFormatted = formatFileSize(file.length()),
                        lastModified = file.lastModified(),
                        category = category,
                        uri = uri
                    )
                }
            }

            // Consultar metadata mediante OpenableColumns (WhatsApp, Gmail, Exploradores)
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameCol = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameCol != -1) displayName = it.getString(nameCol) ?: ""
                        val sizeCol = it.getColumnIndex(OpenableColumns.SIZE)
                        if (sizeCol != -1 && !it.isNull(sizeCol)) sizeBytes = it.getLong(sizeCol)
                    }
                }
            } catch (_: Exception) {}

            if (displayName.isBlank()) {
                displayName = uri.lastPathSegment ?: "documento.pdf"
            }

            var ext = displayName.substringAfterLast('.', "").lowercase()
            if (ext.isBlank() || ext == displayName.lowercase()) {
                val resolvedMime = mimeTypeHint ?: context.contentResolver.getType(uri) ?: ""
                ext = when {
                    resolvedMime.contains("pdf") -> "pdf"
                    resolvedMime.contains("word") || resolvedMime.contains("officedocument.word") -> "docx"
                    resolvedMime.contains("excel") || resolvedMime.contains("spreadsheet") -> "xlsx"
                    resolvedMime.contains("powerpoint") || resolvedMime.contains("presentation") -> "pptx"
                    resolvedMime.contains("text/plain") -> "txt"
                    resolvedMime.contains("text/csv") -> "csv"
                    else -> "pdf"
                }
                if (!displayName.contains(".")) {
                    displayName = "$displayName.$ext"
                }
            }

            // Localizar archivo físico directo si ya está en almacenamiento accesible
            var resolvedFile: File? = null
            try {
                val path = uri.path
                if (path != null) {
                    val f = File(path)
                    if (f.exists() && f.canRead()) resolvedFile = f
                }
            } catch (_: Exception) {}

            if (resolvedFile == null) {
                try {
                    val proj = arrayOf(MediaStore.MediaColumns.DATA)
                    context.contentResolver.query(uri, proj, null, null, null)?.use { c ->
                        if (c.moveToFirst()) {
                            val dataPath = c.getString(0)
                            if (!dataPath.isNullOrBlank()) {
                                val f = File(dataPath)
                                if (f.exists() && f.canRead()) resolvedFile = f
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Si el archivo no es directamente accesible por ruta (ej. stream privado de WhatsApp o Gmail),
            // lo clonamos en cacheDir/opened_docs para que PdfRenderer / parsers de Office puedan leerlo y hacer seek
            var localFile = resolvedFile
            if (localFile?.exists() != true) {
                val cacheFolder = File(context.cacheDir, "opened_docs").apply { mkdirs() }
                val safeName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val tempFile = File(cacheFolder, safeName)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (tempFile.exists()) {
                    localFile = tempFile
                    sizeBytes = tempFile.length()
                }
            }

            val finalFile = localFile ?: File(uri.path ?: displayName)
            val category = resolveCategory(ext)

            DocItem(
                file = finalFile,
                name = displayName,
                extension = ext,
                sizeBytes = sizeBytes,
                sizeFormatted = formatFileSize(sizeBytes),
                lastModified = if (finalFile.exists()) finalFile.lastModified() else System.currentTimeMillis(),
                category = category,
                uri = uri
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
