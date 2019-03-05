package marabillas.loremar.pdfparser.objects

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class EnclosedObjectExtractorTest {
    @Test
    fun testExtract() {
        var s = "(123(345(67)89)0) 123243254435"
        var extracted = EnclosedObjectExtractor(s).extract()
        assertThat(extracted, `is`("(123(345(67)89)0)"))

        s = "<<<<<Hello>World>>>>"
        extracted = EnclosedObjectExtractor(s).extract()
        assertThat(extracted, `is`("<<<<<Hello>World>>>>"))

        s = "<Hello World>!!!>"
        extracted = EnclosedObjectExtractor(s).extract()
        assertThat(extracted, `is`("<Hello World>"))
    }
}