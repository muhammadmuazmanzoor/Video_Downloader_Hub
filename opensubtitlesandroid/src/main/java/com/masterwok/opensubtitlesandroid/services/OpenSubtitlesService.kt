package com.masterwok.opensubtitlesandroid.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.masterwok.opensubtitlesandroid.models.OpenSubtitleItem
import com.masterwok.opensubtitlesandroid.services.contracts.OpenSubtitlesService
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream


class OpenSubtitlesService : OpenSubtitlesService {

    companion object {
        const val TemporaryUserAgent = "TemporaryUserAgent"
    }

    override fun search(userAgent: String, url: String): Array<OpenSubtitleItem> {
        val maxRetries = 2
        val subtitleItems = arrayListOf<OpenSubtitleItem>()
        var attempt = 0

        while (attempt < maxRetries) {
            attempt++
            try {
                Log.d("LOGG", "Attempt $attempt of $maxRetries for URL: $url")

                // Open the connection
                val searchUrl = URL(url)
                val connection = searchUrl.openConnection() as HttpURLConnection

                // Set request method and headers
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", userAgent)
                connection.setRequestProperty("Content-Type", "application/json")

                // Check for HTTP success response (200)
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read the response stream
                    val responseStream = connection.inputStream.bufferedReader().use { it.readText() }

                    if (responseStream.isBlank()) {
                        Log.e("LOGG", "Received blank response from API.")
                    } else {
                        try {
                            // Parse JSON response into OpenSubtitleItem objects
                            val parsedItems = parseSubtitles(responseStream)
                            if (parsedItems.isNotEmpty()) {
                                subtitleItems.addAll(parsedItems)
                                break // Exit the loop if items are found
                            } else {
                                Log.e("LOGG", "Parsed response but no items found.")
                            }
                        } catch (jsonException: Exception) {
                            Log.e("LOGG", "JSON parsing error: ${jsonException.message}", jsonException)
                        }
                    }
                } else {
                    Log.e("LOGG", "Failed to search subtitles, response code: $responseCode")
                }
            } catch (e: Exception) {
                Log.e("LOGG", "Exception in search method on attempt $attempt: ${e.message}", e)
            }

            // Wait before retrying (optional delay)
            if (attempt < maxRetries) {
                Thread.sleep(1000) // 1-second delay before retrying
            }
        }

        return subtitleItems.toTypedArray()
    }

    // Helper function to parse JSON response to a list of OpenSubtitleItems
    fun parseSubtitles(response: String): List<OpenSubtitleItem> {
        val gson = Gson()

        return try {
            gson.fromJson(response, Array<OpenSubtitleItem>::class.java)?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.e("LOGG", "Error parsing JSON response: ${e.message}", e)
            emptyList()
        }
    }



    /**
     * Blocking search of the Open Subtitles REST API.
     */
//    override fun search(
//            userAgent: String
//            , url: String
//    ): Array<OpenSubtitleItem> = url
//            .httpGet()
//            .header("User-Agent" to userAgent)
//            .responseObject(OpenSubtitleItem.Deserializer)
//            .third
//            .get()
    /**
     * Blocking download of a subtitle from the REST API. Sometimes the filename
     * specified in the [OpenSubtitleItem] does not exist within the zip file. When
     * this happens, the largest file with the same extension within the zip file is
     * downloaded.
     */
    override fun downloadSubtitle(
            context: Context
            , subtitleItem: OpenSubtitleItem
            , destinationUri: Uri
    ) {
        val url = URL(subtitleItem.SubDownloadLink)
        val urlConnection = url.openConnection() as HttpURLConnection
        val inputStream = GZIPInputStream(urlConnection.inputStream)

        val outputStream = context
                .contentResolver
                .openOutputStream(destinationUri)
                ?: throw RuntimeException("Failed to open output stream for Uri: $destinationUri")

        inputStream.copyTo(outputStream)

        // Clean up resources
        inputStream.close()
        outputStream.close()
    }

}