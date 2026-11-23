package com.aarongorka.payphonetag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PayphoneIdsTest {
    @Test
    fun parsesIdFromWikiUrl() {
        val url = "https://wiki.payphonetag.com/wiki/Special:AddPayphonePhoto/02983709X2"
        assertEquals("02983709X2", PayphoneIds.parse(url))
    }

    @Test
    fun ignoresQueryAndFragmentAndSurroundingText() {
        val shared = "Look: https://wiki.payphonetag.com/wiki/Special:AddPayphonePhoto/02983709X2?x=1#y"
        assertEquals("02983709X2", PayphoneIds.parse(shared))
    }

    @Test
    fun acceptsBareId() {
        assertEquals("02983709X2", PayphoneIds.parse(" 02983709X2 "))
    }

    @Test
    fun rejectsUnrelatedText() {
        assertNull(PayphoneIds.parse("https://example.com/some page"))
        assertNull(PayphoneIds.parse(null))
    }
}
