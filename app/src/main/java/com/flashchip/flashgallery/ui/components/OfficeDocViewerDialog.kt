// Version 1.15 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CoPresent
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flashchip.flashgallery.data.DocParagraph
import com.flashchip.flashgallery.data.OfficeContent
import com.flashchip.flashgallery.data.OfficeDocumentParser
import com.flashchip.flashgallery.data.SheetData
import com.flashchip.flashgallery.data.SlideData
import com.flashchip.flashgallery.model.DocItem
import com.flashchip.flashgallery.ui.theme.BgDark
import com.flashchip.flashgallery.ui.theme.BorderDark
import com.flashchip.flashgallery.ui.theme.FlashAmber
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.FlashGreen
import com.flashchip.flashgallery.ui.theme.FlashPurple
import com.flashchip.flashgallery.ui.theme.FlashRed
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.SurfaceDarkVariant
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import com.flashchip.flashgallery.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val WordBlue = Color(0xFF2B579A)
private val ExcelGreen = Color(0xFF217346)
private val PptOrange = Color(0xFFD24726)

@Composable
fun OfficeDocViewerDialog(
    doc: DocItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var officeContent by remember { mutableStateOf<OfficeContent?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSheetIndex by remember { mutableIntStateOf(0) }

    val lazyListState = rememberLazyListState()

    // Carga y análisis asíncrono del archivo Office
    LaunchedEffect(doc.file) {
        isLoading = true
        withContext(Dispatchers.IO) {
            officeContent = OfficeDocumentParser.parse(doc)
        }
        isLoading = false
    }

    val ext = doc.extension.lowercase()
    val (themeColor, typeIcon, typeLabel) = when (ext) {
        "docx", "doc", "odt", "rtf" -> Triple(WordBlue, Icons.Default.Article, "Documento de texto")
        "xlsx", "xls", "ods", "csv" -> Triple(ExcelGreen, Icons.Default.TableChart, "Hoja de cálculo")
        "pptx", "ppt", "odp" -> Triple(PptOrange, Icons.Default.CoPresent, "Presentación")
        else -> Triple(FlashCyan, Icons.Default.Article, "Documento Office")
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BgDark
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // TopBar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(SurfaceDarkVariant)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeColor.copy(alpha = 0.2f))
                                .border(1.dp, themeColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = doc.name,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${doc.extension.uppercase()} • ${doc.sizeFormatted} • $typeLabel",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Botones de acción TopBar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Buscar
                        IconButton(
                            onClick = { isSearchOpen = !isSearchOpen },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Buscar",
                                tint = if (isSearchOpen) FlashCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Copiar texto
                        IconButton(
                            onClick = {
                                val allText = extractAllText(officeContent)
                                if (allText.isNotBlank()) {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText(doc.name, allText))
                                    Toast.makeText(context, "Texto copiado al portapapeles", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "No hay texto para copiar", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar texto",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Compartir archivo
                        IconButton(
                            onClick = onShare,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Abrir con aplicación externa (Word, Excel, WPS Office)
                        IconButton(
                            onClick = onOpenExternal,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Abrir con...",
                                tint = FlashCyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                // Barra de búsqueda desplegable
                AnimatedVisibility(
                    visible = isSearchOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceDarkVariant)
                            .border(1.dp, BorderDark)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = FlashCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                            cursorBrush = SolidColor(FlashCyan),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Buscar texto en el documento...", color = TextTertiary, fontSize = 13.sp)
                                }
                                innerTextField()
                            }
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Contenido del documento
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BgDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = themeColor)
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Procesando $typeLabel...",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    when (val content = officeContent) {
                        is OfficeContent.Document -> {
                            WordDocumentView(
                                content = content,
                                searchQuery = searchQuery,
                                themeColor = themeColor,
                                onOpenExternal = onOpenExternal
                            )
                        }
                        is OfficeContent.Spreadsheet -> {
                            SpreadsheetView(
                                content = content,
                                selectedSheetIndex = selectedSheetIndex,
                                onSheetSelect = { selectedSheetIndex = it },
                                searchQuery = searchQuery,
                                themeColor = themeColor,
                                onOpenExternal = onOpenExternal
                            )
                        }
                        is OfficeContent.Presentation -> {
                            PresentationView(
                                content = content,
                                searchQuery = searchQuery,
                                themeColor = themeColor,
                                onOpenExternal = onOpenExternal
                            )
                        }
                        is OfficeContent.FallbackText -> {
                            FallbackView(
                                content = content,
                                ext = ext,
                                themeColor = themeColor,
                                onOpenExternal = onOpenExternal
                            )
                        }
                        null -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No se pudo cargar el documento.", color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vista para documentos de texto (Word .docx, .odt, .rtf)
 */
@Composable
private fun WordDocumentView(
    content: OfficeContent.Document,
    searchQuery: String,
    themeColor: Color,
    onOpenExternal: () -> Unit
) {
    SelectionContainer {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BgDark),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Ficha de estadísticas
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📖 ${content.paragraphs.size} párrafos • ~${content.wordCount} palabras",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    if (content.tableCount > 0) {
                        Text(
                            text = "📊 ${content.tableCount} tablas",
                            color = FlashCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Párrafos del documento
            itemsIndexed(content.paragraphs) { _, para ->
                if (para.isTable && para.tableRows.isNotEmpty()) {
                    // Render de tabla
                    OfficeTableView(para.tableRows, searchQuery, themeColor)
                } else if (para.isHeading) {
                    // Título / Encabezado
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(if (para.headingLevel == 1) 22.dp else 18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(themeColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = highlightSearch(para.text, searchQuery),
                            color = TextPrimary,
                            fontSize = if (para.headingLevel == 1) 18.sp else 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (para.isListItem) {
                    // Elemento de lista
                    Row(modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp)) {
                        Text("• ", color = themeColor, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = highlightSearch(para.text, searchQuery),
                            color = TextPrimary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                    }
                } else {
                    // Párrafo normal
                    Text(
                        text = highlightSearch(para.text, searchQuery),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }

            // Pie con sugerencia de edición externa
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onOpenExternal,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkVariant)
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = FlashCyan, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Editar documento en Word / Office", color = TextPrimary, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Vista para hojas de cálculo (Excel .xlsx, .ods, .csv)
 */
@Composable
private fun SpreadsheetView(
    content: OfficeContent.Spreadsheet,
    selectedSheetIndex: Int,
    onSheetSelect: (Int) -> Unit,
    searchQuery: String,
    themeColor: Color,
    onOpenExternal: () -> Unit
) {
    val currentSheet = content.sheets.getOrNull(selectedSheetIndex) ?: content.sheets.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        // Selector de hojas si hay más de 1
        if (content.sheets.size > 1) {
            ScrollableTabRow(
                selectedTabIndex = selectedSheetIndex,
                containerColor = SurfaceDark,
                contentColor = themeColor,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    if (selectedSheetIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedSheetIndex]),
                            color = themeColor
                        )
                    }
                }
            ) {
                content.sheets.forEachIndexed { index, sheet ->
                    Tab(
                        selected = selectedSheetIndex == index,
                        onClick = { onSheetSelect(index) },
                        text = {
                            Text(
                                text = sheet.name,
                                fontSize = 12.5.sp,
                                fontWeight = if (selectedSheetIndex == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedSheetIndex == index) themeColor else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        // Estadísticas de la hoja
        currentSheet?.let { sheet ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDarkVariant)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📊 ${sheet.name}: ${sheet.totalRows} filas, ${sheet.totalCols} columnas",
                    color = TextSecondary,
                    fontSize = 11.5.sp
                )
                Text(
                    text = "Desliza en 2D para ver celdas",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }

            // Cuadrícula interactiva 2D
            val horizontalScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .horizontalScroll(horizontalScrollState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    itemsIndexed(sheet.rows) { rowIndex, row ->
                        val isHeaderRow = rowIndex == 0
                        Row(
                            modifier = Modifier
                                .background(
                                    if (isHeaderRow) SurfaceDarkVariant
                                    else if (rowIndex % 2 == 0) BgDark
                                    else SurfaceDark.copy(alpha = 0.5f)
                                )
                                .border(0.5.dp, BorderDark.copy(alpha = 0.4f))
                        ) {
                            // Cabecera de fila (número de fila 1, 2, 3...)
                            Box(
                                modifier = Modifier
                                    .width(42.dp)
                                    .background(SurfaceDarkVariant.copy(alpha = 0.8f))
                                    .border(0.5.dp, BorderDark.copy(alpha = 0.4f))
                                    .padding(vertical = 6.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (rowIndex + 1).toString(),
                                    color = TextTertiary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Celdas de la fila
                            row.forEach { cellVal ->
                                Box(
                                    modifier = Modifier
                                        .width(130.dp)
                                        .border(0.5.dp, BorderDark.copy(alpha = 0.3f))
                                        .padding(vertical = 6.dp, horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = highlightSearch(cellVal, searchQuery),
                                        color = if (isHeaderRow) FlashGreen else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (isHeaderRow) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vista para presentaciones (PowerPoint .pptx, .odp)
 */
@Composable
private fun PresentationView(
    content: OfficeContent.Presentation,
    searchQuery: String,
    themeColor: Color,
    onOpenExternal: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "🎞️ ${content.slides.size} Diapositivas encontradas",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        itemsIndexed(content.slides) { _, slide ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, BorderDark, RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Diapositiva ${slide.slideNumber}",
                            color = themeColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = highlightSearch(slide.title, searchQuery),
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (slide.bullets.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        slide.bullets.forEach { bullet ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text("• ", color = themeColor, fontWeight = FontWeight.Bold)
                                Text(
                                    text = highlightSearch(bullet, searchQuery),
                                    color = TextSecondary,
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onOpenExternal,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkVariant)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, tint = FlashCyan, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir presentación en PowerPoint / Office", color = TextPrimary, fontSize = 13.sp)
            }
        }
    }
}

/**
 * Tabla renderizada para documentos Word
 */
@Composable
private fun OfficeTableView(
    rows: List<List<String>>,
    searchQuery: String,
    themeColor: Color
) {
    val horizontalScrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, BorderDark, RoundedCornerShape(8.dp))
            .horizontalScroll(horizontalScrollState)
    ) {
        Column {
            rows.forEachIndexed { rowIndex, row ->
                val isHeader = rowIndex == 0
                Row(
                    modifier = Modifier
                        .background(if (isHeader) SurfaceDarkVariant else if (rowIndex % 2 == 0) BgDark else SurfaceDark)
                ) {
                    row.forEach { cell ->
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .border(0.5.dp, BorderDark.copy(alpha = 0.4f))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = highlightSearch(cell, searchQuery),
                                color = if (isHeader) themeColor else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vista alternativa para formatos binarios heredados (.doc, .xls, .ppt)
 */
@Composable
private fun FallbackView(
    content: OfficeContent.FallbackText,
    ext: String,
    themeColor: Color,
    onOpenExternal: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDark)
                .border(1.dp, themeColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Aviso de formato (${ext.uppercase()})",
                    color = themeColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = content.message,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = onOpenExternal,
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Abrir en Microsoft Office / WPS", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (content.text.isNotBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text("Texto extraído del archivo:", color = TextTertiary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))

            SelectionContainer {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceDark)
                        .border(1.dp, BorderDark, RoundedCornerShape(10.dp))
                        .padding(12.dp)
                ) {
                    item {
                        Text(
                            text = content.text,
                            color = TextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

private fun highlightSearch(text: String, query: String) = buildAnnotatedString {
    append(text)
    if (query.isNotBlank()) {
        var start = 0
        while (true) {
            val idx = text.indexOf(query, start, ignoreCase = true)
            if (idx == -1) break
            addStyle(
                SpanStyle(background = Color(0xFFFFB300), color = Color.Black, fontWeight = FontWeight.Bold),
                idx,
                idx + query.length
            )
            start = idx + query.length
        }
    }
}

private fun extractAllText(content: OfficeContent?): String {
    return when (content) {
        is OfficeContent.Document -> content.paragraphs.joinToString("\n") { it.text }
        is OfficeContent.Spreadsheet -> content.sheets.joinToString("\n\n") { sheet ->
            "${sheet.name}:\n" + sheet.rows.joinToString("\n") { it.joinToString("\t") }
        }
        is OfficeContent.Presentation -> content.slides.joinToString("\n\n") { slide ->
            "--- Diapositiva ${slide.slideNumber}: ${slide.title} ---\n${slide.fullText}"
        }
        is OfficeContent.FallbackText -> content.text
        null -> ""
    }
}
