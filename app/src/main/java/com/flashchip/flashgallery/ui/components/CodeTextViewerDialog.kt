// Version 1.10 - FlashGallery
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WrapText
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flashchip.flashgallery.model.DocItem
import com.flashchip.flashgallery.ui.theme.BgDark
import com.flashchip.flashgallery.ui.theme.BorderDark
import com.flashchip.flashgallery.ui.theme.FlashAmber
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.SurfaceDarkVariant
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import com.flashchip.flashgallery.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CodeTextViewerDialog(
    doc: DocItem,
    onDismiss: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var lines by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isWordWrap by remember { mutableStateOf(false) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchMatches by remember { mutableStateOf<List<Int>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }

    val lazyListState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()

    // Cargar contenido del archivo de forma asíncrona
    LaunchedEffect(doc.file) {
        isLoading = true
        withContext(Dispatchers.IO) {
            try {
                if (doc.file.exists()) {
                    val fileLines = mutableListOf<String>()
                    doc.file.bufferedReader(Charsets.UTF_8).useLines { sequence ->
                        var count = 0
                        for (line in sequence) {
                            fileLines.add(line)
                            count++
                            if (count >= 10000) {
                                fileLines.add("--- [FlashGallery: Archivo truncado a 10.000 líneas por rendimiento] ---")
                                break
                            }
                        }
                    }
                    lines = fileLines
                } else {
                    lines = listOf("El archivo no existe o no se puede leer.")
                }
            } catch (e: Exception) {
                try {
                    // Intento con charset ISO-8859-1 si falla UTF-8
                    lines = doc.file.readLines(Charsets.ISO_8859_1)
                } catch (e2: Exception) {
                    lines = listOf("Error al leer archivo: ${e.localizedMessage}")
                }
            }
        }
        isLoading = false
    }

    // Actualizar coincidencias de búsqueda
    LaunchedEffect(searchQuery, lines) {
        if (searchQuery.isNotBlank() && lines.isNotEmpty()) {
            val matches = mutableListOf<Int>()
            lines.forEachIndexed { index, line ->
                if (line.contains(searchQuery, ignoreCase = true)) {
                    matches.add(index)
                }
            }
            searchMatches = matches
            currentMatchIndex = if (matches.isNotEmpty()) 0 else -1
            if (matches.isNotEmpty()) {
                lazyListState.scrollToItem(matches[0])
            }
        } else {
            searchMatches = emptyList()
            currentMatchIndex = -1
        }
    }

    fun jumpToMatch(next: Boolean) {
        if (searchMatches.isEmpty()) return
        val newIndex = if (next) {
            (currentMatchIndex + 1) % searchMatches.size
        } else {
            if (currentMatchIndex <= 0) searchMatches.size - 1 else currentMatchIndex - 1
        }
        currentMatchIndex = newIndex
        val targetLine = searchMatches[newIndex]
        coroutineScope.launch {
            lazyListState.animateScrollToItem((targetLine - 3).coerceAtLeast(0))
        }
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
                // TopBar estilo Notepad++ / IDE
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
                                tint = FlashCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(FlashCyan.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = doc.extension.uppercase(),
                                        color = FlashCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = doc.name,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${lines.size} líneas • ${doc.sizeFormatted} • UTF-8",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Acciones de la barra superior
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

                        // Ajuste de línea (Word Wrap)
                        IconButton(
                            onClick = { isWordWrap = !isWordWrap },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WrapText,
                                contentDescription = "Ajuste de línea",
                                tint = if (isWordWrap) FlashCyan else TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Copiar todo
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val allText = lines.joinToString("\n")
                                clipboard.setPrimaryClip(ClipData.newPlainText(doc.name, allText))
                                Toast.makeText(context, "Archivo copiado al portapapeles", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copiar",
                                tint = TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        // Abrir con app externa
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

                // Barra de Búsqueda Desplegable
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
                            keyboardActions = KeyboardActions(onSearch = { jumpToMatch(true) }),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Buscar texto en el archivo...", color = TextTertiary, fontSize = 13.sp)
                                }
                                innerTextField()
                            }
                        )

                        if (searchMatches.isNotEmpty()) {
                            Text(
                                text = "${currentMatchIndex + 1}/${searchMatches.size}",
                                color = FlashAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        } else if (searchQuery.isNotBlank()) {
                            Text(
                                text = "Sin resultados",
                                color = TextTertiary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }

                        IconButton(
                            onClick = { jumpToMatch(false) },
                            enabled = searchMatches.isNotEmpty(),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Anterior",
                                tint = if (searchMatches.isNotEmpty()) FlashCyan else TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = { jumpToMatch(true) },
                            enabled = searchMatches.isNotEmpty(),
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Siguiente",
                                tint = if (searchMatches.isNotEmpty()) FlashCyan else TextTertiary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Contenido del Editor Notepad++
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(BgDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = FlashCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(text = "Cargando documento...", color = TextSecondary, fontSize = 13.sp)
                        }
                    }
                } else {
                    // Cálculo de ancho del canal de números (Gutter)
                    val maxLineDigits = lines.size.toString().length.coerceAtLeast(2)
                    val gutterWidth = (maxLineDigits * 10 + 20).dp

                    SelectionContainer {
                        Row(modifier = Modifier.fillMaxSize()) {
                            // Columna de código y números con LazyColumn para rendimiento instantáneo
                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(BgDark),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                itemsIndexed(lines) { index, rawLine ->
                                    val isMatch = searchMatches.contains(index)
                                    val isCurrentMatch = searchMatches.getOrNull(currentMatchIndex) == index

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isCurrentMatch) FlashCyan.copy(alpha = 0.18f)
                                                else if (isMatch) FlashAmber.copy(alpha = 0.12f)
                                                else Color.Transparent
                                            )
                                    ) {
                                        // Número de línea (Gutter)
                                        Box(
                                            modifier = Modifier
                                                .width(gutterWidth)
                                                .background(SurfaceDarkVariant.copy(alpha = 0.6f))
                                                .border(0.5.dp, BorderDark.copy(alpha = 0.3f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            Text(
                                                text = (index + 1).toString(),
                                                color = if (isCurrentMatch) FlashCyan else TextTertiary,
                                                fontSize = 11.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = if (isCurrentMatch) FontWeight.Bold else FontWeight.Normal,
                                                textAlign = TextAlign.End
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Texto de la línea con Resaltado de Sintaxis
                                        val annotatedText = remember(rawLine, doc.extension, searchQuery) {
                                            SyntaxHighlighter.highlight(rawLine, doc.extension, searchQuery)
                                        }

                                        val lineModifier = if (isWordWrap) {
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(end = 12.dp, top = 2.dp, bottom = 2.dp)
                                        } else {
                                            Modifier
                                                .horizontalScroll(horizontalScrollState)
                                                .padding(end = 24.dp, top = 2.dp, bottom = 2.dp)
                                        }

                                        Text(
                                            text = annotatedText,
                                            fontSize = 12.5.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 18.sp,
                                            modifier = lineModifier
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
}
