package com.focusflow.app.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** App-wide channel for background write failures so the UI can tell the user. */
object RepositoryErrors {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun report(message: String) {
        _events.tryEmit(message)
    }
}
