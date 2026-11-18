package com.luudev.vocalens.domain.model

sealed interface CollectionDestination {
    data class Existing(val id: String) : CollectionDestination
    data class New(val name: String) : CollectionDestination
}
