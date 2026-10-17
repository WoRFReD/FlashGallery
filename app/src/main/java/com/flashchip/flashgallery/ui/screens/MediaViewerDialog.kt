// Version 1.17 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.screens

import android.content.Intent
import android.widget.Toast
import kotlin.OptIn
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flashchip.flashgallery.data.MediaRepository
import com.flashchip.flashgallery.model.MediaItem
import com.flashchip.flashgallery.model.ExifMetadata
import com.flashchip.flashgallery.data.OcrScanResult
import com.flashchip.flashgallery.data.OcrScanner
import com.flashchip.flashgallery.ui.components.ExifDetailContent
import com.flashchip.flashgallery.ui.components.OcrSelectionSheet
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.FlashRed
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaViewerDialog(
    mediaList: List<MediaItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onShowExif: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit
) {
    if (mediaList.isEmpty()) {
        onDismiss()
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var showControls by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Estados para la hoja de información EXIF detallada integrada
    var showExifSheet by remember { mutableStateOf(false) }
    var activeExifData by remember { mutableStateOf<ExifMetadata?>(null) }

    // Estados para el reconocimiento de texto OCR (Live Text)
    var isOcrScanning by remember { mutableStateOf(false) }
    var ocrScanResult by remember { mutableStateOf<OcrScanResult?>(null) }
    var selectedOcrText by remember { mutableStateOf<String?>(null) }
    var showOcrSheet by remember { mutableStateOf(false) }

    val safeInitialIndex = initialIndex.coerceIn(0, mediaList.size - 1)
    val pagerState = rememberPagerState(initialPage = safeInitialIndex) { mediaList.size }

    // Nivel de zoom de la foto actual (1f = tamaño original, >1f = ampliado)
    var currentPhotoScale by remember { mutableFloatStateOf(1f) }

    // Al deslizar a otra página se restablece el zoom y se cierran hojas abiertas
    LaunchedEffect(pagerState.currentPage) {
        currentPhotoScale = 1f
        showExifSheet = false
        showOcrSheet = false
        ocrScanResult = null
        selectedOcrText = null
    }

    val currentItem = mediaList.getOrNull(pagerState.currentPage) ?: mediaList.first()

    val formattedDate = remember(currentItem.dateTaken) {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        sdf.format(currentItem.dateTaken)
    }

    fun triggerOcr(
        item: MediaItem,
        touchOffset: Offset?,
        boxSize: IntSize? = null,
        currentScale: Float = 1f,
        currentOffset: Offset = Offset.Zero
    ) {
        if (item.isVideo) return
        isOcrScanning = true
        coroutineScope.launch {
            try {
                val result = OcrScanner.recognizeText(context, item.uri)
                ocrScanResult = result

                var selectedText: String? = null
                if (touchOffset != null && boxSize != null && boxSize.width > 0 && boxSize.height > 0 && result.imageWidth > 0 && result.imageHeight > 0) {
                    val centerX = boxSize.width / 2f
                    val centerY = boxSize.height / 2f
                    val effectiveScale = if (currentScale > 0.1f) currentScale else 1f
                    val unscaledX = (touchOffset.x - currentOffset.x - centerX) / effectiveScale + centerX
                    val unscaledY = (touchOffset.y - currentOffset.y - centerY) / effectiveScale + centerY

                    val scaleFactor = minOf(
                        boxSize.width.toFloat() / result.imageWidth.toFloat(),
                        boxSize.height.toFloat() / result.imageHeight.toFloat()
                    )
                    val renderedW = result.imageWidth * scaleFactor
                    val renderedH = result.imageHeight * scaleFactor
                    val left = (boxSize.width - renderedW) / 2f
                    val top = (boxSize.height - renderedH) / 2f

                    val imgX = (unscaledX - left) / scaleFactor
                    val imgY = (unscaledY - top) / scaleFactor
                    selectedText = OcrScanner.findElementAt(result, imgX, imgY)
                }

                if (result.blocks.isEmpty()) {
                    Toast.makeText(context, "No se detectó ningún texto en esta foto", Toast.LENGTH_SHORT).show()
                } else {
                    selectedOcrText = selectedText ?: result.blocks.firstOrNull()?.text
                    showOcrSheet = true
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error al reconocer texto: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } finally {
                isOcrScanning = false
            }
        }
    }

    Dialog(
        onDismissRequest = {
            if (showOcrSheet) {
                showOcrSheet = false
            } else if (showExifSheet) {
                showExifSheet = false
            } else {
                onDismiss()
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        // Manejo del botón atrás de Android dentro del Dialog
        BackHandler(enabled = showOcrSheet) {
            showOcrSheet = false
        }
        BackHandler(enabled = showExifSheet) {
            showExifSheet = false
        }
        BackHandler(enabled = !showExifSheet && !showOcrSheet) {
            onDismiss()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // FlingBehavior de alta sensibilidad: un ligero deslizamiento del 15% ya avanza de foto
            val flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState,
                snapPositionalThreshold = 0.15f
            )

            // Horizontal Pager: Swipe hacia la izquierda y derecha para cambiar de foto/vídeo
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                flingBehavior = flingBehavior,
                key = { index -> mediaList.getOrNull(index)?.id ?: index },
                userScrollEnabled = currentPhotoScale == 1f
            ) { page ->
                val item = mediaList.getOrNull(page)
                if (item != null) {
                    if (item.isVideo) {
                        VideoPlayerPage(
                            item = item,
                            isCurrentPage = page == pagerState.currentPage,
                            onTap = { showControls = !showControls }
                        )
                    } else {
                        PhotoViewerPage(
                            item = item,
                            onTap = { showControls = !showControls },
                            onLongPress = { touchOffset, boxSize, scale, offset ->
                                if (page == pagerState.currentPage) {
                                    triggerOcr(item, touchOffset, boxSize, scale, offset)
                                }
                            },
                            onScaleChanged = { scale ->
                                if (page == pagerState.currentPage) {
                                    currentPhotoScale = scale
                                }
                            }
                        )
                    }
                }
            }

            // Barra superior fija con información, contador y acciones
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${mediaList.size}",
                                color = FlashCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentItem.name,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Text(
                            text = formattedDate,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Botón Compartir
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (currentItem.isVideo) "video/*" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir multimedia"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = FlashCyan
                        )
                    }

                    // Botón Reconocer Texto OCR (Live Text)
                    if (!currentItem.isVideo) {
                        IconButton(
                            onClick = {
                                triggerOcr(currentItem, null, null)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = "Escanear texto en la foto",
                                tint = FlashCyan
                            )
                        }
                    }

                    // Botón Información EXIF Detallada
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val repo = MediaRepository(context)
                                activeExifData = repo.getExifMetadata(currentItem)
                                showExifSheet = true
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información EXIF",
                            tint = FlashCyan
                        )
                    }

                    // Botón Eliminar
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = FlashRed
                        )
                    }
                }
            }

            // Hoja emergente de información EXIF detallada (renderizada en la misma ventana del visor)
            if (showExifSheet && activeExifData != null) {
                // Fondo oscurecido con clic para cerrar
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                        .clickable { showExifSheet = false }
                )

                AnimatedVisibility(
                    visible = showExifSheet,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.80f)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        color = SurfaceDark,
                        tonalElevation = 12.dp
                    ) {
                        ExifDetailContent(
                            exif = activeExifData!!,
                            onDismiss = { showExifSheet = false }
                        )
                    }
                }
            }

            // Diálogo de confirmación de eliminación
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = {
                        Text(
                            text = if (currentItem.isVideo) "¿Eliminar vídeo?" else "¿Eliminar foto?",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = "Se eliminará '${currentItem.name}' de tu dispositivo. Esta acción no se puede deshacer.",
                            color = TextSecondary
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showDeleteConfirm = false
                                onDelete(currentItem)
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = FlashRed)
                        ) {
                            Text(text = "Eliminar", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showDeleteConfirm = false },
                            colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                        ) {
                            Text(text = "Cancelar")
                        }
                    }
                )
            }

            // Indicador de escaneo OCR
            if (isOcrScanning) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark.copy(alpha = 0.92f))
                        .border(1.dp, FlashCyan.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = FlashCyan,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Detectando caracteres en la foto...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Hoja modal interactiva con los textos detectados y claves copiables
            if (showOcrSheet && ocrScanResult != null) {
                OcrSelectionSheet(
                    scanResult = ocrScanResult!!,
                    initialSelectedText = selectedOcrText,
                    onDismiss = { showOcrSheet = false }
                )
            }
        }
    }
}

/**
 * Página para visualizar una foto:
 * - A escala 1x: El swipe lateral con 1 dedo pasa de foto de inmediato sin interferencias (HorizontalPager).
 * - Con 2 dedos: Permite zoom fluido con pellizco (pinch-to-zoom) y desplazamiento (pan) en tiempo real.
 * - Con doble toque: Alterna suavemente entre zoom centrado en el punto pulsado (2.5x) y 1x.
 * - Con zoom >1x: Permite desplazarse libremente con 1 dedo o 2 dedos dentro de la foto ampliada.
 */
@Composable
private fun PhotoViewerPage(
    item: MediaItem,
    onTap: () -> Unit,
    onLongPress: (touchOffset: Offset, boxSize: IntSize, scale: Float, offset: Offset) -> Unit,
    onScaleChanged: (Float) -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(item.id) {
        scale = 1f
        offset = Offset.Zero
        onScaleChanged(1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates -> boxSize = coordinates.size }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = { tapOffset ->
                        val newScale = if (scale > 1.05f) 1f else 2.5f
                        scale = newScale
                        if (newScale <= 1.05f) {
                            offset = Offset.Zero
                        } else if (boxSize.width > 0 && boxSize.height > 0) {
                            val center = Offset(boxSize.width / 2f, boxSize.height / 2f)
                            val targetOffset = (center - tapOffset) * (newScale - 1f)
                            val maxX = maxOf(0f, (boxSize.width * (newScale - 1f)) / 2f)
                            val maxY = maxOf(0f, (boxSize.height * (newScale - 1f)) / 2f)
                            offset = Offset(
                                targetOffset.x.coerceIn(-maxX, maxX),
                                targetOffset.y.coerceIn(-maxY, maxY)
                            )
                        }
                        onScaleChanged(newScale)
                    },
                    onLongPress = { touchOffset ->
                        onLongPress(touchOffset, boxSize, scale, offset)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (canceled) break

                        val pressedPointers = event.changes.filter { it.pressed }
                        val pointerCount = pressedPointers.size

                        if (pointerCount >= 2) {
                            // Dos o más dedos: Zoom por pellizco (pinch-to-zoom) fluido y proporcional
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()

                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                scale = newScale

                                if (newScale > 1f && boxSize.width > 0 && boxSize.height > 0) {
                                    val maxX = maxOf(0f, (boxSize.width * (newScale - 1f)) / 2f)
                                    val maxY = maxOf(0f, (boxSize.height * (newScale - 1f)) / 2f)
                                    val newX = (offset.x + panChange.x).coerceIn(-maxX, maxX)
                                    val newY = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                    offset = Offset(newX, newY)
                                } else {
                                    offset = Offset.Zero
                                }

                                onScaleChanged(newScale)
                                event.changes.forEach { it.consume() }
                            }
                        } else if (pointerCount == 1 && scale > 1.05f) {
                            // Un solo dedo mientras la imagen está ampliada: Pan / Desplazamiento
                            val panChange = event.calculatePan()
                            if (panChange != Offset.Zero && boxSize.width > 0 && boxSize.height > 0) {
                                val maxX = maxOf(0f, (boxSize.width * (scale - 1f)) / 2f)
                                val maxY = maxOf(0f, (boxSize.height * (scale - 1f)) / 2f)
                                val newX = (offset.x + panChange.x).coerceIn(-maxX, maxX)
                                val newY = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                offset = Offset(newX, newY)

                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
        )
    }
}

/**
 * Página para reproducir un vídeo con ExoPlayer sin atrapar los gestos de deslizamiento lateral
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerPage(
    item: MediaItem,
    isCurrentPage: Boolean,
    onTap: () -> Unit
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }

    val exoPlayer = remember(item.uri) {
        ExoPlayer.Builder(context).build().apply {
            val exoItem = ExoMediaItem.fromUri(item.uri)
            setMediaItem(exoItem)
            prepare()
            playWhenReady = false
        }
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            exoPlayer.pause()
            isPlaying = false
        }
    }

    DisposableEffect(item.uri) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (exoPlayer.isPlaying) {
                            exoPlayer.pause()
                            isPlaying = false
                        } else {
                            exoPlayer.play()
                            isPlaying = true
                        }
                        onTap()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false // Evita que PlayerView capture los gestos táctiles horizontales
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Botón Play/Pause central flotante
        AnimatedVisibility(
            visible = !isPlaying,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .border(2.dp, FlashCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Reproducir vídeo",
                    tint = FlashCyan,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
    }
}
