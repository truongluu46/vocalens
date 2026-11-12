package com.luudev.vocalens.data.remote

import com.luudev.vocalens.domain.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object DictionaryResponseMapper {
    fun map(json: String): List<DictionaryEntry> {
        val root = JSONObject(json)
        val english = root.getString("word").trim()
        if (english.isBlank()) throw DictionaryException(DictionaryError.InvalidResponse)
        val source = root.optJSONObject("source")
        val license = source?.optJSONObject("license")
        val results = mutableListOf<DictionaryEntry>()
        root.optJSONArray("entries").objects().filter {
            it.optJSONObject("language")?.optString("code") == "en"
        }.forEach { entry ->
            val pronunciations = entry.optJSONArray("pronunciations").objects().filter { it.optString("type") == "ipa" }
            fun pronunciationFor(accent: DictionaryAccent) = pronunciations.firstOrNull { pronunciation ->
                val tags = pronunciation.optJSONArray("tags")
                val accepted = if (accent == DictionaryAccent.British)
                    listOf("UK", "Received Pronunciation", "British", "British English")
                    else listOf("US", "USA", "General American", "American", "American English")
                (0 until (tags?.length() ?: 0)).any { tags?.optString(it) in accepted }
            }?.optString("text")?.takeIf { it.isNotBlank() }
            val variants = DictionaryAccent.entries.mapNotNull { accent ->
                pronunciationFor(accent)?.let { accent to it }
            }.ifEmpty { listOf(null to pronunciations.firstOrNull()?.optString("text").orEmpty()) }
            fun visit(senses: JSONArray?) {
                senses.objects().forEach { sense ->
                    val meaning = sense.optJSONArray("translations").objects().filter {
                        it.optJSONObject("language")?.optString("code") == "vi"
                    }.map { it.optString("word").trim() }.filter { it.isNotBlank() }.distinct().joinToString(", ")
                    val definition = sense.optString("definition")
                    if (meaning.isNotBlank() || definition.isNotBlank()) {
                        val part = entry.optString("partOfSpeech").takeIf { it.isNotBlank() }
                        variants.forEach { (accent, ipa) ->
                        val id = UUID.nameUUIDFromBytes("dictionary:$english:$part:$definition:$meaning:$accent:$ipa".toByteArray(Charsets.UTF_8)).toString()
                        results += DictionaryEntry(
                            VocabularyWord(id, english, ipa, meaning, VocabularyCategory.Objects, part,
                                sense.optJSONArray("examples")?.optString(0)?.takeIf { it.isNotBlank() }),
                            definition, source?.optString("url").orEmpty(),
                            license?.optString("name").orEmpty(), license?.optString("url").orEmpty(), accent
                        )
                        }
                    }
                    visit(sense.optJSONArray("subsenses"))
                }
            }
            visit(entry.optJSONArray("senses"))
        }
        if (results.isEmpty()) throw DictionaryException(DictionaryError.NotFound)
        // Keep one representative meaning per accent, preferring a Vietnamese translation.
        val ranked = results.sortedByDescending { it.word.meaning.isNotBlank() }
        val regional = DictionaryAccent.entries.mapNotNull { accent -> ranked.firstOrNull { it.accent == accent } }
        return regional.ifEmpty { listOf(ranked.first()) }
    }

    private fun JSONArray?.objects(): List<JSONObject> =
        (0 until (this?.length() ?: 0)).mapNotNull { this?.optJSONObject(it) }
}
