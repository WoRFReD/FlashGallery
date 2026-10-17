// Version 1.16 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.flashchip.flashgallery.data.OcrScanResult
import com.flashchip.flashgallery.ui.theme.BgDark
import com.flashchip.flashgallery.ui.theme.BorderDark
import com.flashchip.flashgallery.ui.theme.FlashAmber
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.FlashGreen
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.SurfaceDarkVariant
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import com.flashchip.flashgallery.ui.theme.TextTertiary

@Composable
fun OcrSelectionSheet(
    scanResult: OcrScanResult,
    initialSelectedText: String? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var currentSelectedText by remember(initialSelectedText) {
        mutableStateOf(initialSelectedText ?: scanResult.blocks.firstOrNull()?.text)
    }

    // Aplanar todas las líneas de texto detectadas para mostrarlas individualmente
    val allLines = remember(scanResult) {
        scanResult.blocks.flatMap { it.lines.map { line -> line.text.trim() } }.filter { it.isNotBlank() }
    }

    fun copyToClipboard(text: String, label: String = "Texto") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("FlashGallery OCR", text))
        Toast.makeText(context, "$label copiado al portapapeles: \"$text\"", Toast.LENGTH_SHORT).show()
    }

    fun shareText(text: String) {
        try {
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, text)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Compartir texto detectado")
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Error al compartir: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(SurfaceDark)
                .border(1.dp, BorderDark, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Barra superior del diálogo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(FlashCyan.copy(alpha = 0.15f))
                                .border(1.dp, FlashCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DocumentScanner,
                                contentDescription = null,
                                tint = FlashCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Texto detectado en la foto",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${allLines.size} líneas detectadas por OCR",
                                color = FlashCyan,
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(SurfaceDarkVariant)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tarjeta destacada del texto presionado o seleccionado
                    if (!currentSelectedText.isNullOrBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(FlashCyan.copy(alpha = 0.08f))
                                    .border(1.5.dp, FlashCyan.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "TEXTO SELECCIONADO (CLAVE / CARACTERES):",
                                            color = FlashCyan,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(FlashCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Pulsado", color = FlashCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    SelectionContainer {
                                        Text(
                                            text = currentSelectedText!!,
                                            color = Color.White,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 24.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { copyToClipboard(currentSelectedText!!, "Texto seleccionado") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = FlashCyan)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Copiar clave", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }

                                        OutlinedButton(
                                            onClick = { shareText(currentSelectedText!!) },
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(BorderDark))
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Título de lista de líneas detectadas
                    item {
                        Text(
                            text = "TODAS LAS LÍNEAS DETECTADAS EN LA FOTO:",
                            color = TextTertiary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // Lista de cada una de las líneas encontradas con botón de copiar rápido
                    itemsIndexed(allLines) { index, line ->
                        val isSelected = currentSelectedText == line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) FlashCyan.copy(alpha = 0.12f) else SurfaceDarkVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) FlashCyan else BorderDark.copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { currentSelectedText = line }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SelectionContainer(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = line,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }

                                IconButton(
                                    onClick = { copyToClipboard(line, "Línea") },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(SurfaceDark)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copiar línea",
                                        tint = if (isSelected) FlashCyan else TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Barra inferior de acciones globales
                HorizontalDivider(color = BorderDark.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { copyToClipboard(scanResult.fullText, "Todo el texto") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkVariant)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = FlashCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copiar todo", color = TextPrimary, fontSize = 13.sp)
                    }

                    Button(
                        onClick = { shareText(scanResult.fullText) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDarkVariant)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = FlashCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Compartir todo", color = TextPrimary, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
