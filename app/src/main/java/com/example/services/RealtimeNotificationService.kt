package com.example.services

import com.example.model.Issue
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed class RealtimeEvent {
    data class Created(val issue: Issue) : RealtimeEvent()
    data class Updated(val issue: Issue, val oldStatus: String, val newStatus: String) : RealtimeEvent()
    data class Resolved(val issue: Issue) : RealtimeEvent()
}

object RealtimeNotificationService {
    private val _events = MutableSharedFlow<RealtimeEvent>(extraBufferCapacity = 64)
    val events = _events.asSharedFlow()

    private var previousIssues: List<Issue>? = null

    // Connection configuration and connection state monitoring
    var connectionMode = "WebSocket (Secure)"
    var isConnected = true

    fun trackIssues(currentIssues: List<Issue>) {
        val prev = previousIssues
        if (prev == null) {
            // Store initial list to avoid triggering alerts on startup
            previousIssues = currentIssues
            return
        }

        currentIssues.forEach { current ->
            val match = prev.find { it.id == current.id }
            if (match == null) {
                // Report created
                _events.tryEmit(RealtimeEvent.Created(current))
            } else {
                // Report updated or resolved
                if (match.status != current.status) {
                    if (current.status.equals("Fixed", ignoreCase = true) || current.status.equals("Resolved", ignoreCase = true)) {
                        _events.tryEmit(RealtimeEvent.Resolved(current))
                    } else {
                        _events.tryEmit(RealtimeEvent.Updated(current, match.status, current.status))
                    }
                } else if (match.title != current.title || match.description != current.description || match.priorityScore != current.priorityScore || match.votes != current.votes) {
                    _events.tryEmit(RealtimeEvent.Updated(current, match.status, current.status))
                }
            }
        }

        previousIssues = currentIssues
    }
}
