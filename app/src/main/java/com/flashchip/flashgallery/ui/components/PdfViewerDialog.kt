// Version 1.23 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.pdf.PdfRenderer
import android.media.MediaScannerConnection
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.flashchip.flashgallery.model.DocItem
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
import com.flashchip.flashgallery.ui.theme.TextTertiary
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerDialog(
    doc: DocItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onOpenExternal: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pfd by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Relación de aspecto por defecto del documento (ancho / alto)
    var defaultAspectRatio by remember(renderer) { mutableFloatStateOf(0.707f) }

    // Estado del listado y página visible en tiempo real durante el scroll vertical
    val listState = rememberLazyListState()
    val currentVisiblePage by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + 1).coerceIn(1, maxOf(1, pageCount))
        }
    }

    // Caché LRU de páginas y Mutex para acceso concurrente seguro a PdfRenderer
    val pageCache = remember { object : LruCache<Int, Bitmap>(12) {} }
    val renderMutex = remember { Mutex() }

    // Estado para contraseñas de PDFs protegidos
    var isPasswordRequired by remember { mutableStateOf(false) }
    var passwordInput by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isUnlocking by remember { mutableStateOf(false) }
    var decryptedTempFile by remember { mutableStateOf<File?>(null) }
    var showShareOptionsDialog by remember { mutableStateOf(false) }
    var isSavingDecryptedCopy by remember { mutableStateOf(false) }

    // Salto rápido de página y Zoom/Pan
    var showJumpSlider by remember { mutableStateOf(false) }
    var boxSize by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Limpieza de recursos al cerrar
    DisposableEffect(Unit) {
        onDispose {
            try { pfd?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { decryptedTempFile?.delete() } catch (_: Exception) {}
            pageCache.evictAll()
        }
    }

    // Pre-leer la proporción de aspecto de la primera página bajo Mutex thread-safe
    LaunchedEffect(renderer) {
        val r = renderer ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                try {
                    val p = r.openPage(0)
                    val ratio = if (p.height > 0) p.width.toFloat() / p.height.toFloat() else 0.707f
                    p.close()
                    withContext(Dispatchers.Main) {
                        defaultAspectRatio = ratio
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // Función para desbloquear PDF con contraseña
    fun unlockPdf(password: String) {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                isUnlocking = true
                passwordError = null
                try {
                    PDFBoxResourceLoader.init(context)

                    val inputStream = try {
                        context.contentResolver.openInputStream(doc.uri)
                    } catch (_: Exception) {
                        null
                    } ?: if (doc.file.exists()) doc.file.inputStream() else null

                    if (inputStream == null) {
                        passwordError = "No se pudo leer el archivo PDF."
                        isUnlocking = false
                        return@withContext
                    }

                    val document = try {
                        PDDocument.load(inputStream, password)
                    } catch (e: InvalidPasswordException) {
                        passwordError = "Contraseña incorrecta. Inténtalo de nuevo."
                        isUnlocking = false
                        try { inputStream.close() } catch (_: Exception) {}
                        return@withContext
                    } catch (e: Exception) {
                        passwordError = "Error al abrir documento: ${e.localizedMessage ?: "Clave inválida"}"
                        isUnlocking = false
                        try { inputStream.close() } catch (_: Exception) {}
                        return@withContext
                    }

                    if (document.isEncrypted) {
                        document.isAllSecurityToBeRemoved = true
                    }

                    val decryptedFile = File(context.cacheDir, "decrypted_${System.currentTimeMillis()}.pdf")
                    document.save(decryptedFile)
                    document.close()
                    try { inputStream.close() } catch (_: Exception) {}

                    val newDescriptor = ParcelFileDescriptor.open(decryptedFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val newRenderer = PdfRenderer(newDescriptor)

                    try { pfd?.close() } catch (_: Exception) {}
                    try { renderer?.close() } catch (_: Exception) {}

                    withContext(Dispatchers.Main) {
                        pageCache.evictAll()
                        pfd = newDescriptor
                        renderer = newRenderer
                        pageCount = newRenderer.pageCount
                        isPasswordRequired = false
                        passwordError = null
                        decryptedTempFile = decryptedFile
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        passwordError = "Error al procesar PDF: ${e.localizedMessage}"
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        isUnlocking = false
                    }
                }
            }
        }
    }

    // Guardar copia limpia sin contraseña en la carpeta pública Documentos
    fun saveDecryptedCopy() {
        val tempFile = decryptedTempFile ?: return
        if (isSavingDecryptedCopy) return
        isSavingDecryptedCopy = true
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val docsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
                if (!docsDir.exists()) {
                    docsDir.mkdirs()
                }
                val rawName = doc.name.substringBeforeLast(".")
                var targetFile = File(docsDir, "${rawName}_sin_clave.pdf")
                var counter = 1
                while (targetFile.exists()) {
                    targetFile = File(docsDir, "${rawName}_sin_clave_${counter}.pdf")
                    counter++
                }
                tempFile.copyTo(targetFile, overwrite = true)
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Copia guardada sin clave en Documentos:\n${targetFile.name}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Error al guardar copia: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } finally {
                isSavingDecryptedCopy = false
            }
        }
    }

    // Compartir el PDF limpio sin contraseña
    fun shareDecryptedPdf() {
        val tempFile = decryptedTempFile ?: return
        try {
            val rawName = doc.name.substringBeforeLast(".")
            val shareFile = File(context.cacheDir, "${rawName}_sin_clave.pdf")
            tempFile.copyTo(shareFile, overwrite = true)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                shareFile
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Compartir PDF sin contraseña"))
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Error al compartir PDF: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Abrir PdfRenderer inicialmente
    LaunchedEffect(doc.uri, doc.file) {
        withContext(Dispatchers.IO) {
            try {
                val descriptor = try {
                    context.contentResolver.openFileDescriptor(doc.uri, "r")
                } catch (_: Exception) {
                    null
                } ?: if (doc.file.exists()) {
                    try {
                        ParcelFileDescriptor.open(doc.file, ParcelFileDescriptor.MODE_READ_ONLY)
                    } catch (_: Exception) {
                        null
                    }
                } else null

                if (descriptor != null) {
                    val pdfRenderer = PdfRenderer(descriptor)
                    pfd = descriptor
                    renderer = pdfRenderer
                    pageCount = pdfRenderer.pageCount
                    isPasswordRequired = false
                    errorMessage = null
                } else {
                    errorMessage = "No se pudo acceder al archivo PDF."
                }
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: ""
                if (e is SecurityException || msg.contains("password", ignoreCase = true) || msg.contains("Password", ignoreCase = true)) {
                    isPasswordRequired = true
                    errorMessage = null
                } else {
                    errorMessage = "No se pudo abrir el PDF: $msg"
                }
            }
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
                // TopBar del visor PDF
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
                                        .background(FlashAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PDF",
                                        color = FlashAmber,
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
                                text = if (pageCount > 0) "Página $currentVisiblePage de $pageCount • ${doc.sizeFormatted}" else doc.sizeFormatted,
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Acciones en la barra superior
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Botón directo para guardar sin clave si el PDF fue desbloqueado
                        if (decryptedTempFile != null) {
                            IconButton(
                                onClick = { saveDecryptedCopy() },
                                modifier = Modifier.size(36.dp),
                                enabled = !isSavingDecryptedCopy
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Guardar copia sin contraseña",
                                    tint = FlashGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                if (decryptedTempFile != null) {
                                    showShareOptionsDialog = true
                                } else {
                                    onShare()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Compartir",
                                tint = if (decryptedTempFile != null) FlashCyan else TextSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }

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

                // Área principal con Scroll Vertical continuo y gestos táctiles
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF0D1117))
                        .onGloballyPositioned { coordinates -> boxSize = coordinates.size }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    if (showJumpSlider) showJumpSlider = false
                                },
                                onDoubleTap = { tapOffset ->
                                    val newScale = if (scale > 1.05f) 1f else 2.2f
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
                                        // Dos dedos: Zoom por pellizco (pinch-to-zoom)
                                        val zoomChange = event.calculateZoom()
                                        val panChange = event.calculatePan()

                                        if (zoomChange != 1f || panChange != Offset.Zero) {
                                            val newScale = (scale * zoomChange).coerceIn(1f, 4f)
                                            scale = newScale

                                            if (newScale > 1.05f && boxSize.width > 0 && boxSize.height > 0) {
                                                val maxX = maxOf(0f, (boxSize.width * (newScale - 1f)) / 2f)
                                                val maxY = maxOf(0f, (boxSize.height * (newScale - 1f)) / 2f)
                                                val newX = (offset.x + panChange.x).coerceIn(-maxX, maxX)
                                                val newY = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                                offset = Offset(newX, newY)
                                            } else {
                                                offset = Offset.Zero
                                                scale = 1f
                                            }

                                            event.changes.forEach { it.consume() }
                                        }
                                    } else if (pointerCount == 1 && scale > 1.05f) {
                                        // Un dedo mientras está en zoom: Pan / Desplazamiento
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
                    if (isPasswordRequired) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .padding(16.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, FlashCyan.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(FlashAmber.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = FlashAmber,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "PDF protegido con contraseña",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Introduce la clave de acceso para desbloquear y leer el documento:",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = {
                                        passwordInput = it
                                        passwordError = null
                                    },
                                    label = { Text("Contraseña del documento") },
                                    singleLine = true,
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            if (passwordInput.isNotEmpty() && !isUnlocking) {
                                                unlockPdf(passwordInput)
                                            }
                                        }
                                    ),
                                    trailingIcon = {
                                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                            Icon(
                                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = if (isPasswordVisible) "Ocultar clave" else "Mostrar clave",
                                                tint = FlashCyan
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = FlashCyan,
                                        unfocusedBorderColor = BorderDark,
                                        cursorColor = FlashCyan,
                                        focusedLabelColor = FlashCyan,
                                        unfocusedLabelColor = TextSecondary,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (passwordError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = passwordError!!,
                                        color = FlashRed,
                                        fontSize = 12.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (passwordInput.isNotEmpty()) {
                                                unlockPdf(passwordInput)
                                            }
                                        },
                                        enabled = passwordInput.isNotEmpty() && !isUnlocking,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = FlashCyan,
                                            contentColor = BgDark
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        if (isUnlocking) {
                                            CircularProgressIndicator(
                                                color = BgDark,
                                                modifier = Modifier.size(20.dp),
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.LockOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Desbloquear",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = onOpenExternal,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = SurfaceDarkVariant,
                                            contentColor = FlashCyan
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Visor externo",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    } else if (errorMessage != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = FlashAmber,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = errorMessage!!,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onOpenExternal,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceDarkVariant,
                                    contentColor = FlashCyan
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Abrir en visor externo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else if (renderer != null && pageCount > 0) {
                        // Desplazamiento continuo vertical de todas las páginas (Scroll arriba / abajo)
                        LazyColumn(
                            state = listState,
                            userScrollEnabled = scale <= 1.05f,
                            contentPadding = PaddingValues(top = 10.dp, bottom = 60.dp, start = 8.dp, end = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    translationX = offset.x
                                    translationY = offset.y
                                }
                        ) {
                            items(pageCount) { pageIndex ->
                                PdfPageItem(
                                    pageIndex = pageIndex,
                                    defaultAspectRatio = defaultAspectRatio,
                                    renderer = renderer,
                                    renderMutex = renderMutex,
                                    pageCache = pageCache
                                )
                            }
                        }

                        // Badge flotante interactivo con indicador de página visible
                        if (pageCount > 1) {
                            Surface(
                                onClick = {
                                    if (pageCount > 3) showJumpSlider = !showJumpSlider
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = SurfaceDark.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, BorderDark),
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$currentVisiblePage / $pageCount",
                                        color = FlashCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (pageCount > 3) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "• Salto rápido",
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Slider flotante de salto rápido para documentos extensos
                        if (showJumpSlider && pageCount > 1) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = SurfaceDark.copy(alpha = 0.96f),
                                border = BorderStroke(1.dp, FlashCyan.copy(alpha = 0.4f)),
                                shadowElevation = 10.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 54.dp, start = 20.dp, end = 20.dp)
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Ir a página $currentVisiblePage de $pageCount",
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        IconButton(
                                            onClick = { showJumpSlider = false },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Cerrar",
                                                tint = TextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Slider(
                                        value = currentVisiblePage.toFloat(),
                                        onValueChange = { targetVal ->
                                            val targetPageIdx = (targetVal.toInt() - 1).coerceIn(0, pageCount - 1)
                                            coroutineScope.launch {
                                                listState.scrollToItem(targetPageIdx)
                                            }
                                        },
                                        valueRange = 1f..pageCount.toFloat(),
                                        steps = (pageCount - 2).coerceAtLeast(0),
                                        colors = SliderDefaults.colors(
                                            thumbColor = FlashCyan,
                                            activeTrackColor = FlashCyan,
                                            inactiveTrackColor = BorderDark
                                        ),
                                        modifier = Modifier.fillMaxWidth().height(26.dp)
                                    )
                                }
                            }
                        }

                        // Botón flotante para restablecer zoom si la vista fue ampliada
                        if (scale > 1.05f) {
                            Surface(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                shape = RoundedCornerShape(16.dp),
                                color = SurfaceDark.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, FlashAmber.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 16.dp, end = 16.dp)
                            ) {
                                Text(
                                    text = "Restablecer zoom (1x)",
                                    color = FlashAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = FlashCyan)
                    }
                }
            }

            // Diálogo de opciones de compartición para PDFs que fueron desbloqueados
            if (showShareOptionsDialog) {
                AlertDialog(
                    onDismissRequest = { showShareOptionsDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = FlashCyan,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Compartir documento PDF",
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Este documento fue desbloqueado con contraseña. ¿Cómo deseas compartirlo?",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )

                            // Opción 1: Sin contraseña
                            Surface(
                                onClick = {
                                    showShareOptionsDialog = false
                                    shareDecryptedPdf()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceDarkVariant,
                                border = BorderStroke(1.dp, FlashGreen.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = FlashGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Sin contraseña (Desbloqueado)",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Genera un PDF limpio listo para abrir directamente",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Opción 2: Con contraseña
                            Surface(
                                onClick = {
                                    showShareOptionsDialog = false
                                    onShare()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceDarkVariant,
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = FlashAmber,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Con contraseña (Original protegido)",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Comparte el archivo original con cifrado de seguridad",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }

                            // Opción 3: Guardar en Documentos
                            Surface(
                                onClick = {
                                    showShareOptionsDialog = false
                                    saveDecryptedCopy()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = SurfaceDarkVariant,
                                border = BorderStroke(1.dp, BorderDark),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = null,
                                        tint = FlashCyan,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Guardar copia en Documentos",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Guarda el PDF sin clave en tu dispositivo",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showShareOptionsDialog = false }) {
                            Text(text = "Cancelar", color = TextSecondary)
                        }
                    },
                    containerColor = SurfaceDark,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

/**
 * Renderizador de página individual con caché LRU y Mutex thread-safe para PdfRenderer
 */
@Composable
private fun PdfPageItem(
    pageIndex: Int,
    defaultAspectRatio: Float,
    renderer: PdfRenderer?,
    renderMutex: Mutex,
    pageCache: LruCache<Int, Bitmap>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(pageIndex, renderer) { mutableStateOf<Bitmap?>(pageCache.get(pageIndex)) }
    var pageAspectRatio by remember(pageIndex, defaultAspectRatio) { mutableFloatStateOf(defaultAspectRatio) }

    LaunchedEffect(pageIndex, renderer) {
        if (bitmap == null && renderer != null) {
            withContext(Dispatchers.IO) {
                val cached = pageCache.get(pageIndex)
                if (cached != null) {
                    withContext(Dispatchers.Main) {
                        bitmap = cached
                        if (cached.height > 0) {
                            pageAspectRatio = cached.width.toFloat() / cached.height.toFloat()
                        }
                    }
                    return@withContext
                }

                renderMutex.withLock {
                    val cachedInside = pageCache.get(pageIndex)
                    if (cachedInside != null) {
                        withContext(Dispatchers.Main) {
                            bitmap = cachedInside
                            if (cachedInside.height > 0) {
                                pageAspectRatio = cachedInside.width.toFloat() / cachedInside.height.toFloat()
                            }
                        }
                        return@withLock
                    }

                    try {
                        val page = renderer.openPage(pageIndex)
                        val screenPx = context.resources.displayMetrics.widthPixels
                        val targetWidth = screenPx.coerceIn(720, 1440)
                        val targetHeight = (targetWidth.toFloat() / page.width * page.height).toInt()
                        val ratio = if (page.height > 0) page.width.toFloat() / page.height.toFloat() else defaultAspectRatio

                        // Android PdfRenderer exige estrictamente Bitmap.Config.ARGB_8888
                        val bmp = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bmp)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()

                        pageCache.put(pageIndex, bmp)
                        withContext(Dispatchers.Main) {
                            pageAspectRatio = ratio
                            bitmap = bmp
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(pageAspectRatio)
            .clip(RoundedCornerShape(4.dp))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Página ${pageIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(
                color = FlashCyan,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }
    }
}
