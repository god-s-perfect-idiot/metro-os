package com.metro.conversations.tiles

import com.metro.conversations.data.ReplyableConversation
import com.metro.system.MetroAppRegistry
import com.metro.system.MetroTileData
import com.metro.system.MetroTilePeek

object ConversationsTileLogic {
    fun buildTileData(
        conversations: List<ReplyableConversation>,
        packageName: String,
        accentHex: String,
    ): MetroTileData {
        val label = MetroAppRegistry.label(packageName) ?: "conversations"
        val peeks = conversations.map { conversation ->
            MetroTilePeek(
                title = conversation.title.trim().ifBlank { label },
                subtitle = conversation.appLabel.trim().takeIf { it.isNotEmpty() },
                body = conversation.preview.trim().takeIf { it.isNotEmpty() },
            )
        }.filter { it.hasContent }
        return MetroTileData(
            title = label,
            backgroundColorHex = accentHex,
            counter = conversations.size.takeIf { it > 0 },
            peeks = peeks.takeIf { it.isNotEmpty() },
        )
    }
}
