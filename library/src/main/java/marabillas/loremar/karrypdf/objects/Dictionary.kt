package marabillas.loremar.karrypdf.objects

import marabillas.loremar.karrypdf.document.KarryPDFContext
import marabillas.loremar.karrypdf.utils.exts.indexOfClosingChar
import marabillas.loremar.karrypdf.utils.exts.isEnclosingAt
import marabillas.loremar.karrypdf.utils.exts.isWhiteSpaceAt

internal class Dictionary(private val entries: HashMap<String, PDFObject?>) : PDFObject {
    operator fun get(entry: String): PDFObject? {
        return entries[entry]
    }

    fun getKeys(): Set<String> {
        return entries.keys
    }

    fun resolveReferences(): Dictionary {
        entries.asSequence()
            .filter { it.value is Reference }
            .forEach {
                val resolved = (it.value as Reference).resolve()
                entries[it.key] = resolved
            }
        return this
    }

    override fun toString(): String {
        val sb = StringBuilder("dictionary {\n")
        for (entry in entries) {
            sb.append("${entry.key}->${entry.value}\n")
        }
        sb.append("}")
        return sb.toString()
    }
}

internal fun StringBuilder.toDictionary(
    context: KarryPDFContext,
    secondary: StringBuilder,
    obj: Int,
    gen: Int,
    resolveReferences: Boolean = false
): Dictionary {
    val entries = HashMap<String, PDFObject?>()
    var i = this.indexOf('/')
    if (i == -1) return Dictionary(entries)
    i++
    while (i < this.length) {
        // Locate next key position and length
        val keyIndex = i
        var keyLength = 1
        while (i < this.length) {
            val c = this[i]
            if (c == ' ' || c == '/' || c == '(' || c == '<' || c == '[' || c == '{' || c == '\n' || c == '\r') {
                keyLength = i - keyIndex
                break
            }
            i++
        }

        if (i >= this.length) break

        // Skip whitespaces
        while (this.isWhiteSpaceAt(i) && i < this.length)
            i++

        if (i >= this.length) break

        // Locate next value position and length
        val valueIndex = i
        if (this.isEnclosingAt(i))
            i = this.indexOfClosingChar(i)
        else if (this[i] == '/')
            i++
        i = this.indexOf('/', i)
        var valueLength: Int
        if (i == -1) {
            valueLength = this.length - valueIndex
            i = this.length
        } else {
            valueLength = i - valueIndex
        }

        // Store value in a StringBuilder and remove whitespaces and closing '>> for dictionary.
        secondary.clear()
        secondary.append(this, valueIndex, valueIndex + valueLength)
        var hasClosing = (valueIndex + valueLength) == this.length
        while (true) {
            if (secondary.last() == ' ' || secondary.last() == '\n' || secondary.last() == '\r')
                secondary.deleteCharAt(secondary.lastIndex)
            else if (hasClosing && secondary.last() == '>' && secondary[secondary.lastIndex - 1] == '>') {
                secondary.delete(secondary.lastIndex - 1, secondary.length)
                hasClosing = false
            } else break
        }

        // Add entry
        entries[this.substring(keyIndex, keyIndex + keyLength)] =
            secondary.toPDFObject(context, obj, gen, resolveReferences)

        i++
    }
    secondary.clear()
    return Dictionary(entries)
}