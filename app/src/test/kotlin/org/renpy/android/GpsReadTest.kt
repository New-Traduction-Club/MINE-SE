package org.renpy.android

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class GpsReadTest {

    @Test
    fun testParseFirstLineTrue() {
        val input = "true\nsome other line".byteInputStream(Charsets.UTF_8)
        assertTrue(GpsRead.parseFirstLine(input))
    }

    @Test
    fun testParseFirstLineTrueWithWhitespaceAndCase() {
        val input = "   TRUE   \n".byteInputStream(Charsets.UTF_8)
        assertTrue(GpsRead.parseFirstLine(input))
    }

    @Test
    fun testParseFirstLineFalse() {
        val input = "false\n".byteInputStream(Charsets.UTF_8)
        assertFalse(GpsRead.parseFirstLine(input))
    }

    @Test
    fun testParseFirstLineFalseWithWhitespaceAndCase() {
        val input = "  False  \r\n".byteInputStream(Charsets.UTF_8)
        assertFalse(GpsRead.parseFirstLine(input))
    }

    @Test
    fun testParseFirstLineEmptyDefaultsToTrue() {
        val input = "".byteInputStream(Charsets.UTF_8)
        assertTrue(GpsRead.parseFirstLine(input))
    }

    @Test
    fun testParseFirstLineUnknownValueDefaultsToTrue() {
        val input = "unexpected_token\n".byteInputStream(Charsets.UTF_8)
        assertTrue(GpsRead.parseFirstLine(input))
    }
}
