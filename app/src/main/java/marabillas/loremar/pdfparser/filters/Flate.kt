/*
This file is derived from https://github.com/apache/pdfbox/blob/trunk/pdfbox/src/main/java/org/apache/pdfbox/filter/FlateFilter.java
and written into Kotlin. The original work is open-source and licensed under Apache 2.0.
Original authors: Ben Litchfield, Marcel Kammer
 */
package marabillas.loremar.pdfparser.filters

import marabillas.loremar.pdfparser.Dictionary
import java.io.ByteArrayOutputStream
import java.util.zip.DataFormatException
import java.util.zip.Inflater

/**
 * Decompresses data encoded using the zlib/deflate compression method,
 * reproducing the original text or binary data.
 */
class Flate(decodeParams: Dictionary?) : Decoder {
    private val predictor: Int = decodeParams?.entries?.get("Predictor")?.toInt() ?: 1
    private val bitsPerComponent: Int = decodeParams?.entries?.get("BitsPerComponent")?.toInt() ?: 8
    private val columns: Int = decodeParams?.entries?.get("Columns")?.toInt() ?: 1
    private var colors: Int = Math.min(decodeParams?.entries?.get("Colors")?.toInt() ?: 1, 32)

    constructor() : this(null)

    override fun decode(encoded: String): ByteArray {
        val input = encoded.byteInputStream()
        val out = ByteArrayOutputStream()

        val predictedOut = Predictor(
            predictor = predictor,
            bitsPerComponent = bitsPerComponent,
            columns = columns,
            colors = colors,
            bytes = encoded.toByteArray()
        ).wrapPredictor(out)

        // Start decompress

        val buf = ByteArray(2048)
        // skip zlib header
        input.read(buf, 0, 2)
        var read = input.read(buf)
        if (read > 0) {
            // use nowrap mode to bypass zlib-header and checksum to avoid a DataFormatException
            val inflater = Inflater(true)
            inflater.setInput(buf, 0, read)
            val res = ByteArray(1024)
            var dataWritten = false
            while (true) {
                var resRead: Int
                try {
                    resRead = inflater.inflate(res)
                } catch (exception: DataFormatException) {
                    if (dataWritten) {
                        // some data could be read -> don't throw an exception
                        println("FlateFilter: premature end of stream due to a DataFormatException")
                        break
                    } else {
                        // nothing could be read -> re-throw exception
                        throw exception
                    }
                }

                if (resRead != 0) {
                    predictedOut.write(res, 0, resRead)
                    dataWritten = true
                    continue
                }
                if (inflater.finished() || inflater.needsDictionary() || input.available() == 0) {
                    break
                }
                read = input.read(buf)
                inflater.setInput(buf, 0, read)
            }
            inflater.end()
        }
        predictedOut.flush()

        return out.toByteArray()
    }
}