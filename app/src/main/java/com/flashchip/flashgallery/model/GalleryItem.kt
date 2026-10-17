// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.model

import android.net.Uri

sealed class GalleryItem {
    data class MonthHeader(
        val key: String,
        val title: String,
        val year: Int,
        val month: Int,
        val photoCount: Int,
        val videoCount: Int,
        val mediaList: List<MediaItem>
    ) : GalleryItem()

    data class Media(
        val item: MediaItem
    ) : GalleryItem()
}

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val path: String,
    val name: String,
    val dateTaken: Long,
    val isVideo: Boolean,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val bucketName: String = "",
    val appName: String = "",
    val appPackage: String? = null
)

data class AppFolder(
    val id: String,
    val appName: String,
    val packageName: String?,
    val folderName: String,
    val count: Int,
    val photoCount: Int,
    val videoCount: Int,
    val coverItem: MediaItem,
    val items: List<MediaItem>
)

data class ExifMetadata(
    val fileName: String = "",
    val fileType: String = "",
    val dateFormatted: String,
    val resolution: String,
    val sizeFormatted: String,
    val exactBytes: Long = 0L,
    val cameraModel: String? = null,
    val cameraMake: String? = null,
    val software: String? = null,
    val fNumber: String? = null,
    val exposureTime: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val focalLength35mm: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val gpsCoordinates: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isVideo: Boolean = false,
    val videoDurationFormatted: String? = null,
    val path: String
)
