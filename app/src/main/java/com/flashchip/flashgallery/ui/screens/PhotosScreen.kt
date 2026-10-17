// Version 1.09 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.screens

import android.graphics.drawable.Drawable
import kotlin.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.flashchip.flashgallery.data.AppResolver
import com.flashchip.flashgallery.model.AppFolder
import com.flashchip.flashgallery.model.GalleryItem
import com.flashchip.flashgallery.model.MediaItem
import com.flashchip.flashgallery.ui.theme.BgDark
import com.flashchip.flashgallery.ui.theme.BorderDark
import com.flashchip.flashgallery.ui.theme.FlashAmber
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.SurfaceDarkVariant
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import com.flashchip.flashgallery.ui.theme.TextTertiary

@Composable
fun PhotosScreen(
    timelineItems: List<GalleryItem>,
    appFolders: List<AppFolder>,
    selectedAppFolder: AppFolder?,
    photoViewMode: Int, // 0: Cronológico Todo, 1: De tus apps
    gridColumns: Int,
    selectedMediaIds: Set<Long>,
    isSelectionMode: Boolean,
    isLoading: Boolean,
    onViewModeChange: (Int) -> Unit,
    onToggleColumns: () -> Unit,
    onSelectFolder: (AppFolder?) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onToggleSelectMedia: (MediaItem) -> Unit,
    onSelectMonthBatch: (GalleryItem.MonthHeader) -> Unit
) {
    val context = LocalContext.current
    val appResolver = remember { AppResolver(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
    ) {
        // Mode Selector Bar (Cronológico Todo vs De tus apps)
        if (selectedAppFolder == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = photoViewMode == 0,
                        onClick = { onViewModeChange(0) },
                        label = { Text("Línea de tiempo") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlashCyan.copy(alpha = 0.2f),
                            selectedLabelColor = FlashCyan,
                            selectedLeadingIconColor = FlashCyan,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = photoViewMode == 0,
                            borderColor = BorderDark,
                            selectedBorderColor = FlashCyan
                        )
                    )

                    FilterChip(
                        selected = photoViewMode == 1,
                        onClick = { onViewModeChange(1) },
                        label = { Text("De tus apps") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FlashCyan.copy(alpha = 0.2f),
                            selectedLabelColor = FlashCyan,
                            selectedLeadingIconColor = FlashCyan,
                            containerColor = SurfaceDark,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = photoViewMode == 1,
                            borderColor = BorderDark,
                            selectedBorderColor = FlashCyan
                        )
                    )
                }

                // Density column toggle button (3 vs 5 columns)
                if (photoViewMode == 0) {
                    IconButton(
                        onClick = onToggleColumns,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = if (gridColumns == 3) Icons.Default.ViewModule else Icons.Default.GridView,
                            contentDescription = "Cambiar vista",
                            tint = FlashCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        } else {
            // Header when drilled into a specific app folder
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { onSelectFolder(null) },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = FlashCyan
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = selectedAppFolder.appName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${selectedAppFolder.count} elementos (${selectedAppFolder.photoCount} fotos, ${selectedAppFolder.videoCount} vídeos)",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Body content
        if (selectedAppFolder != null) {
            // Specific app folder grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                contentPadding = PaddingValues(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(selectedAppFolder.items, key = { it.id }) { media ->
                    val isSelected = selectedMediaIds.contains(media.id)
                    MediaThumbnailItem(
                        item = media,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                onToggleSelectMedia(media)
                            } else {
                                onMediaClick(media)
                            }
                        },
                        onLongClick = { onToggleSelectMedia(media) }
                    )
                }
            }
        } else if (photoViewMode == 0) {
            // Timeline view con Cabeceras Sticky por Meses y Cuadricula
            if (timelineItems.isEmpty() && isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = FlashCyan)
                }
            } else if (timelineItems.isEmpty()) {
                EmptyStateView(message = "No se encontraron fotos o vídeos en el dispositivo.")
            } else {
                val gridState = rememberLazyGridState()
                val coroutineScope = rememberCoroutineScope()
                var showMonthJumpDialog by remember { mutableStateOf(false) }

                // Lista de todas las cabeceras de mes para el selector
                val allMonthHeaders = remember(timelineItems) {
                    timelineItems.filterIsInstance<GalleryItem.MonthHeader>()
                }

                // Encabezado activo que permanece fijado arriba segun la posicion de scroll
                val activeMonthHeader by remember(timelineItems) {
                    derivedStateOf {
                        val visibleIndex = gridState.firstVisibleItemIndex
                        if (visibleIndex in timelineItems.indices) {
                            for (i in visibleIndex downTo 0) {
                                val item = timelineItems[i]
                                if (item is GalleryItem.MonthHeader) {
                                    return@derivedStateOf item
                                }
                            }
                        }
                        timelineItems.firstOrNull { it is GalleryItem.MonthHeader } as? GalleryItem.MonthHeader
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(gridColumns),
                        contentPadding = PaddingValues(top = 76.dp, bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            count = timelineItems.size,
                            key = { index ->
                                when (val item = timelineItems[index]) {
                                    is GalleryItem.MonthHeader -> "header_${item.key}"
                                    is GalleryItem.Media -> "media_${item.item.id}"
                                }
                            },
                            span = { index ->
                                when (timelineItems[index]) {
                                    is GalleryItem.MonthHeader -> GridItemSpan(maxLineSpan)
                                    is GalleryItem.Media -> GridItemSpan(1)
                                }
                            },
                            contentType = { index ->
                                when (timelineItems[index]) {
                                    is GalleryItem.MonthHeader -> 0
                                    is GalleryItem.Media -> 1
                                }
                            }
                        ) { index ->
                            when (val item = timelineItems[index]) {
                                is GalleryItem.MonthHeader -> {
                                    if (index == 0) {
                                        // El primer mes ya está mostrado fijado arriba en la barra StickyMonthHeaderBar.
                                        // Omitimos la cabecera inline para que NO aparezca duplicada al abrir la galería!
                                        Spacer(modifier = Modifier.height(4.dp))
                                    } else {
                                        MonthHeaderInlineView(
                                            header = item,
                                            onHeaderClick = {
                                                if (allMonthHeaders.isNotEmpty()) {
                                                    showMonthJumpDialog = true
                                                }
                                            },
                                            onSelectBatch = { onSelectMonthBatch(item) }
                                        )
                                    }
                                }
                                is GalleryItem.Media -> {
                                    val isSelected = selectedMediaIds.contains(item.item.id)
                                    MediaThumbnailItem(
                                        item = item.item,
                                        isSelected = isSelected,
                                        isSelectionMode = isSelectionMode,
                                        onClick = {
                                            if (isSelectionMode) {
                                                onToggleSelectMedia(item.item)
                                            } else {
                                                onMediaClick(item.item)
                                            }
                                        },
                                        onLongClick = { onToggleSelectMedia(item.item) }
                                    )
                                }
                            }
                        }
                    }

                    // Encabezado fijado arriba (Sticky Header) que cambia conforme se hace scroll
                    activeMonthHeader?.let { header ->
                        StickyMonthHeaderBar(
                            header = header,
                            onHeaderClick = {
                                if (allMonthHeaders.isNotEmpty()) {
                                    showMonthJumpDialog = true
                                }
                            },
                            onSelectBatch = { onSelectMonthBatch(header) },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                        )
                    }

                    // Dialogo para saltar directamente a un mes y año elegido
                    if (showMonthJumpDialog && allMonthHeaders.isNotEmpty()) {
                        MonthYearJumpDialog(
                            monthHeaders = allMonthHeaders,
                            currentHeader = activeMonthHeader,
                            onSelectMonth = { targetHeader ->
                                showMonthJumpDialog = false
                                val targetIndex = timelineItems.indexOfFirst {
                                    it is GalleryItem.MonthHeader && it.key == targetHeader.key
                                }
                                if (targetIndex != -1) {
                                    coroutineScope.launch {
                                        gridState.scrollToItem(targetIndex)
                                    }
                                }
                            },
                            onDismiss = { showMonthJumpDialog = false }
                        )
                    }
                }
            }
        } else {
            // View "De tus apps"
            if (appFolders.isEmpty()) {
                EmptyStateView(message = "No se detectaron carpetas de aplicaciones con imágenes.")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(appFolders, key = { it.id }) { folder ->
                        AppFolderCard(
                            folder = folder,
                            appResolver = appResolver,
                            onClick = { onSelectFolder(folder) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Cabecera fijada en la parte superior (Sticky Header)
 */
@Composable
fun StickyMonthHeaderBar(
    header: GalleryItem.MonthHeader,
    onHeaderClick: () -> Unit,
    onSelectBatch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = BgDark.copy(alpha = 0.95f),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de mes y año al pulsar sobre la cabecera
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onHeaderClick() }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(FlashCyan)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = header.title,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Saltar a fecha",
                                tint = FlashCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "${header.photoCount} fotos" + if (header.videoCount > 0) ", ${header.videoCount} vídeos" else "",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Botón de selección por mes
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectBatch() }
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = "Seleccionar mes",
                        tint = FlashCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Seleccionar",
                        color = FlashCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(
                color = BorderDark.copy(alpha = 0.6f),
                thickness = 1.dp
            )
        }
    }
}

/**
 * Cabecera inline minimalista ("solo letras") para separar meses dentro del scroll
 */
@Composable
fun MonthHeaderInlineView(
    header: GalleryItem.MonthHeader,
    onHeaderClick: (() -> Unit)? = null,
    onSelectBatch: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 10.dp)
    ) {
        // Línea sutil de separación
        HorizontalDivider(
            color = BorderDark.copy(alpha = 0.5f),
            thickness = 0.8.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Información del mes en letras limpias, sin tarjetas ni adornos pesados
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .then(if (onHeaderClick != null) Modifier.clickable { onHeaderClick() } else Modifier)
            ) {
                Text(
                    text = header.title,
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                val totalCount = header.photoCount + header.videoCount
                Text(
                    text = "• $totalCount elem.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            // Enlace de texto sencillo y limpio para seleccionar este lote
            if (onSelectBatch != null) {
                Text(
                    text = "Seleccionar",
                    color = FlashCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelectBatch() }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Diálogo interactivo para saltar directamente al Año y Mes deseado en la línea de tiempo
 */
@Composable
fun MonthYearJumpDialog(
    monthHeaders: List<GalleryItem.MonthHeader>,
    currentHeader: GalleryItem.MonthHeader?,
    onSelectMonth: (GalleryItem.MonthHeader) -> Unit,
    onDismiss: () -> Unit
) {
    val availableYears = remember(monthHeaders) {
        monthHeaders.map { it.year }.distinct().sortedDescending()
    }
    var selectedYear by remember(currentHeader, availableYears) {
        mutableStateOf(currentHeader?.year ?: availableYears.firstOrNull() ?: 2026)
    }

    val monthsInSelectedYear = remember(selectedYear, monthHeaders) {
        monthHeaders.filter { it.year == selectedYear }.sortedByDescending { it.month }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, FlashCyan.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            color = BgDark,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Cabecera del diálogo con título y botón cerrar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(FlashCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = FlashCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Saltar a fecha",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Elige año y mes para ir al instante",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Selector de Año
                Text(
                    text = "AÑO",
                    color = FlashCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(availableYears) { year ->
                        val isYearSelected = year == selectedYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isYearSelected) FlashCyan.copy(alpha = 0.2f) else SurfaceDark)
                                .border(
                                    1.dp,
                                    if (isYearSelected) FlashCyan else BorderDark,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedYear = year }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = year.toString(),
                                color = if (isYearSelected) FlashCyan else TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = if (isYearSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Selector de Meses disponibles en el año seleccionado
                Text(
                    text = "MESES CON CONTENIDO EN $selectedYear",
                    color = FlashCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (monthsInSelectedYear.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay fotos en este año.",
                            color = TextTertiary,
                            fontSize = 13.sp
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                    ) {
                        items(monthsInSelectedYear, key = { it.key }) { header ->
                            val isCurrentMonth = header.key == currentHeader?.key
                            val monthNameOnly = header.title.split(" ").firstOrNull() ?: header.title
                            val totalElements = header.photoCount + header.videoCount

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isCurrentMonth) FlashCyan.copy(alpha = 0.15f) else SurfaceDark)
                                    .border(
                                        1.dp,
                                        if (isCurrentMonth) FlashCyan else BorderDark,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectMonth(header) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = monthNameOnly,
                                            color = if (isCurrentMonth) FlashCyan else TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCurrentMonth) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(FlashCyan)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "$totalElements elemento${if (totalElements != 1) "s" else ""}",
                                        color = if (isCurrentMonth) FlashCyan.copy(alpha = 0.85f) else TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Acceso rápido a las fotos más recientes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                        .clickable {
                            monthHeaders.firstOrNull()?.let { onSelectMonth(it) }
                        }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowUpward,
                        contentDescription = null,
                        tint = FlashCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ir al principio (Más recientes)",
                        color = FlashCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnailItem(
    item: MediaItem,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .padding(1.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(2.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(SurfaceDark)
    ) {
        // Miniatura instantanea via Coil utilizando la cache de thumbnails por hardware
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(false)
                .size(256)
                .allowRgb565(true)
                .memoryCacheKey("thumb_${item.id}")
                .diskCacheKey("thumb_${item.id}")
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Video Badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Vídeo",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    if (item.durationMs > 0) {
                        Spacer(modifier = Modifier.width(2.dp))
                        val totalSecs = item.durationMs / 1000
                        val mins = totalSecs / 60
                        val secs = totalSecs % 60
                        Text(
                            text = String.format("%02d:%02d", mins, secs),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Selection overlay and checkbox
        if (isSelectionMode || isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (isSelected) FlashCyan.copy(alpha = 0.35f) else Color.Transparent)
            )

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (isSelected) FlashCyan else Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun AppFolderCard(
    folder: AppFolder,
    appResolver: AppResolver,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val appIcon: Drawable? = remember(folder.packageName) {
        appResolver.getAppIcon(folder.packageName)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceDark)
            .border(1.dp, BorderDark, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column {
            // Cover Image with real app badge overlaid in bottom-right corner!
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(folder.coverItem.uri)
                        .crossfade(true)
                        .size(400)
                        .build(),
                    contentDescription = folder.appName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Real App Badge overlay in bottom-right corner as specified in prompt!
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceDark)
                        .border(1.5.dp, FlashCyan, CircleShape)
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (appIcon != null) {
                        Image(
                            bitmap = appIcon.toBitmap(64, 64).asImageBitmap(),
                            contentDescription = folder.appName,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Apps,
                            contentDescription = folder.appName,
                            tint = FlashCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Info row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = folder.appName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${folder.count} elementos",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = TextTertiary,
            fontSize = 14.sp
        )
    }
}
