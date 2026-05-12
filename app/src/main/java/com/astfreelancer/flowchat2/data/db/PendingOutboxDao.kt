package com.astfreelancer.flowchat2.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOutboxDao {

    @Query("SELECT * FROM pending_outbox ORDER BY createdAt ASC")
    fun allPending(): Flow<List<PendingOutboxEntity>>

    @Insert
    suspend fun insert(entry: PendingOutboxEntity)

    @Query("DELETE FROM pending_outbox WHERE messageId = :messageId")
    suspend fun deleteByMessageId(messageId: String)
}
