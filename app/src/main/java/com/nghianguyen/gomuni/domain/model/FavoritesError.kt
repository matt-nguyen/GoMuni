package com.nghianguyen.gomuni.domain.model

sealed interface FavoritesError {
    data object Storage : FavoritesError
}
