// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.model

import android.graphics.drawable.Drawable
import android.net.Uri
import java.io.File

data class ApkItem(
    val file: File,
    val fileName: String,
    val appLabel: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val sizeBytes: Long,
    val sizeFormatted: String,
    val lastModified: Long,
    val icon: Drawable? = null,
    val uri: Uri
)
