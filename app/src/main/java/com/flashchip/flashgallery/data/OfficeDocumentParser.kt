// Version 1.15 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.data

import com.flashchip.flashgallery.model.DocItem
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipFile

sealed class OfficeContent {
    /**
     * Documento de texto / Word / Writer (.docx, .doc, .odt, .rtf)
     */
    data class Document(
        val paragraphs: List<DocParagraph>,
        val wordCount: Int,
        val tableCount: Int
    ) : OfficeContent()

    /**
     * Hoja de cálculo / Excel / Calc (.xlsx, .xls, .ods, .csv)
     */
    data class Spreadsheet(
        val sheets: List<SheetData>
    ) : OfficeContent()

    /**
     * Presentación de diapositivas / PowerPoint / Impress (.pptx, .ppt, .odp)
     */
    data class Presentation(
        val slides: List<SlideData>
    ) : OfficeContent()

    /**
     * Vista de texto extraído para formatos binarios heredados (.doc, .xls, .ppt)
     */
    data class FallbackText(
        val text: String,
        val message: String
    ) : OfficeContent()
}

data class DocParagraph(
    val text: String,
    val isHeading: Boolean = false,
    val headingLevel: Int = 0,
    val isListItem: Boolean = false,
    val isTable: Boolean = false,
    val tableRows: List<List<String>> = emptyList()
)

data class SheetData(
    val name: String,
    val rows: List<List<String>>,
    val totalRows: Int,
    val totalCols: Int
)

data class SlideData(
    val slideNumber: Int,
    val title: String,
    val bullets: List<String>,
    val fullText: String
)

object OfficeDocumentParser {

    fun parse(doc: DocItem): OfficeContent {
        val file = doc.file
        if (!file.exists() || !file.canRead()) {
            return OfficeContent.FallbackText(
                text = "",
                message = "El archivo no existe o no tiene permisos de lectura."
            )
        }

        val ext = doc.extension.lowercase()
        return try {
            when (ext) {
                "docx" -> parseDocx(file)
                "xlsx" -> parseXlsx(file)
                "pptx" -> parsePptx(file)
                "odt" -> parseOdt(file)
                "ods" -> parseOds(file)
                "odp" -> parseOdp(file)
                "csv" -> parseCsv(file)
                "rtf" -> parseRtf(file)
                "doc", "xls", "ppt" -> parseLegacyBinary(file, ext)
                else -> parseGenericText(file)
            }
        } catch (e: Exception) {
            parseLegacyBinary(file, ext, e.localizedMessage)
        }
    }

    /**
     * Parser para Microsoft Word moderno (.docx)
     * Descomprime word/document.xml y extrae títulos, párrafos y tablas.
     */
    private fun parseDocx(file: File): OfficeContent {
        ZipFile(file).use { zip ->
            val docEntry = zip.getEntry("word/document.xml")
                ?: return parseLegacyBinary(file, "docx", "No se encontró word/document.xml")

            zip.getInputStream(docEntry).use { input ->
                val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                val parser = factory.newPullParser()
                parser.setInput(input, "UTF-8")

                val paragraphs = mutableListOf<DocParagraph>()
                var totalWords = 0
                var tableCount = 0

                var inParagraph = false
                var inTable = false
                var inTableRow = false
                var inTableCell = false

                var currentHeadingLevel = 0
                var isListItem = false
                val currentParaText = StringBuilder()
                val currentTableRows = mutableListOf<List<String>>()
                val currentRowCells = mutableListOf<String>()
                val currentCellText = StringBuilder()

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val tag = parser.name?.lowercase() ?: ""

                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (tag) {
                                "p" -> {
                                    inParagraph = true
                                    currentHeadingLevel = 0
                                    isListItem = false
                                    currentParaText.clear()
                                }
                                "pstyle" -> {
                                    val valAttr = parser.getAttributeValue(null, "val")
                                        ?: parser.getAttributeValue("http://schemas.openxmlformats.org/wordprocessingml/2006/main", "val")
                                        ?: ""
                                    val lowerVal = valAttr.lowercase()
                                    if (lowerVal.contains("heading1") || lowerVal.contains("title") || lowerVal.contains("título1")) {
                                        currentHeadingLevel = 1
                                    } else if (lowerVal.contains("heading2") || lowerVal.contains("título2")) {
                                        currentHeadingLevel = 2
                                    } else if (lowerVal.contains("heading3") || lowerVal.contains("título3")) {
                                        currentHeadingLevel = 3
                                    }
                                }
                                "numpr" -> isListItem = true
                                "tbl" -> {
                                    inTable = true
                                    currentTableRows.clear()
                                }
                                "tr" -> {
                                    inTableRow = true
                                    currentRowCells.clear()
                                }
                                "tc" -> {
                                    inTableCell = true
                                    currentCellText.clear()
                                }
                                "t" -> {
                                    val text = parser.nextText()
                                    if (inTableCell) {
                                        currentCellText.append(text)
                                    } else if (inParagraph) {
                                        currentParaText.append(text)
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            when (tag) {
                                "p" -> {
                                    inParagraph = false
                                    if (!inTableCell) {
                                        val text = currentParaText.toString().trim()
                                        if (text.isNotEmpty()) {
                                            paragraphs.add(
                                                DocParagraph(
                                                    text = text,
                                                    isHeading = currentHeadingLevel > 0,
                                                    headingLevel = currentHeadingLevel,
                                                    isListItem = isListItem
                                                )
                                            )
                                            totalWords += text.split(Regex("\\s+")).size
                                        }
                                    }
                                }
                                "tc" -> {
                                    inTableCell = false
                                    currentRowCells.add(currentCellText.toString().trim())
                                }
                                "tr" -> {
                                    inTableRow = false
                                    if (currentRowCells.isNotEmpty()) {
                                        currentTableRows.add(currentRowCells.toList())
                                    }
                                }
                                "tbl" -> {
                                    inTable = false
                                    if (currentTableRows.isNotEmpty()) {
                                        tableCount++
                                        paragraphs.add(
                                            DocParagraph(
                                                text = "Tabla (${currentTableRows.size} filas)",
                                                isTable = true,
                                                tableRows = currentTableRows.toList()
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }

                if (paragraphs.isEmpty()) {
                    return parseGenericText(file)
                }

                return OfficeContent.Document(
                    paragraphs = paragraphs,
                    wordCount = totalWords,
                    tableCount = tableCount
                )
            }
        }
    }

    /**
     * Parser para Microsoft Excel moderno (.xlsx)
     * Extrae strings compartidas de xl/sharedStrings.xml y celdas de las hojas.
     */
    private fun parseXlsx(file: File): OfficeContent {
        ZipFile(file).use { zip ->
            // 1. Extraer tabla de cadenas compartidas
            val sharedStrings = mutableListOf<String>()
            val sharedEntry = zip.getEntry("xl/sharedStrings.xml")
            if (sharedEntry != null) {
                zip.getInputStream(sharedEntry).use { input ->
                    val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                    val parser = factory.newPullParser()
                    parser.setInput(input, "UTF-8")

                    var inT = false
                    val curStr = StringBuilder()
                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val name = parser.name?.lowercase() ?: ""
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (name == "t") inT = true
                                else if (name == "si") curStr.clear()
                            }
                            XmlPullParser.TEXT -> {
                                if (inT) curStr.append(parser.text)
                            }
                            XmlPullParser.END_TAG -> {
                                if (name == "t") inT = false
                                else if (name == "si") sharedStrings.add(curStr.toString())
                            }
                        }
                        eventType = parser.next()
                    }
                }
            }

            // 2. Extraer hojas de cálculo
            val sheets = mutableListOf<SheetData>()
            var sheetIndex = 1

            while (true) {
                val sheetEntry = zip.getEntry("xl/worksheets/sheet$sheetIndex.xml") ?: break
                zip.getInputStream(sheetEntry).use { input ->
                    val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                    val parser = factory.newPullParser()
                    parser.setInput(input, "UTF-8")

                    val rows = mutableListOf<List<String>>()
                    val currentRow = mutableListOf<String>()
                    var isSharedString = false
                    var inValue = false
                    var curCellVal = StringBuilder()

                    var eventType = parser.eventType
                    val maxRowsToParse = 500 // Límite seguro para visualización instantánea

                    while (eventType != XmlPullParser.END_DOCUMENT && rows.size < maxRowsToParse) {
                        val name = parser.name?.lowercase() ?: ""
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                when (name) {
                                    "row" -> currentRow.clear()
                                    "c" -> {
                                        val type = parser.getAttributeValue(null, "t") ?: ""
                                        isSharedString = (type == "s")
                                        curCellVal.clear()
                                    }
                                    "v" -> inValue = true
                                    "t" -> inValue = true
                                }
                            }
                            XmlPullParser.TEXT -> {
                                if (inValue) curCellVal.append(parser.text)
                            }
                            XmlPullParser.END_TAG -> {
                                when (name) {
                                    "v", "t" -> inValue = false
                                    "c" -> {
                                        val rawVal = curCellVal.toString().trim()
                                        val cellDisplay = if (isSharedString) {
                                            val idx = rawVal.toIntOrNull()
                                            if (idx != null && idx >= 0 && idx < sharedStrings.size) {
                                                sharedStrings[idx]
                                            } else rawVal
                                        } else {
                                            rawVal
                                        }
                                        currentRow.add(cellDisplay)
                                    }
                                    "row" -> {
                                        if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                            rows.add(currentRow.toList())
                                        }
                                    }
                                }
                            }
                        }
                        eventType = parser.next()
                    }

                    val maxCols = rows.maxOfOrNull { it.size } ?: 0
                    sheets.add(
                        SheetData(
                            name = "Hoja $sheetIndex",
                            rows = rows,
                            totalRows = rows.size,
                            totalCols = maxCols
                        )
                    )
                }
                sheetIndex++
            }

            if (sheets.isEmpty()) {
                return parseGenericText(file)
            }

            return OfficeContent.Spreadsheet(sheets = sheets)
        }
    }

    /**
     * Parser para Microsoft PowerPoint moderno (.pptx)
     * Extrae texto de ppt/slides/slide*.xml.
     */
    private fun parsePptx(file: File): OfficeContent {
        ZipFile(file).use { zip ->
            val slides = mutableListOf<SlideData>()
            var slideNum = 1

            while (true) {
                val slideEntry = zip.getEntry("ppt/slides/slide$slideNum.xml") ?: break
                zip.getInputStream(slideEntry).use { input ->
                    val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                    val parser = factory.newPullParser()
                    parser.setInput(input, "UTF-8")

                    val paragraphs = mutableListOf<String>()
                    val curPara = StringBuilder()
                    var inText = false

                    var eventType = parser.eventType
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        val name = parser.name?.lowercase() ?: ""
                        when (eventType) {
                            XmlPullParser.START_TAG -> {
                                if (name == "p") curPara.clear()
                                else if (name == "t") inText = true
                            }
                            XmlPullParser.TEXT -> {
                                if (inText) curPara.append(parser.text)
                            }
                            XmlPullParser.END_TAG -> {
                                if (name == "t") inText = false
                                else if (name == "p") {
                                    val text = curPara.toString().trim()
                                    if (text.isNotBlank()) paragraphs.add(text)
                                }
                            }
                        }
                        eventType = parser.next()
                    }

                    val title = paragraphs.firstOrNull() ?: "Diapositiva $slideNum"
                    val bullets = if (paragraphs.size > 1) paragraphs.subList(1, paragraphs.size) else emptyList()

                    slides.add(
                        SlideData(
                            slideNumber = slideNum,
                            title = title,
                            bullets = bullets,
                            fullText = paragraphs.joinToString("\n")
                        )
                    )
                }
                slideNum++
            }

            if (slides.isEmpty()) {
                return parseGenericText(file)
            }

            return OfficeContent.Presentation(slides = slides)
        }
    }

    /**
     * Parser para OpenDocument Text (.odt)
     */
    private fun parseOdt(file: File): OfficeContent {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("content.xml")
                ?: return parseLegacyBinary(file, "odt", "No se encontró content.xml")

            zip.getInputStream(entry).use { input ->
                val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                val parser = factory.newPullParser()
                parser.setInput(input, "UTF-8")

                val paragraphs = mutableListOf<DocParagraph>()
                var totalWords = 0
                val curText = StringBuilder()
                var isHeading = false
                var inPara = false

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val name = parser.name?.lowercase() ?: ""
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (name == "h") {
                                isHeading = true
                                inPara = true
                                curText.clear()
                            } else if (name == "p") {
                                isHeading = false
                                inPara = true
                                curText.clear()
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inPara) curText.append(parser.text)
                        }
                        XmlPullParser.END_TAG -> {
                            if (name == "h" || name == "p") {
                                val text = curText.toString().trim()
                                if (text.isNotBlank()) {
                                    paragraphs.add(
                                        DocParagraph(
                                            text = text,
                                            isHeading = isHeading,
                                            headingLevel = if (isHeading) 1 else 0
                                        )
                                    )
                                    totalWords += text.split(Regex("\\s+")).size
                                }
                                inPara = false
                            }
                        }
                    }
                    eventType = parser.next()
                }

                return OfficeContent.Document(
                    paragraphs = paragraphs,
                    wordCount = totalWords,
                    tableCount = 0
                )
            }
        }
    }

    /**
     * Parser para OpenDocument Spreadsheet (.ods)
     */
    private fun parseOds(file: File): OfficeContent {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("content.xml")
                ?: return parseLegacyBinary(file, "ods", "No se encontró content.xml")

            zip.getInputStream(entry).use { input ->
                val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                val parser = factory.newPullParser()
                parser.setInput(input, "UTF-8")

                val sheets = mutableListOf<SheetData>()
                var currentSheetName = "Hoja 1"
                val currentRows = mutableListOf<List<String>>()
                val currentRow = mutableListOf<String>()
                val curCell = StringBuilder()
                var inCell = false

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val name = parser.name?.lowercase() ?: ""
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            when (name) {
                                "table" -> {
                                    currentSheetName = parser.getAttributeValue(null, "name") ?: "Hoja ${sheets.size + 1}"
                                    currentRows.clear()
                                }
                                "table-row" -> currentRow.clear()
                                "table-cell" -> {
                                    inCell = true
                                    curCell.clear()
                                }
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inCell) curCell.append(parser.text)
                        }
                        XmlPullParser.END_TAG -> {
                            when (name) {
                                "table-cell" -> {
                                    inCell = false
                                    currentRow.add(curCell.toString().trim())
                                }
                                "table-row" -> {
                                    if (currentRow.isNotEmpty() && currentRow.any { it.isNotBlank() }) {
                                        currentRows.add(currentRow.toList())
                                    }
                                }
                                "table" -> {
                                    if (currentRows.isNotEmpty()) {
                                        sheets.add(
                                            SheetData(
                                                name = currentSheetName,
                                                rows = currentRows.toList(),
                                                totalRows = currentRows.size,
                                                totalCols = currentRows.maxOfOrNull { it.size } ?: 0
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    eventType = parser.next()
                }

                return OfficeContent.Spreadsheet(sheets = sheets)
            }
        }
    }

    /**
     * Parser para OpenDocument Presentation (.odp)
     */
    private fun parseOdp(file: File): OfficeContent {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry("content.xml")
                ?: return parseLegacyBinary(file, "odp", "No se encontró content.xml")

            zip.getInputStream(entry).use { input ->
                val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
                val parser = factory.newPullParser()
                parser.setInput(input, "UTF-8")

                val slides = mutableListOf<SlideData>()
                var slideNum = 1
                val curSlideParas = mutableListOf<String>()
                val curText = StringBuilder()
                var inPara = false

                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val name = parser.name?.lowercase() ?: ""
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (name == "page") {
                                curSlideParas.clear()
                            } else if (name == "p") {
                                inPara = true
                                curText.clear()
                            }
                        }
                        XmlPullParser.TEXT -> {
                            if (inPara) curText.append(parser.text)
                        }
                        XmlPullParser.END_TAG -> {
                            if (name == "p") {
                                inPara = false
                                val t = curText.toString().trim()
                                if (t.isNotBlank()) curSlideParas.add(t)
                            } else if (name == "page") {
                                val title = curSlideParas.firstOrNull() ?: "Diapositiva $slideNum"
                                val bullets = if (curSlideParas.size > 1) curSlideParas.subList(1, curSlideParas.size) else emptyList()
                                slides.add(
                                    SlideData(
                                        slideNumber = slideNum,
                                        title = title,
                                        bullets = bullets,
                                        fullText = curSlideParas.joinToString("\n")
                                    )
                                )
                                slideNum++
                            }
                        }
                    }
                    eventType = parser.next()
                }

                return OfficeContent.Presentation(slides = slides)
            }
        }
    }

    /**
     * Parser para archivos CSV (separados por coma, punto y coma o tabulación)
     */
    private fun parseCsv(file: File): OfficeContent {
        val lines = file.bufferedReader().readLines()
        if (lines.isEmpty()) {
            return OfficeContent.Spreadsheet(listOf(SheetData("CSV", emptyList(), 0, 0)))
        }

        // Detección automática del delimitador
        val sample = lines.firstOrNull() ?: ""
        val delimiter = when {
            sample.count { it == ';' } > sample.count { it == ',' } -> ";"
            sample.count { it == '\t' } > sample.count { it == ',' } -> "\t"
            else -> ","
        }

        val rows = lines.take(600).map { line ->
            line.split(delimiter).map { it.trim().removeSurrounding("\"") }
        }

        val maxCols = rows.maxOfOrNull { it.size } ?: 0
        return OfficeContent.Spreadsheet(
            sheets = listOf(
                SheetData(
                    name = file.nameWithoutExtension,
                    rows = rows,
                    totalRows = rows.size,
                    totalCols = maxCols
                )
            )
        )
    }

    /**
     * Parser ligero para RTF (Rich Text Format)
     */
    private fun parseRtf(file: File): OfficeContent {
        val raw = file.readText(Charsets.ISO_8859_1)
        val clean = raw
            .replace(Regex("\\\\[a-zA-Z]+-?\\d* ?"), " ") // Eliminar comandos RTF (\b, \par, etc.)
            .replace(Regex("[{}\\\\]"), "")               // Eliminar llaves y barras
            .trim()

        val paragraphs = clean.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .map { DocParagraph(text = it) }

        return OfficeContent.Document(
            paragraphs = paragraphs,
            wordCount = clean.split(Regex("\\s+")).size,
            tableCount = 0
        )
    }

    /**
     * Para formatos binarios (.doc, .xls, .ppt) donde se extraen secuencias legibles
     * y se ofrece botón directo de apertura externa.
     */
    private fun parseLegacyBinary(file: File, ext: String, errorReason: String? = null): OfficeContent {
        val extracted = extractAsciiStrings(file, minLength = 4, maxStrings = 300)
        val explanation = when (ext) {
            "doc" -> "Documento de Microsoft Word heredado (.doc). Puedes leer el texto extraído o usar 'Abrir con...' para verlo con formato completo en Word o WPS Office."
            "xls" -> "Libro de Excel heredado (.xls). Usa 'Abrir con...' para interactuar con tablas y fórmulas en Excel o WPS Office."
            "ppt" -> "Presentación de PowerPoint heredada (.ppt). Usa 'Abrir con...' para reproducir las diapositivas en PowerPoint o WPS Office."
            else -> "Documento ${ext.uppercase()}. Pulsa 'Abrir con...' para abrirlo en la suite de Office instalada."
        }

        return OfficeContent.FallbackText(
            text = extracted.joinToString("\n\n"),
            message = errorReason?.let { "$explanation\n(Detalle: $it)" } ?: explanation
        )
    }

    private fun parseGenericText(file: File): OfficeContent {
        val lines = file.bufferedReader().useLines { it.take(300).toList() }
        val paragraphs = lines.map { DocParagraph(text = it) }
        return OfficeContent.Document(
            paragraphs = paragraphs,
            wordCount = lines.sumOf { it.split(Regex("\\s+")).size },
            tableCount = 0
        )
    }

    /**
     * Extrae texto ASCII/UTF-8 imprimible de archivos binarios para previsualización inmediata.
     */
    private fun extractAsciiStrings(file: File, minLength: Int = 4, maxStrings: Int = 300): List<String> {
        val result = mutableListOf<String>()
        val bytes = try {
            file.inputStream().use { stream ->
                val buffer = ByteArray(256 * 1024) // Primeros 256KB
                val read = stream.read(buffer)
                if (read > 0) buffer.copyOf(read) else ByteArray(0)
            }
        } catch (_: Exception) {
            return emptyList()
        }

        val current = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (c in ' '..'~' || c in 'á'..'ú' || c in 'Á'..'Ú' || c == 'ñ' || c == 'Ñ' || c == '\n') {
                current.append(c)
            } else {
                if (current.length >= minLength) {
                    val s = current.toString().trim()
                    if (s.length >= minLength && !s.all { it.isDigit() || it.isWhitespace() }) {
                        result.add(s)
                        if (result.size >= maxStrings) break
                    }
                }
                current.clear()
            }
        }

        return result
    }
}
