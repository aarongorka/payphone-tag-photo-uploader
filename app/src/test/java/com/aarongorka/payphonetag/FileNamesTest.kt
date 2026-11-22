package com.aarongorka.payphonetag

import org.junit.Assert.assertEquals
import org.junit.Test

class FileNamesTest {
    @Test
    fun stripsIllegalCharactersAndCollapsesWhitespace() {
        assertEquals("Fitzroy St 2026.jpg", FileNames.forPhoto("  Fitzroy / St: ", "2026"))
    }

    @Test
    fun fallsBackWhenTitleIsOnlyIllegalCharacters() {
        assertEquals("Payphone 2026.jpg", FileNames.forPhoto("#<>", "2026"))
    }
}
