package com.notifiy.noticeboard.data.cache

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import com.notifiy.noticeboard.data.model.Notice
import com.notifiy.noticeboard.data.model.NoticeBoard
import com.notifiy.noticeboard.data.model.Page
import com.notifiy.noticeboard.data.model.User

class CacheManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("noticeboard_cache", Context.MODE_PRIVATE)
    private val gson = GsonBuilder()
        .setLenient() // Allow lenient parsing
        .serializeNulls() // Include null values
        .create()
    
    init {
        checkCacheVersion()
    }
    
    private fun checkCacheVersion() {
        val storedVersion = prefs.getInt(KEY_CACHE_VERSION, 0)
        if (storedVersion != CACHE_VERSION) {
            // Clear all cache when version changes
            clearAllCache()
            prefs.edit().putInt(KEY_CACHE_VERSION, CACHE_VERSION).apply()
        }
    }
    
    companion object {
        private const val CACHE_EXPIRY_TIME = 5 * 60 * 1000L // 5 minutes
        private const val CACHE_VERSION = 1 // Increment this when data models change significantly
        private const val KEY_CACHE_VERSION = "cache_version"
        private const val KEY_USER_PREFIX = "user_"
        private const val KEY_NOTICE_BOARD_PREFIX = "noticeboard_"
        private const val KEY_NOTICE_BOARDS_LIST_PREFIX = "noticeboards_list_"
        private const val KEY_NOTICE_PREFIX = "notice_"
        private const val KEY_NOTICES_LIST_PREFIX = "notices_list_"
        private const val KEY_PAGE_PREFIX = "page_"
        private const val KEY_PAGES_LIST_PREFIX = "pages_list_"
        private const val KEY_ALL_PAGES = "all_pages"
    }
    
    // Cache data models
    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long,
        val lastUpdated: Long
    )
    
    // User caching
    fun cacheUser(userId: String, user: User) {
        val cacheEntry = CacheEntry(user, System.currentTimeMillis(), user.updatedAt)
        val json = gson.toJson(cacheEntry)
        prefs.edit().putString("$KEY_USER_PREFIX$userId", json).apply()
    }
    
    fun getCachedUser(userId: String): User? {
        val json = prefs.getString("$KEY_USER_PREFIX$userId", null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry<User>>() {}.type
            val cacheEntry: CacheEntry<User> = gson.fromJson(json, type)
            
            // Check if cache is still valid (not expired and data hasn't been updated)
            if (isCacheValid(cacheEntry.timestamp) && !hasDataChanged(cacheEntry.lastUpdated, cacheEntry.data.updatedAt)) {
                cacheEntry.data
            } else {
                // Cache is stale, remove it
                prefs.edit().remove("$KEY_USER_PREFIX$userId").apply()
                null
            }
        } catch (e: Exception) {
            // Invalid cache data, remove it
            prefs.edit().remove("$KEY_USER_PREFIX$userId").apply()
            null
        }
    }
    
    // NoticeBoard caching
    fun cacheNoticeBoard(boardId: String, board: NoticeBoard) {
        val cacheEntry = CacheEntry(board, System.currentTimeMillis(), board.updatedAt)
        val json = gson.toJson(cacheEntry)
        prefs.edit().putString("$KEY_NOTICE_BOARD_PREFIX$boardId", json).apply()
    }
    
    fun getCachedNoticeBoard(boardId: String): NoticeBoard? {
        val json = prefs.getString("$KEY_NOTICE_BOARD_PREFIX$boardId", null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry<NoticeBoard>>() {}.type
            val cacheEntry: CacheEntry<NoticeBoard> = gson.fromJson(json, type)
            
            if (isCacheValid(cacheEntry.timestamp) && !hasDataChanged(cacheEntry.lastUpdated, cacheEntry.data.updatedAt)) {
                cacheEntry.data
            } else {
                prefs.edit().remove("$KEY_NOTICE_BOARD_PREFIX$boardId").apply()
                null
            }
        } catch (e: Exception) {
            prefs.edit().remove("$KEY_NOTICE_BOARD_PREFIX$boardId").apply()
            null
        }
    }
    
    fun cacheNoticeBoardsList(key: String, boards: List<NoticeBoard>) {
        val cacheEntry = CacheEntry(boards, System.currentTimeMillis(), boards.maxOfOrNull { it.updatedAt } ?: 0L)
        val json = gson.toJson(cacheEntry)
        prefs.edit().putString("$KEY_NOTICE_BOARDS_LIST_PREFIX$key", json).apply()
    }
    
    fun getCachedNoticeBoardsList(key: String): List<NoticeBoard>? {
        val json = prefs.getString("$KEY_NOTICE_BOARDS_LIST_PREFIX$key", null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry<List<NoticeBoard>>>() {}.type
            val cacheEntry: CacheEntry<List<NoticeBoard>> = gson.fromJson(json, type)
            
            if (isCacheValid(cacheEntry.timestamp) && !hasDataChanged(cacheEntry.lastUpdated, cacheEntry.data.maxOfOrNull { it.updatedAt } ?: 0L)) {
                cacheEntry.data
            } else {
                prefs.edit().remove("$KEY_NOTICE_BOARDS_LIST_PREFIX$key").apply()
                null
            }
        } catch (e: Exception) {
            prefs.edit().remove("$KEY_NOTICE_BOARDS_LIST_PREFIX$key").apply()
            null
        }
    }
    
    // Notice caching
    fun cacheNotice(noticeId: String, notice: Notice) {
        val cacheEntry = CacheEntry(notice, System.currentTimeMillis(), notice.updatedAt)
        val json = gson.toJson(cacheEntry)
        prefs.edit().putString("$KEY_NOTICE_PREFIX$noticeId", json).apply()
    }
    
    fun getCachedNotice(noticeId: String): Notice? {
        val json = prefs.getString("$KEY_NOTICE_PREFIX$noticeId", null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry<Notice>>() {}.type
            val cacheEntry: CacheEntry<Notice> = gson.fromJson(json, type)
            
            if (isCacheValid(cacheEntry.timestamp) && !hasDataChanged(cacheEntry.lastUpdated, cacheEntry.data.updatedAt)) {
                cacheEntry.data
            } else {
                prefs.edit().remove("$KEY_NOTICE_PREFIX$noticeId").apply()
                null
            }
        } catch (e: Exception) {
            prefs.edit().remove("$KEY_NOTICE_PREFIX$noticeId").apply()
            null
        }
    }
    
    fun cacheNoticesList(key: String, notices: List<Notice>) {
        val cacheEntry = CacheEntry(notices, System.currentTimeMillis(), notices.maxOfOrNull { it.updatedAt } ?: 0L)
        val json = gson.toJson(cacheEntry)
        prefs.edit().putString("$KEY_NOTICES_LIST_PREFIX$key", json).apply()
    }
    
    fun getCachedNoticesList(key: String): List<Notice>? {
        val json = prefs.getString("$KEY_NOTICES_LIST_PREFIX$key", null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry<List<Notice>>>() {}.type
            val cacheEntry: CacheEntry<List<Notice>> = gson.fromJson(json, type)
            
            if (isCacheValid(cacheEntry.timestamp) && !hasDataChanged(cacheEntry.lastUpdated, cacheEntry.data.maxOfOrNull { it.updatedAt } ?: 0L)) {
                cacheEntry.data
            } else {
                prefs.edit().remove("$KEY_NOTICES_LIST_PREFIX$key").apply()
                null
            }
        } catch (e: Exception) {
            prefs.edit().remove("$KEY_NOTICES_LIST_PREFIX$key").apply()
            null
        }
    }
    
    // Page caching
    fun cachePage(pageId: String, page: Page) {
        val cacheEntry = CacheEntry(page, System.currentTimeMillis(), page.updatedAt)
        val json = gson.toJson(cacheEntry)
        prefs.edit().putString("$KEY_PAGE_PREFIX$pageId", json).apply()
    }
    
    fun getCachedPage(pageId: String): Page? {
        val json = prefs.getString("$KEY_PAGE_PREFIX$pageId", null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry<Page>>() {}.type
            val cacheEntry: CacheEntry<Page> = gson.fromJson(json, type)
            
            if (isCacheValid(cacheEntry.timestamp) && !hasDataChanged(cacheEntry.lastUpdated, cacheEntry.data.updatedAt)) {
                cacheEntry.data
            } else {
                prefs.edit().remove("$KEY_PAGE_PREFIX$pageId").apply()
                null
            }
        } catch (e: Exception) {
            prefs.edit().remove("$KEY_PAGE_PREFIX$pageId").apply()
            null
        }
    }
    
    fun cachePagesList(key: String, pages: List<Page>) {
        val cacheEntry = CacheEntry(pages, System.currentTimeMillis(), pages.maxOfOrNull { it.updatedAt } ?: 0L)
        val json = gson.toJson(cacheEntry)
        prefs.edit().putString("$KEY_PAGES_LIST_PREFIX$key", json).apply()
    }
    
    fun getCachedPagesList(key: String): List<Page>? {
        val json = prefs.getString("$KEY_PAGES_LIST_PREFIX$key", null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry<List<Page>>>() {}.type
            val cacheEntry: CacheEntry<List<Page>> = gson.fromJson(json, type)
            
            if (isCacheValid(cacheEntry.timestamp) && !hasDataChanged(cacheEntry.lastUpdated, cacheEntry.data.maxOfOrNull { it.updatedAt } ?: 0L)) {
                cacheEntry.data
            } else {
                prefs.edit().remove("$KEY_PAGES_LIST_PREFIX$key").apply()
                null
            }
        } catch (e: Exception) {
            prefs.edit().remove("$KEY_PAGES_LIST_PREFIX$key").apply()
            null
        }
    }
    
    fun cacheAllPages(pages: List<Page>) {
        val cacheEntry = CacheEntry(pages, System.currentTimeMillis(), pages.maxOfOrNull { it.updatedAt } ?: 0L)
        val json = gson.toJson(cacheEntry)
        prefs.edit().putString(KEY_ALL_PAGES, json).apply()
    }
    
    fun getCachedAllPages(): List<Page>? {
        val json = prefs.getString(KEY_ALL_PAGES, null) ?: return null
        return try {
            val type = object : TypeToken<CacheEntry<List<Page>>>() {}.type
            val cacheEntry: CacheEntry<List<Page>> = gson.fromJson(json, type)
            
            if (isCacheValid(cacheEntry.timestamp) && !hasDataChanged(cacheEntry.lastUpdated, cacheEntry.data.maxOfOrNull { it.updatedAt } ?: 0L)) {
                cacheEntry.data
            } else {
                prefs.edit().remove(KEY_ALL_PAGES).apply()
                null
            }
        } catch (e: Exception) {
            prefs.edit().remove(KEY_ALL_PAGES).apply()
            null
        }
    }
    
    // Cache invalidation methods
    fun invalidateUser(userId: String) {
        prefs.edit().remove("$KEY_USER_PREFIX$userId").apply()
    }
    
    fun invalidateNoticeBoard(boardId: String) {
        prefs.edit().remove("$KEY_NOTICE_BOARD_PREFIX$boardId").apply()
        // Also invalidate any lists that might contain this board
        val keys = prefs.all.keys.filter { it.startsWith(KEY_NOTICE_BOARDS_LIST_PREFIX) }
        keys.forEach { prefs.edit().remove(it).apply() }
    }
    
    fun invalidateNotice(noticeId: String) {
        prefs.edit().remove("$KEY_NOTICE_PREFIX$noticeId").apply()
        // Also invalidate any lists that might contain this notice
        val keys = prefs.all.keys.filter { it.startsWith(KEY_NOTICES_LIST_PREFIX) }
        keys.forEach { prefs.edit().remove(it).apply() }
    }
    
    fun invalidatePage(pageId: String) {
        prefs.edit().remove("$KEY_PAGE_PREFIX$pageId").apply()
        // Also invalidate any lists that might contain this page
        val keys = prefs.all.keys.filter { it.startsWith(KEY_PAGES_LIST_PREFIX) }
        keys.forEach { prefs.edit().remove(it).apply() }
        prefs.edit().remove(KEY_ALL_PAGES).apply()
    }
    
    fun invalidateNoticeBoardsList(key: String) {
        prefs.edit().remove("$KEY_NOTICE_BOARDS_LIST_PREFIX$key").apply()
        println("DEBUG: CacheManager - Invalidated notice boards list: $key")
    }
    
    fun clearAllCache() {
        prefs.edit().clear().apply()
    }
    
    // Helper methods
    private fun isCacheValid(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp < CACHE_EXPIRY_TIME
    }
    
    private fun hasDataChanged(cachedTimestamp: Long, currentTimestamp: Long): Boolean {
        return currentTimestamp > cachedTimestamp
    }
}
