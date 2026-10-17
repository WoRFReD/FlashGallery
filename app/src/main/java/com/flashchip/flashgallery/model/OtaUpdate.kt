// Version 1.11 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.model

data class OtaReleaseInfo(
    val tagName: String,
    val title: String,
    val releaseNotes: String,
    val publishedAt: String,
    val downloadUrl: String,
    val apkFileName: String,
    val apkSizeBytes: Long,
    val isNewer: Boolean
)
