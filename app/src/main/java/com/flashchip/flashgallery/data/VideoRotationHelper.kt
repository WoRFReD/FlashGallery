// Version 1.26 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object VideoRotationHelper {

    /**
     * Aplica una rotación a un vídeo MP4 modificando únicamente los metadatos de orientación
     * del contenedor (Track Header / tkhd) mediante remuxing con MediaExtractor y MediaMuxer.
     *
     * NO recodifica el flujo de audio ni vídeo (cero pérdida de calidad, procesamiento a velocidad de disco).
     *
     * @param context Contexto de Android para resolver URIs de contenido.
     * @param sourceUri URI del vídeo original.
     * @param sourceName Nombre del archivo original.
     * @param extraRotationDegrees Grados adicionales a rotar (+90, +180, +270).
     * @return El archivo File del nuevo vídeo rotado, o null si ocurrió un error.
     */
    suspend fun rotateVideoWithoutReencoding(
        context: Context,
        sourceUri: Uri,
        sourceName: String,
        extraRotationDegrees: Int
    ): File? = withContext(Dispatchers.IO) {
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null
        var targetFile: File? = null

        try {
            // 1. Obtener la rotación actual del vídeo con MediaMetadataRetriever
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, sourceUri)
            val currentRotationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
            val currentRotation = currentRotationStr?.toIntOrNull() ?: 0
            retriever.release()

            // 2. Calcular nueva orientación absoluta (múltiplo de 90 entre 0 y 270)
            var targetRotation = (currentRotation + extraRotationDegrees) % 360
            if (targetRotation < 0) targetRotation += 360

            // 3. Inicializar MediaExtractor
            extractor = MediaExtractor().apply {
                setDataSource(context, sourceUri, null)
            }
            val trackCount = extractor.trackCount
            if (trackCount == 0) return@withContext null

            // 4. Preparar carpeta de destino en Movies/FlashGallery
            val moviesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "FlashGallery"
            )
            if (!moviesDir.exists()) {
                moviesDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val rawName = sourceName.substringBeforeLast(".")
            val destFile = File(moviesDir, "${rawName}_rot_${timeStamp}.mp4")
            targetFile = destFile

            // 5. Configurar MediaMuxer con la orientación deseada
            muxer = MediaMuxer(destFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4).apply {
                setOrientationHint(targetRotation)
            }

            val indexMap = HashMap<Int, Int>(trackCount)
            var maxBufferSize = 1024 * 1024 // 1 MB base para fotogramas I-frame grandes

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""

                // Filtrar solo pistas de vídeo y audio para garantizar compatibilidad con MediaMuxer
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val inputBufSize = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (inputBufSize > maxBufferSize) {
                            maxBufferSize = inputBufSize
                        }
                    }
                    val muxerTrackIndex = muxer.addTrack(format)
                    indexMap[i] = muxerTrackIndex
                    extractor.selectTrack(i)
                }
            }

            if (indexMap.isEmpty()) {
                muxer.release()
                extractor.release()
                return@withContext null
            }

            // Margen de seguridad para el buffer
            maxBufferSize += 256 * 1024
            val buffer = ByteBuffer.allocateDirect(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            muxer.start()

            // 6. Copia directa de muestras comprimidas sin transcodificar
            while (true) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }
                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags

                val muxerTrack = indexMap[trackIndex]
                if (muxerTrack != null) {
                    muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                }

                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            muxer = null

            extractor.release()
            extractor = null

            // 7. Notificar a MediaScanner para que la nueva versión rotada aparezca en la galería al instante
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf("video/mp4"),
                null
            )

            destFile
        } catch (e: Exception) {
            e.printStackTrace()
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor?.release() } catch (_: Exception) {}
            targetFile?.let { if (it.exists()) it.delete() }
            null
        }
    }
}
