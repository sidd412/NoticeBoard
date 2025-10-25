import * as functions from "firebase-functions";
import * as admin from "firebase-admin";

// Initialize Firebase Admin SDK
admin.initializeApp();

// Get Firestore instance
const db = admin.firestore();

// Interface for push notification data
interface PushNotificationData {
  id: string;
  userId: string;
  fcmToken: string;
  boardId: string;
  boardCode: string;
  boardName: string;
  title: string;
  body: string;
  type: string;
  queryId?: string;
  status: string;
  createdAt: number;
}

// Function to send FCM notification
async function sendFCMNotification(notificationData: PushNotificationData): Promise<void> {
  try {
    const message = {
      token: notificationData.fcmToken,
      notification: {
        title: notificationData.title,
        body: notificationData.body,
      },
      data: {
        boardId: notificationData.boardId,
        boardCode: notificationData.boardCode,
        boardName: notificationData.boardName,
        type: notificationData.type,
        queryId: notificationData.queryId || "",
      },
      android: {
        notification: {
          channelId: "firebase_notifications",
          priority: "high" as const,
          defaultSound: true,
          defaultVibrateTimings: true,
        },
      },
    };

    const response = await admin.messaging().send(message);
    console.log("Successfully sent FCM message:", response);
    
    // Update notification status to sent
    await db.collection("pushNotifications").doc(notificationData.id).update({
      status: "sent",
      sentAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } catch (error) {
    console.error("Error sending FCM message:", error);
    
    // Update notification status to failed
    await db.collection("pushNotifications").doc(notificationData.id).update({
      status: "failed",
      error: error instanceof Error ? error.message : "Unknown error",
      failedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  }
}

// Cloud Function to process push notifications
export const processPushNotifications = functions.firestore
  .document("pushNotifications/{notificationId}")
  .onCreate(async (snap, context) => {
    const notificationData = snap.data() as PushNotificationData;
    
    console.log("Processing push notification:", notificationData.id);
    
    // Only process if status is pending
    if (notificationData.status === "pending") {
      await sendFCMNotification(notificationData);
    }
  });

// Cloud Function to handle notification triggers (SECURE APPROACH)
export const processNotificationTriggers = functions.firestore
  .document("notificationTriggers/{triggerId}")
  .onCreate(async (snap, context) => {
    const triggerData = snap.data();
    
    console.log("Processing notification trigger:", triggerData.type);
    
    if (triggerData.status === "pending") {
      if (triggerData.type === "board_update") {
        await processBoardUpdateNotification(triggerData);
      } else if (triggerData.type === "query") {
        await processQueryNotification(triggerData);
      } else if (triggerData.type === "query_resolved") {
        await processQueryResolutionNotification(triggerData);
      }
      
      // Mark trigger as processed
      await db.collection("notificationTriggers").doc(context.params.triggerId).update({
        status: "processed",
        processedAt: admin.firestore.FieldValue.serverTimestamp()
      });
    }
  });

// Process board update notifications (SERVER-SIDE - NO DATA EXPOSURE)
async function processBoardUpdateNotification(triggerData: any): Promise<void> {
  try {
    const { boardCode, title, body } = triggerData;
    
    // Get board information
    const boardQuery = await db.collection("noticeBoards")
      .where("organizationCode", "==", boardCode)
      .limit(1)
      .get();
    
    if (!boardQuery.empty) {
      const board = boardQuery.docs[0].data();
      const boardId = boardQuery.docs[0].id;
      
      // Get all subscribed users (SERVER-SIDE - no data exposure to client)
      const subscribedUsersQuery = await db.collection("users")
        .where("subscribedCodes", "array-contains", boardCode)
        .get();
      
      console.log(`Found ${subscribedUsersQuery.size} subscribed users for board ${boardCode}`);
      
      // Create notification records for each subscribed user
      const batch = db.batch();
      
      subscribedUsersQuery.docs.forEach((userDoc) => {
        const user = userDoc.data();
        const userId = userDoc.id; // Use document ID instead of user.id
        
        console.log(`Processing user: ${userId}, FCM Token: ${user.fcmToken ? 'Present' : 'Missing'}`);
        
        if (user.fcmToken) {
          const notificationId = `${userId}_board_update_${boardId}_${Date.now()}`;
          const notificationRef = db.collection("pushNotifications").doc(notificationId);
          
          const notificationData: PushNotificationData = {
            id: notificationId,
            userId: userId, // Use document ID
            fcmToken: user.fcmToken,
            boardId: boardId,
            boardCode: boardCode,
            boardName: board.organizationName,
            title: title,
            body: body,
            type: "board_update",
            status: "pending",
            createdAt: Date.now()
          };
          
          batch.set(notificationRef, notificationData);
          console.log(`Created notification record for user: ${userId}`);
        } else {
          console.log(`Skipping user ${userId} - no FCM token`);
        }
      });
      
      await batch.commit();
      console.log("Push notification records created for board update");
    }
  } catch (error) {
    console.error("Error processing board update notification:", error);
  }
}

// Process query notifications (SERVER-SIDE - NO DATA EXPOSURE)
async function processQueryNotification(triggerData: any): Promise<void> {
  try {
    const { orgCode, queryId, raiserName, question } = triggerData;
    
    // Get board information
    const boardQuery = await db.collection("noticeBoards")
      .where("organizationCode", "==", orgCode)
      .limit(1)
      .get();
    
    if (!boardQuery.empty) {
      const board = boardQuery.docs[0].data();
      const boardId = boardQuery.docs[0].id;
      
      // Get board owner (SERVER-SIDE - no data exposure to client)
      const boardOwnerDoc = await db.collection("users").doc(board.createdBy).get();
      
      if (boardOwnerDoc.exists) {
        const boardOwner = boardOwnerDoc.data();
        const boardOwnerId = boardOwnerDoc.id; // Use document ID
        
        console.log(`Processing board owner: ${boardOwnerId}, FCM Token: ${boardOwner?.fcmToken ? 'Present' : 'Missing'}`);
        
        if (boardOwner && boardOwner.fcmToken) {
          const notificationId = `${boardOwnerId}_query_push_${queryId}_${Date.now()}`;
          const notificationRef = db.collection("pushNotifications").doc(notificationId);
          
          const notificationData: PushNotificationData = {
            id: notificationId,
            userId: boardOwnerId, // Use document ID
            fcmToken: boardOwner.fcmToken,
            boardId: boardId,
            boardCode: orgCode,
            boardName: board.organizationName,
            title: "New Query Received",
            body: `You have received a new query from ${raiserName}: ${question.substring(0, 50)}...`,
            type: "query",
            queryId: queryId,
            status: "pending",
            createdAt: Date.now()
          };
          
          await notificationRef.set(notificationData);
          console.log(`Push notification record created for query to user: ${boardOwnerId}`);
        } else {
          console.log(`Skipping board owner ${boardOwnerId} - no FCM token`);
        }
      }
    }
  } catch (error) {
    console.error("Error processing query notification:", error);
  }
}

// Process query resolution notifications (SERVER-SIDE - NO DATA EXPOSURE)
async function processQueryResolutionNotification(triggerData: any): Promise<void> {
  try {
    const { orgCode, queryId, raiserId } = triggerData;
    
    // Get board information
    const boardQuery = await db.collection("noticeBoards")
      .where("organizationCode", "==", orgCode)
      .limit(1)
      .get();
    
    if (!boardQuery.empty) {
      const board = boardQuery.docs[0].data();
      const boardId = boardQuery.docs[0].id;
      
      // Get query raiser (SERVER-SIDE - no data exposure to client)
      const queryRaiserDoc = await db.collection("users").doc(raiserId).get();
      
      if (queryRaiserDoc.exists) {
        const queryRaiser = queryRaiserDoc.data();
        const queryRaiserId = queryRaiserDoc.id; // Use document ID
        
        console.log(`Processing query raiser: ${queryRaiserId}, FCM Token: ${queryRaiser?.fcmToken ? 'Present' : 'Missing'}`);
        
        if (queryRaiser && queryRaiser.fcmToken) {
          const notificationId = `${queryRaiserId}_query_resolved_push_${queryId}_${Date.now()}`;
          const notificationRef = db.collection("pushNotifications").doc(notificationId);
          
          const notificationData: PushNotificationData = {
            id: notificationId,
            userId: queryRaiserId, // Use document ID
            fcmToken: queryRaiser.fcmToken,
            boardId: boardId,
            boardCode: orgCode,
            boardName: board.organizationName,
            title: "Query Resolved",
            body: `Your query has been resolved by ${board.organizationName}`,
            type: "query_resolved",
            queryId: queryId,
            status: "pending",
            createdAt: Date.now()
          };
          
          await notificationRef.set(notificationData);
          console.log(`Push notification record created for query resolution to user: ${queryRaiserId}`);
        } else {
          console.log(`Skipping query raiser ${queryRaiserId} - no FCM token`);
        }
      }
    }
  } catch (error) {
    console.error("Error processing query resolution notification:", error);
  }
}

// Cleanup function to remove old notification records (optional)
export const cleanupOldNotifications = functions.pubsub
  .schedule("every 24 hours")
  .onRun(async (context) => {
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - 7); // Keep records for 7 days
    
    const oldNotificationsQuery = await db.collection("pushNotifications")
      .where("createdAt", "<", cutoffDate.getTime())
      .get();
    
    const batch = db.batch();
    oldNotificationsQuery.docs.forEach((doc) => {
      batch.delete(doc.ref);
    });
    
    await batch.commit();
    console.log(`Cleaned up ${oldNotificationsQuery.size} old notification records`);
  });
