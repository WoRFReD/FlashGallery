// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.model

import android.net.Uri
import java.io.File

enum class DocCategory(val title: String, val extensions: List<String>) {
    ALL("Todos", emptyList()),
    PDF("PDF", listOf("pdf")),
    TXT_LOG("TXT / Logs", listOf("txt", "log", "md", "csv", "ini", "conf")),
    CODE_DATA("XML / JSON", listOf("xml", "json", "html", "htm", "css", "js", "yaml", "yml")),
    OFFICE("Office / Docs", listOf("doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "rtf")),
    EPUB("EPUB / Libros", listOf("epub", "mobi", "fb2", "cbr", "cbz"))
}

data class DocItem(
    val file: File,
    val name: String,
    val extension: String,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val lastModified: Long,
    val category: DocCategory,
    val uri: Uri
)
