package com.example.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

object PptxParser {
    private const val TAG = "PptxParser"

    data class SlideInfo(
        val slideNumber: Int,
        val text: String
    )

    /**
     * Extracts text from each slide in a PPTX file and returns them ordered by slide number.
     */
    fun extractTextFromPptx(context: Context, uri: Uri): List<SlideInfo> {
        val slidesList = mutableListOf<SlideInfo>()
        
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zipInputStream ->
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        if (name.startsWith("ppt/slides/slide") && name.endsWith(".xml")) {
                            // Extract slide number from name like "ppt/slides/slide1.xml"
                            val numberPart = name.substringAfter("ppt/slides/slide").substringBefore(".xml")
                            val slideNumber = numberPart.toIntOrNull() ?: 0
                            
                            // Read xml content
                            val xmlContent = readEntryContent(zipInputStream)
                            val slideText = parseXmlText(xmlContent)
                            
                            if (slideText.isNotBlank()) {
                                slidesList.add(SlideInfo(slideNumber, slideText))
                            }
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing PPTX", e)
        }
        
        // Sort slides by their slide number
        return slidesList.sortedBy { it.slideNumber }
    }

    private fun readEntryContent(zipInputStream: ZipInputStream): String {
        val reader = BufferedReader(InputStreamReader(zipInputStream, "UTF-8"))
        val content = StringBuilder()
        val buffer = CharArray(4096)
        var charsRead = reader.read(buffer)
        while (charsRead != -1) {
            content.append(buffer, 0, charsRead)
            charsRead = reader.read(buffer)
        }
        return content.toString()
    }

    private fun parseXmlText(xmlContent: String): String {
        // Find everything inside <a:t>...</a:t>
        val regex = Regex("<a:t[^>]*>(.*?)</a:t>")
        val matches = regex.findAll(xmlContent)
        
        val slideTextBuilder = StringBuilder()
        for (match in matches) {
            val rawText = match.groupValues[1]
            val cleanedText = unescapeXml(rawText).trim()
            if (cleanedText.isNotEmpty()) {
                slideTextBuilder.append(cleanedText).append(" ")
            }
        }
        
        return slideTextBuilder.toString().trim()
    }

    private fun unescapeXml(text: String): String {
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
    }
}
