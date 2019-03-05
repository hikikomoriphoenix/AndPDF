package marabillas.loremar.pdfparser

import java.util.*

/**
 * A HashMap containing case-insensitive strings for keys.
 */
class CaseInsensitiveMap<A : Any?> : HashMap<String, A?>() {
    override fun put(key: String, value: A?): A? {
        return super.put(key.toLowerCase(), value)
    }

    override fun get(key: String): A? {
        return super.get(key.toLowerCase())
    }
}