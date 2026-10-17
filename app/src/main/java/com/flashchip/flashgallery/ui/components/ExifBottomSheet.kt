// Version 1.06 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flashchip.flashgallery.model.ExifMetadata
import com.flashchip.flashgallery.ui.theme.BgDark
import com.flashchip.flashgallery.ui.theme.BorderDark
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.SurfaceDark
import com.flashchip.flashgallery.ui.theme.SurfaceDarkVariant
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import com.flashchip.flashgallery.ui.theme.TextTertiary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifBottomSheet(
    exif: ExifMetadata?,
    onDismiss: () -> Unit
) {
    if (exif == null) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceDark,
        dragHandle = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(BorderDark)
                )
            }
        }
    ) {
        ExifDetailContent(exif = exif, onDismiss = onDismiss)
    }
}

/**
 * Contenido altamente detallado con metadatos EXIF, parametros fotograficos y archivo
 */
@Composable
fun ExifDetailContent(
    exif: ExifMetadata,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(scrollState)
    ) {
        // Cabecera con titulo y boton de cerrar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Detalles y Metadatos EXIF",
                    color = FlashCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (exif.isVideo) "Información detallada de vídeo" else "Información fotográfica completa",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = TextSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN 1: ARCHIVO Y FORMATO
        ExifSectionTitle(title = "ARCHIVO Y FORMATO")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDarkVariant)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            ExifDetailRow(
                icon = if (exif.isVideo) Icons.Default.Videocam else Icons.Default.Image,
                title = "Nombre del archivo",
                value = exif.fileName.ifBlank { "Sin nombre" }
            )
            HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
            ExifDetailRow(
                icon = Icons.Default.CalendarMonth,
                title = "Fecha y hora exacta",
                value = exif.dateFormatted
            )
            HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
            ExifDetailRow(
                icon = Icons.Default.Image,
                title = "Resolución",
                value = exif.resolution
            )
            HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
            ExifDetailRow(
                icon = Icons.Default.Storage,
                title = "Tamaño en almacenamiento",
                value = if (exif.exactBytes > 0) "${exif.sizeFormatted} (${String.format("%,d", exif.exactBytes)} bytes)" else exif.sizeFormatted
            )
            if (exif.isVideo && !exif.videoDurationFormatted.isNullOrBlank()) {
                HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                ExifDetailRow(
                    icon = Icons.Default.Videocam,
                    title = "Duración de reproducción",
                    value = exif.videoDurationFormatted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // SECCIÓN 2: CÁMARA Y DISPOSITIVO
        if (!exif.cameraModel.isNullOrBlank() || !exif.cameraMake.isNullOrBlank()) {
            ExifSectionTitle(title = "CÁMARA Y DISPOSITIVO")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDarkVariant)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                if (!exif.cameraMake.isNullOrBlank()) {
                    ExifDetailRow(
                        icon = Icons.Default.CameraAlt,
                        title = "Fabricante",
                        value = exif.cameraMake
                    )
                }
                if (!exif.cameraModel.isNullOrBlank()) {
                    if (!exif.cameraMake.isNullOrBlank()) {
                        HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                    }
                    ExifDetailRow(
                        icon = Icons.Default.PhotoCamera,
                        title = "Modelo de cámara",
                        value = exif.cameraModel
                    )
                }
                if (!exif.software.isNullOrBlank()) {
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                    ExifDetailRow(
                        icon = Icons.Default.CameraAlt,
                        title = "Software de procesado",
                        value = exif.software
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECCIÓN 3: PARÁMETROS DE DISPARO
        val hasSettings = !exif.fNumber.isNullOrBlank() || !exif.exposureTime.isNullOrBlank() ||
                !exif.iso.isNullOrBlank() || !exif.focalLength.isNullOrBlank() ||
                !exif.flash.isNullOrBlank() || !exif.whiteBalance.isNullOrBlank()

        if (hasSettings) {
            ExifSectionTitle(title = "PARÁMETROS DE DISPARO")
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceDarkVariant)
                    .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                if (!exif.fNumber.isNullOrBlank()) {
                    ExifDetailRow(
                        icon = Icons.Default.PhotoCamera,
                        title = "Apertura del diafragma",
                        value = exif.fNumber
                    )
                }
                if (!exif.exposureTime.isNullOrBlank()) {
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                    ExifDetailRow(
                        icon = Icons.Default.PhotoCamera,
                        title = "Tiempo de exposición",
                        value = exif.exposureTime
                    )
                }
                if (!exif.iso.isNullOrBlank()) {
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                    ExifDetailRow(
                        icon = Icons.Default.PhotoCamera,
                        title = "Sensibilidad ISO",
                        value = "ISO ${exif.iso}"
                    )
                }
                if (!exif.focalLength.isNullOrBlank()) {
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                    val focalText = if (!exif.focalLength35mm.isNullOrBlank()) {
                        "${exif.focalLength} (${exif.focalLength35mm})"
                    } else {
                        exif.focalLength
                    }
                    ExifDetailRow(
                        icon = Icons.Default.PhotoCamera,
                        title = "Distancia focal",
                        value = focalText
                    )
                }
                if (!exif.flash.isNullOrBlank()) {
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                    ExifDetailRow(
                        icon = Icons.Default.FlashOn,
                        title = "Modo de flash",
                        value = exif.flash
                    )
                }
                if (!exif.whiteBalance.isNullOrBlank()) {
                    HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp))
                    ExifDetailRow(
                        icon = Icons.Default.WbSunny,
                        title = "Balance de blancos",
                        value = exif.whiteBalance
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // SECCIÓN 4: UBICACIÓN Y RUTA
        ExifSectionTitle(title = "UBICACIÓN Y RUTA EN DISCO")
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceDarkVariant)
                .border(1.dp, BorderDark, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            ExifDetailRow(
                icon = Icons.Default.Folder,
                title = "Ruta completa del archivo",
                value = exif.path
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Ruta de archivo", exif.path))
                    Toast.makeText(context, "Ruta copiada al portapapeles", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FlashCyan),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Copiar ruta", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }

            if (!exif.gpsCoordinates.isNullOrBlank() && exif.latitude != null && exif.longitude != null) {
                HorizontalDivider(color = BorderDark.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 8.dp))
                ExifDetailRow(
                    icon = Icons.Default.LocationOn,
                    title = "Coordenadas GPS",
                    value = "${exif.gpsCoordinates} (${exif.latitude}, ${exif.longitude})"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        try {
                            val mapUri = Uri.parse("geo:${exif.latitude},${exif.longitude}?q=${exif.latitude},${exif.longitude}(Foto)")
                            val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {
                            Toast.makeText(context, "No hay aplicación de mapas instalada", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = FlashCyan, contentColor = BgDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Ver ubicación en Google Maps", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun ExifSectionTitle(title: String) {
    Text(
        text = title,
        color = FlashCyan,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp)
    )
}

@Composable
private fun ExifDetailRow(
    icon: ImageVector,
    title: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(SurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = FlashCyan,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
