package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AvatarConfig
import com.example.data.model.CanvasOpDoc
import com.example.data.model.ChatMessageDoc
import com.example.data.model.CustomWordPack
import com.example.data.model.Friendship
import com.example.data.model.GameMode
import com.example.data.model.GamePhase
import com.example.data.model.GameRoom
import com.example.data.model.GameRulesEngine
import com.example.data.model.ModerationReport
import com.example.data.model.RoomPlayer
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.concurrent.atomic.AtomicLong

/**
 * Egress Meter & Bandwidth Governor (§2.1):
 * Tracks outbound/inbound payload bytes per room and enforces governor thresholds:
 * - At 70% of budget: slows stroke batching (100ms -> 200ms) and pauses live reaction bursts
 * - At 95% of budget: pauses creating new cloud rooms and displays a friendly budget notice
 */
class EgressGovernor(
  val monthlyBudgetBytes: Long = 5L * 1024L * 1024L * 1024L // 5 GB free tier budget
) {
  private val sessionBytes = AtomicLong(0L)

  fun recordPayloadBytes(bytes: Int): Long {
    return sessionBytes.addAndGet(bytes.toLong().coerceAtLeast(0L))
  }

  val totalBytesUsed: Long
    get() = sessionBytes.get()

  val usageFraction: Float
    get() = (sessionBytes.get().toDouble() / monthlyBudgetBytes.toDouble()).toFloat().coerceIn(0f, 1f)

  val isSlowBatchingActive: Boolean
    get() = usageFraction >= 0.70f

  val isRoomCreationPaused: Boolean
    get() = usageFraction >= 0.95f

  val strokeBatchIntervalMs: Long
    get() = if (isSlowBatchingActive) 200L else 100L
}

class DoodleDashRepository(
  private val db: FirebaseFirestore,
  val egressGovernor: EgressGovernor = EgressGovernor()
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    )
  )

  private val auth = Firebase.auth

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  // --- USER PROFILE ---

  fun observeUserProfile(userId: String): Flow<UserProfile?> =
    db.collection("users").document(userId)
      .snapshots()
      .map { snapshot ->
        if (snapshot.exists()) {
          snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.GET, "users/$userId")
        }
        throw error
      }

  suspend fun getUserProfile(userId: String): Result<UserProfile?> = runCatching {
    val path = "users/$userId"
    try {
      val doc = db.collection("users").document(userId).get().await()
      if (doc.exists()) {
        doc.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      } else {
        null
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, path)
      throw e
    }
  }

  suspend fun ensureOrSaveProfile(profile: UserProfile): Result<UserProfile> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      val existing = docRef.get().await()
      if (!existing.exists()) {
        val safeUsername = profile.username.take(20).padEnd(3, 'x')
        val payload = mapOf(
          "userId" to uid,
          "username" to safeUsername,
          "displayName" to profile.displayName.ifBlank { "Sage Sketcher" }.take(40),
          "avatarJson" to profile.avatarJson,
          "bio" to profile.bio.take(160),
          "countryFlag" to profile.countryFlag.take(16),
          "themeColor" to profile.themeColor.take(16),
          "xp" to profile.xp,
          "level" to profile.level.coerceAtLeast(1L),
          "equippedTitle" to profile.equippedTitle.take(40),
          "equippedBadge" to profile.equippedBadge.take(40),
          "gamesPlayed" to profile.gamesPlayed,
          "wins" to profile.wins,
          "totalPoints" to profile.totalPoints,
          "correctGuesses" to profile.correctGuesses,
          "bestGuessMs" to profile.bestGuessMs,
          "bestStreak" to profile.bestStreak,
          "unlockedPacksJson" to profile.unlockedPacksJson.take(1000),
          "achievementsJson" to profile.achievementsJson.take(2000),
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        egressGovernor.recordPayloadBytes(payload.toString().length)
        docRef.set(payload).await()
      } else {
        val currentCreatedAt = existing.getTimestamp("createdAt") ?: FieldValue.serverTimestamp()
        val updatePayload = mapOf(
          "userId" to uid,
          "username" to profile.username.take(20).padEnd(3, 'x'),
          "displayName" to profile.displayName.ifBlank { "Sage Sketcher" }.take(40),
          "avatarJson" to profile.avatarJson,
          "bio" to profile.bio.take(160),
          "countryFlag" to profile.countryFlag.take(16),
          "themeColor" to profile.themeColor.take(16),
          "xp" to profile.xp,
          "level" to profile.level.coerceAtLeast(1L),
          "equippedTitle" to profile.equippedTitle.take(40),
          "equippedBadge" to profile.equippedBadge.take(40),
          "gamesPlayed" to profile.gamesPlayed,
          "wins" to profile.wins,
          "totalPoints" to profile.totalPoints,
          "correctGuesses" to profile.correctGuesses,
          "bestGuessMs" to profile.bestGuessMs,
          "bestStreak" to profile.bestStreak,
          "unlockedPacksJson" to profile.unlockedPacksJson.take(1000),
          "achievementsJson" to profile.achievementsJson.take(2000),
          "createdAt" to currentCreatedAt,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
        egressGovernor.recordPayloadBytes(updatePayload.toString().length)
        docRef.set(updatePayload).await()
      }
      val saved = docRef.get().await()
      saved.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: profile.copy(id = uid, userId = uid)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path)
      throw e
    }
  }

  suspend fun deleteUserAccountAndProfile(): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- FRIENDSHIPS (/users/{userId}/friends/{friendId}) ---

  fun observeFriends(userId: String): Flow<List<Friendship>> =
    db.collection("users").document(userId).collection("friends")
      .snapshots()
      .map { snap ->
        snap.toObjects(Friendship::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/friends")
        }
        throw error
      }

  suspend fun addFriend(
    friendUsername: String,
    friendDisplayName: String,
    friendCode: String,
    roomCode: String = ""
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("friends").document()
    val payload = mapOf(
      "userId" to uid,
      "friendCode" to friendCode.ifBlank { "FRIEND01" }.take(32),
      "friendUsername" to friendUsername.trim().lowercase().take(20).padEnd(3, 'x'),
      "friendDisplayName" to friendDisplayName.trim().ifBlank { friendUsername }.take(40),
      "friendAvatarJson" to AvatarConfig.random(friendUsername.hashCode()).toJson(),
      "status" to "ACCEPTED",
      "roomCode" to roomCode.take(12),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun removeFriend(friendDocId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("friends").document(friendDocId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- CUSTOM WORD PACKS (/users/{userId}/word_packs/{packId}) ---

  fun observeCustomWordPacks(userId: String): Flow<List<CustomWordPack>> =
    db.collection("users").document(userId).collection("word_packs")
      .snapshots()
      .map { snap ->
        snap.toObjects(CustomWordPack::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "users/$userId/word_packs")
        }
        throw error
      }

  suspend fun createCustomWordPack(
    name: String,
    difficulty: String,
    isFamilyFriendly: Boolean,
    words: List<String>
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val cleanedWords = words.map { it.trim().take(32) }.filter { it.isNotEmpty() }.take(500)
    require(cleanedWords.size >= 3) { "At least 3 custom words are required." }
    val docRef = db.collection("users").document(uid).collection("word_packs").document()
    val payload = mapOf(
      "userId" to uid,
      "name" to name.trim().ifBlank { "Custom Botanicals" }.take(50),
      "language" to "EN",
      "difficulty" to difficulty,
      "isFamilyFriendly" to isFamilyFriendly,
      "wordsCsv" to cleanedWords.joinToString(","),
      "wordCount" to cleanedWords.size.toLong(),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun deleteCustomWordPack(packId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("word_packs").document(packId)
    try {
      docRef.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path)
      throw e
    }
  }

  // --- MULTIPLAYER ROOMS (/rooms/{roomId}) ---

  fun observePublicRooms(): Flow<List<GameRoom>> =
    db.collection("rooms")
      .whereEqualTo("isPublic", true)
      .snapshots()
      .map { snap ->
        snap.toObjects(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "rooms")
        }
        throw error
      }

  fun observeMyRooms(userId: String): Flow<List<GameRoom>> =
    db.collection("rooms")
      .whereArrayContains("memberUids", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "rooms")
        }
        throw error
      }

  fun observeRoom(roomId: String): Flow<GameRoom?> =
    db.collection("rooms").document(roomId)
      .snapshots()
      .map { snap ->
        if (snap.exists()) {
          snap.toObject(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } else {
          null
        }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
          handleFirestoreError(exceptionToLog, OperationType.GET, "rooms/$roomId")
        }
        throw error
      }

  suspend fun getRoomById(roomId: String): Result<GameRoom> = runCatching {
    val docRef = db.collection("rooms").document(roomId)
    try {
      val snap = docRef.get().await()
      snap.toObject(GameRoom::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        ?: throw IllegalStateException("Room $roomId not found")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, docRef.path)
      throw e
    }
  }

  suspend fun createRoom(
    roomCode: String = GameRulesEngine.generateRoomCode(),
    hostName: String,
    hostAvatarJson: String,
    isPublic: Boolean = true,
    maxRounds: Int = 3,
    drawTimeSec: Int = 80,
    wordChoicesCount: Int = 3,
    hintsCount: Int = 2,
    gameMode: GameMode = GameMode.CLASSIC,
    difficulty: String = "MIXED",
    wordPackCategory: String = "All Categories",
    customWordsCsv: String = "",
    initialPlayers: List<RoomPlayer> = emptyList(),
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val normalizedCode = roomCode.uppercase().take(6).padEnd(6, 'X')
    val roomId = "room_$normalizedCode"
    val docRef = db.collection("rooms").document(roomId)
    val players = initialPlayers.ifEmpty {
      listOf(
        RoomPlayer(
          uid = uid,
          username = hostName.lowercase().replace(" ", "_").take(16),
          displayName = hostName,
          avatarJson = hostAvatarJson,
          isHost = true
        )
      )
    }
    val payload = mapOf(
      "roomCode" to normalizedCode,
      "hostId" to uid,
      "hostName" to hostName.ifBlank { "Host" }.take(40),
      "isPublic" to isPublic,
      "phase" to GamePhase.LOBBY.name,
      "round" to 1L,
      "maxRounds" to maxRounds.coerceIn(2, 10).toLong(),
      "drawTimeSec" to drawTimeSec.coerceIn(30, 180).toLong(),
      "wordChoicesCount" to wordChoicesCount.coerceIn(1, 5).toLong(),
      "hintsCount" to hintsCount.coerceIn(0, 3).toLong(),
      "gameMode" to gameMode.name,
      "difficulty" to difficulty,
      "wordPackCategory" to wordPackCategory.take(40),
      "customWordsCsv" to customWordsCsv.take(16000),
      "currentDrawerId" to uid,
      "currentDrawerName" to hostName.ifBlank { "Host" }.take(40),
      "wordMask" to "_ _ _ _ _",
      "secretWord" to "",
      "phaseEndsAtMs" to (System.currentTimeMillis() + 60_000L),
      "memberUids" to listOf(uid),
      "playersJson" to RoomPlayer.listToJson(players),
      "reactionCountsJson" to "{}",
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp(),
    )
    try {
      egressGovernor.recordPayloadBytes(payload.toString().length)
      docRef.set(payload).await()
      roomId
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  suspend fun updateRoomState(room: GameRoom): Result<Unit> = runCatching {
    val uid = requireUserId()
    require(room.id.isNotBlank()) { "Room ID must not be blank" }
    val docRef = db.collection("rooms").document(room.id)
    try {
      val snap = docRef.get().await()
      val existingCreatedAt = snap.getTimestamp("createdAt") ?: room.createdAt ?: FieldValue.serverTimestamp()
      val updatedMembers = if (uid in room.memberUids) room.memberUids else (room.memberUids + uid).take(12)
      val payload = mapOf(
        "roomCode" to room.roomCode,
        "hostId" to room.hostId,
        "hostName" to room.hostName.take(40),
        "isPublic" to room.isPublic,
        "phase" to room.phase,
        "round" to room.round.coerceIn(1L, 10L),
        "maxRounds" to room.maxRounds.coerceIn(2L, 10L),
        "drawTimeSec" to room.drawTimeSec.coerceIn(30L, 180L),
        "wordChoicesCount" to room.wordChoicesCount.coerceIn(1L, 5L),
        "hintsCount" to room.hintsCount.coerceIn(0L, 3L),
        "gameMode" to room.gameMode,
        "difficulty" to room.difficulty,
        "wordPackCategory" to room.wordPackCategory.take(40),
        "customWordsCsv" to room.customWordsCsv.take(16000),
        "currentDrawerId" to room.currentDrawerId.take(128),
        "currentDrawerName" to room.currentDrawerName.take(40),
        "wordMask" to room.wordMask.take(80),
        "secretWord" to room.secretWord.take(40),
        "phaseEndsAtMs" to room.phaseEndsAtMs.coerceAtLeast(0L),
        "memberUids" to updatedMembers,
        "playersJson" to room.playersJson.take(16000),
        "reactionCountsJson" to room.reactionCountsJson.take(2000),
        "createdAt" to existingCreatedAt,
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      egressGovernor.recordPayloadBytes(payload.toString().length)
      docRef.set(payload).await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path)
      throw e
    }
  }

  // --- CANVAS OP-LOG (/rooms/{roomId}/ops/{opId}) ---

  fun observeCanvasOps(roomId: String, userId: String): Flow<List<CanvasOpDoc>> =
    db.collection("rooms").document(roomId).collection("ops")
      .whereArrayContains("memberUids", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(CanvasOpDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.seq }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "rooms/$roomId/ops")
        }
        throw error
      }

  suspend fun appendCanvasOp(
    roomId: String,
    seq: Long,
    opType: String,
    tool: String,
    colorHex: String,
    sizePx: Int,
    opacity: Int,
    symmetryMode: String,
    seed: Int,
    encodedData: String,
    memberUids: List<String>,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("rooms").document(roomId).collection("ops").document()
    val members = (if (uid in memberUids) memberUids else (memberUids + uid)).distinct().take(12)
    val payload = mapOf(
      "roomId" to roomId,
      "authorId" to uid,
      "seq" to seq.coerceIn(0L, 10000L),
      "opType" to opType,
      "tool" to tool.take(32),
      "colorHex" to colorHex.take(12),
      "sizePx" to sizePx.coerceIn(1, 80).toLong(),
      "opacity" to opacity.coerceIn(1, 100).toLong(),
      "symmetryMode" to symmetryMode,
      "seed" to seed.toLong(),
      "encodedData" to encodedData.take(65536),
      "memberUids" to members,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      egressGovernor.recordPayloadBytes(encodedData.length + 64)
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  // --- ROOM CHAT & STICKERS (/rooms/{roomId}/messages/{messageId}) ---

  fun observeRoomMessages(roomId: String, userId: String): Flow<List<ChatMessageDoc>> =
    db.collection("rooms").document(roomId).collection("messages")
      .whereArrayContains("memberUids", userId)
      .snapshots()
      .map { snap ->
        snap.toObjects(ChatMessageDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
          .sortedBy { it.createdAt?.toDate()?.time ?: 0L }
      }
      .catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
          .filterIsInstance<FirebaseFirestoreException>()
          .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == userId) {
          handleFirestoreError(exceptionToLog, OperationType.LIST, "rooms/$roomId/messages")
        }
        throw error
      }

  suspend fun sendRoomMessage(
    roomId: String,
    senderName: String,
    channel: String,
    messageType: String,
    content: String,
    stickerId: String = "",
    memberUids: List<String>,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("rooms").document(roomId).collection("messages").document()
    val members = (if (uid in memberUids) memberUids else (memberUids + uid)).distinct().take(12)
    val payload = mapOf(
      "roomId" to roomId,
      "senderId" to uid,
      "senderName" to senderName.ifBlank { "Player" }.take(40),
      "channel" to channel,
      "messageType" to messageType,
      "content" to content.take(140),
      "stickerId" to stickerId.take(40),
      "memberUids" to members,
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      egressGovernor.recordPayloadBytes(content.length + 48)
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }

  // --- MODERATION REPORTS (/reports/{reportId}) ---

  suspend fun submitReport(
    targetUserId: String,
    targetUsername: String,
    roomCode: String,
    reason: String,
    chatSnapshotJson: String,
  ): Result<String> = runCatching {
    val uid = requireUserId()
    val docRef = db.collection("reports").document()
    val payload = mapOf(
      "reporterId" to uid,
      "targetUserId" to targetUserId.ifBlank { "unknown" }.take(128),
      "targetUsername" to targetUsername.ifBlank { "player" }.take(40),
      "roomCode" to roomCode.take(6).padEnd(6, 'X'),
      "reason" to reason.ifBlank { "Inappropriate behavior" }.take(200),
      "chatSnapshotJson" to chatSnapshotJson.take(8000),
      "status" to "OPEN",
      "createdAt" to FieldValue.serverTimestamp(),
    )
    try {
      docRef.set(payload).await()
      docRef.id
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path)
      throw e
    }
  }
}
