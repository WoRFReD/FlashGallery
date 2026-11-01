// Version 1.20 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.flashchip.flashgallery.data.MediaStoreThumbnailFetcher
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class FlashGalleryApp : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        try {
            PDFBoxResourceLoader.init(applicationContext)
        } catch (_: Exception) {}
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.35)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("gallery_thumbnails"))
                    .maxSizeBytes(300L * 1024 * 1024)
                    .build()
            }
            .components {
                add(MediaStoreThumbnailFetcher.Factory(this@FlashGalleryApp))
                add(VideoFrameDecoder.Factory())
            }
            .allowRgb565(true)
            .crossfade(false)
            .respectCacheHeaders(false)
            .build()
    }
}
