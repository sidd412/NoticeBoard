# Caching System - Schema Change Guide

## How Caching Handles New Fields

### ✅ **Automatic Handling (No Code Changes Needed):**

1. **Adding New Optional Fields**
   ```kotlin
   // Before
   data class User(
       val id: String = "",
       val name: String = "",
       val email: String = ""
   )
   
   // After - Adding new field
   data class User(
       val id: String = "",
       val name: String = "",
       val email: String = "",
       val phoneNumber: String = "", // NEW FIELD
       val profilePicture: String = "" // NEW FIELD
   )
   ```
   **Result**: ✅ Works automatically! Old cached data will have `null`/default values for new fields.

2. **Adding New Required Fields with Defaults**
   ```kotlin
   data class NoticeBoard(
       val id: String = "",
       val organizationName: String = "",
       val organizationCode: String = "",
       val isActive: Boolean = true, // NEW FIELD with default
       val tags: List<String> = emptyList() // NEW FIELD with default
   )
   ```
   **Result**: ✅ Works automatically!

### ⚠️ **Requires Cache Version Increment:**

1. **Removing Fields**
   ```kotlin
   // Before
   data class User(
       val id: String = "",
       val name: String = "",
       val oldField: String = "" // TO BE REMOVED
   )
   
   // After
   data class User(
       val id: String = "",
       val name: String = ""
       // oldField removed
   )
   ```
   **Action**: Increment `CACHE_VERSION` in `CacheManager.kt`

2. **Changing Field Types**
   ```kotlin
   // Before
   data class NoticeBoard(
       val subscriptionExpiry: String = "" // String type
   )
   
   // After
   data class NoticeBoard(
       val subscriptionExpiry: Long = 0L // Changed to Long
   )
   ```
   **Action**: Increment `CACHE_VERSION` in `CacheManager.kt`

3. **Renaming Fields**
   ```kotlin
   // Before
   data class User(
       val phoneNumber: String = ""
   )
   
   // After
   data class User(
       val mobileNumber: String = "" // Renamed field
   )
   ```
   **Action**: Increment `CACHE_VERSION` in `CacheManager.kt`

## How to Handle Schema Changes

### **Step 1: Update Data Models**
```kotlin
// Add your new fields with appropriate defaults
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val newField: String = "", // NEW FIELD
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
```

### **Step 2: Check if Cache Version Needs Update**
- **Adding fields**: ✅ No change needed
- **Removing/changing/renaming fields**: ⚠️ Increment version

### **Step 3: Update Cache Version (if needed)**
```kotlin
// In CacheManager.kt
companion object {
    private const val CACHE_VERSION = 2 // Increment from 1 to 2
    // ... rest of constants
}
```

### **Step 4: Test**
- Old cached data will be automatically cleared
- New data will be cached with new schema
- No crashes or data corruption

## Migration Examples

### **Example 1: Adding User Profile Picture**
```kotlin
// Before
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = ""
)

// After
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val profilePicture: String = "" // NEW FIELD
)
```
**Action**: ✅ No code changes needed! Caching works automatically.

### **Example 2: Changing Notice Priority from String to Enum**
```kotlin
// Before
data class Notice(
    val priority: String = "NORMAL"
)

// After
enum class NoticePriority { LOW, NORMAL, HIGH, URGENT }

data class Notice(
    val priority: NoticePriority = NoticePriority.NORMAL
)
```
**Action**: ⚠️ Increment `CACHE_VERSION` from 1 to 2

### **Example 3: Adding Complex Nested Objects**
```kotlin
// Before
data class NoticeBoard(
    val id: String = "",
    val organizationName: String = ""
)

// After
data class ContactInfo(
    val email: String = "",
    val phone: String = "",
    val address: String = ""
)

data class NoticeBoard(
    val id: String = "",
    val organizationName: String = "",
    val contactInfo: ContactInfo = ContactInfo() // NEW NESTED OBJECT
)
```
**Action**: ✅ No code changes needed! Gson handles nested objects automatically.

## Best Practices

1. **Always provide default values** for new fields
2. **Use nullable types** for truly optional fields
3. **Increment cache version** for breaking changes
4. **Test with old cached data** before releasing
5. **Use `@SerializedName`** for field name mapping if needed

## Cache Version Management

```kotlin
// When to increment CACHE_VERSION:
// ✅ Adding new fields with defaults
// ✅ Adding optional nullable fields
// ✅ Adding nested objects with defaults
// ⚠️ Removing fields
// ⚠️ Changing field types
// ⚠️ Renaming fields
// ⚠️ Changing enum values
// ⚠️ Changing default values significantly
```

The caching system is designed to be resilient and handle most schema changes automatically!
