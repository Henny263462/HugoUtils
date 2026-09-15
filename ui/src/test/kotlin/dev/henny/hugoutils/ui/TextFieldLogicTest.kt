package dev.henny.hugoutils.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TextFieldLogicTest {
    @Test
    fun `selection supports copy cut and paste`() {
        val field = TextFieldLogic("hello")
        field.selectAll()
        assertEquals("hello", field.copy())
        assertEquals("hello", field.cut())
        assertEquals("", field.text)
        field.paste("world")
        assertEquals("world", field.text)
    }

    @Test
    fun `typing replaces selection and respects max length`() {
        val field = TextFieldLogic("abc", maxLength = 4)
        field.selectAll()
        field.insert("long value")
        assertEquals("long", field.text)
        field.moveCursor(-1)
        field.backspace()
        assertEquals("log", field.text)
    }
}
