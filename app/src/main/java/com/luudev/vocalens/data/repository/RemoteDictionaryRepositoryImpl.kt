package com.luudev.vocalens.data.repository

import com.luudev.vocalens.data.remote.DictionaryResponseMapper
import com.luudev.vocalens.domain.model.*
import com.luudev.vocalens.domain.repository.DictionaryRepository
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException

class RemoteDictionaryRepositoryImpl : DictionaryRepository {
    override suspend fun lookup(query: String): List<DictionaryEntry> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8").replace("+", "%20")
        val connection = URL("https://freedictionaryapi.com/api/v1/entries/en/$encoded?translations=true")
            .openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("Accept", "application/json")
            when (connection.responseCode) {
                404 -> throw DictionaryException(DictionaryError.NotFound)
                in 200..299 -> connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                    DictionaryResponseMapper.map(it.readText())
                }
                else -> throw DictionaryException(DictionaryError.Network)
            }
        } catch (error: IOException) {
            throw DictionaryException(DictionaryError.Network)
        } catch (error: JSONException) {
            throw DictionaryException(DictionaryError.InvalidResponse)
        } finally {
            connection.disconnect()
        }
    }
}
