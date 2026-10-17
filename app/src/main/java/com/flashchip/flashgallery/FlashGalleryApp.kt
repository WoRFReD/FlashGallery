// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.flashchip.flashgallery.data.MediaStoreThumbnailFetcher

/**
 * Clase Application personalizada para configurar el ImageLoader de Coil
 * optimizado al maximo para rendimiento de galeria de fotos:
 * - 35% de memoria RAM para cache instantanea de miniaturas
 * - Cache en disco de 300MB
 * - Decodificacion RGB_565 (2x mas rapida y la mitad de RAM)
 * - Thumbnails nativos de Android por hardware
 * - Cero animaciones de retardo artificial en el scroll
 */
class FlashGalleryApp : Application(), ImageLoaderFactory {

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
