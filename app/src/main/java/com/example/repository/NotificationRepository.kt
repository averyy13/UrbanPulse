package com.example.repository

import com.example.model.Notification
import com.example.services.FirestoreService

interface NotificationRepository {
    suspend fun createNotification(notification: Notification): Result<String>
    suspend fun getNotificationsForUser(userId: String): Result<List<Notification>>
    fun observeNotifications(userId: String): kotlinx.coroutines.flow.Flow<List<Notification>>
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>
    suspend fun deleteNotification(notificationId: String): Result<Unit>
}

class NotificationRepositoryImpl(
    private val firestoreService: FirestoreService = FirestoreService()
) : NotificationRepository {
    override suspend fun createNotification(notification: Notification): Result<String> = firestoreService.createNotification(notification)
    override suspend fun getNotificationsForUser(userId: String): Result<List<Notification>> = firestoreService.getNotificationsForUser(userId)
    override fun observeNotifications(userId: String): kotlinx.coroutines.flow.Flow<List<Notification>> = firestoreService.observeNotifications(userId)
    override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = firestoreService.markNotificationAsRead(notificationId)
    override suspend fun deleteNotification(notificationId: String): Result<Unit> = firestoreService.deleteNotification(notificationId)
}
