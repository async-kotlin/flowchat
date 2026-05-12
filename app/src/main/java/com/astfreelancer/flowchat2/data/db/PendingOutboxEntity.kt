// data/db/PendingOutboxEntity.kt
package com.astfreelancer.flowchat2.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_outbox")
data class PendingOutboxEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val messageId: String,
    val createdAt: Long = System.currentTimeMillis()
)
