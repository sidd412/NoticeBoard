package com.notifiy.noticeboard.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.notifiy.noticeboard.data.cache.CacheManager
import com.notifiy.noticeboard.data.model.BoardDeletionRequest
import com.notifiy.noticeboard.data.model.DataExportRequest
import com.notifiy.noticeboard.data.model.Notice
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.data.model.Plan
import com.notifiy.noticeboard.data.model.UpdateConfig
import com.notifiy.noticeboard.data.model.User
import com.notifiy.noticeboard.data.model.UserNotification
import com.notifiy.noticeboard.services.LocalNotificationService
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlin.random.Random

class FirebaseRepository(private val context: Context? = null) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val cacheManager: CacheManager? = context?.let { CacheManager(it) }
    
    // User operations
    suspend fun getCurrentUser(): User? {
        return try {
            val currentUser = auth.currentUser ?: return null
            // Check cache first
            val cachedUser = cacheManager?.getCachedUser(currentUser.uid)
            if (cachedUser != null) {
                return cachedUser
            }
            
            val document = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                // Cache the user
                user?.let { cacheManager?.cacheUser(currentUser.uid, it) }
                user
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createUser(user: User): Result<User> {
        return try {
            firestore.collection("users")
                .document(user.id)
                .set(user)
                .await()
            // Cache the created user
            cacheManager?.cacheUser(user.id, user)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUser(user: User): Result<User> {
        return try {
            val updatedUser = user.copy(updatedAt = System.currentTimeMillis())
            firestore.collection("users")
                .document(user.id)
                .set(updatedUser)
                .await()
            // Cache the updated user and invalidate old cache
            cacheManager?.cacheUser(user.id, updatedUser)
            Result.success(updatedUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            // Delete user document from Firestore
            firestore.collection("users")
                .document(userId)
                .delete()
                .await()
            
            // Clear user from cache
            cacheManager?.invalidateUser(userId)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteAllUserNoticeBoards(userId: String): Result<Int> {
        return try {
            println("accountdeletion: FirebaseRepository - Starting deletion of all notice boards for user: $userId")
            
            // Get all boards created by this user
            val userBoards = getUserNoticeBoards(userId)
            println("accountdeletion: FirebaseRepository - Found ${userBoards.size} boards to delete")
            
            var deletedCount = 0
            var failedCount = 0
            
            // Delete each board
            for (board in userBoards) {
                try {
                    val deleteResult = deleteNoticeBoard(board.id)
                    if (deleteResult.isSuccess) {
                        deletedCount++
                        println("accountdeletion: FirebaseRepository - Successfully deleted board: ${board.organizationName}")
                    } else {
                        failedCount++
                        println("accountdeletion: FirebaseRepository - Failed to delete board: ${board.organizationName}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    println("accountdeletion: FirebaseRepository - Exception deleting board ${board.organizationName}: ${e.message}")
                }
            }
            
            println("accountdeletion: FirebaseRepository - Board deletion completed: $deletedCount deleted, $failedCount failed")
            Result.success(deletedCount)
        } catch (e: Exception) {
            println("accountdeletion: FirebaseRepository - Error deleting user notice boards: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun clearAllCache(): Result<Unit> {
        return try {
            println("accountdeletion: FirebaseRepository - Starting cache clearing")
            // Clear all cached data
            cacheManager?.clearAllCache()
            println("accountdeletion: FirebaseRepository - All cache cleared successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            println("accountdeletion: FirebaseRepository - Error clearing cache: ${e.message}")
            println("accountdeletion: FirebaseRepository - Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // Notice Board operations
    suspend fun getNoticeBoardById(boardId: String): NoticeBoard? {
        return try {
            // Check cache first
            val cachedBoard = cacheManager?.getCachedNoticeBoard(boardId)
            if (cachedBoard != null) {
                return cachedBoard
            }
            
            val document = firestore.collection("noticeBoards")
                .document(boardId)
                .get()
                .await()
            
            if (document.exists()) {
                val board = document.toObject(NoticeBoard::class.java)
                // Cache the board
                board?.let { cacheManager?.cacheNoticeBoard(boardId, it) }
                board
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getNoticeBoardByCode(code: String): NoticeBoard? {
        return try {
            // Check cache first - we'll use code as cache key for this method
            val cachedBoard = cacheManager?.getCachedNoticeBoard("code_$code")
            if (cachedBoard != null) {
                return cachedBoard
            }
            
            // Remove isActive filter since it's stored as null in Firestore
            val querySnapshot = firestore.collection("noticeBoards")
                .whereEqualTo("organizationCode", code)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val board = querySnapshot.documents.first().toObject(NoticeBoard::class.java)
                // Cache the board with both ID and code keys
                board?.let { 
                    cacheManager?.cacheNoticeBoard(board.id, it)
                    cacheManager?.cacheNoticeBoard("code_$code", it)
                }
                board
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getNoticeBoardByEmail(email: String): NoticeBoard? {
        return try {
            val querySnapshot = firestore.collection("noticeBoards")
                .whereEqualTo("organizationEmail", email)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents.first().toObject(NoticeBoard::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun getNoticeBoardByWhatsapp(whatsapp: String): NoticeBoard? {
        return try {
            val querySnapshot = firestore.collection("noticeBoards")
                .whereEqualTo("organizationWhatsapp", whatsapp)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                querySnapshot.documents.first().toObject(NoticeBoard::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createNoticeBoard(noticeBoard: NoticeBoard): Result<NoticeBoard> {
        return try {
            // Check if organization code already exists in noticeBoards collection
            if (noticeBoard.organizationCode.isNotBlank()) {
                val existingBoard = getNoticeBoardByCode(noticeBoard.organizationCode)
                if (existingBoard != null) {
                    return Result.failure(Exception("Organization code '${noticeBoard.organizationCode}' already exists. Please generate a new code."))
                }
            }
            
            // Create the notice board
            firestore.collection("noticeBoards")
                .document(noticeBoard.id)
                .set(noticeBoard)
                .await()
            
            // Add the organization code to NotexpCOded collection
            if (noticeBoard.organizationCode.isNotBlank()) {
                addCodeToNotexpCOded(noticeBoard.organizationCode, noticeBoard.id)
            }
            
            // Cache the created board and invalidate related caches
            cacheManager?.cacheNoticeBoard(noticeBoard.id, noticeBoard)
            cacheManager?.cacheNoticeBoard("code_${noticeBoard.organizationCode}", noticeBoard)
            cacheManager?.invalidateNoticeBoard(noticeBoard.id)
            Result.success(noticeBoard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun generateUniqueOrganizationCode(): String {
        var code: String
        var isUnique = false
        var attempts = 0
        val maxAttempts = 10
        
        do {
            code = generateRandomCode()
            val existingBoard = getNoticeBoardByCode(code)
            isUnique = (existingBoard == null)
            attempts++
            
            if (attempts >= maxAttempts) {
                throw Exception("Unable to generate unique organization code after $maxAttempts attempts")
            }
        } while (!isUnique)
        
        return code
    }
    
    private fun generateRandomCode(): String {
        val letters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val numbers = "0123456789"
        
        // Generate 3 random letters
        val randomLetters = (1..3).map { letters.random() }.joinToString("")
        
        // Generate 3 random numbers
        val randomNumbers = (1..3).map { numbers.random() }.joinToString("")
        
        return randomLetters + randomNumbers
    }
    
    suspend fun addCodeToNotexpCOded(code: String, boardId: String): Result<Boolean> {
        return try {
            val codeData = mapOf(
                "code" to code,
                "boardId" to boardId,
                "createdAt" to System.currentTimeMillis(),
                "isActive" to true
            )
            
            firestore.collection("noteXpCodes")
                .document(code) // Use code as document ID for easy lookup
                .set(codeData)
                .await()
            
            println("DEBUG: Successfully added code '$code' to noteXpCodes collection")
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: Error adding code to noteXpCodes collection: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getNotexpCodeByCode(code: String): Any? {
        return try {
            val document = firestore.collection("noteXpCodes")
                .document(code)
                .get()
                .await()
            
            if (document.exists()) {
                document.data
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun updateNoticeBoard(noticeBoard: NoticeBoard): Result<NoticeBoard> {
        return try {
            val updatedBoard = noticeBoard.copy(updatedAt = System.currentTimeMillis())
            firestore.collection("noticeBoards")
                .document(noticeBoard.id)
                .set(updatedBoard)
                .await()
            // Cache the updated board and invalidate related caches
            cacheManager?.cacheNoticeBoard(noticeBoard.id, updatedBoard)
            cacheManager?.cacheNoticeBoard("code_${noticeBoard.organizationCode}", updatedBoard)
            cacheManager?.invalidateNoticeBoard(noticeBoard.id)
            Result.success(updatedBoard)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteNoticeBoard(boardId: String): Result<Boolean> {
        return try {
            android.util.Log.d("sidxp", "FirebaseRepository.deleteNoticeBoard - Starting deletion for board: $boardId")
            
            // First, get the board to get the organization code and createdBy
            val board = getNoticeBoardById(boardId)
            if (board == null) {
                android.util.Log.e("sidxp", "FirebaseRepository.deleteNoticeBoard - Board not found: $boardId")
                return Result.failure(Exception("Board not found"))
            }
            
            val organizationCode = board.organizationCode
            val createdBy = board.createdBy
            
            android.util.Log.d("sidxp", "FirebaseRepository.deleteNoticeBoard - Board details: code=$organizationCode, createdBy=$createdBy")
            
            // Delete the notice board document
            firestore.collection("noticeBoards")
                .document(boardId)
                .delete()
                .await()
            
            android.util.Log.d("sidxp", "FirebaseRepository.deleteNoticeBoard - Board deleted successfully")
            
            // Remove the institute code from user's instituteCodes array
            if (createdBy.isNotEmpty() && organizationCode.isNotEmpty()) {
                android.util.Log.d("sidxp", "FirebaseRepository.deleteNoticeBoard - Removing institute code $organizationCode from user $createdBy")
                
                val userDoc = firestore.collection("users")
                    .document(createdBy)
                    .get()
                    .await()
                
                if (userDoc.exists()) {
                    val currentCodes = userDoc.get("instituteCodes") as? List<String> ?: emptyList()
                    val updatedCodes = currentCodes.filter { it != organizationCode }
                    
                    android.util.Log.d("sidxp", "FirebaseRepository.deleteNoticeBoard - Current codes: $currentCodes")
                    android.util.Log.d("sidxp", "FirebaseRepository.deleteNoticeBoard - Updated codes: $updatedCodes")
                    
                    firestore.collection("users")
                        .document(createdBy)
                        .update(
                            "instituteCodes", updatedCodes,
                            "updatedAt", System.currentTimeMillis()
                        )
                        .await()
                    
                    android.util.Log.d("sidxp", "FirebaseRepository.deleteNoticeBoard - Institute code removed from user")
                }
            }
            
            // Invalidate all related caches
            cacheManager?.invalidateNoticeBoard(boardId)
            cacheManager?.invalidateNoticeBoard("code_$organizationCode")
            cacheManager?.invalidateUser(createdBy)
            cacheManager?.invalidateUser("current")
            cacheManager?.invalidateNoticeBoardsList("user_$createdBy")
            cacheManager?.invalidateNoticeBoardsList("subscribed_$createdBy")
            
            // Clear all cached data to force fresh fetch
            cacheManager?.clearAllCache()
            
            android.util.Log.d("sidxp", "FirebaseRepository.deleteNoticeBoard - Cache invalidated and data reloaded")
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("sidxp", "FirebaseRepository.deleteNoticeBoard - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun getUserNoticeBoards(userId: String): List<NoticeBoard> {
        return try {
            println("DEBUG: Getting user notice boards for userId: $userId")
            
            // Check cache first
            val cachedBoards = cacheManager?.getCachedNoticeBoardsList("user_$userId")
            if (cachedBoards != null) {
                println("DEBUG: Returning cached user notice boards: ${cachedBoards.size}")
                return cachedBoards
            }
            
            val user = getCurrentUser()
            println("DEBUG: Current user: $user")
            
            if (user != null && user.instituteCodes.isNotEmpty()) {
                println("DEBUG: User has institute codes: ${user.instituteCodes}")
                val boards = mutableListOf<NoticeBoard>()
                for (code in user.instituteCodes) {
                    val board = getNoticeBoardByCode(code)
                    println("DEBUG: Found board for code $code: $board")
                    board?.let { boards.add(it) }
                }
                println("DEBUG: Found ${boards.size} boards from institute codes")
                // Cache the boards list
                cacheManager?.cacheNoticeBoardsList("user_$userId", boards)
                boards
            } else {
                println("DEBUG: User has no institute codes, trying fallback by createdBy")
                // If user document doesn't exist or has no institute codes,
                // try to find boards by createdBy field as fallback
                val querySnapshot = firestore.collection("noticeBoards")
                    .whereEqualTo("createdBy", userId)
                    .get()
                    .await()
                
                val boards = querySnapshot.documents.mapNotNull { it.toObject(NoticeBoard::class.java) }
                println("DEBUG: Found ${boards.size} boards by createdBy fallback")
                // Cache the boards list
                cacheManager?.cacheNoticeBoardsList("user_$userId", boards)
                boards
            }
        } catch (e: Exception) {
            println("DEBUG: Error getting user notice boards: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun getSubscribedBoards(userId: String): List<NoticeBoard> {
        return try {
            println("DEBUG: getSubscribedBoards called for userId: $userId")
            
            // Check cache first
            val cachedBoards = cacheManager?.getCachedNoticeBoardsList("subscribed_$userId")
            if (cachedBoards != null) {
                println("DEBUG: Returning cached subscribed boards: ${cachedBoards.size}")
                return cachedBoards
            }
            
            val user = getCurrentUser()
            println("DEBUG: getSubscribedBoards - User: $user")
            if (user != null && user.subscribedCodes.isNotEmpty()) {
                println("DEBUG: getSubscribedBoards - User has subscribed codes: ${user.subscribedCodes}")
                val boards = mutableListOf<NoticeBoard>()
                for (code in user.subscribedCodes) {
                    val board = getNoticeBoardByCode(code)
                    println("DEBUG: getSubscribedBoards - Found board for subscribed code $code: $board")
                    board?.let { boards.add(it) }
                }
                println("DEBUG: getSubscribedBoards - Returning ${boards.size} subscribed boards")
                // Cache the boards list
                cacheManager?.cacheNoticeBoardsList("subscribed_$userId", boards)
                boards
            } else {
                println("DEBUG: getSubscribedBoards - User has no subscribed codes, returning empty list")
                emptyList()
            }
        } catch (e: Exception) {
            println("DEBUG: getSubscribedBoards - Error: ${e.message}")
            emptyList()
        }
    }
    
    // Notice operations
    suspend fun getNoticesByBoardId(boardId: String): List<Notice> {
        return try {
            // Check cache first
            val cachedNotices = cacheManager?.getCachedNoticesList("board_$boardId")
            if (cachedNotices != null) {
                println("DEBUG: Returning cached notices for board: ${cachedNotices.size}")
                return cachedNotices
            }
            
            val querySnapshot = firestore.collection("notices")
                .whereEqualTo("noticeBoardId", boardId)
                .whereEqualTo("isActive", true)
                .orderBy("publishedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val notices = querySnapshot.documents.mapNotNull { it.toObject(Notice::class.java) }
            // Cache the notices list
            cacheManager?.cacheNoticesList("board_$boardId", notices)
            notices
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun createNotice(notice: Notice): Result<Notice> {
        return try {
            firestore.collection("notices")
                .document(notice.id)
                .set(notice)
                .await()
            // Cache the created notice and invalidate related caches
            cacheManager?.cacheNotice(notice.id, notice)
            cacheManager?.invalidateNotice(notice.id)
            Result.success(notice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateNotice(notice: Notice): Result<Notice> {
        return try {
            val updatedNotice = notice.copy(updatedAt = System.currentTimeMillis())
            firestore.collection("notices")
                .document(notice.id)
                .set(updatedNotice)
                .await()
            // Cache the updated notice and invalidate related caches
            cacheManager?.cacheNotice(notice.id, updatedNotice)
            cacheManager?.invalidateNotice(notice.id)
            Result.success(updatedNotice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteNotice(noticeId: String): Result<Boolean> {
        return try {
            firestore.collection("notices")
                .document(noticeId)
                .update("isActive", false)
                .await()
            // Invalidate cache for this notice
            cacheManager?.invalidateNotice(noticeId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // User subscription operations
    suspend fun subscribeToBoardByCode(userId: String, instituteCode: String): Result<Boolean> {
        return try {
            println("DEBUG: subscribeToBoardByCode called with userId: $userId, instituteCode: $instituteCode")
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java)
                
                if (user != null) {
                    // User document exists, update it
                    if (!user.subscribedCodes.contains(instituteCode)) {
                        val updatedSubscribedCodes = user.subscribedCodes + instituteCode
                        val updatedUser = user.copy(
                            subscribedCodes = updatedSubscribedCodes,
                            updatedAt = System.currentTimeMillis()
                        )
                        println("DEBUG: subscribeToBoardByCode - Updating user with subscribed codes: $updatedSubscribedCodes")
                        transaction.set(userRef, updatedUser)
                    } else {
                        println("DEBUG: subscribeToBoardByCode - User already subscribed to code: $instituteCode")
                    }
                } else {
                    // User document doesn't exist, create it with the subscribed code
                    val newUser = User(
                        id = userId,
                        name = "User",
                        email = "",
                        phoneNumber = "",
                        instituteCodes = emptyList(),
                        subscribedCodes = listOf(instituteCode),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    println("DEBUG: subscribeToBoardByCode - Creating new user with subscribed code: $instituteCode")
                    transaction.set(userRef, newUser)
                }
            }.await()
            println("DEBUG: subscribeToBoardByCode - Subscription successful")
            // Invalidate user cache and subscribed boards cache
            cacheManager?.invalidateUser(userId)
            cacheManager?.invalidateNoticeBoardsList("subscribed_$userId")
            println("DEBUG: subscribeToBoardByCode - Cache invalidated for user and subscribed boards")
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: subscribeToBoardByCode - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    fun clearSubscribedBoardsCache(userId: String) {
        println("DEBUG: FirebaseRepository.clearSubscribedBoardsCache called for userId: $userId")
        cacheManager?.invalidateUser(userId)
        cacheManager?.invalidateNoticeBoardsList("subscribed_$userId")
    }
    
    suspend fun unsubscribeFromBoardByCode(userId: String, instituteCode: String): Result<Boolean> {
        return try {
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java)
                
                if (user != null) {
                    val updatedSubscribedCodes = user.subscribedCodes.filter { it != instituteCode }
                    val updatedUser = user.copy(
                        subscribedCodes = updatedSubscribedCodes,
                        updatedAt = System.currentTimeMillis()
                    )
                    transaction.set(userRef, updatedUser)
                }
            }.await()
            // Invalidate user cache and subscribed boards cache
            cacheManager?.invalidateUser(userId)
            cacheManager?.invalidateNoticeBoardsList("subscribed_$userId")
            println("DEBUG: unsubscribeFromBoardByCode - Cache invalidated for user and subscribed boards")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateUserPlan(userId: String, planId: String): Result<Boolean> {
        return try {
            println("DEBUG: FirebaseRepository.updateUserPlan - Updating user plan to $planId for user $userId")
            
            if (userId.isEmpty()) {
                println("DEBUG: FirebaseRepository.updateUserPlan - User ID is empty")
                return Result.failure(Exception("User ID is empty"))
            }
            
            if (planId.isEmpty()) {
                println("DEBUG: FirebaseRepository.updateUserPlan - Plan ID is empty")
                return Result.failure(Exception("Plan ID is empty"))
            }
            
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java)
                
                if (user != null) {
                    // User document exists, update it
                    val updatedUser = user.copy(
                        currentPlanId = planId,
                        updatedAt = System.currentTimeMillis()
                    )
                    println("DEBUG: FirebaseRepository.updateUserPlan - Updating existing user with plan ID: $planId")
                    transaction.set(userRef, updatedUser)
                } else {
                    // User document doesn't exist, create it with the plan ID
                    val newUser = User(
                        id = userId,
                        name = "User",
                        email = "",
                        phoneNumber = "",
                        instituteCodes = emptyList(),
                        subscribedCodes = emptyList(),
                        currentPlanId = planId,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    println("DEBUG: FirebaseRepository.updateUserPlan - Creating new user with plan ID: $planId")
                    transaction.set(userRef, newUser)
                }
            }.await()
            println("DEBUG: FirebaseRepository.updateUserPlan - Successfully updated user plan")
            // Invalidate user cache
            cacheManager?.invalidateUser(userId)
            return Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.updateUserPlan - Error updating user plan: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun addInstituteCodeToUser(userId: String, instituteCode: String): Result<Boolean> {
        return try {
            println("DEBUG: Adding institute code $instituteCode to user $userId")
            val userRef = firestore.collection("users").document(userId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(userRef)
                val user = snapshot.toObject(User::class.java)
                
                if (user != null) {
                    // User document exists, update it
                    if (!user.instituteCodes.contains(instituteCode)) {
                        val updatedInstituteCodes = user.instituteCodes + instituteCode
                        val updatedUser = user.copy(
                            instituteCodes = updatedInstituteCodes,
                            updatedAt = System.currentTimeMillis()
                        )
                        println("DEBUG: Updating existing user with institute codes: $updatedInstituteCodes")
                        transaction.set(userRef, updatedUser)
                    } else {
                        println("DEBUG: User already has institute code $instituteCode")
                    }
                } else {
                    // User document doesn't exist, create it with the institute code
                    val newUser = User(
                        id = userId,
                        name = "User",
                        email = "",
                        phoneNumber = "",
                        instituteCodes = listOf(instituteCode),
                        subscribedCodes = emptyList(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    println("DEBUG: Creating new user with institute code: $instituteCode")
                    transaction.set(userRef, newUser)
                }
            }.await()
            println("DEBUG: Successfully added institute code to user")
            // Invalidate user cache and user boards cache
            cacheManager?.invalidateUser(userId)
            return Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: Error adding institute code to user: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Debug method to check if a board exists
    suspend fun debugCheckBoardExists(code: String): NoticeBoard? {
        return try {
            println("DEBUG: Checking if board with code '$code' exists in Firestore")
            val querySnapshot = firestore.collection("noticeBoards")
                .whereEqualTo("organizationCode", code)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val board = querySnapshot.documents.first().toObject(NoticeBoard::class.java)
                println("DEBUG: Board with code '$code' EXISTS: $board")
                board
            } else {
                println("DEBUG: Board with code '$code' does NOT exist in Firestore")
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Error checking board existence: ${e.message}")
            null
        }
    }
    
    // Page operations
    suspend fun getPagesByBoardCode(boardCode: String): List<Page> {
        return try {
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - Starting query for boardCode: $boardCode")
            
            // Get current user ID
            val currentUser = auth.currentUser
            if (currentUser == null) {
                println("DEBUG: FirebaseRepository.getPagesByBoardCode - No authenticated user")
                return emptyList()
            }
            
            val userId = currentUser.uid
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - User ID: $userId")
            
            // Check cache first
            val cacheKey = "board_code_${boardCode}_user_${userId}"
            val cachedPages = cacheManager?.getCachedPagesList(cacheKey)
            if (cachedPages != null) {
                println("DEBUG: Returning cached pages for board code: ${cachedPages.size}")
                return cachedPages
            }
            
            val querySnapshot = firestore.collection("pages")
                .whereEqualTo("code", boardCode)
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - Query returned ${querySnapshot.size()} documents")
            
            val pages = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val page = doc.toObject(Page::class.java)
                    println("DEBUG: FirebaseRepository.getPagesByBoardCode - Document ${doc.id}: code=${page?.code}, userId=${page?.userId}, title=${page?.title}")
                    page
                } catch (e: Exception) {
                    println("DEBUG: FirebaseRepository.getPagesByBoardCode - Error converting document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - Successfully converted ${pages.size} pages")
            // Sort by createdAt in descending order (newest first)
            val sortedPages = pages.sortedByDescending { it.createdAt }
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - Sorted ${sortedPages.size} pages")
            // Cache the pages list
            cacheManager?.cachePagesList(cacheKey, sortedPages)
            sortedPages
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - Error: ${e.message}")
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - Error type: ${e.javaClass.simpleName}")
            emptyList()
        }
    }
    
    suspend fun getPageById(pageId: String): Page? {
        return try {
            // Check cache first
            val cachedPage = cacheManager?.getCachedPage(pageId)
            if (cachedPage != null) {
                println("DEBUG: Returning cached page: $cachedPage")
                return cachedPage
            }
            
            val document = firestore.collection("pages")
                .document(pageId)
                .get()
                .await()
            
            if (document.exists()) {
                val page = document.toObject(Page::class.java)
                // Cache the page
                page?.let { cacheManager?.cachePage(pageId, it) }
                page
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun createPage(page: Page): Result<Page> {
        return try {
            println("DEBUG: FirebaseRepository.createPage - Starting to create page: $page")
            val currentUser = auth.currentUser
            println("DEBUG: FirebaseRepository.createPage - Current Firebase user: ${currentUser?.uid}")
            
            if (currentUser == null) {
                println("DEBUG: FirebaseRepository.createPage - No authenticated user")
                return Result.failure(Exception("User not authenticated"))
            }
            
            firestore.collection("pages")
                .document(page.id)
                .set(page)
                .await()
            println("DEBUG: FirebaseRepository.createPage - Page created successfully")
            // Cache the created page and invalidate related caches
            cacheManager?.cachePage(page.id, page)
            cacheManager?.invalidatePage(page.id)
            Result.success(page)
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.createPage - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun updatePage(page: Page): Result<Page> {
        return try {
            val updatedPage = page.copy(updatedAt = System.currentTimeMillis())
            firestore.collection("pages")
                .document(page.id)
                .set(updatedPage)
                .await()
            // Cache the updated page and invalidate related caches
            cacheManager?.cachePage(page.id, updatedPage)
            cacheManager?.invalidatePage(page.id)
            Result.success(updatedPage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePage(pageId: String): Result<Boolean> {
        return try {
            firestore.collection("pages")
                .document(pageId)
                .delete()
                .await()
            // Invalidate cache for this page
            cacheManager?.invalidatePage(pageId)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getAllPages(): List<Page> {
        return try {
            println("DEBUG: FirebaseRepository.getAllPages - Getting all pages for current user")
            
            // Get current user ID
            val currentUser = auth.currentUser
            if (currentUser == null) {
                println("DEBUG: FirebaseRepository.getAllPages - No authenticated user")
                return emptyList()
            }
            
            val userId = currentUser.uid
            println("DEBUG: FirebaseRepository.getAllPages - User ID: $userId")
            
            // Check cache first
            val cacheKey = "all_pages_user_${userId}"
            val cachedPages = cacheManager?.getCachedAllPages()
            if (cachedPages != null) {
                println("DEBUG: Returning cached all pages: ${cachedPages.size}")
                return cachedPages
            }
            
            val querySnapshot = firestore.collection("pages")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            println("DEBUG: FirebaseRepository.getAllPages - Query returned ${querySnapshot.size()} documents")
            
            val pages = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val page = doc.toObject(Page::class.java)
                    println("DEBUG: FirebaseRepository.getAllPages - Document ${doc.id}: code=${page?.code}, userId=${page?.userId}, title=${page?.title}")
                    page
                } catch (e: Exception) {
                    println("DEBUG: FirebaseRepository.getAllPages - Error converting document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            println("DEBUG: FirebaseRepository.getAllPages - Successfully converted ${pages.size} pages")
            // Cache all pages
            cacheManager?.cacheAllPages(pages)
            pages
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.getAllPages - Error: ${e.message}")
            emptyList()
        }
    }
    
    // Subscription operations
    suspend fun updateUserSubscription(
        userId: String, 
        subscriptionExpiry: Long, 
        currentPlanId: String = "",
        subscriptionPeriod: String = "",
        planName: String = ""
    ): Result<Boolean> {
        return try {
            android.util.Log.d("sidxp", "FirebaseRepository.updateUserSubscription - Updating subscription for user: $userId")
            android.util.Log.d("sidxp", "FirebaseRepository.updateUserSubscription - Period: $subscriptionPeriod, expiry: $subscriptionExpiry, planId: $currentPlanId, planName: $planName")
            
            // Check if user exists first
            val userDoc = firestore.collection("users")
                .document(userId)
                .get()
                .await()
            
            if (!userDoc.exists()) {
                android.util.Log.d("sidxp", "FirebaseRepository.updateUserSubscription - User $userId does not exist")
                return Result.failure(Exception("User not found"))
            }
            
            android.util.Log.d("sidxp", "FirebaseRepository.updateUserSubscription - User exists, updating subscription")
            val updateData = mutableMapOf<String, Any>(
                "subscriptionExpiry" to subscriptionExpiry,
                "updatedAt" to System.currentTimeMillis()
            )
            
            if (currentPlanId.isNotEmpty()) {
                updateData["currentPlanId"] = currentPlanId
            }
            
            if (subscriptionPeriod.isNotEmpty()) {
                updateData["subscriptionPeriod"] = subscriptionPeriod
            }
            
            if (planName.isNotEmpty()) {
                updateData["planName"] = planName
            }
            
            firestore.collection("users")
                .document(userId)
                .update(updateData)
                .await()
            
            android.util.Log.d("sidxp", "FirebaseRepository.updateUserSubscription - Subscription updated successfully")
            
            // Invalidate all relevant caches
            cacheManager?.invalidateUser(userId)
            cacheManager?.invalidateUser("current")
            cacheManager?.invalidateNoticeBoardsList("user_$userId")
            cacheManager?.invalidateNoticeBoardsList("subscribed_$userId")
            
            // Clear all cached data to force fresh fetch
            cacheManager?.clearAllCache()
            
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("sidxp", "FirebaseRepository.updateUserSubscription - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    suspend fun checkBoardLimit(userId: String): Result<Pair<Boolean, Int>> {
        return try {
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - Checking board limit for user: $userId")
            
            // Get user's current plan
            val user = getCurrentUser()
            if (user == null) {
                android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - User not found")
                return Result.failure(Exception("User not found"))
            }
            
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - User found: ${user.name}")
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - User currentPlanId: '${user.currentPlanId}'")
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - User planName: '${user.planName}'")
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - User instituteCodes: ${user.instituteCodes}")
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - User instituteCodes.size: ${user.instituteCodes.size}")
            
            // Get all plans to find user's plan
            val plans = getAllPlans()
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - Available plans: ${plans.map { "${it.planName} (${it.boards} boards)" }}")
            
            val userPlan = plans.find { plan ->
                val nameMatch = plan.planName.equals(user.planName, ignoreCase = true)
                val idMatch = plan.planId.contains(user.currentPlanId) || plan.id == user.currentPlanId
                android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - Checking plan ${plan.planName}: nameMatch=$nameMatch, idMatch=$idMatch")
                nameMatch || idMatch
            }
            
            if (userPlan == null) {
                android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - User plan not found, allowing 1 board without plan")
                // Allow 1 board even without a plan
                val currentBoardCount = user.instituteCodes.size
                val canCreate = currentBoardCount < 1
                android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - No plan: $currentBoardCount/1 boards, can create: $canCreate")
                android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - Returning: canCreate=$canCreate, limit=1")
                return Result.success(Pair(canCreate, 1))
            }
            
            // Get current board count
            val currentBoardCount = user.instituteCodes.size
            val canCreate = currentBoardCount < userPlan.boards
            
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - User plan: ${userPlan.planName}, boards: ${userPlan.boards}")
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - Current boards: $currentBoardCount, can create: $canCreate")
            android.util.Log.d("board limit", "FirebaseRepository.checkBoardLimit - Returning: canCreate=$canCreate, limit=${userPlan.boards}")
            
            Result.success(Pair(canCreate, userPlan.boards))
        } catch (e: Exception) {
            android.util.Log.e("board limit", "FirebaseRepository.checkBoardLimit - Error: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    // Notification operations
    suspend fun getUserNotificationCount(userId: String): Int {
        return try {
            println("DEBUG: FirebaseRepository.getUserNotificationCount - Getting notification count for userId: $userId")
            
            // Check cache first
            val cachedCount = cacheManager?.getCachedNotificationCount("user_$userId")
            if (cachedCount != null) {
                println("DEBUG: Returning cached notification count: $cachedCount")
                return cachedCount
            }
            
            val querySnapshot = firestore.collection("userNotifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val totalCount = querySnapshot.documents.sumOf { doc ->
                val notification = doc.toObject(UserNotification::class.java)
                notification?.unreadCount ?: 0
            }
            
            println("DEBUG: FirebaseRepository.getUserNotificationCount - Total notification count: $totalCount")
            // Cache the notification count
            cacheManager?.cacheNotificationCount("user_$userId", totalCount)
            totalCount
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.getUserNotificationCount - Error: ${e.message}")
            0
        }
    }
    
    suspend fun getUserNotifications(userId: String): List<UserNotification> {
        return try {
            println("DEBUG: FirebaseRepository.getUserNotifications - Getting notifications for userId: $userId")
            
            val querySnapshot = firestore.collection("userNotifications")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val notifications = querySnapshot.documents.mapNotNull { doc ->
                doc.toObject(UserNotification::class.java)
            }
            
            println("DEBUG: FirebaseRepository.getUserNotifications - Found ${notifications.size} notifications")
            notifications
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.getUserNotifications - Error: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun markNotificationAsRead(userId: String, boardId: String): Result<Boolean> {
        return try {
            println("DEBUG: FirebaseRepository.markNotificationAsRead - Marking notification as read for userId: $userId, boardId: $boardId")
            
            val querySnapshot = firestore.collection("userNotifications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("boardId", boardId)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                println("DEBUG: FirebaseRepository.markNotificationAsRead - No notification found")
                return Result.success(true)
            }
            
            val batch = firestore.batch()
            querySnapshot.documents.forEach { doc ->
                batch.update(doc.reference, mapOf(
                    "unreadCount" to 0,
                    "lastViewedAt" to System.currentTimeMillis(),
                    "updatedAt" to System.currentTimeMillis()
                ))
            }
            
            batch.commit().await()
            println("DEBUG: FirebaseRepository.markNotificationAsRead - Notification marked as read")
            
            // Invalidate notification cache
            cacheManager?.invalidateNotificationCount("user_$userId")
            
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.markNotificationAsRead - Error: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun incrementNotificationCount(boardId: String, boardCode: String): Result<Boolean> {
        return try {
            println("DEBUG: FirebaseRepository.incrementNotificationCount - Incrementing notification count for boardId: $boardId")
            
            // Get all users subscribed to this board
            val subscribedUsersQuery = firestore.collection("users")
                .whereArrayContains("subscribedCodes", boardCode)
                .get()
                .await()
            
            val subscribedUsers = subscribedUsersQuery.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
            
            println("DEBUG: FirebaseRepository.incrementNotificationCount - Found ${subscribedUsers.size} subscribed users")
            
            val batch = firestore.batch()
            
            subscribedUsers.forEach { user ->
                val notificationId = "${user.id}_$boardId"
                val notificationRef = firestore.collection("userNotifications").document(notificationId)
                
                // Check if notification already exists
                val existingNotification = notificationRef.get().await().toObject(UserNotification::class.java)
                
                if (existingNotification != null) {
                    // Update existing notification
                    batch.update(notificationRef, mapOf(
                        "unreadCount" to (existingNotification.unreadCount + 1),
                        "updatedAt" to System.currentTimeMillis()
                    ))
                } else {
                    // Create new notification
                    val newNotification = UserNotification(
                        id = notificationId,
                        userId = user.id,
                        boardId = boardId,
                        boardCode = boardCode,
                        unreadCount = 1
                    )
                    batch.set(notificationRef, newNotification)
                }
                
                // Invalidate cache for this user
                cacheManager?.invalidateNotificationCount("user_${user.id}")
            }
            
            batch.commit().await()
            println("DEBUG: FirebaseRepository.incrementNotificationCount - Notification count incremented successfully")
            
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.incrementNotificationCount - Error: ${e.message}")
            Result.failure(e)
        }
    }

    // Search operations
    suspend fun searchNoticeBoards(query: String): List<NoticeBoard> {
        return try {
            println("DEBUG: FirebaseRepository.searchNoticeBoards - Searching for: $query")
            
            if (query.isBlank()) {
                return emptyList()
            }
            
            val searchQuery = query.lowercase().trim()
            val allBoards = getAllNoticeBoards()
            
            println("DEBUG: FirebaseRepository.searchNoticeBoards - Total boards loaded: ${allBoards.size}")
            allBoards.forEach { board ->
                println("DEBUG: Board - Name: '${board.organizationName}', Location: '${board.organizationLocation}', Code: '${board.organizationCode}'")
            }
            
            val filteredBoards = allBoards.filter { board ->
                val nameMatch = board.organizationName.lowercase().contains(searchQuery)
                val locationMatch = board.organizationLocation.lowercase().contains(searchQuery)
                val codeMatch = board.organizationCode.lowercase().contains(searchQuery)
                val emailMatch = board.organizationEmail.lowercase().contains(searchQuery)
                
                val matches = nameMatch || locationMatch || codeMatch || emailMatch
                if (matches) {
                    println("DEBUG: Match found - Name: '${board.organizationName}', Location: '${board.organizationLocation}', Code: '${board.organizationCode}'")
                }
                
                matches
            }
            
            println("DEBUG: FirebaseRepository.searchNoticeBoards - Found ${filteredBoards.size} matching boards for query: '$searchQuery'")
            filteredBoards
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.searchNoticeBoards - Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getAllPlans(): List<Plan> {
        return try {
            android.util.Log.d("sidxp", "getAllPlans - Getting all plans")
            
            val querySnapshot = firestore.collection("plans")
                .get()
                .await()
            
            android.util.Log.d("sidxp", "getAllPlans - Query returned ${querySnapshot.size()} documents")
            
            val plans = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val plan = doc.toObject(Plan::class.java)
                    android.util.Log.d("sidxp", "getAllPlans - Document ${doc.id}: planName='${plan?.planName}', pages=${plan?.pages}")
                    plan
                } catch (e: Exception) {
                    android.util.Log.e("sidxp", "getAllPlans - Error converting document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            android.util.Log.d("sidxp", "getAllPlans - Successfully converted ${plans.size} plans")
            plans
        } catch (e: Exception) {
            android.util.Log.e("sidxp", "getAllPlans - Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getAllNoticeBoards(): List<NoticeBoard> {
        return try {
            println("DEBUG: FirebaseRepository.getAllNoticeBoards - Getting all notice boards")
            
            // Check cache first, but only if it has data
            val cachedBoards = cacheManager?.getCachedNoticeBoardsList("all_boards")
            if (cachedBoards != null && cachedBoards.isNotEmpty()) {
                println("DEBUG: Returning cached all boards: ${cachedBoards.size}")
                cachedBoards.forEach { board ->
                    println("DEBUG: Cached Board - Name: '${board.organizationName}', Location: '${board.organizationLocation}', Code: '${board.organizationCode}'")
                }
                return cachedBoards
            }
            
            println("DEBUG: No cached boards found or cache is empty, fetching from Firestore...")
            
            // Try without the isActive filter first
            var querySnapshot = firestore.collection("noticeBoards")
                .get()
                .await()
            
            println("DEBUG: Firestore query (all boards) returned ${querySnapshot.size()} documents")
            
            // If no results, try with isActive filter
            if (querySnapshot.isEmpty) {
                println("DEBUG: No boards found without filter, trying with isActive=true...")
                querySnapshot = firestore.collection("noticeBoards")
                    .whereEqualTo("isActive", true)
                    .get()
                    .await()
                println("DEBUG: Firestore query (active boards) returned ${querySnapshot.size()} documents")
            }
            
            val boards = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val board = doc.toObject(NoticeBoard::class.java)
                    println("DEBUG: Firestore Board - Name: '${board?.organizationName}', Location: '${board?.organizationLocation}', Code: '${board?.organizationCode}', Active: '${board?.isActive}'")
                    board
                } catch (e: Exception) {
                    println("DEBUG: Error converting document ${doc.id}: ${e.message}")
                    null
                }
            }
            
            println("DEBUG: FirebaseRepository.getAllNoticeBoards - Successfully converted ${boards.size} boards")
            
            // Only cache if we have results
            if (boards.isNotEmpty()) {
                cacheManager?.cacheNoticeBoardsList("all_boards", boards)
            }
            
            boards
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.getAllNoticeBoards - Error: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    // Clear search cache for testing
    fun clearSearchCache() {
        cacheManager?.invalidateNoticeBoardsList("all_boards")
        println("DEBUG: FirebaseRepository.clearSearchCache - Search cache cleared")
    }

    // Local notification management
    suspend fun sendLocalNotificationToSubscribers(boardId: String, boardCode: String, title: String, body: String): Result<Boolean> {
        return try {
            println("DEBUG: FirebaseRepository.sendLocalNotificationToSubscribers - Sending local notification for boardId: $boardId")
            
            // Get all users subscribed to this board
            val subscribedUsersQuery = firestore.collection("users")
                .whereArrayContains("subscribedCodes", boardCode)
                .get()
                .await()
            
            val subscribedUsers = subscribedUsersQuery.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)
            }
            
            println("DEBUG: FirebaseRepository.sendLocalNotificationToSubscribers - Found ${subscribedUsers.size} subscribed users")
            
            // Show local notification (only for current user if they're subscribed)
            val currentUser = getCurrentUser()
            val isCurrentUserSubscribed = currentUser?.let { user ->
                subscribedUsers.any { it.id == user.id }
            } ?: false
            
            if (isCurrentUserSubscribed && context != null) {
                val notificationService = LocalNotificationService(context)
                val boardName = getNoticeBoardById(boardId)?.organizationName ?: "Notice Board"
                notificationService.showNotification(title, body, boardId, boardName)
                println("DEBUG: FirebaseRepository.sendLocalNotificationToSubscribers - Local notification shown to current user")
            }
            
            println("DEBUG: FirebaseRepository.sendLocalNotificationToSubscribers - Local notification process completed")
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.sendLocalNotificationToSubscribers - Error: ${e.message}")
            Result.failure(e)
        }
    }

    // Board Deletion Request operations
    suspend fun createBoardDeletionRequest(request: BoardDeletionRequest): Result<BoardDeletionRequest> {
        return try {
            println("DEBUG: Creating board deletion request for board: ${request.boardId}")
            firestore.collection("boardDeletionRequests")
                .document(request.id)
                .set(request)
                .await()
            println("DEBUG: Board deletion request created successfully")
            Result.success(request)
        } catch (e: Exception) {
            println("DEBUG: Error creating board deletion request: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getBoardDeletionRequestByBoardId(boardId: String): BoardDeletionRequest? {
        return try {
            println("DEBUG: Checking for existing deletion request for board: $boardId")
            val querySnapshot = firestore.collection("boardDeletionRequests")
                .whereEqualTo("boardId", boardId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val request = querySnapshot.documents.first().toObject(BoardDeletionRequest::class.java)
                println("DEBUG: Found existing deletion request: $request")
                request
            } else {
                println("DEBUG: No existing deletion request found for board: $boardId")
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Error checking for existing deletion request: ${e.message}")
            null
        }
    }
    
    suspend fun getAllBoardDeletionRequests(): List<BoardDeletionRequest> {
        return try {
            println("DEBUG: Getting all board deletion requests")
            val querySnapshot = firestore.collection("boardDeletionRequests")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val requests = querySnapshot.documents.mapNotNull { it.toObject(BoardDeletionRequest::class.java) }
            println("DEBUG: Found ${requests.size} board deletion requests")
            requests
        } catch (e: Exception) {
            println("DEBUG: Error getting all board deletion requests: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun updateBoardDeletionRequestStatus(requestId: String, status: String): Result<Boolean> {
        return try {
            println("DEBUG: Updating board deletion request status: $requestId to $status")
            firestore.collection("boardDeletionRequests")
                .document(requestId)
                .update(
                    mapOf(
                        "status" to status,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            println("DEBUG: Board deletion request status updated successfully")
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: Error updating board deletion request status: ${e.message}")
            Result.failure(e)
        }
    }

    // Data Export Request operations
    suspend fun createDataExportRequest(request: DataExportRequest): Result<DataExportRequest> {
        return try {
            println("DEBUG: Creating data export request for user: ${request.userId}")
            firestore.collection("dataExportRequests")
                .document(request.id)
                .set(request)
                .await()
            println("DEBUG: Data export request created successfully")
            Result.success(request)
        } catch (e: Exception) {
            println("DEBUG: Error creating data export request: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getDataExportRequestByUserId(userId: String): DataExportRequest? {
        return try {
            println("DEBUG: Checking for existing export request for user: $userId")
            val querySnapshot = firestore.collection("dataExportRequests")
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                val request = querySnapshot.documents.first().toObject(DataExportRequest::class.java)
                println("DEBUG: Found existing export request: $request")
                request
            } else {
                println("DEBUG: No existing export request found for user: $userId")
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Error checking for existing export request: ${e.message}")
            null
        }
    }
    
    suspend fun getAllDataExportRequests(): List<DataExportRequest> {
        return try {
            println("DEBUG: Getting all data export requests")
            val querySnapshot = firestore.collection("dataExportRequests")
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
            
            val requests = querySnapshot.documents.mapNotNull { it.toObject(DataExportRequest::class.java) }
            println("DEBUG: Found ${requests.size} data export requests")
            requests
        } catch (e: Exception) {
            println("DEBUG: Error getting all data export requests: ${e.message}")
            emptyList()
        }
    }
    
    suspend fun updateDataExportRequestStatus(requestId: String, status: String, downloadUrl: String = "", fileSize: Long = 0L, adminNotes: String = ""): Result<Boolean> {
        return try {
            println("DEBUG: Updating data export request status: $requestId to $status")
            val updateData = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            
            if (downloadUrl.isNotEmpty()) {
                updateData["downloadUrl"] = downloadUrl
            }
            if (fileSize > 0) {
                updateData["fileSize"] = fileSize
            }
            if (adminNotes.isNotEmpty()) {
                updateData["adminNotes"] = adminNotes
            }
            if (status == "COMPLETED") {
                updateData["processedAt"] = System.currentTimeMillis()
            }
            
            firestore.collection("dataExportRequests")
                .document(requestId)
                .update(updateData)
                .await()
            println("DEBUG: Data export request status updated successfully")
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: Error updating data export request status: ${e.message}")
            Result.failure(e)
        }
    }
    
    // Update config operations
    suspend fun getUpdateConfig(): UpdateConfig? {
        return try {
            // Check cache first
            val cachedConfig = cacheManager?.getCachedUpdateConfig()
            if (cachedConfig != null) {
                return cachedConfig
            }
            
            // Add timeout to prevent hanging
            val document = withTimeout(8000) { // 8 seconds timeout
                firestore.collection("noteXpConfig")
                    .document("JaPhY3e1ohDp1r5sDugs") // Using the document ID from your Firebase
                    .get()
                    .await()
            }
            
            if (document.exists()) {
                // Try both approaches: automatic mapping and manual mapping
                val updateConfig = try {
                    document.toObject(UpdateConfig::class.java)
                } catch (e: Exception) {
                    null // If automatic mapping fails, use manual mapping
                }
                
                // Manual mapping as fallback
                val data = document.data ?: emptyMap()
                val manualConfig = UpdateConfig(
                    updateLink = data["update_link"] as? String ?: "",
                    latestVersionCode = (data["latest_version_code"] as? Long)?.toInt() ?: 0,
                    latestVersionName = data["latest_version_name"] as? String ?: "",
                    forceUpdate = data["force_update"] as? Boolean ?: false,
                    skipableUpdate = data["skipable_update"] as? Boolean ?: true,
                    updatedAt = System.currentTimeMillis()
                )
                
                // Use manual config if auto config has default values or is null
                val finalConfig = if (updateConfig?.latestVersionCode == 0 && manualConfig.latestVersionCode > 0) {
                    manualConfig
                } else {
                    updateConfig ?: manualConfig
                }
                
                // Validate the final config before caching
                if (finalConfig.latestVersionCode > 0 && finalConfig.updateLink.isNotBlank()) {
                    // Cache the update config
                    cacheManager?.cacheUpdateConfig(finalConfig)
                    finalConfig
                } else {
                    null // Invalid config, don't cache
                }
            } else {
                null
            }
        } catch (e: TimeoutCancellationException) {
            // Handle timeout gracefully
            null
        } catch (e: Exception) {
            // Handle any other exceptions (network, parsing, etc.)
            null
        }
    }
}
