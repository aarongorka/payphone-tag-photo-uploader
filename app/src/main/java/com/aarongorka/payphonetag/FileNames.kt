package com.aarongorka.payphonetag

object FileNames {
    private val illegal = Regex("""[#<>\[\]|{}:/\\?*"%]""")
    private val whitespace = Regex("""\s+""")

    fun forPhoto(title: String, timestamp: String): String {
        val clean = title.replace(illegal, " ").replace(whitespace, " ").trim()
        return if (clean.isEmpty()) "Payphone $timestamp.jpg" else "$clean $timestamp.jpg"
    }
}
