// MongoDB initialization script for Nearly Microservices
// Creates all necessary databases and collections

// Switch to admin to create users if needed
db = db.getSiblingDB('admin');

// Create databases for each microservice
var databases = [
    'nearly_chat',
    'nearly_video', 
    'nearly_pai',
    'nearly_users',
    'nearly_messaging',
    'nearly_activity',
    'nearly_moments',
    'nearly_notifications',
    'nearly_reports',
    'nearly_search'
];

databases.forEach(function(dbName) {
    db = db.getSiblingDB(dbName);
    
    // Create a dummy collection to initialize the database
    db.createCollection('_init');
    db._init.drop();
    
    print('Created database: ' + dbName);
});

// Create indexes for better performance
// Chat service
db = db.getSiblingDB('nearly_chat');
db.online_users.createIndex({ "lastActive": 1 });
db.online_users.createIndex({ "sessionId": 1 }, { unique: true });
db.chat_rooms.createIndex({ "sessionId1": 1 });
db.chat_rooms.createIndex({ "sessionId2": 1 });
db.chat_rooms.createIndex({ "active": 1 });

// Video service
db = db.getSiblingDB('nearly_video');
db.video_online_users.createIndex({ "lastActive": 1 });
db.video_online_users.createIndex({ "sessionId": 1 }, { unique: true });

// PAI service
db = db.getSiblingDB('nearly_pai');
db.match_queues.createIndex({ "chatMode": 1, "createdAt": 1 });
db.match_queues.createIndex({ "sessionId": 1 }, { unique: true });
db.match_ratings.createIndex({ "sessionId": 1 });
db.match_ratings.createIndex({ "matchedSessionId": 1 });
db.match_history.createIndex({ "sessionId1": 1 });
db.match_history.createIndex({ "sessionId2": 1 });
db.match_history.createIndex({ "roomId": 1 });

// User service
db = db.getSiblingDB('nearly_users');
db.users.createIndex({ "username": 1 }, { unique: true });
db.users.createIndex({ "email": 1 }, { unique: true });
db.follows.createIndex({ "followerId": 1, "followingId": 1 }, { unique: true });
db.follow_requests.createIndex({ "requesterId": 1, "targetUserId": 1 }, { unique: true });
db.saved_posts.createIndex({ "userId": 1, "postId": 1 }, { unique: true });

// Messaging service  
db = db.getSiblingDB('nearly_messaging');
db.messages.createIndex({ "conversationId": 1, "createdAt": -1 });
db.conversations.createIndex({ "participantIds": 1 });
db.message_reactions.createIndex({ "messageId": 1, "userId": 1 }, { unique: true });
db.poll_votes.createIndex({ "messageId": 1, "userId": 1 }, { unique: true });
db.message_requests.createIndex({ "senderId": 1, "recipientId": 1 }, { unique: true });
db.message_seen.createIndex({ "messageId": 1, "userId": 1 }, { unique: true });

// Activity service
db = db.getSiblingDB('nearly_activity');
db.activities.createIndex({ "userId": 1 });
db.activities.createIndex({ "category": 1 });
db.activities.createIndex({ "createdAt": -1 });
db.posts.createIndex({ "userId": 1 });
db.posts.createIndex({ "createdAt": -1 });
db.polls.createIndex({ "userId": 1 });
db.polls.createIndex({ "createdAt": -1 });
db.questions.createIndex({ "userId": 1 });
db.questions.createIndex({ "tags": 1 });
db.questions.createIndex({ "createdAt": -1 });
db.discussions.createIndex({ "userId": 1 });
db.discussions.createIndex({ "category": 1 });
db.discussions.createIndex({ "createdAt": -1 });
db.comments.createIndex({ "targetType": 1, "targetId": 1 });
db.likes.createIndex({ "userId": 1, "targetType": 1, "targetId": 1 }, { unique: true });
db.activity_poll_votes.createIndex({ "pollId": 1, "userId": 1 }, { unique: true });
db.answers.createIndex({ "questionId": 1 });

// Moments service
db = db.getSiblingDB('nearly_moments');
db.moments.createIndex({ "userId": 1 });
db.moments.createIndex({ "visibility": 1 });
db.moments.createIndex({ "expiresAt": 1 });
db.direct_moments.createIndex({ "recipientId": 1, "isViewed": 1 });
db.moment_streaks.createIndex({ "userId1": 1, "userId2": 1 }, { unique: true });
db.moment_comments.createIndex({ "momentId": 1 });

// Notifications service
db = db.getSiblingDB('nearly_notifications');
db.notifications.createIndex({ "userId": 1, "createdAt": -1 });
db.notifications.createIndex({ "userId": 1, "isRead": 1 });

print('MongoDB initialization completed for Nearly microservices');
