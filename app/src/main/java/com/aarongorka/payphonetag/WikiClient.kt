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

class WikiClient(private val apiUrl: String = DEFAULT_API) {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)

    fun login(username: String, password: String) {
        val token = tokenOf("login")
        val result = post(
            mapOf("action" to "login", "lgname" to username, "lgpassword" to password, "lgtoken" to token),
        ).getJSONObject("login")
        check(result.getString("result") == "Success") { result.optString("reason", "Login failed") }
    }

    fun upload(file: File, fileName: String, pageText: String, comment: String): String {
        val token = tokenOf("csrf")
        val fields = mapOf(
            "action" to "upload",
            "format" to "json",
            "filename" to fileName,
            "text" to pageText,
            "comment" to comment,
            "token" to token,
        )
        val response = multipart(fields, file, fileName)
        response.optJSONObject("error")?.let { error(it.optString("info", "Upload failed")) }
        val upload = response.getJSONObject("upload")
        check(upload.getString("result") == "Success") { "Upload rejected: ${upload.opt("warnings") ?: upload}" }
        return upload.getJSONObject("imageinfo").getString("descriptionurl")
    }

    private fun tokenOf(type: String): String {
        val json = get("action=query&meta=tokens&format=json&type=$type")
        return json.getJSONObject("query").getJSONObject("tokens").getString("${type}token")
    }

    private fun get(query: String): JSONObject = send("$apiUrl?$query", "GET", null, null)

    private fun post(params: Map<String, String>): JSONObject {
        val body = (params + ("format" to "json")).entries.joinToString("&") { (k, v) -> "${enc(k)}=${enc(v)}" }
        return send(apiUrl, "POST", "application/x-www-form-urlencoded", body.toByteArray())
    }

    private fun multipart(fields: Map<String, String>, file: File, fileName: String): JSONObject {
        val boundary = "----${UUID.randomUUID()}"
        val out = ByteArrayOutputStream()
        fun write(text: String) = out.write(text.toByteArray())
        fields.forEach { (k, v) ->
            write("--$boundary\r\nContent-Disposition: form-data; name=\"$k\"\r\n\r\n$v\r\n")
        }
        write(
            "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"${enc(fileName)}\"\r\n" +
                "Content-Type: image/jpeg\r\n\r\n",
        )
        out.write(file.readBytes())
        write("\r\n--$boundary--\r\n")
        return send(apiUrl, "POST", "multipart/form-data; boundary=$boundary", out.toByteArray())
    }

    private fun send(url: String, method: String, contentType: String?, body: ByteArray?): JSONObject {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
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
            val stream = if (code in HTTP_OK_RANGE) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            check(code in HTTP_OK_RANGE) { "HTTP $code" }
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    private fun enc(value: String) = URLEncoder.encode(value, "UTF-8")

    companion object {
        const val DEFAULT_API = "https://wiki.payphonetag.com/api.php"
        private const val USER_AGENT = "PayphoneTagPhotoUploader (Android)"
        private const val TIMEOUT_MS = 30_000
        private val HTTP_OK_RANGE = 200..299
    }
}
