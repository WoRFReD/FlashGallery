// Version 1.10 - FlashGallery
// Developed for FlashChip by WoRFReD

package com.flashchip.flashgallery.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.flashchip.flashgallery.ui.theme.FlashAmber
import com.flashchip.flashgallery.ui.theme.FlashCyan
import com.flashchip.flashgallery.ui.theme.FlashGreen
import com.flashchip.flashgallery.ui.theme.FlashPurple
import com.flashchip.flashgallery.ui.theme.FlashRed
import com.flashchip.flashgallery.ui.theme.TextPrimary
import com.flashchip.flashgallery.ui.theme.TextSecondary
import java.util.regex.Pattern

object SyntaxHighlighter {

    private val commentColor = Color(0xFF7CA668) // Verde clásico Notepad++
    private val stringColor = Color(0xFFCE9178)  // Naranja/Coral suave para cadenas
    private val numberColor = FlashAmber         // Ámbar para números
    private val keywordColor = FlashPurple       // Morado/Violeta para palabras clave
    private val tagColor = FlashCyan             // Cyan brillante para tags
    private val attrColor = Color(0xFF9CDCFE)    // Azul claro / celeste para atributos
    private val searchMatchBg = Color(0xFFFFB300)

    // Regex para XML / HTML
    private val xmlCommentPattern = Pattern.compile("<!--.*?-->")
    private val xmlProcInstPattern = Pattern.compile("<\\?.*?\\?>|<!DOCTYPE.*?>")
    private val xmlTagPattern = Pattern.compile("</?[a-zA-Z0-9_:\\-]+|/?>")
    private val xmlAttrPattern = Pattern.compile("([a-zA-Z0-9_:\\-]+)(?=\\s*=)")
    private val xmlStringPattern = Pattern.compile("\"[^\"]*\"|'[^']*'")
    private val xmlEntityPattern = Pattern.compile("&[a-zA-Z0-9#]+;")

    // Regex para JSON
    private val jsonKeyPattern = Pattern.compile("\"([^\"]+)\"\\s*:")
    private val jsonStringPattern = Pattern.compile(":\\s*\"([^\"]*)\"")
    private val jsonNumberPattern = Pattern.compile(":\\s*(-?\\d+(\\.\\d+)?([eE][+-]?\\d+)?)")
    private val jsonBoolNullPattern = Pattern.compile(":\\s*(true|false|null)\\b")

    // Regex para Código y Scripts
    private val codeCommentPattern = Pattern.compile("(//.*|/\\*.*?\\*/|#.*|rem\\b.*|::.*)", Pattern.CASE_INSENSITIVE)
    private val codeStringPattern = Pattern.compile("\"(\\\\.|[^\"])*\"|'(\\\\.|[^'])*'")
    private val codeKeywordsPattern = Pattern.compile(
        "\\b(fun|val|var|class|interface|object|import|package|return|if|else|when|while|for|in|is|try|catch|finally|throw|new|public|private|protected|internal|override|def|lambda|echo|set|exit|goto|call|select|from|where|insert|update|delete|function|const|let|async|await|switch|case|break|continue|default|typeof|null|true|false)\\b"
    )
    private val codeNumberPattern = Pattern.compile("\\b(\\d+(\\.\\d+)?|0x[0-9a-fA-F]+)\\b")
    private val codeFunctionPattern = Pattern.compile("\\b([a-zA-Z0-9_]+)(?=\\s*\\()")

    // Regex para Logs y Configs
    private val logSuccessPattern = Pattern.compile("\\[(INFO|OK|SUCCESS|DEBUG)\\]", Pattern.CASE_INSENSITIVE)
    private val logErrorPattern = Pattern.compile("\\[(ERROR|FATAL|FAIL|EXCEPTION)\\]|Exception\\b", Pattern.CASE_INSENSITIVE)
    private val logWarnPattern = Pattern.compile("\\[(WARN|WARNING)\\]", Pattern.CASE_INSENSITIVE)
    private val configSectionPattern = Pattern.compile("^\\[[^\\]]+\\]")
    private val configKeyPattern = Pattern.compile("^([a-zA-Z0-9_.-]+)(?=\\s*=)")
    private val mdHeaderPattern = Pattern.compile("^#{1,6}\\s+.*")

    fun highlight(line: String, extension: String, searchQuery: String = ""): AnnotatedString {
        val ext = extension.lowercase()
        val builder = AnnotatedString.Builder(line)

        // Estilo base
        builder.addStyle(SpanStyle(color = TextPrimary), 0, line.length)

        when (ext) {
            "xml", "html", "htm", "svg" -> highlightXml(builder, line)
            "json" -> highlightJson(builder, line)
            "kt", "kts", "java", "js", "ts", "py", "sh", "bat", "cmd", "sql", "css", "c", "cpp", "h" -> highlightCode(builder, line)
            "log", "txt", "ini", "conf", "properties", "cfg", "md" -> highlightLogOrConfig(builder, line)
            else -> highlightCode(builder, line)
        }

        // Resaltar coincidencias de búsqueda si existen
        if (searchQuery.isNotBlank()) {
            var startIndex = 0
            while (true) {
                val index = line.indexOf(searchQuery, startIndex, ignoreCase = true)
                if (index == -1) break
                builder.addStyle(
                    SpanStyle(
                        background = searchMatchBg,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    ),
                    index,
                    index + searchQuery.length
                )
                startIndex = index + searchQuery.length
            }
        }

        return builder.toAnnotatedString()
    }

    private fun highlightXml(builder: AnnotatedString.Builder, line: String) {
        // Comments (<!-- ... -->)
        val commentMatcher = xmlCommentPattern.matcher(line)
        while (commentMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = commentColor, fontStyle = FontStyle.Italic),
                commentMatcher.start(),
                commentMatcher.end()
            )
        }

        // Processing instructions (<?xml ... ?>)
        val procMatcher = xmlProcInstPattern.matcher(line)
        while (procMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold),
                procMatcher.start(),
                procMatcher.end()
            )
        }

        // Tags (<tag, </tag, >)
        val tagMatcher = xmlTagPattern.matcher(line)
        while (tagMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = tagColor, fontWeight = FontWeight.Bold),
                tagMatcher.start(),
                tagMatcher.end()
            )
        }

        // Attribute names (name=)
        val attrMatcher = xmlAttrPattern.matcher(line)
        while (attrMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = attrColor),
                attrMatcher.start(),
                attrMatcher.end()
            )
        }

        // Strings ("...")
        val strMatcher = xmlStringPattern.matcher(line)
        while (strMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = stringColor),
                strMatcher.start(),
                strMatcher.end()
            )
        }

        // Entities (&amp;)
        val entityMatcher = xmlEntityPattern.matcher(line)
        while (entityMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = FlashGreen, fontWeight = FontWeight.SemiBold),
                entityMatcher.start(),
                entityMatcher.end()
            )
        }
    }

    private fun highlightJson(builder: AnnotatedString.Builder, line: String) {
        // Keys ("key":)
        val keyMatcher = jsonKeyPattern.matcher(line)
        while (keyMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = tagColor, fontWeight = FontWeight.SemiBold),
                keyMatcher.start(),
                keyMatcher.end()
            )
        }

        // String values (: "value")
        val strMatcher = jsonStringPattern.matcher(line)
        while (strMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = stringColor),
                strMatcher.start(1),
                strMatcher.end(1)
            )
        }

        // Numbers (: 123)
        val numMatcher = jsonNumberPattern.matcher(line)
        while (numMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = numberColor),
                numMatcher.start(1),
                numMatcher.end(1)
            )
        }

        // Bool / Null (: true)
        val boolMatcher = jsonBoolNullPattern.matcher(line)
        while (boolMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold),
                boolMatcher.start(1),
                boolMatcher.end(1)
            )
        }
    }

    private fun highlightCode(builder: AnnotatedString.Builder, line: String) {
        // Keywords
        val kwMatcher = codeKeywordsPattern.matcher(line)
        while (kwMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold),
                kwMatcher.start(),
                kwMatcher.end()
            )
        }

        // Functions
        val fnMatcher = codeFunctionPattern.matcher(line)
        while (fnMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = tagColor),
                fnMatcher.start(1),
                fnMatcher.end(1)
            )
        }

        // Numbers
        val numMatcher = codeNumberPattern.matcher(line)
        while (numMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = numberColor),
                numMatcher.start(),
                numMatcher.end()
            )
        }

        // Strings
        val strMatcher = codeStringPattern.matcher(line)
        while (strMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = stringColor),
                strMatcher.start(),
                strMatcher.end()
            )
        }

        // Comments (override string or keyword styles)
        val commentMatcher = codeCommentPattern.matcher(line)
        while (commentMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = commentColor, fontStyle = FontStyle.Italic),
                commentMatcher.start(),
                commentMatcher.end()
            )
        }
    }

    private fun highlightLogOrConfig(builder: AnnotatedString.Builder, line: String) {
        // Log Success
        val succMatcher = logSuccessPattern.matcher(line)
        while (succMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = FlashGreen, fontWeight = FontWeight.Bold),
                succMatcher.start(),
                succMatcher.end()
            )
        }

        // Log Error
        val errMatcher = logErrorPattern.matcher(line)
        while (errMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = FlashRed, fontWeight = FontWeight.Bold),
                errMatcher.start(),
                errMatcher.end()
            )
        }

        // Log Warn
        val warnMatcher = logWarnPattern.matcher(line)
        while (warnMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = FlashAmber, fontWeight = FontWeight.Bold),
                warnMatcher.start(),
                warnMatcher.end()
            )
        }

        // Section header [Section]
        val secMatcher = configSectionPattern.matcher(line)
        if (secMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = tagColor, fontWeight = FontWeight.Bold),
                secMatcher.start(),
                secMatcher.end()
            )
        }

        // Config Key (key=)
        val keyMatcher = configKeyPattern.matcher(line)
        if (keyMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = attrColor),
                keyMatcher.start(),
                keyMatcher.end()
            )
        }

        // Markdown Header (# Title)
        val mdMatcher = mdHeaderPattern.matcher(line)
        if (mdMatcher.find()) {
            builder.addStyle(
                SpanStyle(color = FlashCyan, fontWeight = FontWeight.Bold),
                mdMatcher.start(),
                mdMatcher.end()
            )
        }
    }
}
