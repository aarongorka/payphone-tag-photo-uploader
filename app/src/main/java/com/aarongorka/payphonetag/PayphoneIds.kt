package com.aarongorka.payphonetag

import java.net.URLDecoder

object PayphoneIds {
    private const val PAGE = "Special:AddPayphonePhoto/"
    private val valid = Regex("""[A-Za-z0-9_-]+""")

    /** Extracts a payphone ID from a wiki upload-page URL, shared text containing one, or a bare ID. */
    fun parse(input: String?): String? {
        val text = input?.trim().orEmpty()
        val index = text.indexOf(PAGE)
        val candidate = if (index >= 0) {
            text.substring(index + PAGE.length).takeWhile { it !in "?#/ \n\t" }
        } else {
            text
        }
        val decoded = runCatching { URLDecoder.decode(candidate, "UTF-8") }.getOrDefault(candidate)
        return decoded.takeIf { valid.matches(it) }
    }
}
