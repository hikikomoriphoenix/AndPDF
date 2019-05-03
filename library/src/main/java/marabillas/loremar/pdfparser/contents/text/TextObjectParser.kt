package marabillas.loremar.pdfparser.contents.text

import marabillas.loremar.pdfparser.isEnclosingAt
import marabillas.loremar.pdfparser.isUnEnclosingAt
import marabillas.loremar.pdfparser.isWhiteSpaceAt
import marabillas.loremar.pdfparser.objects.EnclosedObjectExtractor
import marabillas.loremar.pdfparser.objects.PDFObject
import marabillas.loremar.pdfparser.objects.toPDFObject
import marabillas.loremar.pdfparser.objects.toPDFString
import marabillas.loremar.pdfparser.toDouble

internal class TextObjectParser {
    private val operandsIndices = IntArray(6)
    private val operand = StringBuilder()
    private var pos = 0
    private var td = FloatArray(2)
    private var tf = StringBuilder()
    private var tfDef = StringBuilder()
    private var ts = 0f
    private var tl = 0f

    fun parse(
        s: StringBuilder,
        textObj: TextObject,
        tfDefault: StringBuilder = tfDef,
        startIndex: Int,
        ctm: FloatArray
    ): Int {
        if (tfDefault.isNotBlank()) {
            tf.clear().append(tfDefault)
        }

        pos = startIndex
        var operandsCount = 0
        var expectToken = true
        while (pos < s.length) {
            if (expectToken) {
                if (s[pos].isDigit() || s[pos] == '<' || s[pos] == '(' ||
                    s[pos] == '.' || s[pos] == '[' || s[pos] == '/' || s[pos] == '-'
                ) {
                    operandsIndices[operandsCount] = pos
                    operandsCount++

                    if (s.isEnclosingAt(pos)) {
                        pos = EnclosedObjectExtractor.indexOfClosingChar(s, pos)
                        pos--
                    }
                } else if (s[pos] == 'T') {
                    pos++
                    if (s[pos] == 'j' || s[pos] == 'J') {
                        var tjEnd = pos - 2
                        if (s.isUnEnclosingAt(tjEnd))
                            tjEnd = pos - 1
                        operand.clear().append(s, operandsIndices[0], tjEnd)
                        addTextElement(textObj, operand.toPDFObject() ?: "()".toPDFString(), ctm)
                    } else if (s[pos] == 'd') {
                        positionText(s)
                    } else if (s[pos] == 'm') {
                        td[0] = operand
                            .clear()
                            .append(s, operandsIndices[4], operandsIndices[5] - 1)
                            .toDouble()
                            .toFloat()
                        td[1] = operand
                            .clear()
                            .append(s, operandsIndices[5], pos - 2)
                            .toDouble()
                            .toFloat()
                        val sx = operand
                            .clear()
                            .append(s, operandsIndices[0], operandsIndices[1] - 1)
                            .toDouble()
                            .toFloat()
                        if (sx < 0) {
                            td[0] = td[0] * (-1)
                        }
                        val sy = operand
                            .clear()
                            .append(s, operandsIndices[3], operandsIndices[4] - 1)
                            .toDouble()
                            .toFloat()
                        if (sy < 0) {
                            td[1] = td[1] * (-1)
                        }
                    } else if (s[pos] == 'f') {
                        tf.clear().append(s, operandsIndices[0], pos - 2)
                    } else if (s[pos] == 'D') {
                        positionText(s)
                        tl = -td[1]
                    } else if (s[pos] == 'L') {
                        tl = operand
                            .clear()
                            .append(s, operandsIndices[0], pos - 2)
                            .toDouble()
                            .toFloat()
                    } else if (s[pos] == '*') {
                        td[0] = 0f
                        td[1] = -tl
                    } else if (s[pos] == 's') {
                        ts = operand
                            .clear()
                            .append(s, operandsIndices[0], pos - 2)
                            .toDouble()
                            .toFloat()
                    }
                    operandsCount = 0
                } else if (s[pos] == 'E' && s[pos + 1] == 'T') {
                    pos += 2
                    return pos
                } else if (s[pos] == '\"') {
                    operandsCount = 0
                } else if (s[pos] == '\'') {
                    operandsCount = 0
                } else {
                    operandsCount = 0
                    if (s.isEnclosingAt(pos)) {
                        pos = EnclosedObjectExtractor.indexOfClosingChar(s, pos)
                        pos--
                    }
                }
                expectToken = false
            } else if (s.isWhiteSpaceAt(pos) || s.isUnEnclosingAt(pos)) {
                expectToken = true
            } else if (s[pos] == '/' || s.isEnclosingAt(pos)) {
                operandsIndices[operandsCount] = pos
                operandsCount++
            }
            pos++
        }
        return pos
    }

    private fun addTextElement(textObj: TextObject, tj: PDFObject, ctm: FloatArray) {
        td[0] = td[0] * ctm[0] + ctm[4]
        td[1] = td[1] * ctm[3] + ctm[5]
        val content = TextElement(
            tf = tf.toString(),
            td = td.copyOf(),
            tj = tj,
            ts = ts
        )
        textObj.add(content)
        if (textObj.count() == 1) {
            textObj.td[0] = td[0]
            textObj.td[1] = td[1]
        }
    }

    private fun positionText(s: StringBuilder) {
        td[0] = operand
            .clear()
            .append(s, operandsIndices[0], operandsIndices[1] - 1)
            .toDouble()
            .toFloat()
        td[1] = operand
            .clear()
            .append(s, operandsIndices[1], pos - 2)
            .toDouble()
            .toFloat()
    }
}