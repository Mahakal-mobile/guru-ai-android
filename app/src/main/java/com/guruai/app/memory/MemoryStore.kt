package com.guruai.app.memory

import android.content.Context

class MemoryStore(context: Context) {
    private val dao = AppDatabase.getDatabase(context).memoryDao()

    suspend fun saveMessage(role: String, content: String) {
        dao.insert(MessageEntity(role = role, content = content))
    }

    suspend fun getAllMessages(): List<MessageEntity> {
        return dao.getAll()
    }

    suspend fun clearAll() {
        dao.clear()
    }
}
