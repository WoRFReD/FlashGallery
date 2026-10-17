// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetcher ultra-rapido que consulta los thumbnails pre-generados por hardware de Android
 * a traves de ContentResolver.loadThumbnail en lugar de decodificar imagenes originales
 * completas de 50 Megapixeles.
 */
class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val data: Uri,
    private val options: Options
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        // Solo interceptar peticiones de miniaturas (<= 512px); para el visor a pantalla completa dejar foto HD original
        val isThumbnailRequest = when (val w = options.size.width) {
            is coil.size.Dimension.Pixels -> w.px <= 512
            else -> false
        }
        if (!isThumbnailRequest) return@withContext null

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                // Solicitar directamente el thumbnail nativo de 256x256 al sistema Android
                val bitmap = context.contentResolver.loadThumbnail(data, Size(256, 256), null)
                return@withContext DrawableResult(
                    drawable = BitmapDrawable(context.resources, bitmap),
                    isSampled = true,
                    dataSource = DataSource.DISK
                )
            } catch (_: Exception) {
                // Si el thumbnail de hardware no estuviera disponible, Coil continuara con el fetcher estandar
            }
        }
        null
    }

    class Factory(private val context: Context) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            if (data.scheme == ContentResolver.SCHEME_CONTENT &&
                (data.authority == MediaStore.AUTHORITY || data.authority?.startsWith("media") == true)
            ) {
                return MediaStoreThumbnailFetcher(context, data, options)
            }
            return null
        }
    }
}
