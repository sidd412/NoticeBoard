# Firebase Push Notifications Setup Guide

This guide will help you set up Firebase Cloud Messaging (FCM) push notifications for your NoticeBoard app.

## Overview

The implementation includes:
1. **Local Notifications**: For app-specific alerts (file downloads, etc.)
2. **Firebase Push Notifications**: For real-time updates across devices

### Notification Types

1. **Board Updates**: When a board's pages are created/updated → subscribers get notified
2. **Query Notifications**: When a user makes a query → board owner gets notified  
3. **Query Resolution**: When board owner resolves a query → query raiser gets notified

## Step-by-Step Setup

### 1. Firebase Console Setup

#### Enable Cloud Messaging
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** → **Cloud Messaging**
4. Note down your **Server Key** (you'll need this for Cloud Functions)

#### Enable Cloud Functions
1. In Firebase Console, go to **Functions**
2. Click **Get started** if not already enabled
3. This will enable the Cloud Functions API

### 2. Install Firebase CLI

```bash
npm install -g firebase-tools
```

### 3. Login to Firebase

```bash
firebase login
```

### 4. Initialize Firebase Functions

```bash
cd functions
npm install
```

### 5. Deploy Cloud Functions

```bash
# Build and deploy functions
firebase deploy --only functions
```

### 6. Android App Configuration

The Android app is already configured with:
- FCM dependency in `build.gradle.kts`
- Firebase Messaging Service (`MyFirebaseMessagingService`)
- FCM Token Manager (`FCMTokenManager`)
- Updated repository methods for push notifications

### 7. Test the Setup

#### Test Board Update Notifications
1. Create a new page in a board
2. Check that subscribers receive push notifications
3. Verify notification counts update on home screen

#### Test Query Notifications
1. Make a query to a board
2. Check that board owner receives push notification
3. Verify query count updates on "Your Boards" screen

#### Test Query Resolution
1. Resolve a query as board owner
2. Check that query raiser receives push notification

## Database Structure

### New Collections

#### `pushNotifications`
```javascript
{
  id: "user123_board_update_board456_1234567890",
  userId: "user123",
  fcmToken: "fcm_token_here",
  boardId: "board456",
  boardCode: "ORG001",
  boardName: "Organization Name",
  title: "New Notice Update",
  body: "A new notice has been added...",
  type: "board_update", // or "query", "query_resolved"
  queryId: "query123", // optional, for query-related notifications
  status: "pending", // "pending", "sent", "failed"
  createdAt: 1234567890
}
```

### Updated User Model
```javascript
{
  // ... existing fields
  fcmToken: "fcm_token_here" // New field for FCM token
}
```

## Cloud Functions

### Functions Created

1. **`processPushNotifications`**: Processes pending push notifications
2. **`onBoardPageUpdate`**: Triggers when board pages are created/updated
3. **`onUserQueryCreate`**: Triggers when user queries are created
4. **`onUserQueryUpdate`**: Triggers when queries are resolved
5. **`cleanupOldNotifications`**: Cleans up old notification records (runs daily)

### Function Triggers

- **Board Updates**: `pages/{pageId}` document write
- **Query Creation**: `userQueries/{queryId}` document create
- **Query Resolution**: `userQueries/{queryId}` document update (status change)
- **Push Processing**: `pushNotifications/{notificationId}` document create

## Notification Count System

### Home Screen Board Cards
- Shows notification count for subscribed boards
- Count increases when board gets new pages
- Count resets to 0 when user clicks on board card

### Your Boards Screen
- Shows unresolved query count for owned boards
- Count increases when new queries are received
- Count decreases when queries are resolved

## Troubleshooting

### Common Issues

1. **Functions not deploying**
   ```bash
   # Check Firebase CLI version
   firebase --version
   
   # Re-login if needed
   firebase login --reauth
   ```

2. **FCM tokens not updating**
   - Check if user is authenticated
   - Verify FCM token is being stored in user document
   - Check Firebase Console for token registration

3. **Notifications not received**
   - Verify FCM token is valid
   - Check device notification permissions
   - Verify Cloud Functions are deployed and running

### Debug Commands

```bash
# View function logs
firebase functions:log

# Test functions locally
firebase emulators:start --only functions

# Check function status
firebase functions:list
```

## Security Rules

Update your Firestore rules to allow Cloud Functions to write to `pushNotifications`:

```javascript
// Allow Cloud Functions to write to pushNotifications
match /pushNotifications/{notificationId} {
  allow read, write: if false; // Only Cloud Functions can access
}
```

## Monitoring

### Firebase Console
- **Functions**: Monitor function execution and errors
- **Cloud Messaging**: View message delivery statistics
- **Firestore**: Monitor database operations

### Logs
- Function logs: `firebase functions:log`
- App logs: Check Android Studio Logcat for FCM-related messages

## Next Steps

1. Deploy the Cloud Functions
2. Test all notification scenarios
3. Monitor function performance and error rates
4. Set up alerts for function failures
5. Consider implementing notification preferences per user

## Support

If you encounter issues:
1. Check Firebase Console for errors
2. Review function logs
3. Verify FCM token registration
4. Test with Firebase emulators locally



