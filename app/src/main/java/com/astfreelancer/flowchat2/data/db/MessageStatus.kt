// data/db/MessageStatus.kt
package com.astfreelancer.flowchat2.data.db

enum class MessageStatus {
    PENDING, // сохранено локально, ждет отправки
    SENT, // сервер принял
    DELIVERED, // доставлено на устройство собеседника
    READ // собеседник прочитал
}
