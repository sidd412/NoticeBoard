# Update System Implementation Guide

## Overview
This implementation provides a custom update mechanism for the NoticeBoard app that checks for updates from Firebase Firestore and displays appropriate popups based on configuration.

## Features
- ✅ Firebase Firestore integration with caching
- ✅ Version comparison (version code and version name)
- ✅ Force update support (non-dismissible popup)
- ✅ Skipable update support (user can skip)
- ✅ Beautiful Material Design 3 popup UI
- ✅ Automatic Play Store redirection
- ✅ Efficient caching to avoid repeated API calls

## Firebase Configuration

### Collection Structure
The update configuration is stored in Firebase Firestore under the `noteXpConfig` collection:

```
noteXpConfig/
└── JaPhY3e1ohDp1r5sDugs/
    ├── update_link: "https://play.google.com/store/apps/details?id=com.notifiy.noticeboard"
    ├── latest_version_code: 2
    ├── latest_version_name: "1.0.0"
    ├── force_update: false
    └── skipable_update: true
```

### Field Descriptions
- **update_link**: Play Store URL for the app update
- **latest_version_code**: Integer version code (must be higher than current app version)
- **latest_version_name**: String version name (e.g., "1.0.0")
- **force_update**: Boolean - if true, user cannot dismiss the popup
- **skipable_update**: Boolean - if true, user can skip the update

## Update Flow Logic

### Version Comparison
The system checks for updates using two criteria:
1. **Version Code**: If current app version code < latest version code
2. **Version Name**: If version codes are equal but version names differ

### Popup Behavior

#### Force Update (force_update = true)
- Popup cannot be dismissed by back button or outside click
- Only "Update Now" button is shown
- User must update to continue using the app

#### Skipable Update (skipable_update = true, force_update = false)
- Popup can be dismissed
- Both "Skip" and "Update Now" buttons are shown
- User can choose to skip the update

#### Non-skipable Update (skipable_update = false, force_update = false)
- Popup can be dismissed
- Only "Update Now" button is shown
- User can dismiss but will see the popup again on next app launch

## Implementation Details

### Files Created/Modified

1. **UpdateConfig.kt** - Data model for update configuration
2. **CacheManager.kt** - Extended with update config caching methods
3. **FirebaseRepository.kt** - Added getUpdateConfig() method
4. **UpdateService.kt** - Core update checking logic
5. **UpdateDialog.kt** - Beautiful Material Design 3 popup UI
6. **MainActivity.kt** - Integrated update check on app startup

### Caching Strategy
- Update config is cached for 5 minutes (same as other data)
- Cache is invalidated when data is updated in Firebase
- Prevents unnecessary Firebase calls on every app launch

### Error Handling
- Graceful fallback if Firebase is unavailable
- Handles missing update config gracefully
- Logs errors for debugging purposes

## Testing Scenarios

### Test Case 1: Force Update
```json
{
  "update_link": "https://play.google.com/store/apps/details?id=com.notifiy.noticeboard",
  "latest_version_code": 3,
  "latest_version_name": "1.1.0",
  "force_update": true,
  "skipable_update": false
}
```
**Expected**: Non-dismissible popup with only "Update Now" button

### Test Case 2: Skipable Update
```json
{
  "update_link": "https://play.google.com/store/apps/details?id=com.notifiy.noticeboard",
  "latest_version_code": 3,
  "latest_version_name": "1.1.0",
  "force_update": false,
  "skipable_update": true
}
```
**Expected**: Dismissible popup with "Skip" and "Update Now" buttons

### Test Case 3: No Update Needed
```json
{
  "update_link": "https://play.google.com/store/apps/details?id=com.notifiy.noticeboard",
  "latest_version_code": 1,
  "latest_version_name": "1.0.0",
  "force_update": false,
  "skipable_update": true
}
```
**Expected**: No popup shown

## Usage Instructions

### For Developers
1. Update the version code and name in your app's `build.gradle.kts`
2. Update the Firebase document with new version information
3. Set appropriate `force_update` and `skipable_update` flags
4. Test the update flow before releasing

### For Administrators
1. Access Firebase Console
2. Navigate to Firestore Database
3. Go to `noteXpConfig` collection
4. Edit the `JaPhY3e1ohDp1r5sDugs` document
5. Update the version fields and flags as needed

## Security Considerations
- Update link should always point to official Play Store
- Version codes should be incremented properly
- Force updates should be used sparingly for critical security updates

## Future Enhancements
- [ ] Support for multiple update channels (beta, stable)
- [ ] Update progress tracking
- [ ] Custom update messages
- [ ] Update scheduling (e.g., only show during certain hours)
- [ ] A/B testing for update prompts
