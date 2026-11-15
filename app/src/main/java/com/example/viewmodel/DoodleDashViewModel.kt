package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AchievementDef
import com.example.data.model.AvatarConfig
import com.example.data.model.BuiltInCatalog
import com.example.data.model.CanvasOpDoc
import com.example.data.model.ChatMessageDoc
import com.example.data.model.CustomWordPack
import com.example.data.model.DrawingTool
import com.example.data.model.Friendship
import com.example.data.model.GameMode
import com.example.data.model.GamePhase
import com.example.data.model.GameRoom
import com.example.data.model.GameRulesEngine
import com.example.data.model.RoomPlayer
import com.example.data.model.SymmetryMode
import com.example.data.model.UserProfile
import com.example.data.repository.DoodleDashRepository
import com.example.ui.audio.ProceduralSoundEngine
import com.example.ui.canvas.DoodleCanvasEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.max

data class FloatingReaction(
  val id: Long,
  val emoji: String,
  val xFraction: Float,
)

data class ActiveRoomUiState(
  val room: GameRoom? = null,
  val players: List<RoomPlayer> = emptyList(),
  val ops: List<CanvasOpDoc> = emptyList(),
  val effectiveOps: List<CanvasOpDoc> = emptyList(),
  val messages: List<ChatMessageDoc> = emptyList(),
  val candidateWords: List<String> = emptyList(),
  val timeRemainingSec: Int = 0,
  val isDrawer: Boolean = false,
  val isHost: Boolean = false,
  val hasGuessedCurrentTurn: Boolean = false,
  val closeGuessToast: String? = null,
  val scorePopupText: String? = null,
  val timelapseProgressFraction: Float = 1f,
  val floatingReactions: List<FloatingReaction> = emptyList(),
  val selectedTool: DrawingTool = DrawingTool.INK_PEN,
  val selectedColorHex: String = "#26231F",
  val recentColorsHex: List<String> = listOf("#26231F", "#C86446", "#5F7A61", "#DDA15E", "#3B6E4C", "#BC4749", "#457B9D", "#F7F3EB"),
  val brushSizePx: Int = 10,
  val brushOpacity: Int = 100,
  val stabilizerStrength: Float = 0.35f,
  val symmetryMode: SymmetryMode = SymmetryMode.NONE,
  val fillShapeEnabled: Boolean = false,
  val colorblindPaletteEnabled: Boolean = false,
  val highContrastEnabled: Boolean = false,
  val isMuted: Boolean = false,
  val statusBanner: String? = null,
)

class DoodleDashViewModel(
  private val repository: DoodleDashRepository,
  val currentUserId: String,
  initialDisplayName: String = "Sage Sketcher",
) : ViewModel() {

  private val _userProfile = MutableStateFlow(
    UserProfile(
      id = currentUserId,
      userId = currentUserId,
      username = "artist_${currentUserId.take(5).lowercase()}",
      displayName = initialDisplayName.ifBlank { "Sage Sketcher" },
      avatarJson = AvatarConfig.random(currentUserId.hashCode()).toJson()
    )
  )
  val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

  val publicRooms: StateFlow<List<GameRoom>> = repository.observePublicRooms()
    .catch { e ->
      Log.w(TAG, "Error observing public rooms", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val friends: StateFlow<List<Friendship>> = repository.observeFriends(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing friends", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  val customWordPacks: StateFlow<List<CustomWordPack>> = repository.observeCustomWordPacks(currentUserId)
    .catch { e ->
      Log.w(TAG, "Error observing word packs", e)
      emit(emptyList())
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

  private val _roomUiState = MutableStateFlow(ActiveRoomUiState())
  val roomUiState: StateFlow<ActiveRoomUiState> = _roomUiState.asStateFlow()

  private var roomListenerJob: Job? = null
  private var opsListenerJob: Job? = null
  private var messagesListenerJob: Job? = null
  private var timerTickerJob: Job? = null
  private var botSimulationJob: Job? = null
  private var currentStreak = 0
  private val usedColorsInTurn = mutableSetOf<String>()
  private var usedSymmetryInTurn = false
  private var usedFloodFillInTurn = false
  private var sentSocialInMatch = false

  // 20-op Bitmap Checkpoint cache (§5)
  private val checkpoints = mutableMapOf<Int, Bitmap>()

  init {
    observeAndInitializeProfile()
  }

  private fun observeAndInitializeProfile() {
    viewModelScope.launch {
      val existing = repository.getUserProfile(currentUserId).getOrNull()
      if (existing == null) {
        val randomNum = kotlin.math.abs(currentUserId.hashCode() % 90) + 10
        val friendlyHandle = "sage_panda_$randomNum"
        val defaultProfile = UserProfile(
          id = currentUserId,
          userId = currentUserId,
          username = friendlyHandle,
          displayName = "SagePanda$randomNum",
          avatarJson = AvatarConfig.random(currentUserId.hashCode()).toJson()
        )
        repository.ensureOrSaveProfile(defaultProfile)
      }
    }
    viewModelScope.launch {
      repository.observeUserProfile(currentUserId)
        .catch { e -> Log.w(TAG, "Profile listener error", e) }
        .collect { profile ->
          if (profile != null) {
            _userProfile.value = profile
          }
        }
    }
  }

  // --- PROFILE, AVATAR, FRIENDS & WORD PACKS ---

  fun saveProfileEdits(
    username: String,
    displayName: String,
    bio: String,
    countryFlag: String,
    themeColor: String,
    equippedTitle: String,
    equippedBadge: String,
    avatarConfig: AvatarConfig,
  ) {
    val cleanUsername = GameRulesEngine.filterProfanityAndSecret(username, "", false)
      .lowercase()
      .replace(Regex("[^a-z0-9_]"), "")
      .take(16)
      .padEnd(3, 'x')
    val cleanDisplay = GameRulesEngine.filterProfanityAndSecret(displayName, "", false)
      .trim()
      .ifBlank { "Sage Sketcher" }
      .take(30)
    val cleanBio = GameRulesEngine.filterProfanityAndSecret(bio, "", false).take(120)

    val updated = _userProfile.value.copy(
      username = cleanUsername,
      displayName = cleanDisplay,
      bio = cleanBio,
      countryFlag = countryFlag.ifBlank { "🌿" },
      themeColor = themeColor,
      equippedTitle = equippedTitle,
      equippedBadge = equippedBadge,
      avatarJson = avatarConfig.toJson()
    )
    _userProfile.value = updated
    viewModelScope.launch {
      repository.ensureOrSaveProfile(updated)
      showBanner("Profile & Avatar saved!")
    }
  }

  fun deleteAccountAndStats(onComplete: () -> Unit) {
    viewModelScope.launch {
      repository.deleteUserAccountAndProfile()
      onComplete()
    }
  }

  fun addFriendByUsername(username: String, displayName: String) {
    if (username.isBlank()) return
    viewModelScope.launch {
      val code = GameRulesEngine.generateRoomCode(username.hashCode().toLong())
      repository.addFriend(
        friendUsername = username,
        friendDisplayName = displayName.ifBlank { username },
        friendCode = code,
        roomCode = _roomUiState.value.room?.roomCode ?: ""
      )
      showBanner("Added @$username to your Studio Friends!")
    }
  }

  fun removeFriend(friendId: String) {
    viewModelScope.launch {
      repository.removeFriend(friendId)
    }
  }

  fun saveCustomWordPack(name: String, difficulty: String, isFamilyFriendly: Boolean, rawWordsText: String) {
    val words = rawWordsText.split(",", "\n")
      .map { GameRulesEngine.filterProfanityAndSecret(it.trim(), "", false).take(32) }
      .filter { it.length >= 2 }
    if (words.size < 3) {
      showBanner("Please enter at least 3 words (separated by commas).")
      return
    }
    viewModelScope.launch {
      repository.createCustomWordPack(name, difficulty, isFamilyFriendly, words)
      if (words.size >= 5) {
        unlockAchievement("word_smith")
      }
      showBanner("Custom word pack '$name' (${words.size} words) saved!")
    }
  }

  fun deleteCustomWordPack(packId: String) {
    viewModelScope.launch {
      repository.deleteCustomWordPack(packId)
    }
  }

  // --- ROOM CREATION, JOINING & BOT MATCH ---

  fun createAndJoinRoom(
    isPublic: Boolean = true,
    maxRounds: Int = 3,
    drawTimeSec: Int = 80,
    wordChoicesCount: Int = 3,
    hintsCount: Int = 2,
    gameMode: GameMode = GameMode.CLASSIC,
    difficulty: String = "MIXED",
    wordPackCategory: String = "All Categories",
    customWordsCsv: String = "",
    botCount: Int = 3,
    onRoomReady: () -> Unit = {},
  ) {
    if (repository.egressGovernor.isRoomCreationPaused) {
      showBanner("Monthly egress budget at 95% — switching to low-bandwidth mode.")
    }
    viewModelScope.launch {
      val profile = _userProfile.value
      val code = GameRulesEngine.generateRoomCode()
      val hostPlayer = RoomPlayer(
        uid = currentUserId,
        username = profile.username,
        displayName = profile.displayName,
        avatarJson = profile.avatarJson,
        isHost = true,
        team = 0
      )
      val bots = buildBotPlayers(botCount)
      val allPlayers = listOf(hostPlayer) + bots
      val effectiveDrawTime = if (gameMode == GameMode.SPEED_30S) 30 else drawTimeSec

      val result = repository.createRoom(
        roomCode = code,
        hostName = profile.displayName,
        hostAvatarJson = profile.avatarJson,
        isPublic = isPublic,
        maxRounds = maxRounds,
        drawTimeSec = effectiveDrawTime,
        wordChoicesCount = wordChoicesCount,
        hintsCount = hintsCount,
        gameMode = gameMode,
        difficulty = difficulty,
        wordPackCategory = wordPackCategory,
        customWordsCsv = customWordsCsv,
        initialPlayers = allPlayers
      )
      result.onSuccess { roomId ->
        subscribeToRoom(roomId)
        onRoomReady()
      }.onFailure { err ->
        showBanner("Could not create room: ${err.localizedMessage ?: "Try again"}")
      }
    }
  }

  fun joinRoomByCode(rawCode: String, onJoined: () -> Unit = {}) {
    val cleanCode = rawCode.trim().uppercase().take(6)
    if (cleanCode.length != 6) {
      showBanner("Enter a valid 6-character room code.")
      return
    }
    val roomId = "room_$cleanCode"
    viewModelScope.launch {
      val roomRes = repository.getRoomById(roomId)
      val existingRoom = roomRes.getOrNull()
      if (existingRoom == null) {
        // Offer to create a fresh room with that exact code (§4 Edge cases)
        createAndJoinRoomWithCode(cleanCode, onJoined)
        return@launch
      }
      val currentPlayers = RoomPlayer.listFromJson(existingRoom.playersJson).toMutableList()
      val profile = _userProfile.value
      if (currentPlayers.none { it.uid == currentUserId }) {
        currentPlayers.add(
          RoomPlayer(
            uid = currentUserId,
            username = profile.username,
            displayName = profile.displayName,
            avatarJson = profile.avatarJson,
            isHost = false,
            team = currentPlayers.size % 2
          )
        )
      }
      val updatedMembers = (existingRoom.memberUids + currentUserId).distinct().take(12)
      repository.updateRoomState(
        existingRoom.copy(
          memberUids = updatedMembers,
          playersJson = RoomPlayer.listToJson(currentPlayers)
        )
      )
      subscribeToRoom(roomId)
      onJoined()
    }
  }

  private suspend fun createAndJoinRoomWithCode(code: String, onJoined: () -> Unit) {
    val profile = _userProfile.value
    val hostPlayer = RoomPlayer(
      uid = currentUserId,
      username = profile.username,
      displayName = profile.displayName,
      avatarJson = profile.avatarJson,
      isHost = true
    )
    val bots = buildBotPlayers(3)
    val res = repository.createRoom(
      roomCode = code,
      hostName = profile.displayName,
      hostAvatarJson = profile.avatarJson,
      isPublic = true,
      initialPlayers = listOf(hostPlayer) + bots
    )
    res.onSuccess { roomId ->
      subscribeToRoom(roomId)
      onJoined()
    }
  }

  private fun buildBotPlayers(count: Int): List<RoomPlayer> {
    val botProfiles = listOf(
      Triple("bot_fern", "fern_fox", "Fern Fox 🌿"),
      Triple("bot_terracotta", "clay_owl", "Clay Owl 🏺"),
      Triple("bot_ochre", "sunny_bee", "Sunny Bee 🐝"),
      Triple("bot_moss", "moss_frog", "Moss Frog 🐸"),
      Triple("bot_lotus", "lotus_cat", "Lotus Cat 🪷"),
      Triple("bot_cedar", "cedar_bear", "Cedar Bear 🌲"),
      Triple("bot_iris", "iris_dove", "Iris Dove 🎨"),
    )
    return botProfiles.take(count.coerceIn(0, 7)).mapIndexed { idx, (id, handle, name) ->
      RoomPlayer(
        uid = id,
        username = handle,
        displayName = name,
        avatarJson = AvatarConfig.random(id.hashCode()).toJson(),
        isBot = true,
        isHost = false,
        team = (idx + 1) % 2
      )
    }
  }

  fun addBotToLobby() {
    val room = _roomUiState.value.room ?: return
    val currentPlayers = _roomUiState.value.players
    if (currentPlayers.size >= 12) {
      showBanner("Room is at maximum capacity (12 players).")
      return
    }
    val existingBotIds = currentPlayers.map { it.uid }.toSet()
    val nextBot = buildBotPlayers(7).firstOrNull { it.uid !in existingBotIds } ?: return
    val updatedPlayers = currentPlayers + nextBot
    viewModelScope.launch {
      repository.updateRoomState(room.copy(playersJson = RoomPlayer.listToJson(updatedPlayers)))
    }
  }

  fun kickOrVoteKickPlayer(targetUid: String) {
    val room = _roomUiState.value.room ?: return
    val players = _roomUiState.value.players
    if (targetUid == currentUserId) return

    val updated = if (room.hostId == currentUserId) {
      players.filterNot { it.uid == targetUid }
    } else {
      // Majority vote-kick (§4)
      val majority = max(2, (players.size / 2) + 1)
      players.mapNotNull { p ->
        if (p.uid == targetUid) {
          val newVotes = (p.voteKickedBy + currentUserId).distinct()
          if (newVotes.size >= majority) null else p.copy(voteKickedBy = newVotes)
        } else p
      }
    }
    viewModelScope.launch {
      repository.updateRoomState(
        room.copy(
          playersJson = RoomPlayer.listToJson(updated),
          memberUids = room.memberUids.filterNot { it == targetUid }.ifEmpty { listOf(currentUserId) }
        )
      )
    }
  }

  fun transferHost(newHostUid: String) {
    val room = _roomUiState.value.room ?: return
    if (room.hostId != currentUserId) return
    val players = _roomUiState.value.players.map {
      it.copy(isHost = it.uid == newHostUid)
    }
    val newHostName = players.firstOrNull { it.uid == newHostUid }?.displayName ?: room.hostName
    viewModelScope.launch {
      repository.updateRoomState(
        room.copy(
          hostId = newHostUid,
          hostName = newHostName,
          playersJson = RoomPlayer.listToJson(players)
        )
      )
    }
  }

  fun updateLobbySettings(
    maxRounds: Int,
    drawTimeSec: Int,
    wordChoicesCount: Int,
    hintsCount: Int,
    gameMode: GameMode,
    difficulty: String,
    wordPackCategory: String,
    customWordsCsv: String,
  ) {
    val room = _roomUiState.value.room ?: return
    val effectiveDrawSec = if (gameMode == GameMode.SPEED_30S) 30 else drawTimeSec
    viewModelScope.launch {
      repository.updateRoomState(
        room.copy(
          maxRounds = maxRounds.coerceIn(2, 10).toLong(),
          drawTimeSec = effectiveDrawSec.coerceIn(30, 180).toLong(),
          wordChoicesCount = wordChoicesCount.coerceIn(1, 5).toLong(),
          hintsCount = hintsCount.coerceIn(0, 3).toLong(),
          gameMode = gameMode.name,
          difficulty = difficulty,
          wordPackCategory = wordPackCategory,
          customWordsCsv = customWordsCsv
        )
      )
      showBanner("Room settings updated!")
    }
  }

  fun leaveCurrentRoom() {
    roomListenerJob?.cancel()
    opsListenerJob?.cancel()
    messagesListenerJob?.cancel()
    timerTickerJob?.cancel()
    botSimulationJob?.cancel()
    checkpoints.clear()
    _roomUiState.value = ActiveRoomUiState()
  }

  private fun subscribeToRoom(roomId: String) {
    roomListenerJob?.cancel()
    opsListenerJob?.cancel()
    messagesListenerJob?.cancel()
    timerTickerJob?.cancel()

    roomListenerJob = viewModelScope.launch {
      repository.observeRoom(roomId)
        .catch { e -> Log.w(TAG, "Room stream error", e) }
        .collect { room ->
          if (room != null) {
            val players = RoomPlayer.listFromJson(room.playersJson)
            val me = players.firstOrNull { it.uid == currentUserId }
            val isDrawer = room.currentDrawerId == currentUserId
            val isHost = room.hostId == currentUserId
            val candidates = if (room.phase == GamePhase.WORD_CHOICE.name && isDrawer) {
              _roomUiState.value.candidateWords.ifEmpty {
                BuiltInCatalog.pickCandidateWords(
                  count = room.wordChoicesCount.toInt(),
                  category = room.wordPackCategory,
                  difficulty = room.difficulty,
                  customWordsCsv = room.customWordsCsv
                )
              }
            } else emptyList()

            _roomUiState.update { state ->
              state.copy(
                room = room,
                players = players,
                isDrawer = isDrawer,
                isHost = isHost,
                hasGuessedCurrentTurn = me?.hasGuessedThisTurn == true,
                candidateWords = candidates
              )
            }
          }
        }
    }

    opsListenerJob = viewModelScope.launch {
      repository.observeCanvasOps(roomId, currentUserId)
        .catch { e -> Log.w(TAG, "Ops stream error", e) }
        .collect { ops ->
          val effective = DoodleCanvasEngine.resolveEffectiveOps(ops)
          rebuildCheckpointsIfNeeded(effective)
          _roomUiState.update { it.copy(ops = ops, effectiveOps = effective) }
        }
    }

    messagesListenerJob = viewModelScope.launch {
      repository.observeRoomMessages(roomId, currentUserId)
        .catch { e -> Log.w(TAG, "Messages stream error", e) }
        .collect { messages ->
          _roomUiState.update { it.copy(messages = messages) }
        }
    }

    startRoomClockTicker()
  }

  private fun rebuildCheckpointsIfNeeded(effectiveOps: List<CanvasOpDoc>) {
    if (effectiveOps.isEmpty()) {
      checkpoints.clear()
      return
    }
    val targetCheckpointIndex = (effectiveOps.size / DoodleCanvasEngine.CHECKPOINT_INTERVAL) * DoodleCanvasEngine.CHECKPOINT_INTERVAL
    if (targetCheckpointIndex > 0 && !checkpoints.containsKey(targetCheckpointIndex)) {
      val bmp = DoodleCanvasEngine.createBlankCanvasBitmap(false)
      val canvas = AndroidCanvas(bmp)
      for (i in 0 until targetCheckpointIndex) {
        DoodleCanvasEngine.renderOpToBitmapCanvas(canvas, effectiveOps[i])
      }
      checkpoints[targetCheckpointIndex] = bmp
    }
  }

  /**
   * Renders the current committed canvas state using the nearest 20-op Bitmap checkpoint (§5).
   */
  fun renderCurrentCanvasBitmap(maxOpsLimit: Int? = null): Bitmap {
    val effective = _roomUiState.value.effectiveOps
    val limit = maxOpsLimit?.coerceIn(0, effective.size) ?: effective.size
    val checkpointIdx = ((limit / DoodleCanvasEngine.CHECKPOINT_INTERVAL) * DoodleCanvasEngine.CHECKPOINT_INTERVAL)
      .let { if (checkpoints.containsKey(it)) it else 0 }

    val baseBitmap = checkpoints[checkpointIdx]?.copy(Bitmap.Config.ARGB_8888, true)
      ?: DoodleCanvasEngine.createBlankCanvasBitmap(false)
    val canvas = AndroidCanvas(baseBitmap)
    for (i in checkpointIdx until limit) {
      DoodleCanvasEngine.renderOpToBitmapCanvas(canvas, effective[i])
    }
    return baseBitmap
  }

  // --- GAME STATE MACHINE (LOBBY -> WORD_CHOICE -> DRAWING -> TURN_END -> GAME_END) ---

  fun startGameFromLobby() {
    val room = _roomUiState.value.room ?: return
    val players = _roomUiState.value.players.let { current ->
      if (current.size < 2) {
        current + buildBotPlayers(3)
      } else current
    }.map { it.copy(score = 0, lastTurnDelta = 0, hasGuessedThisTurn = false, guessRankThisTurn = 0) }

    val firstDrawer = players.first()
    val candidates = BuiltInCatalog.pickCandidateWords(
      count = room.wordChoicesCount.toInt(),
      category = room.wordPackCategory,
      difficulty = room.difficulty,
      customWordsCsv = room.customWordsCsv
    )
    _roomUiState.update { it.copy(candidateWords = candidates) }
    ProceduralSoundEngine.playTurnStart()

    viewModelScope.launch {
      // Clear canvas for new match
      repository.appendCanvasOp(
        roomId = room.id,
        seq = (_roomUiState.value.ops.maxOfOrNull { it.seq } ?: 0L) + 1L,
        opType = "CLEAR",
        tool = DrawingTool.INK_PEN.name,
        colorHex = "#F7F3EB",
        sizePx = 1,
        opacity = 100,
        symmetryMode = SymmetryMode.NONE.name,
        seed = 0,
        encodedData = "",
        memberUids = room.memberUids
      )
      repository.updateRoomState(
        room.copy(
          phase = GamePhase.WORD_CHOICE.name,
          round = 1L,
          currentDrawerId = firstDrawer.uid,
          currentDrawerName = firstDrawer.displayName,
          wordMask = "Choosing a word...",
          secretWord = "",
          phaseEndsAtMs = System.currentTimeMillis() + 15_000L,
          playersJson = RoomPlayer.listToJson(players)
        )
      )
    }
  }

  fun selectSecretWord(word: String) {
    val room = _roomUiState.value.room ?: return
    val cleanWord = word.trim().lowercase()
    val mask = GameRulesEngine.buildWordMask(cleanWord, emptySet())
    val drawDurationMs = room.drawTimeSec.coerceIn(30L, 180L) * 1000L
    usedColorsInTurn.clear()
    usedSymmetryInTurn = false
    usedFloodFillInTurn = false
    ProceduralSoundEngine.playTurnStart()

    val resetPlayers = _roomUiState.value.players.map {
      it.copy(hasGuessedThisTurn = false, guessRankThisTurn = 0, lastTurnDelta = 0)
    }

    viewModelScope.launch {
      repository.appendCanvasOp(
        roomId = room.id,
        seq = (_roomUiState.value.ops.maxOfOrNull { it.seq } ?: 0L) + 1L,
        opType = "CLEAR",
        tool = DrawingTool.INK_PEN.name,
        colorHex = "#F7F3EB",
        sizePx = 1,
        opacity = 100,
        symmetryMode = SymmetryMode.NONE.name,
        seed = 0,
        encodedData = "",
        memberUids = room.memberUids
      )
      repository.updateRoomState(
        room.copy(
          phase = GamePhase.DRAWING.name,
          secretWord = cleanWord,
          wordMask = mask,
          phaseEndsAtMs = System.currentTimeMillis() + drawDurationMs,
          playersJson = RoomPlayer.listToJson(resetPlayers)
        )
      )
      scheduleBotTurnBehavior(room.id, cleanWord, room.currentDrawerId, room.memberUids)
    }
  }

  private fun scheduleBotTurnBehavior(
    roomId: String,
    secretWord: String,
    drawerId: String,
    memberUids: List<String>
  ) {
    botSimulationJob?.cancel()
    botSimulationJob = viewModelScope.launch {
      // If a Bot is the drawer, draw progressive vector strokes onto the canvas
      if (drawerId.startsWith("bot_")) {
        delay(900L)
        val botOps = DoodleCanvasEngine.generateBotIllustrationOps(roomId, drawerId, secretWord, memberUids)
        for ((idx, op) in botOps.withIndex()) {
          val currentRoom = _roomUiState.value.room ?: break
          if (currentRoom.phase != GamePhase.DRAWING.name) break
          val nextSeq = (_roomUiState.value.ops.maxOfOrNull { it.seq } ?: 0L) + 1L
          repository.appendCanvasOp(
            roomId = roomId,
            seq = nextSeq,
            opType = op.opType,
            tool = op.tool,
            colorHex = op.colorHex,
            sizePx = op.sizePx.toInt(),
            opacity = op.opacity.toInt(),
            symmetryMode = op.symmetryMode,
            seed = op.seed.toInt(),
            encodedData = op.encodedData,
            memberUids = memberUids
          )
          delay(1400L + idx * 600L)
        }
      }

      // Bots who are guessers submit lively guesses and eventually solve the word
      val bots = _roomUiState.value.players.filter { it.isBot && it.uid != drawerId }
      for ((idx, bot) in bots.withIndex()) {
        delay(3800L + idx * 3500L)
        val latestRoom = _roomUiState.value.room ?: break
        if (latestRoom.phase != GamePhase.DRAWING.name) break

        // 50% chance bot first makes a fun decoy guess before guessing right
        if (idx % 2 == 0) {
          val decoy = BuiltInCatalog.pickCandidateWords(1, latestRoom.wordPackCategory).firstOrNull() ?: "sprout"
          if (decoy != secretWord) {
            repository.sendRoomMessage(
              roomId = roomId,
              senderName = bot.displayName,
              channel = "EVERYONE",
              messageType = "TEXT",
              content = "Is it a $decoy?",
              memberUids = memberUids
            )
            delay(2600L)
          }
        }
        val activeRoomNow = _roomUiState.value.room ?: break
        if (activeRoomNow.phase == GamePhase.DRAWING.name) {
          recordCorrectGuessForPlayer(bot.uid, bot.displayName)
        }
      }
    }
  }

  private fun startRoomClockTicker() {
    timerTickerJob?.cancel()
    timerTickerJob = viewModelScope.launch {
      while (true) {
        delay(1000L)
        val room = _roomUiState.value.room ?: continue
        val now = System.currentTimeMillis()
        val remainingSec = ((room.phaseEndsAtMs - now) / 1000L).toInt().coerceAtLeast(0)
        _roomUiState.update { it.copy(timeRemainingSec = remainingSec) }

        when (room.phase) {
          GamePhase.WORD_CHOICE.name -> {
            // If a Bot is the drawer, pick a word after 2 seconds; or auto-pick on timeout (§4)
            if (room.currentDrawerId.startsWith("bot_") && remainingSec <= 13) {
              val botPick = BuiltInCatalog.pickCandidateWords(
                count = 1,
                category = room.wordPackCategory,
                difficulty = room.difficulty,
                customWordsCsv = room.customWordsCsv
              ).firstOrNull() ?: "sunflower"
              selectSecretWord(botPick)
            } else if (remainingSec == 0 && (room.isHostOrCurrentDrawer())) {
              val autoWord = _roomUiState.value.candidateWords.firstOrNull()
                ?: BuiltInCatalog.pickCandidateWords(1, room.wordPackCategory).first()
              selectSecretWord(autoWord)
            }
          }

          GamePhase.DRAWING.name -> {
            if (remainingSec in 1..10) {
              ProceduralSoundEngine.playLowTimeTick()
            }
            // Progressive letter hints across the second half of the turn (§4)
            val totalSec = room.drawTimeSec.toFloat().coerceAtLeast(30f)
            val elapsedFraction = ((totalSec - remainingSec) / totalSec).coerceIn(0f, 1f)
            val hintIndices = GameRulesEngine.computeHintIndices(
              room.secretWord,
              elapsedFraction,
              room.hintsCount.toInt()
            )
            val newMask = GameRulesEngine.buildWordMask(room.secretWord, hintIndices)
            if (newMask != room.wordMask && room.isHostOrCurrentDrawer()) {
              repository.updateRoomState(room.copy(wordMask = newMask))
            }

            if (remainingSec == 0 && room.isHostOrCurrentDrawer()) {
              transitionToTurnEnd()
            }
          }

          GamePhase.TURN_END.name -> {
            // Animate 3-second timelapse replay during TURN_END
            val turnEndTotalMs = 6000f
            val elapsedMs = (turnEndTotalMs - (room.phaseEndsAtMs - now).coerceIn(0L, 6000L))
            val replayProgress = (elapsedMs / 3000f).coerceIn(0.1f, 1f)
            _roomUiState.update { it.copy(timelapseProgressFraction = replayProgress) }

            if (remainingSec == 0 && room.isHostOrCurrentDrawer()) {
              advanceToNextDrawerOrGameEnd()
            }
          }
        }
      }
    }
  }

  private fun GameRoom.isHostOrCurrentDrawer(): Boolean {
    return hostId == currentUserId || currentDrawerId == currentUserId
  }

  private fun transitionToTurnEnd() {
    val room = _roomUiState.value.room ?: return
    if (room.phase == GamePhase.TURN_END.name) return
    botSimulationJob?.cancel()

    val players = _roomUiState.value.players
    val guessers = players.filter { it.uid != room.currentDrawerId }
    val correctGuessers = guessers.count { it.hasGuessedThisTurn }
    val drawerBonus = GameRulesEngine.calculateDrawerScore(correctGuessers, guessers.size)

    val updatedPlayers = players.map { p ->
      if (p.uid == room.currentDrawerId) {
        p.copy(
          score = p.score + drawerBonus,
          lastTurnDelta = drawerBonus
        )
      } else p
    }

    // Check Mona Lisa achievement (everyone guessed in under 20s)
    val elapsedSec = room.drawTimeSec - _roomUiState.value.timeRemainingSec
    if (room.currentDrawerId == currentUserId && guessers.isNotEmpty() && correctGuessers == guessers.size && elapsedSec <= 20) {
      unlockAchievement("mona_lisa")
    }
    if (room.currentDrawerId == currentUserId && room.wordPackCategory == "Botanical & Garden") {
      unlockAchievement("botanical_sketcher")
    }

    viewModelScope.launch {
      repository.updateRoomState(
        room.copy(
          phase = GamePhase.TURN_END.name,
          wordMask = room.secretWord.uppercase().toCharArray().joinToString(" "),
          phaseEndsAtMs = System.currentTimeMillis() + 6_000L,
          playersJson = RoomPlayer.listToJson(updatedPlayers)
        )
      )
    }
  }

  private fun advanceToNextDrawerOrGameEnd() {
    val room = _roomUiState.value.room ?: return
    val players = _roomUiState.value.players
    if (players.isEmpty()) return

    val currentIdx = players.indexOfFirst { it.uid == room.currentDrawerId }.coerceAtLeast(0)
    val nextIdx = (currentIdx + 1) % players.size
    val nextRound = if (nextIdx == 0) room.round + 1L else room.round

    if (nextRound > room.maxRounds) {
      // Transition to GAME_END podium
      ProceduralSoundEngine.playPodiumFanfare()
      finalizeMatchProgression(room, players)
      viewModelScope.launch {
        repository.updateRoomState(
          room.copy(
            phase = GamePhase.GAME_END.name,
            phaseEndsAtMs = System.currentTimeMillis() + 30_000L
          )
        )
      }
    } else {
      val nextDrawer = players[nextIdx]
      val candidates = BuiltInCatalog.pickCandidateWords(
        count = room.wordChoicesCount.toInt(),
        category = room.wordPackCategory,
        difficulty = room.difficulty,
        customWordsCsv = room.customWordsCsv
      )
      _roomUiState.update { it.copy(candidateWords = candidates, timelapseProgressFraction = 1f) }
      ProceduralSoundEngine.playTurnStart()
      viewModelScope.launch {
        if (room.gameMode != GameMode.PASS_THE_DOODLE.name) {
          repository.appendCanvasOp(
            roomId = room.id,
            seq = (_roomUiState.value.ops.maxOfOrNull { it.seq } ?: 0L) + 1L,
            opType = "CLEAR",
            tool = DrawingTool.INK_PEN.name,
            colorHex = "#F7F3EB",
            sizePx = 1,
            opacity = 100,
            symmetryMode = SymmetryMode.NONE.name,
            seed = 0,
            encodedData = "",
            memberUids = room.memberUids
          )
        }
        repository.updateRoomState(
          room.copy(
            phase = GamePhase.WORD_CHOICE.name,
            round = nextRound,
            currentDrawerId = nextDrawer.uid,
            currentDrawerName = nextDrawer.displayName,
            wordMask = "Choosing a word...",
            secretWord = "",
            phaseEndsAtMs = System.currentTimeMillis() + 15_000L
          )
        )
      }
    }
  }

  private fun finalizeMatchProgression(room: GameRoom, players: List<RoomPlayer>) {
    // Sort players with tie-breaker: more correct guesses, then faster average guess (§4)
    val sorted = players.sortedWith(
      compareByDescending<RoomPlayer> { it.score }
        .thenByDescending { it.correctGuessesCount }
        .thenBy { if (it.correctGuessesCount > 0) it.totalGuessTimeMs / it.correctGuessesCount else Long.MAX_VALUE }
    )
    val myRank = sorted.indexOfFirst { it.uid == currentUserId } + 1
    val me = sorted.firstOrNull { it.uid == currentUserId } ?: return
    val isWinner = myRank == 1

    val earnedXp = 120L + me.score.toLong() / 4L + (if (isWinner) 150L else 0L)
    val prevProfile = _userProfile.value
    val newXp = prevProfile.xp + earnedXp
    val newLevel = (1L + (newXp / 500L)).coerceAtMost(500L)

    val unlockedPacks = prevProfile.unlockedPacksJson.split(",").filter { it.isNotBlank() }.toMutableSet()
    unlockedPacks.add("starter")
    unlockedPacks.add("reactions")
    if (newLevel >= 2L) unlockedPacks.add("critters")
    if (newLevel >= 3L) {
      unlockedPacks.add("master")
      unlockAchievement("sticker_collector")
    }
    if (myRank in 1..3) unlockAchievement("podium_regular")
    if (me.score >= 1000) unlockAchievement("century_club")
    if (room.hostId == currentUserId) unlockAchievement("host_with_most")
    if (room.gameMode == GameMode.DAILY_CHALLENGE.name) unlockAchievement("daily_challenger")

    val updatedProfile = _userProfile.value.copy(
      xp = newXp,
      level = newLevel,
      gamesPlayed = prevProfile.gamesPlayed + 1L,
      wins = prevProfile.wins + (if (isWinner) 1L else 0L),
      totalPoints = prevProfile.totalPoints + me.score.toLong(),
      correctGuesses = prevProfile.correctGuesses + me.correctGuessesCount.toLong(),
      unlockedPacksJson = unlockedPacks.joinToString(",")
    )
    _userProfile.value = updatedProfile
    viewModelScope.launch {
      repository.ensureOrSaveProfile(updatedProfile)
    }
  }

  // --- CHAT, GUESS EVALUATION, EMOJI SHORTCODES & STICKERS ---

  fun submitChatOrGuess(rawText: String) {
    val room = _roomUiState.value.room ?: return
    val trimmed = rawText.trim()
    if (trimmed.isEmpty()) return

    val expandedEmoji = expandEmojiShortcodes(trimmed)
    val isDrawingPhase = room.phase == GamePhase.DRAWING.name
    val isDrawer = room.currentDrawerId == currentUserId
    val alreadyGuessed = _roomUiState.value.hasGuessedCurrentTurn

    // If guessing during DRAWING phase and user is an un-guessed guesser:
    if (isDrawingPhase && !isDrawer && !alreadyGuessed && room.secretWord.isNotBlank()) {
      when (GameRulesEngine.evaluateGuess(expandedEmoji, room.secretWord)) {
        GameRulesEngine.GuessEvaluation.EXACT_MATCH -> {
          recordCorrectGuessForPlayer(currentUserId, _userProfile.value.displayName)
          return
        }
        GameRulesEngine.GuessEvaluation.CLOSE_MATCH -> {
          ProceduralSoundEngine.playCloseGuess()
          _roomUiState.update { it.copy(closeGuessToast = "🔥 So close! '${expandedEmoji.take(24)}' is 1 letter off!") }
          viewModelScope.launch {
            delay(3200L)
            _roomUiState.update { it.copy(closeGuessToast = null) }
          }
          return
        }
        GameRulesEngine.GuessEvaluation.NO_MATCH -> {
          // Fall through and broadcast normal chat message
        }
      }
    }

    // Separate channel for players who already guessed + the drawer (§4)
    val channel = if (isDrawingPhase && (isDrawer || alreadyGuessed)) "GUESSED" else "EVERYONE"
    val filteredContent = GameRulesEngine.filterProfanityAndSecret(
      text = expandedEmoji,
      secretWord = room.secretWord,
      blockSecret = isDrawingPhase
    )

    viewModelScope.launch {
      repository.sendRoomMessage(
        roomId = room.id,
        senderName = _userProfile.value.displayName,
        channel = channel,
        messageType = "TEXT",
        content = filteredContent,
        memberUids = room.memberUids
      )
    }
  }

  private fun recordCorrectGuessForPlayer(guesserUid: String, guesserName: String) {
    val room = _roomUiState.value.room ?: return
    val players = _roomUiState.value.players
    val target = players.firstOrNull { it.uid == guesserUid } ?: return
    if (target.hasGuessedThisTurn) return

    val currentRank = players.count { it.hasGuessedThisTurn } + 1
    val timeLeftSec = _roomUiState.value.timeRemainingSec.toDouble()
    val drawTimeSec = room.drawTimeSec.toDouble()
    val pointsEarned = GameRulesEngine.calculateGuesserScore(timeLeftSec, drawTimeSec, currentRank)
    val guessElapsedMs = ((drawTimeSec - timeLeftSec).coerceAtLeast(1.0) * 1000.0).toLong()

    if (guesserUid == currentUserId) {
      ProceduralSoundEngine.playCorrectGuess()
      currentStreak += 1
      if (currentRank == 1) unlockAchievement("first_blood")
      if (guessElapsedMs <= 6000L) unlockAchievement("speed_demon")
      if (currentStreak >= 3) unlockAchievement("streak_master")

      val prevBest = _userProfile.value.bestGuessMs
      val newBest = if (prevBest == 0L || guessElapsedMs < prevBest) guessElapsedMs else prevBest
      val newStreak = max(_userProfile.value.bestStreak, currentStreak.toLong())
      _userProfile.update { it.copy(bestGuessMs = newBest, bestStreak = newStreak) }

      _roomUiState.update {
        it.copy(
          hasGuessedCurrentTurn = true,
          scorePopupText = "+$pointsEarned pts! (#$currentRank)"
        )
      }
      viewModelScope.launch {
        delay(2800L)
        _roomUiState.update { it.copy(scorePopupText = null) }
      }
    }

    val updatedPlayers = players.map { p ->
      if (p.uid == guesserUid) {
        p.copy(
          score = p.score + pointsEarned,
          lastTurnDelta = pointsEarned,
          correctGuessesCount = p.correctGuessesCount + 1,
          totalGuessTimeMs = p.totalGuessTimeMs + guessElapsedMs,
          hasGuessedThisTurn = true,
          guessRankThisTurn = currentRank
        )
      } else p
    }

    val nonDrawerPlayers = updatedPlayers.filter { it.uid != room.currentDrawerId }
    val allHaveGuessed = nonDrawerPlayers.isNotEmpty() && nonDrawerPlayers.all { it.hasGuessedThisTurn }

    viewModelScope.launch {
      repository.sendRoomMessage(
        roomId = room.id,
        senderName = guesserName,
        channel = "EVERYONE",
        messageType = "GUESS_RESULT",
        content = "🎉 $guesserName guessed the word! (+$pointsEarned pts)",
        memberUids = room.memberUids
      )
      repository.updateRoomState(
        room.copy(playersJson = RoomPlayer.listToJson(updatedPlayers))
      )
      if (allHaveGuessed) {
        delay(400L)
        transitionToTurnEnd()
      }
    }
  }

  fun sendSticker(stickerId: String) {
    val room = _roomUiState.value.room ?: return
    val sticker = BuiltInCatalog.ALL_STICKERS[stickerId] ?: return
    sentSocialInMatch = true
    unlockAchievement("social_butterfly")
    val channel = if (room.phase == GamePhase.DRAWING.name && (_roomUiState.value.isDrawer || _roomUiState.value.hasGuessedCurrentTurn)) {
      "GUESSED"
    } else {
      "EVERYONE"
    }
    viewModelScope.launch {
      repository.sendRoomMessage(
        roomId = room.id,
        senderName = _userProfile.value.displayName,
        channel = channel,
        messageType = "STICKER",
        content = "${sticker.emojiArt} ${sticker.badgeText}",
        stickerId = sticker.id,
        memberUids = room.memberUids
      )
    }
  }

  fun sendLiveReaction(emoji: String) {
    if (repository.egressGovernor.isSlowBatchingActive) return
    ProceduralSoundEngine.playReactionPop()
    sentSocialInMatch = true
    unlockAchievement("social_butterfly")
    val reaction = FloatingReaction(
      id = System.nanoTime(),
      emoji = emoji,
      xFraction = (0.15f + (kotlin.math.abs(System.nanoTime() % 70) / 100f))
    )
    _roomUiState.update { state ->
      state.copy(floatingReactions = (state.floatingReactions + reaction).takeLast(12))
    }
    viewModelScope.launch {
      delay(2200L)
      _roomUiState.update { state ->
        state.copy(floatingReactions = state.floatingReactions.filterNot { it.id == reaction.id })
      }
    }
  }

  private fun expandEmojiShortcodes(text: String): String {
    val shortcodes = mapOf(
      ":fire:" to "🔥",
      ":clap:" to "👏",
      ":heart:" to "❤️",
      ":joy:" to "😂",
      ":mindblown:" to "🤯",
      ":sprout:" to "🌱",
      ":sunflower:" to "🌻",
      ":art:" to "🎨",
      ":trophy:" to "🏆",
      ":star:" to "✨",
      ":gg:" to "🎉",
      ":thinking:" to "🤔"
    )
    var out = text
    for ((code, emoji) in shortcodes) {
      out = out.replace(code, emoji, ignoreCase = true)
    }
    return out
  }

  // --- CANVAS TOOL & VECTOR OP-LOG HANDLERS ---

  fun selectTool(tool: DrawingTool) {
    _roomUiState.update { it.copy(selectedTool = tool) }
  }

  fun selectColorHex(hex: String) {
    val normalized = if (hex.startsWith("#")) hex.uppercase() else "#${hex.uppercase()}"
    _roomUiState.update { state ->
      val updatedRecents = (listOf(normalized) + state.recentColorsHex.filterNot { it == normalized }).take(8)
      state.copy(selectedColorHex = normalized, recentColorsHex = updatedRecents)
    }
  }

  fun setBrushSize(sizePx: Int) {
    _roomUiState.update { it.copy(brushSizePx = sizePx.coerceIn(1, 80)) }
  }

  fun setBrushOpacity(opacity: Int) {
    _roomUiState.update { it.copy(brushOpacity = opacity.coerceIn(10, 100)) }
  }

  fun setStabilizerStrength(strength: Float) {
    _roomUiState.update { it.copy(stabilizerStrength = strength.coerceIn(0f, 0.85f)) }
  }

  fun setSymmetryMode(mode: SymmetryMode) {
    _roomUiState.update { it.copy(symmetryMode = mode) }
  }

  fun toggleFillShape() {
    _roomUiState.update { it.copy(fillShapeEnabled = !it.fillShapeEnabled) }
  }

  fun toggleColorblindPalette() {
    _roomUiState.update { it.copy(colorblindPaletteEnabled = !it.colorblindPaletteEnabled) }
  }

  fun toggleHighContrast() {
    _roomUiState.update { it.copy(highContrastEnabled = !it.highContrastEnabled) }
  }

  fun toggleMuteAudio() {
    val newMuted = !_roomUiState.value.isMuted
    ProceduralSoundEngine.isMuted = newMuted
    _roomUiState.update { it.copy(isMuted = newMuted) }
  }

  fun commitUserStrokeOrShape(points: List<DoodleCanvasEngine.TimedPoint>) {
    val room = _roomUiState.value.room ?: return
    val state = _roomUiState.value
    if (points.isEmpty()) return

    val tool = state.selectedTool
    if (tool == DrawingTool.EYEDROPPER) {
      val bmp = renderCurrentCanvasBitmap()
      val pt = points.last()
      val sampledHex = DoodleCanvasEngine.sampleEyedropperHex(bmp, pt.x, pt.y)
      selectColorHex(sampledHex)
      selectTool(DrawingTool.INK_PEN)
      showBanner("Sampled color $sampledHex")
      return
    }

    if (tool == DrawingTool.FILL_BUCKET) {
      val bmp = renderCurrentCanvasBitmap()
      val pt = points.first()
      val spans = DoodleCanvasEngine.computeScanlineFloodFill(
        bitmap = bmp,
        logicalX = pt.x,
        logicalY = pt.y,
        fillColorHex = state.selectedColorHex
      )
      if (spans.isEmpty()) return
      val encodedSpans = DoodleCanvasEngine.encodeFillSpans(spans)
      usedFloodFillInTurn = true
      unlockAchievement("flood_fill_pro")
      appendOpInternal(
        room = room,
        opType = "FILL",
        tool = tool.name,
        colorHex = state.selectedColorHex,
        sizePx = state.brushSizePx,
        opacity = state.brushOpacity,
        symmetryMode = SymmetryMode.NONE.name,
        seed = 0,
        encodedData = encodedSpans
      )
      return
    }

    val simplified = if (tool.isShape) {
      listOf(points.first(), points.last())
    } else {
      DoodleCanvasEngine.simplifyPoints(points, epsilon = 0.6f)
    }
    val encoded = DoodleCanvasEngine.encodeStrokePoints(simplified)
    usedColorsInTurn.add(state.selectedColorHex)
    if (usedColorsInTurn.size >= 5) unlockAchievement("color_maestro")
    if (state.symmetryMode != SymmetryMode.NONE) {
      usedSymmetryInTurn = true
      unlockAchievement("symmetry_wizard")
    }

    val seed = if (tool.isShape) {
      if (state.fillShapeEnabled) 1 else 0
    } else {
      (System.nanoTime() and 0x7FFFFFFFL).toInt()
    }

    appendOpInternal(
      room = room,
      opType = if (tool.isShape) "SHAPE" else "STROKE",
      tool = tool.name,
      colorHex = state.selectedColorHex,
      sizePx = state.brushSizePx,
      opacity = state.brushOpacity,
      symmetryMode = state.symmetryMode.name,
      seed = seed,
      encodedData = encoded
    )
  }

  fun undoCanvasOp() {
    val room = _roomUiState.value.room ?: return
    if (_roomUiState.value.effectiveOps.isEmpty()) return
    appendOpInternal(
      room = room,
      opType = "UNDO",
      tool = DrawingTool.INK_PEN.name,
      colorHex = "#000000",
      sizePx = 1,
      opacity = 100,
      symmetryMode = SymmetryMode.NONE.name,
      seed = 0,
      encodedData = ""
    )
  }

  fun redoCanvasOp() {
    val room = _roomUiState.value.room ?: return
    appendOpInternal(
      room = room,
      opType = "REDO",
      tool = DrawingTool.INK_PEN.name,
      colorHex = "#000000",
      sizePx = 1,
      opacity = 100,
      symmetryMode = SymmetryMode.NONE.name,
      seed = 0,
      encodedData = ""
    )
  }

  fun clearCanvasOps() {
    val room = _roomUiState.value.room ?: return
    checkpoints.clear()
    appendOpInternal(
      room = room,
      opType = "CLEAR",
      tool = DrawingTool.INK_PEN.name,
      colorHex = "#F7F3EB",
      sizePx = 1,
      opacity = 100,
      symmetryMode = SymmetryMode.NONE.name,
      seed = 0,
      encodedData = ""
    )
  }

  private fun appendOpInternal(
    room: GameRoom,
    opType: String,
    tool: String,
    colorHex: String,
    sizePx: Int,
    opacity: Int,
    symmetryMode: String,
    seed: Int,
    encodedData: String,
  ) {
    val nextSeq = (_roomUiState.value.ops.maxOfOrNull { it.seq } ?: 0L) + 1L
    val optimisticOp = CanvasOpDoc(
      id = "local_$nextSeq",
      roomId = room.id,
      authorId = currentUserId,
      seq = nextSeq,
      opType = opType,
      tool = tool,
      colorHex = colorHex,
      sizePx = sizePx.toLong(),
      opacity = opacity.toLong(),
      symmetryMode = symmetryMode,
      seed = seed.toLong(),
      encodedData = encodedData,
      memberUids = room.memberUids
    )
    val updatedOps = _roomUiState.value.ops + optimisticOp
    val effective = DoodleCanvasEngine.resolveEffectiveOps(updatedOps)
    rebuildCheckpointsIfNeeded(effective)
    _roomUiState.update { it.copy(ops = updatedOps, effectiveOps = effective) }

    viewModelScope.launch {
      repository.appendCanvasOp(
        roomId = room.id,
        seq = nextSeq,
        opType = opType,
        tool = tool,
        colorHex = colorHex,
        sizePx = sizePx,
        opacity = opacity,
        symmetryMode = symmetryMode,
        seed = seed,
        encodedData = encodedData,
        memberUids = room.memberUids
      )
    }
  }

  fun exportDrawingToGallery(context: Context, transparent: Boolean = false) {
    viewModelScope.launch {
      val result = DoodleCanvasEngine.saveCanvasToGalleryPng(
        context = context,
        ops = _roomUiState.value.ops,
        transparentBackground = transparent
      )
      result.onSuccess { filename ->
        showBanner("Saved '$filename' to Pictures/DoodleDash!")
      }.onFailure { err ->
        showBanner("Export failed: ${err.localizedMessage ?: "Unknown error"}")
      }
    }
  }

  fun submitPlayerReport(targetUid: String, targetUsername: String, reason: String) {
    val room = _roomUiState.value.room
    val roomCode = room?.roomCode ?: "LOBBY0"
    val last20Messages = _roomUiState.value.messages.takeLast(20).map {
      "${it.senderName}: ${it.content}"
    }
    viewModelScope.launch {
      repository.submitReport(
        targetUserId = targetUid,
        targetUsername = targetUsername,
        roomCode = roomCode,
        reason = reason,
        chatSnapshotJson = JSONObject().put("lines", last20Messages).toString()
      )
      showBanner("Safety report submitted with 20-line chat snapshot.")
    }
  }

  private fun unlockAchievement(achievementId: String) {
    val def: AchievementDef = BuiltInCatalog.ACHIEVEMENTS.firstOrNull { it.id == achievementId } ?: return
    val currentProfile = _userProfile.value
    val existingIds = currentProfile.achievementsJson.split(",").filter { it.isNotBlank() }.toMutableSet()
    if (achievementId in existingIds) return

    existingIds.add(achievementId)
    val newXp = currentProfile.xp + def.xpReward.toLong()
    val newLevel = (1L + (newXp / 500L)).coerceAtMost(500L)
    val updated = currentProfile.copy(
      xp = newXp,
      level = newLevel,
      equippedTitle = def.unlockedTitle,
      equippedBadge = def.id,
      achievementsJson = existingIds.joinToString(",")
    )
    _userProfile.value = updated
    showBanner("🏆 Achievement Unlocked: ${def.name} (+${def.xpReward} XP)!")
    viewModelScope.launch {
      repository.ensureOrSaveProfile(updated)
    }
  }

  fun showBanner(message: String) {
    _roomUiState.update { it.copy(statusBanner = message) }
    viewModelScope.launch {
      delay(3200L)
      _roomUiState.update { state ->
        if (state.statusBanner == message) state.copy(statusBanner = null) else state
      }
    }
  }

  val egressUsedBytes: Long
    get() = repository.egressGovernor.totalBytesUsed

  val egressUsagePercent: Int
    get() = (repository.egressGovernor.usageFraction * 100f).toInt()

  private companion object {
    const val TAG = "DoodleDashVM"
  }
}
