package com.aarongorka.payphonetag

import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.UUID

class WikiClient(private val baseUrl: String = DEFAULT_BASE) {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)

    private class Response(val code: Int, val text: String)

    fun login(username: String, password: String) {
        val token = loginToken()
        val result = postForm(
            mapOf(
                "action" to "clientlogin",
                "username" to username,
                "password" to password,
                "logintoken" to token,
                "loginreturnurl" to baseUrl,
            ),
        ).getJSONObject("clientlogin")
        check(result.getString("status") == "PASS") {
            result.optString("message").ifBlank { "Login failed (${result.getString("status")})" }
        }
    }

    fun uploadPhoto(payphoneId: String, type: PhotoType, caption: String, file: File) {
        val pageUrl = "$baseUrl/index.php?title=Special:AddPayphonePhoto/${enc(payphoneId)}"
        val page = request(pageUrl, "GET", null, null)
        check(page.code == HTTP_OK) { "Could not open upload page (HTTP ${page.code})" }
        val token = TOKEN_PATTERN.find(page.text)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }
        checkNotNull(token) { "Upload form not found; are you logged in?" }

        val boundary = "----${UUID.randomUUID()}"
        val out = ByteArrayOutputStream()
        fun write(text: String) = out.write(text.toByteArray())
        mapOf(
            "wpType" to type.value,
            "wpCaption" to caption,
            "wpFormSubmitted" to "1",
            "wpEditToken" to unescape(token),
            "wpUpload" to "Upload",
        ).forEach { (k, v) -> write("--$boundary\r\nContent-Disposition: form-data; name=\"$k\"\r\n\r\n$v\r\n") }
        write(
            "--$boundary\r\nContent-Disposition: form-data; name=\"wpFile\"; filename=\"photo.jpg\"\r\n" +
                "Content-Type: image/jpeg\r\n\r\n",
        )
        out.write(file.readBytes())
        write("\r\n--$boundary--\r\n")

        val response = request(pageUrl, "POST", "multipart/form-data; boundary=$boundary", out.toByteArray())
        if (response.code in HTTP_REDIRECTS) return
        val reason = ERROR_PATTERN.find(response.text)?.groupValues?.get(1)?.let(::stripTags)
        error(reason?.takeIf { it.isNotBlank() } ?: "Upload not accepted (HTTP ${response.code})")
    }

    private fun loginToken(): String {
        val url = "$baseUrl/api.php?action=query&meta=tokens&format=json&type=login"
        val json = JSONObject(request(url, "GET", null, null).text)
        return json.getJSONObject("query").getJSONObject("tokens").getString("logintoken")
    }

    private fun postForm(params: Map<String, String>): JSONObject {
        val body = (params + ("format" to "json")).entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        val response = request("$baseUrl/api.php", "POST", "application/x-www-form-urlencoded", body.toByteArray())
        check(response.code == HTTP_OK) { "HTTP ${response.code}" }
        return JSONObject(response.text)
    }

    private fun request(url: String, method: String, contentType: String?, body: ByteArray?): Response {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.instanceFollowRedirects = false
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            cookies.get(connection.url.toURI(), emptyMap()).forEach { (name, values) ->
                if (name.equals("Cookie", ignoreCase = true)) {
                    connection.setRequestProperty("Cookie", values.joinToString("; "))
                }
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType)
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            cookies.put(connection.url.toURI(), connection.headerFields.filterKeys { it != null })
            val stream = if (code < HTTP_ERROR) connection.inputStream else connection.errorStream
            return Response(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    private fun unescape(value: String) = value.replace("&#43;", "+").replace("&amp;", "&").replace("&quot;", "\"")

    private fun stripTags(html: String) =
        unescape(html.replace(Regex("<[^>]*>"), " ")).replace(Regex("\\s+"), " ").trim()

    companion object {
        const val DEFAULT_BASE = "https://wiki.payphonetag.com"
        private const val USER_AGENT = "PayphoneTagPhotoUploader (Android)"
        private const val TIMEOUT_MS = 30_000
        private const val HTTP_OK = 200
        private const val HTTP_ERROR = 400
        private val HTTP_REDIRECTS = 301..303
        private val TOKEN_PATTERN =
            Regex("""name="wpEditToken"[^>]*value="([^"]+)"|value="([^"]+)"[^>]*name="wpEditToken"""")
        private val ERROR_PATTERN =
            Regex("""<div[^>]*class="[^"]*error[^"]*"[^>]*>(.*?)</div>""", RegexOption.DOT_MATCHES_ALL)
    }
}
