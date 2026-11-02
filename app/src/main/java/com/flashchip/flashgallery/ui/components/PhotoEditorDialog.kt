// Version 1.24 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoSizeSelectActual
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.flashchip.flashgallery.model.MediaItem
import com.flashchip.flashgallery.ui.theme.BgDark
import com.flashchip.flashgallery.ui.theme.BorderDark
import com.flashchip.flashgallery.ui.theme.FlashAmber
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.FlashGreen
import com.flashchip.flashgallery.ui.theme.FlashRed
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.SurfaceDarkVariant
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * Tipos de herramientas de marcado
 */
enum class MarkupTool {
    PEN,       // Pincel libre
    ARROW,     // Flecha de señalización
    RECTANGLE, // Recuadro / caja
    MOSAIC,    // Censura / Pixelado
    REDACT     // Tachador confidencial oscuro
}

/**
 * Pestañas principales de edición
 */
enum class EditorTab {
    MARKUP,   // Anotaciones y dibujo
    ROTATE,   // Rotación y giro
    FILTERS,  // Modo Documento / B&N / Realce
    RESIZE    // Calidad y compresión
}

/**
 * Modos de filtro de imagen
 */
enum class ImageFilterMode(val label: String) {
    NONE("Original"),
    DOC_BW("Documento"),     // Alto contraste B/N para tickets y textos
    GRAYSCALE("B/N Suave"),  // Blanco y negro fotográfico
    ENHANCE("Realce")        // Mayor saturación y contraste
}

/**
 * Acciones de dibujo normalizadas (coordenadas entre 0.0 y 1.0)
 */
sealed class DrawAction {
    data class PathStroke(
        val points: List<Offset>,
        val color: Color,
        val strokeWidthNorm: Float
    ) : DrawAction()

    data class Arrow(
        val start: Offset,
        val end: Offset,
        val color: Color,
        val strokeWidthNorm: Float
    ) : DrawAction()

    data class Rectangle(
        val start: Offset,
        val end: Offset,
        val color: Color,
        val strokeWidthNorm: Float
    ) : DrawAction()

    data class Mosaic(
        val points: List<Offset>,
        val radiusNorm: Float
    ) : DrawAction()

    data class Redact(
        val points: List<Offset>,
        val strokeWidthNorm: Float
    ) : DrawAction()
}

@Composable
fun PhotoEditorDialog(
    item: MediaItem,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Carga segura del bitmap original con orientación EXIF
    val baseBitmap by produceState<Bitmap?>(initialValue = null, key1 = item.uri) {
        value = withContext(Dispatchers.IO) {
            loadSafeBitmap(context, item.uri)
        }
    }

    // Estados de edición
    var activeTab by remember { mutableStateOf(EditorTab.MARKUP) }
    var activeTool by remember { mutableStateOf(MarkupTool.ARROW) }
    var rotationDegrees by remember { mutableIntStateOf(0) }
    var isFlippedHorizontal by remember { mutableStateOf(false) }
    var activeFilter by remember { mutableStateOf(ImageFilterMode.NONE) }
    var exportResolutionMaxDim by remember { mutableIntStateOf(0) } // 0 = original

    // Colores y grosor
    val palette = listOf(FlashCyan, FlashAmber, FlashGreen, FlashRed, Color.White, Color.Black)
    var selectedColor by remember { mutableStateOf(FlashCyan) }
    var strokeThickness by remember { mutableFloatStateOf(8f) } // en dp aproximado

    // Historial de acciones (para Deshacer / Undo)
    val actions = remember { mutableStateListOf<DrawAction>() }

    // Trazo en curso mientras se arrastra el dedo
    var currentDragPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var currentDragStart by remember { mutableStateOf<Offset?>(null) }
    var currentDragEnd by remember { mutableStateOf<Offset?>(null) }

    // Estado de guardado
    var isSaving by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BgDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TopBar del Editor
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceDarkVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancelar",
                                tint = FlashRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "Editor FlashChip",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = item.name,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón Deshacer (Undo)
                        IconButton(
                            onClick = {
                                if (actions.isNotEmpty()) {
                                    actions.removeAt(actions.size - 1)
                                }
                            },
                            enabled = actions.isNotEmpty(),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Deshacer",
                                tint = if (actions.isNotEmpty()) FlashAmber else TextSecondary.copy(alpha = 0.3f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Botón Guardar Copia
                        IconButton(
                            onClick = {
                                if (!isSaving && baseBitmap != null) {
                                    isSaving = true
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val savedFile = saveEditedImage(
                                            context = context,
                                            sourceItem = item,
                                            baseBitmap = baseBitmap!!,
                                            actions = actions.toList(),
                                            rotation = rotationDegrees,
                                            flipped = isFlippedHorizontal,
                                            filter = activeFilter,
                                            maxDim = exportResolutionMaxDim
                                        )
                                        withContext(Dispatchers.Main) {
                                            isSaving = false
                                            if (savedFile != null) {
                                                Toast.makeText(
                                                    context,
                                                    "Copia guardada con éxito:\n${savedFile.name}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onSaved()
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Error al guardar imagen",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSaving && baseBitmap != null,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FlashCyan.copy(alpha = 0.15f))
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    color = FlashCyan,
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Guardar",
                                    tint = FlashCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Lienzo interactivo central
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF090D14)),
                    contentAlignment = Alignment.Center
                ) {
                    if (baseBitmap != null) {
                        val currentBmp = baseBitmap!!

                        // Canvas de dibujo y previsualización
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(activeTab, activeTool, selectedColor, strokeThickness) {
                                    if (activeTab != EditorTab.MARKUP) return@pointerInput

                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            // Solo actuar si está dentro del área visible de la imagen
                                            val bounds = calculateImageBounds(size.toSize(), currentBmp, rotationDegrees)
                                            if (bounds.contains(offset)) {
                                                val norm = toNorm(offset, bounds)
                                                when (activeTool) {
                                                    MarkupTool.PEN, MarkupTool.MOSAIC, MarkupTool.REDACT -> {
                                                        currentDragPoints = listOf(norm)
                                                    }
                                                    MarkupTool.ARROW, MarkupTool.RECTANGLE -> {
                                                        currentDragStart = norm
                                                        currentDragEnd = norm
                                                    }
                                                }
                                            }
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            val bounds = calculateImageBounds(size.toSize(), currentBmp, rotationDegrees)
                                            val norm = toNorm(change.position, bounds)
                                            when (activeTool) {
                                                MarkupTool.PEN, MarkupTool.MOSAIC, MarkupTool.REDACT -> {
                                                    currentDragPoints = currentDragPoints + norm
                                                }
                                                MarkupTool.ARROW, MarkupTool.RECTANGLE -> {
                                                    currentDragEnd = norm
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val normThickness = strokeThickness / 1000f
                                            when (activeTool) {
                                                MarkupTool.PEN -> {
                                                    if (currentDragPoints.size > 1) {
                                                        actions.add(DrawAction.PathStroke(currentDragPoints, selectedColor, normThickness))
                                                    }
                                                }
                                                MarkupTool.MOSAIC -> {
                                                    if (currentDragPoints.isNotEmpty()) {
                                                        actions.add(DrawAction.Mosaic(currentDragPoints, 0.05f))
                                                    }
                                                }
                                                MarkupTool.REDACT -> {
                                                    if (currentDragPoints.isNotEmpty()) {
                                                        actions.add(DrawAction.Redact(currentDragPoints, normThickness * 3f))
                                                    }
                                                }
                                                MarkupTool.ARROW -> {
                                                    if (currentDragStart != null && currentDragEnd != null && currentDragStart != currentDragEnd) {
                                                        actions.add(DrawAction.Arrow(currentDragStart!!, currentDragEnd!!, selectedColor, normThickness))
                                                    }
                                                }
                                                MarkupTool.RECTANGLE -> {
                                                    if (currentDragStart != null && currentDragEnd != null && currentDragStart != currentDragEnd) {
                                                        actions.add(DrawAction.Rectangle(currentDragStart!!, currentDragEnd!!, selectedColor, normThickness))
                                                    }
                                                }
                                            }
                                            currentDragPoints = emptyList()
                                            currentDragStart = null
                                            currentDragEnd = null
                                        },
                                        onDragCancel = {
                                            currentDragPoints = emptyList()
                                            currentDragStart = null
                                            currentDragEnd = null
                                        }
                                    )
                                }
                        ) {
                            val bounds = calculateImageBounds(size, currentBmp, rotationDegrees)

                            // 1. Dibujar imagen de fondo con rotación, filtro y espejo
                            val matrix = Matrix().apply {
                                val scaleX = bounds.width / currentBmp.width * (if (isFlippedHorizontal) -1f else 1f)
                                val scaleY = bounds.height / currentBmp.height
                                postScale(scaleX, scaleY)
                                postRotate(rotationDegrees.toFloat(), bounds.width / 2f, bounds.height / 2f)
                                postTranslate(bounds.left + if (isFlippedHorizontal) bounds.width else 0f, bounds.top)
                            }

                            // Previsualizar bitmap con drawContext nativo
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.save()
                                val paint = Paint().apply {
                                    isAntiAlias = true
                                    isFilterBitmap = true
                                    when (activeFilter) {
                                        ImageFilterMode.DOC_BW -> {
                                            val cm = ColorMatrix(floatArrayOf(
                                                2.2f, 0f, 0f, 0f, -140f,
                                                0f, 2.2f, 0f, 0f, -140f,
                                                0f, 0f, 2.2f, 0f, -140f,
                                                0f, 0f, 0f, 1f, 0f
                                            ))
                                            val gray = ColorMatrix().apply { setSaturation(0f) }
                                            cm.preConcat(gray)
                                            colorFilter = ColorMatrixColorFilter(cm)
                                        }
                                        ImageFilterMode.GRAYSCALE -> {
                                            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
                                        }
                                        ImageFilterMode.ENHANCE -> {
                                            val cm = ColorMatrix(floatArrayOf(
                                                1.25f, 0f, 0f, 0f, -15f,
                                                0f, 1.25f, 0f, 0f, -15f,
                                                0f, 0f, 1.25f, 0f, -15f,
                                                0f, 0f, 0f, 1f, 0f
                                            ))
                                            cm.postConcat(ColorMatrix().apply { setSaturation(1.3f) })
                                            colorFilter = ColorMatrixColorFilter(cm)
                                        }
                                        ImageFilterMode.NONE -> {}
                                    }
                                }
                                canvas.nativeCanvas.drawBitmap(currentBmp, matrix, paint)
                                canvas.nativeCanvas.restore()
                            }

                            // 2. Dibujar acciones guardadas en el historial
                            for (action in actions) {
                                renderAction(action, bounds)
                            }

                            // 3. Dibujar acción en curso (arrastre activo)
                            if (currentDragPoints.isNotEmpty()) {
                                when (activeTool) {
                                    MarkupTool.PEN -> {
                                        val normThickness = strokeThickness / 1000f
                                        renderAction(DrawAction.PathStroke(currentDragPoints, selectedColor, normThickness), bounds)
                                    }
                                    MarkupTool.MOSAIC -> {
                                        renderAction(DrawAction.Mosaic(currentDragPoints, 0.05f), bounds)
                                    }
                                    MarkupTool.REDACT -> {
                                        val normThickness = strokeThickness / 1000f
                                        renderAction(DrawAction.Redact(currentDragPoints, normThickness * 3f), bounds)
                                    }
                                    else -> {}
                                }
                            } else if (currentDragStart != null && currentDragEnd != null) {
                                val normThickness = strokeThickness / 1000f
                                when (activeTool) {
                                    MarkupTool.ARROW -> {
                                        renderAction(DrawAction.Arrow(currentDragStart!!, currentDragEnd!!, selectedColor, normThickness), bounds)
                                    }
                                    MarkupTool.RECTANGLE -> {
                                        renderAction(DrawAction.Rectangle(currentDragStart!!, currentDragEnd!!, selectedColor, normThickness), bounds)
                                    }
                                    else -> {}
                                }
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = FlashCyan)
                    }
                }

                // Panel Inferior de Herramientas (Estilo Cyberpunk)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark)
                        .padding(12.dp)
                ) {
                    // Contenido contextual según la pestaña activa
                    when (activeTab) {
                        EditorTab.MARKUP -> {
                            // Sub-barra de herramientas: Pincel, Flecha, Caja, Censura, Tachador
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ToolIconBadge(
                                    icon = Icons.Default.TrendingFlat,
                                    label = "Flecha",
                                    selected = activeTool == MarkupTool.ARROW,
                                    onClick = { activeTool = MarkupTool.ARROW }
                                )
                                ToolIconBadge(
                                    icon = Icons.Default.Draw,
                                    label = "Pincel",
                                    selected = activeTool == MarkupTool.PEN,
                                    onClick = { activeTool = MarkupTool.PEN }
                                )
                                ToolIconBadge(
                                    icon = Icons.Default.Crop,
                                    label = "Recuadro",
                                    selected = activeTool == MarkupTool.RECTANGLE,
                                    onClick = { activeTool = MarkupTool.RECTANGLE }
                                )
                                ToolIconBadge(
                                    icon = Icons.Default.Lock,
                                    label = "Pixelar",
                                    selected = activeTool == MarkupTool.MOSAIC,
                                    onClick = { activeTool = MarkupTool.MOSAIC }
                                )
                                ToolIconBadge(
                                    icon = Icons.Default.BorderColor,
                                    label = "Tachar",
                                    selected = activeTool == MarkupTool.REDACT,
                                    onClick = { activeTool = MarkupTool.REDACT }
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Paleta de colores y grosor si la herramienta lo permite
                            if (activeTool != MarkupTool.MOSAIC && activeTool != MarkupTool.REDACT) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Colores
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        for (color in palette) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .clickable { selectedColor = color }
                                                    .border(
                                                        width = if (selectedColor == color) 2.dp else 1.dp,
                                                        color = if (selectedColor == color) Color.White else Color.Black.copy(alpha = 0.4f),
                                                        shape = CircleShape
                                                    )
                                            )
                                        }
                                    }

                                    // Selector de grosor
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        ThicknessBadge("Fino", 4f, strokeThickness) { strokeThickness = 4f }
                                        ThicknessBadge("Medio", 8f, strokeThickness) { strokeThickness = 8f }
                                        ThicknessBadge("Grueso", 14f, strokeThickness) { strokeThickness = 14f }
                                    }
                                }
                            } else {
                                Text(
                                    text = if (activeTool == MarkupTool.MOSAIC) "Desliza el dedo sobre caras, DNI o matrículas para pixelar" else "Pasa el rotulador para ocultar datos confidenciales",
                                    color = FlashAmber,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                )
                            }
                        }

                        EditorTab.ROTATE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ActionButton(
                                    icon = Icons.Default.RotateRight,
                                    text = "Girar 90°",
                                    onClick = { rotationDegrees = (rotationDegrees + 90) % 360 }
                                )
                                ActionButton(
                                    icon = Icons.Default.Flip,
                                    text = "Espejo",
                                    onClick = { isFlippedHorizontal = !isFlippedHorizontal }
                                )
                                ActionButton(
                                    icon = Icons.Default.Undo,
                                    text = "Restablecer",
                                    onClick = {
                                        rotationDegrees = 0
                                        isFlippedHorizontal = false
                                    }
                                )
                            }
                        }

                        EditorTab.FILTERS -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                for (mode in ImageFilterMode.values()) {
                                    FilterBadge(
                                        label = mode.label,
                                        selected = activeFilter == mode,
                                        onClick = { activeFilter = mode }
                                    )
                                }
                            }
                        }

                        EditorTab.RESIZE -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                QualityBadge("Original", 0, exportResolutionMaxDim) { exportResolutionMaxDim = 0 }
                                QualityBadge("1080p (FHD)", 1920, exportResolutionMaxDim) { exportResolutionMaxDim = 1920 }
                                QualityBadge("720p (Rápido)", 1280, exportResolutionMaxDim) { exportResolutionMaxDim = 1280 }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Selector de Pestaña Principal (Anotar / Girar / Filtros / Calidad)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDarkVariant)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        TabBadge("Anotar", Icons.Default.Draw, activeTab == EditorTab.MARKUP) { activeTab = EditorTab.MARKUP }
                        TabBadge("Girar", Icons.Default.RotateRight, activeTab == EditorTab.ROTATE) { activeTab = EditorTab.ROTATE }
                        TabBadge("Filtros", Icons.Default.Filter, activeTab == EditorTab.FILTERS) { activeTab = EditorTab.FILTERS }
                        TabBadge("Tamaño", Icons.Default.PhotoSizeSelectActual, activeTab == EditorTab.RESIZE) { activeTab = EditorTab.RESIZE }
                    }
                }
            }
        }
    }
}

// =========================================================================
// Helpers de UI para el Editor
// =========================================================================

@Composable
private fun ToolIconBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .background(if (selected) FlashCyan.copy(alpha = 0.2f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) FlashCyan else TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = if (selected) FlashCyan else TextSecondary,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun TabBadge(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .background(if (selected) FlashCyan else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) BgDark else TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = if (selected) BgDark else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
private fun ThicknessBadge(
    label: String,
    thickness: Float,
    current: Float,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        color = if (current == thickness) FlashCyan.copy(alpha = 0.2f) else SurfaceDarkVariant,
        border = BorderStroke(1.dp, if (current == thickness) FlashCyan else BorderDark)
    ) {
        Text(
            text = label,
            color = if (current == thickness) FlashCyan else TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDarkVariant,
        border = BorderStroke(1.dp, BorderDark)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = FlashCyan, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = text, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun FilterBadge(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) FlashCyan.copy(alpha = 0.25f) else SurfaceDarkVariant,
        border = BorderStroke(1.dp, if (selected) FlashCyan else BorderDark)
    ) {
        Text(
            text = label,
            color = if (selected) FlashCyan else TextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun QualityBadge(
    label: String,
    maxDim: Int,
    current: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (current == maxDim) FlashCyan.copy(alpha = 0.25f) else SurfaceDarkVariant,
        border = BorderStroke(1.dp, if (current == maxDim) FlashCyan else BorderDark)
    ) {
        Text(
            text = label,
            color = if (current == maxDim) FlashCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (current == maxDim) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

// =========================================================================
// Funciones de Dibujo en Compose Canvas
// =========================================================================

private fun androidx.compose.ui.graphics.drawscope.DrawScope.renderAction(action: DrawAction, bounds: Rect) {
    when (action) {
        is DrawAction.PathStroke -> {
            if (action.points.size > 1) {
                val realThickness = action.strokeWidthNorm * bounds.width
                for (i in 0 until action.points.size - 1) {
                    val p1 = fromNorm(action.points[i], bounds)
                    val p2 = fromNorm(action.points[i + 1], bounds)
                    drawLine(
                        color = action.color,
                        start = p1,
                        end = p2,
                        strokeWidth = realThickness,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
        is DrawAction.Arrow -> {
            val p1 = fromNorm(action.start, bounds)
            val p2 = fromNorm(action.end, bounds)
            val realThickness = action.strokeWidthNorm * bounds.width
            drawLine(
                color = action.color,
                start = p1,
                end = p2,
                strokeWidth = realThickness,
                cap = StrokeCap.Round
            )
            // Cabeza de la flecha
            val angle = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
            val headLen = (realThickness * 2.8f + 14f).toDouble()
            val arrowAngle = Math.PI / 6.0 // 30 grados
            val wing1 = Offset(
                (p2.x - headLen * cos(angle - arrowAngle)).toFloat(),
                (p2.y - headLen * sin(angle - arrowAngle)).toFloat()
            )
            val wing2 = Offset(
                (p2.x - headLen * cos(angle + arrowAngle)).toFloat(),
                (p2.y - headLen * sin(angle + arrowAngle)).toFloat()
            )
            drawLine(color = action.color, start = p2, end = wing1, strokeWidth = realThickness, cap = StrokeCap.Round)
            drawLine(color = action.color, start = p2, end = wing2, strokeWidth = realThickness, cap = StrokeCap.Round)
        }
        is DrawAction.Rectangle -> {
            val p1 = fromNorm(action.start, bounds)
            val p2 = fromNorm(action.end, bounds)
            val left = minOf(p1.x, p2.x)
            val top = minOf(p1.y, p2.y)
            val w = kotlin.math.abs(p2.x - p1.x)
            val h = kotlin.math.abs(p2.y - p1.y)
            val realThickness = action.strokeWidthNorm * bounds.width
            drawRect(
                color = action.color,
                topLeft = Offset(left, top),
                size = Size(w, h),
                style = Stroke(width = realThickness, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
        is DrawAction.Mosaic -> {
            // Previsualización de censura pixelada sobre el canvas
            val blockSize = 14f
            for (pt in action.points) {
                val center = fromNorm(pt, bounds)
                val rad = action.radiusNorm * bounds.width
                val left = center.x - rad
                val top = center.y - rad
                var cy = top
                while (cy < top + rad * 2) {
                    var cx = left
                    while (cx < left + rad * 2) {
                        drawRect(
                            color = Color(0x992B3A4F),
                            topLeft = Offset(cx, cy),
                            size = Size(blockSize - 1f, blockSize - 1f)
                        )
                        cx += blockSize
                    }
                    cy += blockSize
                }
            }
        }
        is DrawAction.Redact -> {
            val realThickness = action.strokeWidthNorm * bounds.width
            for (i in 0 until action.points.size - 1) {
                val p1 = fromNorm(action.points[i], bounds)
                val p2 = fromNorm(action.points[i + 1], bounds)
                drawLine(
                    color = Color.Black.copy(alpha = 0.95f),
                    start = p1,
                    end = p2,
                    strokeWidth = realThickness,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

private fun calculateImageBounds(canvasSize: Size, bmp: Bitmap, rotation: Int): Rect {
    val isRotated90 = (rotation % 180) != 0
    val bmpW = if (isRotated90) bmp.height.toFloat() else bmp.width.toFloat()
    val bmpH = if (isRotated90) bmp.width.toFloat() else bmp.height.toFloat()
    val bmpAspect = bmpW / bmpH
    val canvasAspect = canvasSize.width / canvasSize.height

    val (w, h) = if (canvasAspect > bmpAspect) {
        Pair(canvasSize.height * bmpAspect, canvasSize.height)
    } else {
        Pair(canvasSize.width, canvasSize.width / bmpAspect)
    }
    val left = (canvasSize.width - w) / 2f
    val top = (canvasSize.height - h) / 2f
    return Rect(left, top, left + w, top + h)
}

private fun toNorm(pt: Offset, bounds: Rect): Offset {
    val nx = ((pt.x - bounds.left) / bounds.width).coerceIn(0f, 1f)
    val ny = ((pt.y - bounds.top) / bounds.height).coerceIn(0f, 1f)
    return Offset(nx, ny)
}

private fun fromNorm(norm: Offset, bounds: Rect): Offset {
    return Offset(bounds.left + norm.x * bounds.width, bounds.top + norm.y * bounds.height)
}

// =========================================================================
// Procesamiento de Bitmap y Guardado Final
// =========================================================================

private fun loadSafeBitmap(context: Context, uri: Uri, maxDim: Int = 2560): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        var inSampleSize = 1
        while (options.outWidth / inSampleSize > maxDim || options.outHeight / inSampleSize > maxDim) {
            inSampleSize *= 2
        }
        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inMutable = true
        }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        // Corregir orientación EXIF si está girada en origen
        val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
        val rotation = when (exif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation != 0f) {
            val matrix = Matrix().apply { postRotate(rotation) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        } else {
            decoded
        }
    } catch (_: Exception) {
        null
    }
}

private fun saveEditedImage(
    context: Context,
    sourceItem: MediaItem,
    baseBitmap: Bitmap,
    actions: List<DrawAction>,
    rotation: Int,
    flipped: Boolean,
    filter: ImageFilterMode,
    maxDim: Int
): File? {
    return try {
        // 1. Aplicar rotación y espejo
        var processed = if (rotation != 0 || flipped) {
            val matrix = Matrix().apply {
                if (flipped) postScale(-1f, 1f)
                if (rotation != 0) postRotate(rotation.toFloat())
            }
            Bitmap.createBitmap(baseBitmap, 0, 0, baseBitmap.width, baseBitmap.height, matrix, true)
        } else {
            baseBitmap.copy(Bitmap.Config.ARGB_8888, true)
        }

        // 2. Aplicar filtro si se seleccionó
        processed = when (filter) {
            ImageFilterMode.DOC_BW -> applyDocumentFilter(processed)
            ImageFilterMode.GRAYSCALE -> applyGrayscaleFilter(processed)
            ImageFilterMode.ENHANCE -> applyEnhanceFilter(processed)
            ImageFilterMode.NONE -> processed
        }

        // 3. Renderizar acciones de dibujo y censura pixelada sobre el bitmap nativo
        val canvas = Canvas(processed)
        val paint = Paint().apply {
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        val bmpBounds = Rect(0f, 0f, processed.width.toFloat(), processed.height.toFloat())

        for (action in actions) {
            when (action) {
                is DrawAction.PathStroke -> {
                    if (action.points.size > 1) {
                        paint.color = action.color.toArgb()
                        paint.strokeWidth = action.strokeWidthNorm * bmpBounds.width
                        paint.style = Paint.Style.STROKE
                        val path = android.graphics.Path()
                        val p0 = fromNorm(action.points[0], bmpBounds)
                        path.moveTo(p0.x, p0.y)
                        for (i in 1 until action.points.size) {
                            val pt = fromNorm(action.points[i], bmpBounds)
                            path.lineTo(pt.x, pt.y)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
                is DrawAction.Arrow -> {
                    paint.color = action.color.toArgb()
                    val strokeW = action.strokeWidthNorm * bmpBounds.width
                    paint.strokeWidth = strokeW
                    paint.style = Paint.Style.STROKE
                    val p1 = fromNorm(action.start, bmpBounds)
                    val p2 = fromNorm(action.end, bmpBounds)
                    canvas.drawLine(p1.x, p1.y, p2.x, p2.y, paint)

                    val angle = atan2((p2.y - p1.y).toDouble(), (p2.x - p1.x).toDouble())
                    val headLen = (strokeW * 3f + 20f).toDouble()
                    val arrowAngle = Math.PI / 6.0
                    val wing1X = (p2.x - headLen * cos(angle - arrowAngle)).toFloat()
                    val wing1Y = (p2.y - headLen * sin(angle - arrowAngle)).toFloat()
                    val wing2X = (p2.x - headLen * cos(angle + arrowAngle)).toFloat()
                    val wing2Y = (p2.y - headLen * sin(angle + arrowAngle)).toFloat()
                    canvas.drawLine(p2.x, p2.y, wing1X, wing1Y, paint)
                    canvas.drawLine(p2.x, p2.y, wing2X, wing2Y, paint)
                }
                is DrawAction.Rectangle -> {
                    paint.color = action.color.toArgb()
                    val strokeW = action.strokeWidthNorm * bmpBounds.width
                    paint.strokeWidth = strokeW
                    paint.style = Paint.Style.STROKE
                    val p1 = fromNorm(action.start, bmpBounds)
                    val p2 = fromNorm(action.end, bmpBounds)
                    canvas.drawRect(minOf(p1.x, p2.x), minOf(p1.y, p2.y), maxOf(p1.x, p2.x), maxOf(p1.y, p2.y), paint)
                }
                is DrawAction.Mosaic -> {
                    // Pixelado real en los píxeles del bitmap
                    val pixelRadius = (action.radiusNorm * bmpBounds.width).toInt().coerceAtLeast(10)
                    for (pt in action.points) {
                        val center = fromNorm(pt, bmpBounds)
                        pixelateRegion(processed, center.x.toInt(), center.y.toInt(), pixelRadius, blockSize = 24)
                    }
                }
                is DrawAction.Redact -> {
                    paint.color = android.graphics.Color.BLACK
                    paint.strokeWidth = action.strokeWidthNorm * bmpBounds.width
                    paint.style = Paint.Style.STROKE
                    val path = android.graphics.Path()
                    val p0 = fromNorm(action.points[0], bmpBounds)
                    path.moveTo(p0.x, p0.y)
                    for (i in 1 until action.points.size) {
                        val pt = fromNorm(action.points[i], bmpBounds)
                        path.lineTo(pt.x, pt.y)
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }

        // 4. Redimensionar si se solicitó calidad FHD o HD
        if (maxDim > 0 && (processed.width > maxDim || processed.height > maxDim)) {
            val scale = minOf(maxDim.toFloat() / processed.width, maxDim.toFloat() / processed.height)
            val newW = (processed.width * scale).toInt()
            val newH = (processed.height * scale).toInt()
            processed = Bitmap.createScaledBitmap(processed, newW, newH, true)
        }

        // 5. Guardar en carpeta pública Pictures/FlashGallery
        val picturesDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "FlashGallery")
        if (!picturesDir.exists()) {
            picturesDir.mkdirs()
        }

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val rawName = sourceItem.name.substringBeforeLast(".")
        val targetFile = File(picturesDir, "${rawName}_edit_${timeStamp}.jpg")

        FileOutputStream(targetFile).use { out ->
            processed.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        // 6. Indexar inmediatamente con MediaScanner para que aparezca en la galería
        MediaScannerConnection.scanFile(
            context,
            arrayOf(targetFile.absolutePath),
            arrayOf("image/jpeg"),
            null
        )

        targetFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun pixelateRegion(bitmap: Bitmap, centerX: Int, centerY: Int, radius: Int, blockSize: Int = 20) {
    val left = (centerX - radius).coerceIn(0, bitmap.width - 1)
    val right = (centerX + radius).coerceIn(0, bitmap.width)
    val top = (centerY - radius).coerceIn(0, bitmap.height - 1)
    val bottom = (centerY + radius).coerceIn(0, bitmap.height)
    if (right <= left || bottom <= top) return

    val w = right - left
    val h = bottom - top
    val pixels = IntArray(w * h)
    bitmap.getPixels(pixels, 0, w, left, top, w, h)

    for (y in 0 until h step blockSize) {
        for (x in 0 until w step blockSize) {
            val blockW = minOf(blockSize, w - x)
            val blockH = minOf(blockSize, h - y)

            var r = 0L; var g = 0L; var b = 0L; var count = 0
            for (by in 0 until blockH) {
                for (bx in 0 until blockW) {
                    val color = pixels[(y + by) * w + (x + bx)]
                    r += android.graphics.Color.red(color)
                    g += android.graphics.Color.green(color)
                    b += android.graphics.Color.blue(color)
                    count++
                }
            }
            if (count > 0) {
                val avgColor = android.graphics.Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
                for (by in 0 until blockH) {
                    for (bx in 0 until blockW) {
                        pixels[(y + by) * w + (x + bx)] = avgColor
                    }
                }
            }
        }
    }
    bitmap.setPixels(pixels, 0, w, left, top, w, h)
}

private fun applyDocumentFilter(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint().apply {
        val cm = ColorMatrix(floatArrayOf(
            2.2f, 0f, 0f, 0f, -140f,
            0f, 2.2f, 0f, 0f, -140f,
            0f, 0f, 2.2f, 0f, -140f,
            0f, 0f, 0f, 1f, 0f
        ))
        val gray = ColorMatrix().apply { setSaturation(0f) }
        cm.preConcat(gray)
        colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

private fun applyGrayscaleFilter(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint().apply {
        colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

private fun applyEnhanceFilter(bitmap: Bitmap): Bitmap {
    val result = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(result)
    val paint = Paint().apply {
        val cm = ColorMatrix(floatArrayOf(
            1.25f, 0f, 0f, 0f, -15f,
            0f, 1.25f, 0f, 0f, -15f,
            0f, 0f, 1.25f, 0f, -15f,
            0f, 0f, 0f, 1f, 0f
        ))
        cm.postConcat(ColorMatrix().apply { setSaturation(1.3f) })
        colorFilter = ColorMatrixColorFilter(cm)
    }
    canvas.drawBitmap(bitmap, 0f, 0f, paint)
    return result
}

private fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}
