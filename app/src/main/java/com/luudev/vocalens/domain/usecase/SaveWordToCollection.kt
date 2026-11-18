package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.model.CollectionDestination
import com.luudev.vocalens.domain.model.VocabularyWord
import com.luudev.vocalens.domain.repository.WordCollectionRepository
import java.util.UUID

class SaveWordToCollection(private val repository: WordCollectionRepository) {
    suspend operator fun invoke(userId: String, word: VocabularyWord, destination: CollectionDestination) {
        require(userId.isNotBlank())
        val clean = word.copy(english = word.english.trim(), meaning = word.meaning.trim(),
            phonetic = word.phonetic.trim(), partOfSpeech = word.partOfSpeech?.trim()?.takeIf { it.isNotEmpty() },
            exampleSentence = word.exampleSentence?.trim()?.takeIf { it.isNotEmpty() })
        require(clean.english.length in 1..120 && clean.meaning.length in 1..2000)
        require(clean.phonetic.length <= 200 && (clean.exampleSentence?.length ?: 0) <= 4000)
        val target = when (destination) {
            is CollectionDestination.Existing -> destination.also { require(it.id.isNotBlank()) }
            is CollectionDestination.New -> CollectionDestination.New(destination.name.trim()).also {
                require(it.name.length in 1..120)
            }
        }
        // Content-based IDs make repeated saves idempotent; edits create a new word instead of
        // overwriting shared dictionary content belonging to other collections/accounts.
        val identity = listOf(clean.english, clean.meaning, clean.phonetic, clean.category.name,
            clean.partOfSpeech.orEmpty(), clean.exampleSentence.orEmpty()).joinToString("") { "${it.length}:$it" }
        val id = UUID.nameUUIDFromBytes("camera:$identity".toByteArray(Charsets.UTF_8)).toString()
        repository.save(userId, clean.copy(id = id), target)
    }
}
