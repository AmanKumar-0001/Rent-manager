package com.tenantmanager.data.sync

import com.tenantmanager.domain.sync.SheetTabData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SpreadsheetRef(
    val id: String,
    val url: String,
    val isNewlyCreated: Boolean
)

interface SheetsApiClient {
    suspend fun findOrCreateSpreadsheet(
        title: String,
        tabTitles: List<String>,
        accessToken: String
    ): SpreadsheetRef

    suspend fun batchUpdateValues(
        spreadsheetId: String,
        tabs: List<SheetTabData>,
        accessToken: String
    ): Boolean
}

class RealSheetsApiClient(
    private val json: Json = Json { ignoreUnknownKeys = true }
) : SheetsApiClient {

    override suspend fun findOrCreateSpreadsheet(
        title: String,
        tabTitles: List<String>,
        accessToken: String
    ): SpreadsheetRef {
        // 1. Search existing files using drive.file scope
        val encodedQuery = URLEncoder.encode("mimeType='application/vnd.google-apps.spreadsheet' and trashed=false and name='$title'", "UTF-8")
        val searchUrl = "https://www.googleapis.com/drive/v3/files?q=$encodedQuery&spaces=drive"

        val searchResponse = executeGet(searchUrl, accessToken)
        val filesArray = json.parseToJsonElement(searchResponse).jsonObject["files"]?.jsonArray
        if (filesArray != null && filesArray.isNotEmpty()) {
            val fileObj = filesArray[0].jsonObject
            val id = fileObj["id"]?.jsonPrimitive?.content ?: ""
            if (id.isNotBlank()) {
                return SpreadsheetRef(
                    id = id,
                    url = "https://docs.google.com/spreadsheets/d/$id",
                    isNewlyCreated = false
                )
            }
        }

        // 2. Create new spreadsheet
        val createUrl = "https://sheets.googleapis.com/v4/spreadsheets"
        val createBody = buildJsonObject {
            put("properties", buildJsonObject {
                put("title", JsonPrimitive(title))
            })
            put("sheets", buildJsonArray {
                for (tabTitle in tabTitles) {
                    add(buildJsonObject {
                        put("properties", buildJsonObject {
                            put("title", JsonPrimitive(tabTitle))
                        })
                    })
                }
            })
        }.toString()

        val createResponse = executePost(createUrl, createBody, accessToken)
        val createObj = json.parseToJsonElement(createResponse).jsonObject
        val newId = createObj["spreadsheetId"]?.jsonPrimitive?.content
            ?: throw IOException("Failed to obtain spreadsheetId from creation response")

        return SpreadsheetRef(
            id = newId,
            url = "https://docs.google.com/spreadsheets/d/$newId",
            isNewlyCreated = true
        )
    }

    override suspend fun batchUpdateValues(
        spreadsheetId: String,
        tabs: List<SheetTabData>,
        accessToken: String
    ): Boolean {
        // Optional clear of existing ranges
        val clearUrl = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchClear"
        val clearBody = buildJsonObject {
            put("ranges", buildJsonArray {
                for (tab in tabs) {
                    add(JsonPrimitive("${tab.title}!A1:Z5000"))
                }
            })
        }.toString()

        try {
            executePost(clearUrl, clearBody, accessToken)
        } catch (_: Exception) {
            // Ignore clear failure if sheet was fresh or ranges are newly created
        }

        // Single batchUpdate writing all 3 tabs in one request
        val batchUrl = "https://sheets.googleapis.com/v4/spreadsheets/$spreadsheetId/values:batchUpdate"
        val batchBody = buildJsonObject {
            put("valueInputOption", JsonPrimitive("USER_ENTERED"))
            put("data", buildJsonArray {
                for (tab in tabs) {
                    add(buildJsonObject {
                        put("range", JsonPrimitive("${tab.title}!A1"))
                        put("values", buildJsonArray {
                            for (row in tab.allValues) {
                                add(buildJsonArray {
                                    for (cell in row) {
                                        add(JsonPrimitive(cell))
                                    }
                                })
                            }
                        })
                    })
                }
            })
        }.toString()

        executePost(batchUrl, batchBody, accessToken)
        return true
    }

    private fun executeGet(urlString: String, token: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Accept", "application/json")
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        val code = conn.responseCode
        if (code == 401 || code == 403) {
            throw SecurityException("Google authorization expired or revoked (HTTP $code)")
        }
        if (code !in 200..299) {
            val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw IOException("Google API request failed: HTTP $code - $error")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }

    private fun executePost(urlString: String, jsonBody: String, token: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Authorization", "Bearer $token")
        conn.setRequestProperty("Content-Type", "application/json")
        conn.setRequestProperty("Accept", "application/json")
        conn.doOutput = true
        conn.connectTimeout = 15000
        conn.readTimeout = 15000

        conn.outputStream.bufferedWriter().use { it.write(jsonBody) }

        val code = conn.responseCode
        if (code == 401 || code == 403) {
            throw SecurityException("Google authorization expired or revoked (HTTP $code)")
        }
        if (code !in 200..299) {
            val error = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw IOException("Google API request failed: HTTP $code - $error")
        }

        return conn.inputStream.bufferedReader().use { it.readText() }
    }
}

