package com.notifiy.noticeboard.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.notifiy.noticeboard.data.cache.CacheManager
import com.notifiy.noticeboard.data.model.Notice
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.data.model.User
import kotlinx.coroutines.tasks.await

class FirebaseRepository(private val context: Context? = null) {
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val cacheManager: CacheManager? = context?.let { CacheManager(it) }
    
    // User operations
    suspend fun getCurrentUser(): User? {
        return try {
            val currentUser = auth.currentUser ?: return null
            println("DEBUG: Firebase Auth current user: ${currentUser.uid}")
            
            // Check cache first
            val cachedUser = cacheManager?.getCachedUser(currentUser.uid)
            if (cachedUser != null) {
                println("DEBUG: Returning cached user: $cachedUser")
                return cachedUser
            }
            
            val document = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()
            
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                println("DEBUG: Found user document: $user")
                // Cache the user
                user?.let { cacheManager?.cacheUser(currentUser.uid, it) }
                user
            } else {
                println("DEBUG: No user document found for ${currentUser.uid}")
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Error getting current user: ${e.message}")
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
    
    // Notice Board operations
    suspend fun getNoticeBoardById(boardId: String): NoticeBoard? {
        return try {
            // Check cache first
            val cachedBoard = cacheManager?.getCachedNoticeBoard(boardId)
            if (cachedBoard != null) {
                println("DEBUG: Returning cached notice board: $cachedBoard")
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
            println("DEBUG: Searching for board with code: $code")
            
            // Check cache first - we'll use code as cache key for this method
            val cachedBoard = cacheManager?.getCachedNoticeBoard("code_$code")
            if (cachedBoard != null) {
                println("DEBUG: Returning cached board by code: $cachedBoard")
                return cachedBoard
            }
            
            // Remove isActive filter since it's stored as null in Firestore
            val querySnapshot = firestore.collection("noticeBoards")
                .whereEqualTo("organizationCode", code)
                .get()
                .await()
            
            println("DEBUG: Query returned ${querySnapshot.size()} documents")
            if (!querySnapshot.isEmpty) {
                val board = querySnapshot.documents.first().toObject(NoticeBoard::class.java)
                println("DEBUG: Found board by code: $board")
                // Cache the board with both ID and code keys
                board?.let { 
                    cacheManager?.cacheNoticeBoard(board.id, it)
                    cacheManager?.cacheNoticeBoard("code_$code", it)
                }
                board
            } else {
                println("DEBUG: No board found with code: $code")
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Error finding board by code $code: ${e.message}")
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
            println("DEBUG: Creating notice board with ID: ${noticeBoard.id}")
            println("DEBUG: Board data: $noticeBoard")
            firestore.collection("noticeBoards")
                .document(noticeBoard.id)
                .set(noticeBoard)
                .await()
            println("DEBUG: Notice board created successfully in Firestore")
            // Cache the created board and invalidate related caches
            cacheManager?.cacheNoticeBoard(noticeBoard.id, noticeBoard)
            cacheManager?.cacheNoticeBoard("code_${noticeBoard.organizationCode}", noticeBoard)
            cacheManager?.invalidateNoticeBoard(noticeBoard.id)
            Result.success(noticeBoard)
        } catch (e: Exception) {
            println("DEBUG: Error creating notice board: ${e.message}")
            Result.failure(e)
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
            Result.success(true)
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
    suspend fun getPagesByBoardCode(boardCode: Int): List<Page> {
        return try {
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - Starting query for boardCode: $boardCode")
            
            // Check cache first
            val cachedPages = cacheManager?.getCachedPagesList("board_code_$boardCode")
            if (cachedPages != null) {
                println("DEBUG: Returning cached pages for board code: ${cachedPages.size}")
                return cachedPages
            }
            
            val querySnapshot = firestore.collection("pages")
                .whereEqualTo("code", boardCode)
                .get()
                .await()
            
            println("DEBUG: FirebaseRepository.getPagesByBoardCode - Query returned ${querySnapshot.size()} documents")
            
            val pages = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val page = doc.toObject(Page::class.java)
                    println("DEBUG: FirebaseRepository.getPagesByBoardCode - Document ${doc.id}: $page")
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
            cacheManager?.cachePagesList("board_code_$boardCode", sortedPages)
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
            println("DEBUG: FirebaseRepository.getAllPages - Getting all pages")
            
            // Check cache first
            val cachedPages = cacheManager?.getCachedAllPages()
            if (cachedPages != null) {
                println("DEBUG: Returning cached all pages: ${cachedPages.size}")
                return cachedPages
            }
            
            val querySnapshot = firestore.collection("pages")
                .get()
                .await()
            
            println("DEBUG: FirebaseRepository.getAllPages - Query returned ${querySnapshot.size()} documents")
            
            val pages = querySnapshot.documents.mapNotNull { doc ->
                try {
                    val page = doc.toObject(Page::class.java)
                    println("DEBUG: FirebaseRepository.getAllPages - Document ${doc.id}: code=${page?.code}, title=${page?.title}")
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
    suspend fun updateNoticeBoardSubscription(boardId: String, subscriptionType: String, subscriptionExpiry: Long): Result<Boolean> {
        return try {
            println("DEBUG: FirebaseRepository.updateNoticeBoardSubscription - Updating subscription for board: $boardId")
            firestore.collection("noticeBoards")
                .document(boardId)
                .update(
                    mapOf(
                        "subscriptionType" to subscriptionType,
                        "subscriptionExpiry" to subscriptionExpiry,
                        "updatedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            println("DEBUG: FirebaseRepository.updateNoticeBoardSubscription - Subscription updated successfully")
            // Invalidate board cache since subscription was updated
            cacheManager?.invalidateNoticeBoard(boardId)
            Result.success(true)
        } catch (e: Exception) {
            println("DEBUG: FirebaseRepository.updateNoticeBoardSubscription - Error: ${e.message}")
            Result.failure(e)
        }
    }
}
