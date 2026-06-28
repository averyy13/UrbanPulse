package com.example.services

import android.util.Log
import com.example.model.Comment
import com.example.model.Notification
import com.example.model.Report
import com.example.model.User
import com.example.model.Vote
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.awaitClose

class FirestoreService {

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.e("FirestoreService", "Failed to initialize FirebaseFirestore: ${e.message}")
            null
        }
    }

    // --- Users Collection Operations ---

    suspend fun createUser(user: User): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        db.collection("users").document(user.uid).set(user).await()
    }

    suspend fun getUser(uid: String): Result<User?> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val snapshot = db.collection("users").document(uid).get().await()
        if (snapshot.exists()) {
            snapshot.toObject(User::class.java)
        } else {
            null
        }
    }

    suspend fun updateUser(user: User): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        db.collection("users").document(user.uid).set(user).await()
    }

    suspend fun incrementUserReportCount(uid: String): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        if (uid.contains("@")) {
            val querySnapshot = db.collection("users").whereEqualTo("email", uid).get().await()
            for (doc in querySnapshot.documents) {
                doc.reference.update("reportCount", FieldValue.increment(1)).await()
            }
        } else {
            val docRef = db.collection("users").document(uid)
            try {
                docRef.update("reportCount", FieldValue.increment(1)).await()
            } catch (e: Exception) {
                // If user document does not exist yet (e.g. sandbox/demo/anonymous mode), initialize it
                val snapshot = docRef.get().await()
                if (!snapshot.exists()) {
                    val newUser = User(
                        uid = uid,
                        fullName = if (uid == "demo_user_id") "Demo Citizen" else "Active Citizen",
                        email = if (uid == "demo_user_id") "demo@example.com" else "anonymous@example.com",
                        createdAt = System.currentTimeMillis(),
                        reportCount = 1
                    )
                    docRef.set(newUser).await()
                } else {
                    throw e
                }
            }
        }
    }

    // --- Reports Collection Operations ---

    suspend fun createReport(report: Report): Result<String> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val ref = if (report.reportId.isEmpty()) {
            db.collection("reports").document()
        } else {
            db.collection("reports").document(report.reportId)
        }
        val finalReport = report.copy(reportId = ref.id)
        ref.set(finalReport).await()
        
        // Increment reportCount for this user
        if (report.userId.isNotEmpty()) {
            incrementUserReportCount(report.userId).getOrThrow()
        }
        
        finalReport.reportId
    }

    suspend fun getReport(reportId: String): Result<Report?> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val snapshot = db.collection("reports").document(reportId).get().await()
        if (snapshot.exists()) {
            snapshot.toObject(Report::class.java)
        } else {
            null
        }
    }

    suspend fun getReports(): Result<List<Report>> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val snapshot = db.collection("reports")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get().await()
        snapshot.toObjects(Report::class.java)
    }

    fun observeReports(): kotlinx.coroutines.flow.Flow<List<Report>> = kotlinx.coroutines.flow.callbackFlow {
        val db = firestore
        if (db == null) {
            close(Exception("Firestore is not initialized"))
            return@callbackFlow
        }
        val subscription = db.collection("reports")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Report::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun updateReport(report: Report): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        db.collection("reports").document(report.reportId).set(report).await()
    }

    suspend fun deleteReport(reportId: String): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val snapshot = db.collection("reports").document(reportId).get().await()
        if (snapshot.exists()) {
            val report = snapshot.toObject(Report::class.java)
            db.collection("reports").document(reportId).delete().await()
            if (report != null && report.userId.isNotEmpty()) {
                if (report.userId.contains("@")) {
                    val querySnapshot = db.collection("users").whereEqualTo("email", report.userId).get().await()
                    for (doc in querySnapshot.documents) {
                        doc.reference.update("reportCount", FieldValue.increment(-1)).await()
                    }
                } else {
                    try {
                        db.collection("users").document(report.userId)
                            .update("reportCount", FieldValue.increment(-1)).await()
                    } catch (e: Exception) {
                        // Suppress if user document does not exist
                    }
                }
            }
        }
    }

    // --- Comments Collection Operations ---

    suspend fun addComment(comment: Comment): Result<String> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val ref = db.collection("comments").document()
        val finalComment = comment.copy(commentId = ref.id, id = ref.id)
        val reportRef = db.collection("reports").document(comment.reportId)
        
        db.runTransaction { transaction ->
            // Perform all reads first
            val reportSnapshot = transaction.get(reportRef)
            
            // Perform all writes second
            transaction.set(ref, finalComment)
            if (reportSnapshot.exists()) {
                val currentCount = reportSnapshot.getLong("commentCount") ?: 0
                transaction.update(reportRef, "commentCount", currentCount + 1)
            }
        }.await()

        finalComment.commentId
    }

    suspend fun updateComment(comment: Comment): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        db.collection("comments").document(comment.commentId).set(comment).await()
    }

    suspend fun getCommentsForReport(reportId: String): Result<List<Comment>> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val snapshot = db.collection("comments")
            .whereEqualTo("reportId", reportId)
            .get().await()
        
        val list = snapshot.toObjects(Comment::class.java)
        list.sortedByDescending { it.createdAt }
    }

    suspend fun deleteComment(commentId: String): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val commentRef = db.collection("comments").document(commentId)
        val snapshot = commentRef.get().await()
        if (snapshot.exists()) {
            val comment = snapshot.toObject(Comment::class.java)
            if (comment != null && comment.reportId.isNotEmpty()) {
                val reportRef = db.collection("reports").document(comment.reportId)
                db.runTransaction { transaction ->
                    // Perform all reads first
                    val reportSnapshot = transaction.get(reportRef)
                    
                    // Perform all writes second
                    transaction.delete(commentRef)
                    if (reportSnapshot.exists()) {
                        val currentCount = reportSnapshot.getLong("commentCount") ?: 0
                        val newCount = if (currentCount > 0) currentCount - 1 else 0
                        transaction.update(reportRef, "commentCount", newCount)
                    }
                }.await()
            } else {
                commentRef.delete().await()
            }
        }
    }

    // --- Votes Collection Operations ---

    suspend fun addVote(vote: Vote): Result<String> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val voteDocId = "${vote.userId}_${vote.reportId}"
        val voteRef = db.collection("votes").document(voteDocId)
        val reportRef = db.collection("reports").document(vote.reportId)
        
        db.runTransaction { transaction ->
            // Perform all reads first
            val voteSnapshot = transaction.get(voteRef)
            val reportSnapshot = transaction.get(reportRef)
            
            if (voteSnapshot.exists()) {
                throw Exception("Already voted")
            }
            
            // Perform all writes second
            val finalVote = vote.copy(voteId = voteDocId)
            transaction.set(voteRef, finalVote)
            
            if (reportSnapshot.exists()) {
                val currentVotes = reportSnapshot.getLong("voteCount") ?: 0
                transaction.update(reportRef, "voteCount", currentVotes + 1)
            }
        }.await()
        
        voteDocId
    }

    suspend fun removeVote(reportId: String, userId: String): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val voteDocId = "${userId}_$reportId"
        db.collection("votes").document(voteDocId).delete().await()

        // Decrement vote count in report document
        db.collection("reports").document(reportId)
            .update("voteCount", FieldValue.increment(-1)).await()
    }

    suspend fun getVotesForReport(reportId: String): Result<List<Vote>> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val snapshot = db.collection("votes")
            .whereEqualTo("reportId", reportId)
            .get().await()
        snapshot.toObjects(Vote::class.java)
    }

    suspend fun hasUserVoted(reportId: String, userId: String): Result<Boolean> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val voteDocId = "${userId}_$reportId"
        val snapshot = db.collection("votes").document(voteDocId).get().await()
        snapshot.exists()
    }

    // --- Notifications Collection Operations ---

    suspend fun createNotification(notification: Notification): Result<String> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val ref = db.collection("notifications").document()
        val finalNotification = notification.copy(notificationId = ref.id)
        ref.set(finalNotification).await()
        finalNotification.notificationId
    }

    suspend fun getNotificationsForUser(userId: String): Result<List<Notification>> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        val snapshot = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .get().await()
        val list = snapshot.toObjects(Notification::class.java)
        list.sortedByDescending { it.createdAt }
    }

    fun observeNotifications(userId: String): kotlinx.coroutines.flow.Flow<List<Notification>> = kotlinx.coroutines.flow.callbackFlow {
        val db = firestore
        if (db == null) {
            close(Exception("Firestore is not initialized"))
            return@callbackFlow
        }
        val subscription = db.collection("notifications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val list = snapshot.toObjects(Notification::class.java).sortedByDescending { it.createdAt }
                    trySend(list)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        db.collection("notifications").document(notificationId)
            .update("isRead", true).await()
    }

    suspend fun deleteNotification(notificationId: String): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        db.collection("notifications").document(notificationId).delete().await()
    }

    suspend fun seedHeatmapDemoData(): Result<Unit> = runCatching {
        val db = firestore ?: throw Exception("Firestore is not initialized")
        
        val demoUsers = listOf(
            User(
                uid = "user_demo_1",
                fullName = "Jane Doe",
                email = "jane.doe@urbanpulse.org",
                createdAt = System.currentTimeMillis(),
                reportCount = 2
            ),
            User(
                uid = "user_demo_2",
                fullName = "John Smith",
                email = "john.smith@urbanpulse.org",
                createdAt = System.currentTimeMillis(),
                reportCount = 1
            ),
            User(
                uid = "user_demo_3",
                fullName = "Sarah Jenkins",
                email = "sarah.j@urbanpulse.org",
                createdAt = System.currentTimeMillis(),
                reportCount = 1
            )
        )
        
        for (user in demoUsers) {
            db.collection("users").document(user.uid).set(user).await()
        }
        
        val demoReports = listOf(
            Report(
                reportId = "report_demo_critical_1",
                userId = "user_demo_1",
                category = "Flooding",
                description = "Severe water leakage blocking the intersection of Market St & 5th St.",
                latitude = 37.7842,
                longitude = -122.4075,
                address = "Market St & 5th St, San Francisco, CA",
                priority = "Critical",
                status = "Pending",
                voteCount = 15,
                createdAt = System.currentTimeMillis() - 3600000
            ),
            Report(
                reportId = "report_demo_high_1",
                userId = "user_demo_2",
                category = "Pothole",
                description = "Deep road pothole on Mission St causing transit delays.",
                latitude = 37.7785,
                longitude = -122.4118,
                address = "1012 Mission St, San Francisco, CA",
                priority = "High",
                status = "Reviewing",
                voteCount = 8,
                createdAt = System.currentTimeMillis() - 7200000
            ),
            Report(
                reportId = "report_demo_medium_1",
                userId = "user_demo_3",
                category = "Broken Streetlight",
                description = "Flickering streetlight outside the Civic Center Library.",
                latitude = 37.7798,
                longitude = -122.4172,
                address = "Civic Center Plaza, San Francisco, CA",
                priority = "Medium",
                status = "Pending",
                voteCount = 4,
                createdAt = System.currentTimeMillis() - 10800000
            ),
            Report(
                reportId = "report_demo_low_1",
                userId = "user_demo_1",
                category = "Garbage",
                description = "Overfilled public trash bin on Valencia St.",
                latitude = 37.7618,
                longitude = -122.4215,
                address = "792 Valencia St, San Francisco, CA",
                priority = "Low",
                status = "Fixed",
                voteCount = 2,
                createdAt = System.currentTimeMillis() - 14400000
            )
        )
        
        for (report in demoReports) {
            db.collection("reports").document(report.reportId).set(report).await()
        }
    }
}
