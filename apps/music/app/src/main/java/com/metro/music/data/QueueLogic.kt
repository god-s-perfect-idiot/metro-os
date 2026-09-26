package com.metro.music.data

/**
 * Pure helpers for the now-playing queue list (logical playlist, not the short
 * MediaController materialisation window).
 */
object QueueLogic {
    fun indexOfSong(queue: List<Song>, songId: String?): Int {
        if (songId.isNullOrEmpty()) return -1
        return queue.indexOfFirst { it.id == songId }
    }

    fun upNext(queue: List<Song>, currentSongId: String?): Song? {
        val index = indexOfSong(queue, currentSongId)
        if (index < 0 || index >= queue.lastIndex) return null
        return queue[index + 1]
    }

    fun upNextLabel(queue: List<Song>, currentSongId: String?): String {
        val next = upNext(queue, currentSongId) ?: return "Up next: —"
        return "Up next: ${next.title}"
    }

    /** Songs after [currentIndex] that should be in the Media3 window but are not yet. */
    fun missingAhead(
        queue: List<Song>,
        currentIndex: Int,
        lookahead: Int,
        materialisedIds: Set<String>,
    ): List<Song> {
        if (currentIndex < 0 || lookahead <= 0) return emptyList()
        return queue.drop(currentIndex + 1).take(lookahead)
            .filter { it.id !in materialisedIds }
    }

    /** Songs before [currentIndex] that should be in the Media3 window but are not yet. */
    fun missingBehind(
        queue: List<Song>,
        currentIndex: Int,
        lookbehind: Int,
        materialisedIds: Set<String>,
    ): List<Song> {
        if (currentIndex <= 0 || lookbehind <= 0) return emptyList()
        return queue.take(currentIndex).takeLast(lookbehind)
            .filter { it.id !in materialisedIds }
    }

    /**
     * Shuffle the queue for playback: current track stays first so it keeps playing;
     * everything else is randomised. Pass [random] for tests.
     */
    fun shuffleKeepingCurrent(
        queue: List<Song>,
        currentSongId: String?,
        random: java.util.Random = java.util.Random(),
    ): List<Song> {
        if (queue.size <= 1) return queue
        val index = indexOfSong(queue, currentSongId)
        if (index < 0) {
            return queue.toMutableList().also { it.shuffle(random) }
        }
        val current = queue[index]
        val rest = (queue.take(index) + queue.drop(index + 1)).toMutableList()
        rest.shuffle(random)
        return listOf(current) + rest
    }
}
