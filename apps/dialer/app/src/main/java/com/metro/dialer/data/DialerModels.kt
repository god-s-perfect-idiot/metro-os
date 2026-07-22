package com.metro.dialer.data

enum class CallDirection {
    Incoming,
    Outgoing,
    Missed,
}

data class CallEntry(
    val id: Long,
    val phoneNumber: String,
    val normalizedNumber: String,
    val type: CallDirection,
    val timestamp: Long,
    val durationSeconds: Int,
    val contactName: String?,
)

data class CallGroup(
    val normalizedNumber: String,
    val phoneNumber: String,
    val displayName: String,
    val latestType: CallDirection,
    val latestTimestamp: Long,
    val callCount: Int,
    val calls: List<CallEntry>,
)

data class ContactSuggestion(
    val displayName: String,
    val phoneNumber: String,
    val normalizedNumber: String,
)

data class SpeedDialEntry(
    val id: String,
    val displayName: String,
    val phoneNumber: String,
)

data class ActiveCall(
    val phoneNumber: String,
    val displayName: String,
    val startedAtMillis: Long,
    val direction: CallDirection,
    val connected: Boolean = false,
)
