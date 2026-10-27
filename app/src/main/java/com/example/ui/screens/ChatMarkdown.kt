package com.example.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/** Colours one message body needs, resolved for the bubble it sits in. */
data class ChatMarkdownColors(
    val text: Color,
    val muted: Color,
    val codeBackground: Color,
    val codeText: Color,
    val link: Color,
    /** Search highlight: green marker background with dark ink on top. */
    val highlightBackground: Color,
    val highlightText: Color
)

/**
 * A deliberately small Markdown renderer for chat bubbles.
 *
 * Supports what students actually type in a chat, not a full CommonMark parser:
 * `**bold**` (two asterisks), `_italic_` (two underscores), `~~strike~~`,
 * `` `code` ``, fenced blocks, `# headings`, `-`/`*`/`1.` lists, `>` quotes and
 * `[links](https://…)`.
 *
 * Deliberately *not* supported: a single `*` for italic (it collides with
 * bullets and with maths like 2 * 3 * 4) and `__bold__` (underscores double as
 * part of identifiers such as student_id).
 *
 * Everything is rendered into a single [AnnotatedString], so a message still
 * lays out as one text block (no extra composables per line) and the existing
 * emoji-only sizing and search highlighting keep working.
 */
object ChatMarkdown {

    fun render(
        source: String,
        colors: ChatMarkdownColors,
        highlight: String? = null
    ): AnnotatedString = buildAnnotatedString {
        var inFence = false
        val lines = source.split('\n')

        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trimEnd()

            // ── fenced code block ────────────────────────────────────────────
            if (line.trimStart().startsWith("```")) {
                inFence = !inFence
                return@forEachIndexed  // the fence markers themselves are hidden
            }

            if (inFence) {
                appendPlain(
                    text = line,
                    base = SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        color = colors.codeText,
                        background = colors.codeBackground
                    ),
                    colors = colors,
                    highlight = highlight
                )
                if (index != lines.lastIndex) append("\n")
                return@forEachIndexed
            }

            val trimmed = line.trimStart()

            // ── headings ─────────────────────────────────────────────────────
            val headingMatch = Regex("^(#{1,3})\\s+(.*)$").find(trimmed)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                appendInline(
                    text = headingMatch.groupValues[2],
                    base = SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = when (level) {
                            1 -> 17.sp
                            2 -> 15.5.sp
                            else -> 14.5.sp
                        },
                        color = colors.text
                    ),
                    colors = colors,
                    highlight = highlight
                )
                if (index != lines.lastIndex) append("\n")
                return@forEachIndexed
            }

            // ── block quote ──────────────────────────────────────────────────
            if (trimmed.startsWith(">")) {
                append("▍ ")
                appendInline(
                    text = trimmed.removePrefix(">").trim(),
                    base = SpanStyle(
                        fontStyle = FontStyle.Italic,
                        color = colors.muted
                    ),
                    colors = colors,
                    highlight = highlight
                )
                if (index != lines.lastIndex) append("\n")
                return@forEachIndexed
            }

            // ── lists ────────────────────────────────────────────────────────
            val bulletMatch = Regex("^[-*•]\\s+(.*)$").find(trimmed)
            if (bulletMatch != null) {
                append("•  ")
                appendInline(
                    text = bulletMatch.groupValues[1],
                    base = SpanStyle(color = colors.text),
                    colors = colors,
                    highlight = highlight
                )
                if (index != lines.lastIndex) append("\n")
                return@forEachIndexed
            }

            val numberedMatch = Regex("^(\\d{1,2})[.)]\\s+(.*)$").find(trimmed)
            if (numberedMatch != null) {
                append(numberedMatch.groupValues[1] + ".  ")
                appendInline(
                    text = numberedMatch.groupValues[2],
                    base = SpanStyle(color = colors.text),
                    colors = colors,
                    highlight = highlight
                )
                if (index != lines.lastIndex) append("\n")
                return@forEachIndexed
            }

            // ── plain paragraph line ─────────────────────────────────────────
            appendInline(
                text = line,
                base = SpanStyle(color = colors.text),
                colors = colors,
                highlight = highlight
            )
            if (index != lines.lastIndex) append("\n")
        }
    }

    // -----------------------------------------------------------------------
    // Inline parsing
    // -----------------------------------------------------------------------

    private fun AnnotatedString.Builder.appendInline(
        text: String,
        base: SpanStyle,
        colors: ChatMarkdownColors,
        highlight: String?
    ) {
        var index = 0
        while (index < text.length) {
            val rest = text.substring(index)

            // `code` — never parsed further, so ** inside code stays literal.
            if (rest.startsWith("`")) {
                val end = text.indexOf('`', index + 1)
                if (end > index) {
                    withSpan(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = colors.codeBackground,
                            color = colors.codeText,
                            fontSize = 12.5.sp
                        )
                    ) {
                        appendPlain(text.substring(index + 1, end), base, colors, highlight)
                    }
                    index = end + 1
                    continue
                }
            }

            // **bold** — asterisks doubled. A single * is left as typed.
            val bold = matchDelimited(text, index, "**", "**")
            if (bold != null) {
                withSpan(base.copy(fontWeight = FontWeight.Bold)) {
                    appendInline(bold.first, base.copy(fontWeight = FontWeight.Bold), colors, highlight)
                }
                index = bold.second
                continue
            }

            // ~~strikethrough~~
            val strike = matchDelimited(text, index, "~~", "~~")
            if (strike != null) {
                withSpan(base.copy(textDecoration = TextDecoration.LineThrough)) {
                    appendInline(
                        strike.first,
                        base.copy(textDecoration = TextDecoration.LineThrough),
                        colors,
                        highlight
                    )
                }
                index = strike.second
                continue
            }

            // _italic_ — underscores only. Guarded so snake_case / file_names
            // are not half-italicised: the opening _ must not follow a letter or
            // digit, and the closing one must not precede one.
            val italic = matchUnderscoreItalic(text, index)
            if (italic != null) {
                withSpan(base.copy(fontStyle = FontStyle.Italic)) {
                    appendInline(italic.first, base.copy(fontStyle = FontStyle.Italic), colors, highlight)
                }
                index = italic.second
                continue
            }

            // [label](url)
            if (rest.startsWith("[")) {
                val close = text.indexOf(']', index + 1)
                if (close > index && close + 1 < text.length && text[close + 1] == '(') {
                    val end = text.indexOf(')', close + 2)
                    if (end > close) {
                        withSpan(
                            base.copy(
                                color = colors.link,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            appendPlain(text.substring(index + 1, close), base, colors, highlight)
                        }
                        index = end + 1
                        continue
                    }
                }
            }

            // Ordinary character: consumed one run at a time for speed.
            var next = index
            while (next < text.length && "`*_~[".indexOf(text[next]) < 0) next++
            if (next == index) next = index + 1
            appendPlain(text.substring(index, next), base, colors, highlight)
            index = next
        }
    }

    /**
     * `_italic_`, with the word-boundary guard described above.
     */
    private fun matchUnderscoreItalic(text: String, start: Int): Pair<String, Int>? {
        if (start >= text.length || text[start] != '_') return null
        if (start > 0 && (text[start - 1].isLetterOrDigit())) return null

        val contentStart = start + 1
        if (contentStart >= text.length) return null
        if (text[contentStart] == ' ' || text[contentStart] == '_') return null

        var cursor = contentStart
        while (cursor < text.length) {
            if (text[cursor] == '_') {
                val after = cursor + 1
                val closesWord = after < text.length && text[after].isLetterOrDigit()
                if (!closesWord) {
                    return text.substring(contentStart, cursor) to after
                }
            }
            cursor++
        }
        return null
    }

    /** Returns the content and the index just past the closing marker. */
    private fun matchDelimited(
        text: String,
        start: Int,
        open: String,
        close: String
    ): Pair<String, Int>? {
        if (!text.startsWith(open, start)) return null
        val contentStart = start + open.length
        if (contentStart >= text.length) return null
        if (text[contentStart] == ' ') return null  // "* text" is a bullet, not italic
        val end = text.indexOf(close, contentStart)
        if (end <= contentStart) return null
        return text.substring(contentStart, end) to end + close.length
    }

    /**
     * Appends visible text, splitting it so every occurrence of the search term
     * is drawn with the green highlight on top of the markdown style.
     */
    private fun AnnotatedString.Builder.appendPlain(
        text: String,
        base: SpanStyle,
        colors: ChatMarkdownColors,
        highlight: String?
    ) {
        if (text.isEmpty()) return
        val span = base.copy(color = base.color.takeIf { it != Color.Unspecified } ?: colors.text)

        val needle = highlight?.trim().orEmpty()
        if (needle.isEmpty()) {
            withSpan(span) { append(text) }
            return
        }

        var cursor = 0
        while (cursor < text.length) {
            val found = text.indexOf(needle, cursor, ignoreCase = true)
            if (found < 0) {
                withSpan(span) { append(text.substring(cursor)) }
                break
            }
            if (found > cursor) {
                withSpan(span) { append(text.substring(cursor, found)) }
            }
            pushStyle(
                span.copy(
                    color = colors.highlightText,
                    background = colors.highlightBackground,
                    fontWeight = FontWeight.Bold
                )
            )
            append(text.substring(found, found + needle.length))
            pop()
            cursor = found + needle.length
        }
    }

    private inline fun AnnotatedString.Builder.withSpan(style: SpanStyle, block: () -> Unit) {
        pushStyle(style)
        block()
        pop()
    }
}
