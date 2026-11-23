package com.aarongorka.payphonetag

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WikiClientTest {
    @Test
    fun botUsernamesContainAtSign() {
        assertTrue(WikiClient.isBotUsername("User@Uploader"))
        assertFalse(WikiClient.isBotUsername("User"))
    }
}
