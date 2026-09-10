package com.guruai.app.memory

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,          // "user", "assistant", "tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
