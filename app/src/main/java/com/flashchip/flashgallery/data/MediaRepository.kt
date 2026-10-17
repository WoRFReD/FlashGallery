// Version 1.01 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.flashchip.flashgallery.model.AppFolder
import com.flashchip.flashgallery.model.ExifMetadata
import com.flashchip.flashgallery.model.GalleryItem
import com.flashchip.flashgallery.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MediaRepository(private val context: Context) {

    private val appResolver = AppResolver(context)

    suspend fun loadAllMedia(): Pair<List<GalleryItem>, List<AppFolder>> {
        return loadMediaProgressive(null)
    }

    suspend fun loadMediaProgressive(
        onInitialBatch: ((List<GalleryItem>, List<AppFolder>) -> Unit)? = null
    ): Pair<List<GalleryItem>, List<AppFolder>> = withContext(Dispatchers.IO) {
        val rawList = mutableListOf<MediaItem>()

        val imageProjection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        // 1. Carga ultra rapida del primer lote (ultimas 60 fotos) para pintar la pantalla de inmediato (<30ms)
        if (onInitialBatch != null) {
            val fastList = mutableListOf<MediaItem>()
            queryContentUri(
                uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection = imageProjection,
                isVideo = false,
                target = fastList,
                limit = 60
            )
            if (fastList.isNotEmpty()) {
                val (fastTimeline, fastFolders) = buildTimelineAndFolders(fastList)
                onInitialBatch(fastTimeline, fastFolders)
            }
        }

        // 2. Cargar coleccion completa de fotos
        queryContentUri(
            uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection = imageProjection,
            isVideo = false,
            target = rawList
        )

        // 3. Cargar videos
        val videoProjection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DURATION
        )

        queryContentUri(
            uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection = videoProjection,
            isVideo = true,
            target = rawList
        )

        buildTimelineAndFolders(rawList)
    }

    private fun buildTimelineAndFolders(rawList: List<MediaItem>): Pair<List<GalleryItem>, List<AppFolder>> {
        val sortedList = rawList.sortedByDescending { it.dateTaken }
        val timelineItems = mutableListOf<GalleryItem>()
        val monthGroups = LinkedHashMap<String, MutableList<MediaItem>>()
        val monthTitles = mutableMapOf<String, String>()
        val monthYearMap = mutableMapOf<String, Pair<Int, Int>>()

        val monthNameFormat = SimpleDateFormat("MMMM yyyy", Locale("es", "ES"))
        val cal = Calendar.getInstance()

        for (item in sortedList) {
            cal.timeInMillis = item.dateTaken
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            val key = String.format(Locale.ROOT, "%04d-%02d", year, month)

            if (!monthGroups.containsKey(key)) {
                monthGroups[key] = mutableListOf()
                val rawTitle = monthNameFormat.format(cal.time)
                val capitalized = rawTitle.replaceFirstChar { it.uppercase(Locale("es", "ES")) }
                monthTitles[key] = capitalized
                monthYearMap[key] = Pair(year, month)
            }
            monthGroups[key]?.add(item)
        }

        for ((key, list) in monthGroups) {
            val title = monthTitles[key] ?: key
            val (year, month) = monthYearMap[key] ?: Pair(0, 0)
            val photoCount = list.count { !it.isVideo }
            val videoCount = list.count { it.isVideo }

            timelineItems.add(
                GalleryItem.MonthHeader(
                    key = key,
                    title = title,
                    year = year,
                    month = month,
                    photoCount = photoCount,
                    videoCount = videoCount,
                    mediaList = list
                )
            )
            for (media in list) {
                timelineItems.add(GalleryItem.Media(media))
            }
        }

        // Group by App
        val appFolderMap = LinkedHashMap<String, MutableList<MediaItem>>()
        for (item in sortedList) {
            val appKey = item.appName.ifBlank { "Otras fotos" }
            if (!appFolderMap.containsKey(appKey)) {
                appFolderMap[appKey] = mutableListOf()
            }
            appFolderMap[appKey]?.add(item)
        }

        val appFolders = appFolderMap.map { (appName, items) ->
            val first = items.first()
            AppFolder(
                id = appName,
                appName = appName,
                packageName = first.appPackage,
                folderName = first.bucketName,
                count = items.size,
                photoCount = items.count { !it.isVideo },
                videoCount = items.count { it.isVideo },
                coverItem = first,
                items = items
            )
        }.sortedByDescending { it.count }

        return Pair(timelineItems, appFolders)
    }

    private fun queryContentUri(
        uri: Uri,
        projection: Array<String>,
        isVideo: Boolean,
        target: MutableList<MediaItem>,
        limit: Int? = null
    ) {
        val sortOrder = if (limit != null && limit > 0) {
            "${MediaStore.MediaColumns.DATE_ADDED} DESC LIMIT $limit"
        } else {
            "${MediaStore.MediaColumns.DATE_ADDED} DESC"
        }
        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(uri, projection, null, null, sortOrder)
            if (cursor != null) {
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val dateTakenCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_TAKEN)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)
                val bucketCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val durationCol = if (isVideo) cursor.getColumnIndex(MediaStore.Video.Media.DURATION) else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(uri, id)
                    val path = if (dataCol != -1) cursor.getString(dataCol) ?: "" else ""
                    val name = cursor.getString(nameCol) ?: "media_$id"
                    val rawDateTaken = if (dateTakenCol != -1) cursor.getLong(dateTakenCol) else 0L
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val date = if (rawDateTaken > 0) rawDateTaken else dateAdded
                    val size = cursor.getLong(sizeCol)
                    val width = if (widthCol != -1) cursor.getInt(widthCol) else 0
                    val height = if (heightCol != -1) cursor.getInt(heightCol) else 0
                    val bucket = if (bucketCol != -1) cursor.getString(bucketCol) ?: "" else ""
                    val duration = if (durationCol != -1 && !cursor.isNull(durationCol)) cursor.getLong(durationCol) else 0L

                    val appMatch = appResolver.resolveApp(path, bucket)

                    target.add(
                        MediaItem(
                            id = id,
                            uri = contentUri,
                            path = path,
                            name = name,
                            dateTaken = date,
                            isVideo = isVideo,
                            durationMs = duration,
                            sizeBytes = size,
                            width = width,
                            height = height,
                            bucketName = bucket,
                            appName = appMatch.appName,
                            appPackage = appMatch.packageName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }
    }

    suspend fun getExifMetadata(item: MediaItem): ExifMetadata = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        val dateFormatted = dateFormat.format(item.dateTaken)
        val sizeMb = if (item.sizeBytes >= 1024 * 1024) {
            String.format(Locale.US, "%.2f MB", item.sizeBytes / (1024.0 * 1024.0))
        } else {
            String.format(Locale.US, "%.1f KB", item.sizeBytes / 1024.0)
        }

        val fileName = if (item.path.isNotBlank()) File(item.path).name else item.name
        val extension = fileName.substringAfterLast('.', "").uppercase()
        val fileType = if (item.isVideo) "Vídeo ($extension)" else "Fotografía ($extension)"

        var cameraMake: String? = null
        var cameraModel: String? = null
        var software: String? = null
        var fNumber: String? = null
        var exposureTime: String? = null
        var iso: String? = null
        var focalLength: String? = null
        var focalLength35mm: String? = null
        var flash: String? = null
        var whiteBalance: String? = null
        var gpsCoordinates: String? = null
        var latitude: Double? = null
        var longitude: Double? = null
        var videoDurationFormatted: String? = null
        var width = item.width
        var height = item.height

        if (item.isVideo) {
            try {
                val retriever = MediaMetadataRetriever()
                if (item.path.isNotBlank() && File(item.path).exists()) {
                    retriever.setDataSource(item.path)
                } else {
                    retriever.setDataSource(context, item.uri)
                }
                val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                if (durationMs > 0) {
                    val totalSec = durationMs / 1000
                    val min = totalSec / 60
                    val sec = totalSec % 60
                    videoDurationFormatted = String.format(Locale.US, "%02d:%02d (%d s)", min, sec, totalSec)
                }
                val vW = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
                val vH = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
                if (vW != null && vW > 0) width = vW
                if (vH != null && vH > 0) height = vH
                retriever.release()
            } catch (_: Exception) {}
        } else {
            try {
                val exifInterface: ExifInterface? = if (item.path.isNotBlank() && File(item.path).exists()) {
                    ExifInterface(item.path)
                } else {
                    context.contentResolver.openInputStream(item.uri)?.use { stream ->
                        ExifInterface(stream)
                    }
                }

                if (exifInterface != null) {
                    cameraMake = exifInterface.getAttribute(ExifInterface.TAG_MAKE)?.trim()
                    cameraModel = exifInterface.getAttribute(ExifInterface.TAG_MODEL)?.trim()
                    software = exifInterface.getAttribute(ExifInterface.TAG_SOFTWARE)?.trim()
                    val f = exifInterface.getAttribute(ExifInterface.TAG_F_NUMBER)
                    if (!f.isNullOrBlank()) fNumber = "f/$f"

                    val exp = exifInterface.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                    if (!exp.isNullOrBlank()) {
                        val expVal = exp.toDoubleOrNull()
                        exposureTime = if (expVal != null && expVal < 1.0 && expVal > 0) {
                            "1/${Math.round(1.0 / expVal)} s"
                        } else {
                            "${exp} s"
                        }
                    }

                    iso = exifInterface.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    val focal = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                    if (!focal.isNullOrBlank()) focalLength = "${focal} mm"

                    val focal35 = exifInterface.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)
                    if (!focal35.isNullOrBlank()) focalLength35mm = "${focal35} mm (eq. 35mm)"

                    val flashVal = exifInterface.getAttributeInt(ExifInterface.TAG_FLASH, -1)
                    if (flashVal != -1) {
                        flash = if ((flashVal and 1) != 0) "Flash disparado" else "Sin flash"
                    }

                    val wbVal = exifInterface.getAttributeInt(ExifInterface.TAG_WHITE_BALANCE, -1)
                    if (wbVal != -1) {
                        whiteBalance = if (wbVal == ExifInterface.WHITE_BALANCE_AUTO.toInt()) "Automático" else "Manual"
                    }

                    val latLong = exifInterface.latLong
                    if (latLong != null && latLong.size >= 2) {
                        latitude = latLong[0]
                        longitude = latLong[1]
                        gpsCoordinates = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
                    }

                    val exifW = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                    val exifH = exifInterface.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                    if (exifW > 0 && exifH > 0) {
                        width = exifW
                        height = exifH
                    }
                }
            } catch (_: Exception) {}
        }

        val resolution = if (width > 0 && height > 0) {
            val mp = String.format(Locale.US, "%.1f MP", (width.toLong() * height.toLong()) / 1_000_000.0)
            "${width} × ${height} ($mp)"
        } else {
            "Desconocida"
        }

        ExifMetadata(
            fileName = fileName,
            fileType = fileType,
            dateFormatted = dateFormatted,
            resolution = resolution,
            sizeFormatted = sizeMb,
            exactBytes = item.sizeBytes,
            cameraModel = cameraModel,
            cameraMake = cameraMake,
            software = software,
            fNumber = fNumber,
            exposureTime = exposureTime,
            iso = iso,
            focalLength = focalLength,
            focalLength35mm = focalLength35mm,
            flash = flash,
            whiteBalance = whiteBalance,
            gpsCoordinates = gpsCoordinates,
            latitude = latitude,
            longitude = longitude,
            isVideo = item.isVideo,
            videoDurationFormatted = videoDurationFormatted,
            path = item.path.ifBlank { item.uri.toString() }
        )
    }

    suspend fun deleteMedia(item: MediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val deletedRows = context.contentResolver.delete(item.uri, null, null)
            if (deletedRows > 0) return@withContext true
        } catch (_: Exception) {
        }

        try {
            if (item.path.isNotBlank()) {
                val file = File(item.path)
                if (file.exists()) return@withContext file.delete()
            }
        } catch (_: Exception) {
        }
        false
    }

    suspend fun getAvailableFolders(): List<File> = withContext(Dispatchers.IO) {
        val folders = mutableSetOf<File>()
        try {
            val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (dcim.exists()) {
                dcim.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.let { folders.addAll(it) }
                folders.add(dcim)
            }
            val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            if (pictures.exists()) {
                pictures.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.let { folders.addAll(it) }
                folders.add(pictures)
            }
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloads.exists()) folders.add(downloads)
            val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
            if (movies.exists()) {
                movies.listFiles()?.filter { it.isDirectory && !it.name.startsWith(".") }?.let { folders.addAll(it) }
                folders.add(movies)
            }
        } catch (_: Exception) {}
        folders.toList().sortedBy { it.name.lowercase() }
    }

    suspend fun moveMedia(items: List<MediaItem>, targetFolder: File): Int = withContext(Dispatchers.IO) {
        if (!targetFolder.exists()) {
            targetFolder.mkdirs()
        }
        var movedCount = 0
        val scannedOldPaths = mutableListOf<String>()
        val scannedNewPaths = mutableListOf<String>()

        for (item in items) {
            try {
                if (item.path.isBlank()) continue
                val sourceFile = File(item.path)
                if (!sourceFile.exists()) continue

                val targetFile = File(targetFolder, sourceFile.name)
                val finalTargetFile = if (targetFile.exists()) {
                    val nameWithoutExt = sourceFile.nameWithoutExtension
                    val ext = sourceFile.extension
                    File(targetFolder, "${nameWithoutExt}_${System.currentTimeMillis()}.$ext")
                } else {
                    targetFile
                }

                val success = sourceFile.renameTo(finalTargetFile)
                if (success) {
                    movedCount++
                    scannedOldPaths.add(sourceFile.absolutePath)
                    scannedNewPaths.add(finalTargetFile.absolutePath)
                    try {
                        context.contentResolver.delete(item.uri, null, null)
                    } catch (_: Exception) {}
                } else {
                    sourceFile.inputStream().use { input ->
                        finalTargetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    if (finalTargetFile.exists() && finalTargetFile.length() == sourceFile.length()) {
                        sourceFile.delete()
                        movedCount++
                        scannedOldPaths.add(sourceFile.absolutePath)
                        scannedNewPaths.add(finalTargetFile.absolutePath)
                        try {
                            context.contentResolver.delete(item.uri, null, null)
                        } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}
        }

        val allPaths = (scannedOldPaths + scannedNewPaths).toTypedArray()
        if (allPaths.isNotEmpty()) {
            MediaScannerConnection.scanFile(context, allPaths, null, null)
        }

        movedCount
    }
}
