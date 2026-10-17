// Version 1.16 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class RecognizedTextElement(
    val text: String,
    val boundingBox: Rect? = null
)

data class RecognizedTextLine(
    val text: String,
    val elements: List<RecognizedTextElement>,
    val boundingBox: Rect? = null
)

data class RecognizedTextBlock(
    val text: String,
    val lines: List<RecognizedTextLine>,
    val boundingBox: Rect? = null
)

data class OcrScanResult(
    val fullText: String,
    val blocks: List<RecognizedTextBlock>,
    val imageWidth: Int,
    val imageHeight: Int
)

object OcrScanner {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun recognizeText(context: Context, uri: Uri): OcrScanResult = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val blocks = visionText.textBlocks.map { block ->
                            RecognizedTextBlock(
                                text = block.text,
                                lines = block.lines.map { line ->
                                    RecognizedTextLine(
                                        text = line.text,
                                        elements = line.elements.map { el ->
                                            RecognizedTextElement(
                                                text = el.text,
                                                boundingBox = el.boundingBox
                                            )
                                        },
                                        boundingBox = line.boundingBox
                                    )
                                },
                                boundingBox = block.boundingBox
                            )
                        }
                        continuation.resume(
                            OcrScanResult(
                                fullText = visionText.text,
                                blocks = blocks,
                                imageWidth = inputImage.width,
                                imageHeight = inputImage.height
                            )
                        )
                    }
                    .addOnFailureListener { error ->
                        continuation.resumeWithException(error)
                    }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    /**
     * Busca la palabra o línea más cercana o que contiene el punto tocado (x, y) en coordenadas de la imagen.
     */
    fun findElementAt(result: OcrScanResult, imageX: Float, imageY: Float): String? {
        val x = imageX.toInt()
        val y = imageY.toInt()

        // 1. Coincidencia exacta a nivel de elemento (palabra)
        for (block in result.blocks) {
            for (line in block.lines) {
                for (el in line.elements) {
                    val box = el.boundingBox
                    if (box != null && box.contains(x, y)) {
                        return el.text
                    }
                }
            }
        }

        // 2. Coincidencia exacta a nivel de línea
        for (block in result.blocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null && box.contains(x, y)) {
                    return line.text
                }
            }
        }

        // 3. Coincidencia a nivel de bloque
        for (block in result.blocks) {
            val box = block.boundingBox
            if (box != null && box.contains(x, y)) {
                return block.text
            }
        }

        // 4. Si no cayó exactamente dentro, buscar la línea más cercana (margen de tolerancia de 60px)
        var closestText: String? = null
        var minDistance = Double.MAX_VALUE

        for (block in result.blocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                if (box != null) {
                    val cx = box.centerX().toDouble()
                    val cy = box.centerY().toDouble()
                    val dist = Math.hypot(x - cx, y - cy)
                    if (dist < minDistance) {
                        minDistance = dist
                        closestText = line.text
                    }
                }
            }
        }

        return closestText
    }
}
