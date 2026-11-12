package com.luudev.vocalens.domain.usecase

import com.luudev.vocalens.domain.repository.CollectionRepository

class ObserveCollections(private val repository: CollectionRepository) {
    operator fun invoke(userId: String) = repository.observeCollections(userId)
}

class ObserveCollectionWords(private val repository: CollectionRepository) {
    operator fun invoke(userId: String, id: String) = repository.observeWords(userId, id)
}

class SaveCollection(private val repository: CollectionRepository) {
    suspend operator fun invoke(userId: String, id: String?, name: String, description: String) {
        require(name.trim().length in 1..120 && description.trim().length <= 1000)
        if (id == null) repository.create(userId, name.trim(), description.trim())
        else repository.update(userId, id, name.trim(), description.trim())
    }
}

class DeleteCollection(private val repository: CollectionRepository) {
    suspend operator fun invoke(userId: String, id: String) = repository.delete(userId, id)
}

class SetCollectionWords(private val repository: CollectionRepository) {
    suspend operator fun invoke(userId: String, id: String, wordIds: Set<String>) =
        repository.setWords(userId, id, wordIds)
}

class RemoveCollectionWord(private val repository: CollectionRepository) {
    suspend operator fun invoke(userId: String, id: String, wordId: String) =
        repository.removeWord(userId, id, wordId)
}
