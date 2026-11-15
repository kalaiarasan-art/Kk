package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

const val APP_TITLE = "Doodle Dash"

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val username: String = "sage_sketcher",
  val displayName: String = "Sage Sketcher",
  val avatarJson: String = AvatarConfig().toJson(),
  val bio: String = "Sketching botanical wonders & racing the clock!",
  val countryFlag: String = "🌿",
  val themeColor: String = "#C86446",
  val xp: Long = 0L,
  val level: Long = 1L,
  val equippedTitle: String = "Botanical Rookie",
  val equippedBadge: String = "first_blood",
  val gamesPlayed: Long = 0L,
  val wins: Long = 0L,
  val totalPoints: Long = 0L,
  val correctGuesses: Long = 0L,
  val bestGuessMs: Long = 0L,
  val bestStreak: Long = 0L,
  val unlockedPacksJson: String = "starter,reactions",
  val achievementsJson: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class Friendship(
  @DocumentId val id: String = "",
  val userId: String = "",
  val friendCode: String = "",
  val friendUsername: String = "",
  val friendDisplayName: String = "",
  val friendAvatarJson: String = AvatarConfig().toJson(),
  val status: String = "ACCEPTED",
  val roomCode: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CustomWordPack(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val language: String = "EN",
  val difficulty: String = "MIXED",
  val isFamilyFriendly: Boolean = true,
  val wordsCsv: String = "",
  val wordCount: Long = 3L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class GameRoom(
  @DocumentId val id: String = "",
  val roomCode: String = "DASH01",
  val hostId: String = "",
  val hostName: String = "",
  val isPublic: Boolean = true,
  val phase: String = GamePhase.LOBBY.name,
  val round: Long = 1L,
  val maxRounds: Long = 3L,
  val drawTimeSec: Long = 80L,
  val wordChoicesCount: Long = 3L,
  val hintsCount: Long = 2L,
  val gameMode: String = GameMode.CLASSIC.name,
  val difficulty: String = "MIXED",
  val wordPackCategory: String = "All Categories",
  val customWordsCsv: String = "",
  val currentDrawerId: String = "",
  val currentDrawerName: String = "",
  val wordMask: String = "",
  val secretWord: String = "",
  val phaseEndsAtMs: Long = 0L,
  val memberUids: List<String> = emptyList(),
  val playersJson: String = "[]",
  val reactionCountsJson: String = "{}",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CanvasOpDoc(
  @DocumentId val id: String = "",
  val roomId: String = "",
  val authorId: String = "",
  val seq: Long = 0L,
  val opType: String = "STROKE",
  val tool: String = DrawingTool.INK_PEN.name,
  val colorHex: String = "#26231F",
  val sizePx: Long = 8L,
  val opacity: Long = 100L,
  val symmetryMode: String = SymmetryMode.NONE.name,
  val seed: Long = 42L,
  val encodedData: String = "",
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ChatMessageDoc(
  @DocumentId val id: String = "",
  val roomId: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val channel: String = "EVERYONE",
  val messageType: String = "TEXT",
  val content: String = "",
  val stickerId: String = "",
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class ModerationReport(
  @DocumentId val id: String = "",
  val reporterId: String = "",
  val targetUserId: String = "",
  val targetUsername: String = "",
  val roomCode: String = "",
  val reason: String = "",
  val chatSnapshotJson: String = "[]",
  val status: String = "OPEN",
  @ServerTimestamp val createdAt: Timestamp? = null,
)

enum class GamePhase(val label: String) {
  LOBBY("Lobby"),
  WORD_CHOICE("Choosing Word"),
  DRAWING("Drawing"),
  TURN_END("Turn Replay"),
  GAME_END("Final Podium"),
}

enum class GameMode(val title: String, val subtitle: String, val defaultDrawSec: Int) {
  CLASSIC("Classic Studio", "Standard rotating turns with progressive hints", 80),
  SPEED_30S("Speed Dash (30s)", "Fast 30-second rounds — pure instinct!", 30),
  BLIND_DRAWER("Blind Ink", "The drawer's strokes turn invisible while sketching", 60),
  TEAM_BATTLE("Team Terracotta vs Sage", "2v2 / 4v4 cooperative guessing", 75),
  PASS_THE_DOODLE("Pass-the-Doodle", "Canvas relays across drawers each round", 60),
  DAILY_CHALLENGE("Daily Botanical Challenge", "Curated daily words for global high score", 60),
}

enum class DrawingTool(val label: String, val isShape: Boolean = false) {
  INK_PEN("Ink Pen"),
  PENCIL("Grain Pencil"),
  MARKER("Chisel Marker"),
  HIGHLIGHTER("Highlighter"),
  CALLIGRAPHY("Calligraphy Nib"),
  SPRAY("Airbrush Spray"),
  FILL_BUCKET("Flood Fill"),
  HARD_ERASER("Hard Eraser"),
  SOFT_ERASER("Soft Eraser"),
  EYEDROPPER("Eyedropper"),
  LINE("Line", isShape = true),
  RECTANGLE("Rectangle", isShape = true),
  ELLIPSE("Ellipse", isShape = true),
  TRIANGLE("Triangle", isShape = true),
}

enum class SymmetryMode(val label: String, val shortLabel: String) {
  NONE("No Symmetry", "Off"),
  MIRROR_V("Vertical Mirror", "Mir-V"),
  MIRROR_H("Horizontal Mirror", "Mir-H"),
  RADIAL_4("4-Fold Radial", "Rad-4"),
  RADIAL_6("6-Fold Kaleidoscope", "Rad-6"),
  RADIAL_8("8-Fold Mandala", "Rad-8"),
}

data class AvatarConfig(
  val faceShape: Int = 0,       // 0: Round, 1: Squircle, 2: Oval, 3: Heart
  val skinToneIndex: Int = 1,   // 0..7 earthy & warm tones
  val eyeStyle: Int = 0,        // 0: Happy, 1: Wink, 2: Starry, 3: Focused, 4: Cozylashed
  val browStyle: Int = 0,       // 0: Soft, 1: Expressive, 2: Determined, 3: Raised
  val mouthStyle: Int = 0,      // 0: Smile, 1: Grin, 2: Cat, 3: Whistling, 4: Smirk
  val hairHatStyle: Int = 1,    // 0: Leaf Sprout, 1: Beret, 2: Sunhat, 3: Curly Crown, 4: Beanie, 5: Flower Crown
  val hairColorIndex: Int = 0,  // 0..7
  val accessoryStyle: Int = 0,  // 0: None, 1: Round Glasses, 2: Monocle, 3: Paint Cheek, 4: Botanical Earring
  val bgColorIndex: Int = 0,    // 0..7 botanical background swatches
) {
  fun toJson(): String {
    return JSONObject().apply {
      put("f", faceShape)
      put("s", skinToneIndex)
      put("e", eyeStyle)
      put("b", browStyle)
      put("m", mouthStyle)
      put("h", hairHatStyle)
      put("hc", hairColorIndex)
      put("a", accessoryStyle)
      put("bg", bgColorIndex)
    }.toString()
  }

  companion object {
    fun fromJson(json: String): AvatarConfig {
      return try {
        val obj = JSONObject(json)
        AvatarConfig(
          faceShape = obj.optInt("f", 0),
          skinToneIndex = obj.optInt("s", 1),
          eyeStyle = obj.optInt("e", 0),
          browStyle = obj.optInt("b", 0),
          mouthStyle = obj.optInt("m", 0),
          hairHatStyle = obj.optInt("h", 1),
          hairColorIndex = obj.optInt("hc", 0),
          accessoryStyle = obj.optInt("a", 0),
          bgColorIndex = obj.optInt("bg", 0),
        )
      } catch (_: Exception) {
        AvatarConfig()
      }
    }

    fun random(seed: Int): AvatarConfig {
      val s = kotlin.math.abs(seed)
      return AvatarConfig(
        faceShape = s % 4,
        skinToneIndex = (s / 3) % 6,
        eyeStyle = (s / 7) % 5,
        browStyle = (s / 11) % 4,
        mouthStyle = (s / 13) % 5,
        hairHatStyle = (s / 17) % 6,
        hairColorIndex = (s / 19) % 6,
        accessoryStyle = (s / 23) % 5,
        bgColorIndex = (s / 29) % 6,
      )
    }
  }
}

data class RoomPlayer(
  val uid: String,
  val username: String,
  val displayName: String,
  val avatarJson: String = AvatarConfig().toJson(),
  val score: Int = 0,
  val lastTurnDelta: Int = 0,
  val correctGuessesCount: Int = 0,
  val totalGuessTimeMs: Long = 0L,
  val hasGuessedThisTurn: Boolean = false,
  val guessRankThisTurn: Int = 0,
  val isBot: Boolean = false,
  val isHost: Boolean = false,
  val team: Int = 0, // 0: Terracotta, 1: Sage
  val voteKickedBy: List<String> = emptyList(),
) {
  fun toJsonObject(): JSONObject = JSONObject().apply {
    put("uid", uid)
    put("username", username)
    put("displayName", displayName)
    put("avatarJson", avatarJson)
    put("score", score)
    put("lastTurnDelta", lastTurnDelta)
    put("correctGuessesCount", correctGuessesCount)
    put("totalGuessTimeMs", totalGuessTimeMs)
    put("hasGuessedThisTurn", hasGuessedThisTurn)
    put("guessRankThisTurn", guessRankThisTurn)
    put("isBot", isBot)
    put("isHost", isHost)
    put("team", team)
    put("voteKickedBy", JSONArray(voteKickedBy))
  }

  companion object {
    fun fromJsonObject(obj: JSONObject): RoomPlayer {
      val votesArr = obj.optJSONArray("voteKickedBy")
      val votes = mutableListOf<String>()
      if (votesArr != null) {
        for (i in 0 until votesArr.length()) {
          votes.add(votesArr.optString(i))
        }
      }
      return RoomPlayer(
        uid = obj.optString("uid", ""),
        username = obj.optString("username", "player"),
        displayName = obj.optString("displayName", "Player"),
        avatarJson = obj.optString("avatarJson", AvatarConfig().toJson()),
        score = obj.optInt("score", 0),
        lastTurnDelta = obj.optInt("lastTurnDelta", 0),
        correctGuessesCount = obj.optInt("correctGuessesCount", 0),
        totalGuessTimeMs = obj.optLong("totalGuessTimeMs", 0L),
        hasGuessedThisTurn = obj.optBoolean("hasGuessedThisTurn", false),
        guessRankThisTurn = obj.optInt("guessRankThisTurn", 0),
        isBot = obj.optBoolean("isBot", false),
        isHost = obj.optBoolean("isHost", false),
        team = obj.optInt("team", 0),
        voteKickedBy = votes,
      )
    }

    fun listToJson(players: List<RoomPlayer>): String {
      val arr = JSONArray()
      players.forEach { arr.put(it.toJsonObject()) }
      return arr.toString()
    }

    fun listFromJson(json: String): List<RoomPlayer> {
      return try {
        val arr = JSONArray(json)
        List(arr.length()) { idx -> fromJsonObject(arr.getJSONObject(idx)) }
      } catch (_: Exception) {
        emptyList()
      }
    }
  }
}

/**
 * Pure functions for Doodle Dash scoring, room code generation, and NFKD guess matching (§4).
 */
object GameRulesEngine {
  private const val ROOM_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"

  fun generateRoomCode(seed: Long = System.nanoTime()): String {
    var s = seed xor 0x5DEECE66DL
    return buildString(6) {
      repeat(6) {
        s = s * 6364136223846793005L + 1442695040888963407L
        val idx = ((s ushr 32).toInt() and Int.MAX_VALUE) % ROOM_CODE_ALPHABET.length
        append(ROOM_CODE_ALPHABET[idx])
      }
    }
  }

  /**
   * Normalize guess with NFKD, strip diacritics, lowercase, keep only letters and digits (Unicode-aware).
   */
  fun normalizeGuess(input: String): String {
    val decomposed = java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFKD)
    val stripped = decomposed.replace(Regex("\\p{M}+"), "")
    return buildString {
      for (ch in stripped.lowercase()) {
        if (ch.isLetterOrDigit()) {
          append(ch)
        }
      }
    }
  }

  /**
   * Computes Levenshtein edit distance between two normalized strings.
   */
  fun editDistance(a: String, b: String): Int {
    if (a == b) return 0
    if (a.isEmpty()) return b.length
    if (b.isEmpty()) return a.length
    val prev = IntArray(b.length + 1) { it }
    val curr = IntArray(b.length + 1)
    for (i in a.indices) {
      curr[0] = i + 1
      for (j in b.indices) {
        val cost = if (a[i] == b[j]) 0 else 1
        curr[j + 1] = minOf(
          curr[j] + 1,
          prev[j + 1] + 1,
          prev[j] + cost
        )
      }
      System.arraycopy(curr, 0, prev, 0, prev.size)
    }
    return prev[b.length]
  }

  enum class GuessEvaluation {
    EXACT_MATCH,
    CLOSE_MATCH,
    NO_MATCH
  }

  /**
   * Evaluates a player's chat message against the secret word:
   * - Exact normalized match or message containing the normalized secret word -> EXACT_MATCH
   * - Edit distance 1 on words of 5+ letters -> CLOSE_MATCH ("So close!")
   * - Otherwise -> NO_MATCH
   */
  fun evaluateGuess(guessText: String, secretWord: String): GuessEvaluation {
    val normSecret = normalizeGuess(secretWord)
    if (normSecret.isEmpty()) return GuessEvaluation.NO_MATCH
    val normGuess = normalizeGuess(guessText)
    if (normGuess.isEmpty()) return GuessEvaluation.NO_MATCH

    if (normGuess == normSecret || (normSecret.length >= 3 && normGuess.contains(normSecret))) {
      return GuessEvaluation.EXACT_MATCH
    }
    if (normSecret.length >= 5 && editDistance(normGuess, normSecret) == 1) {
      return GuessEvaluation.CLOSE_MATCH
    }
    return GuessEvaluation.NO_MATCH
  }

  /**
   * Guesser score formula (§4):
   * round(500 * (0.2 + 0.8 * timeLeft / drawTime)) - 25 * (rank - 1), minimum 50.
   */
  fun calculateGuesserScore(timeLeftSec: Double, drawTimeSec: Double, rank: Int): Int {
    val safeDrawTime = drawTimeSec.coerceAtLeast(1.0)
    val clampedTimeLeft = timeLeftSec.coerceIn(0.0, safeDrawTime)
    val base = (500.0 * (0.2 + 0.8 * (clampedTimeLeft / safeDrawTime))).roundToInt()
    val rankPenalty = 25 * (rank.coerceAtLeast(1) - 1)
    return (base - rankPenalty).coerceAtLeast(50)
  }

  /**
   * Drawer score formula (§4):
   * 50 per correct guesser, +100 if everyone guessed, capped at 400.
   */
  fun calculateDrawerScore(correctGuessers: Int, totalGuessers: Int): Int {
    if (correctGuessers <= 0) return 0
    val base = correctGuessers * 50
    val allGuessedBonus = if (totalGuessers > 0 && correctGuessers >= totalGuessers) 100 else 0
    return (base + allGuessedBonus).coerceAtMost(400)
  }

  /**
   * Builds word mask with spaces and hyphens visible, revealing up to [revealedCount] letters
   * evenly across the second half of the turn.
   */
  fun buildWordMask(secretWord: String, revealedIndices: Set<Int> = emptySet()): String {
    return secretWord.mapIndexed { idx, ch ->
      when {
        ch == ' ' -> "  "
        ch == '-' -> "-"
        idx in revealedIndices -> ch.uppercaseChar().toString()
        else -> "_"
      }
    }.joinToString(" ")
  }

  fun computeHintIndices(secretWord: String, elapsedFraction: Float, maxHints: Int): Set<Int> {
    if (maxHints <= 0 || elapsedFraction < 0.5f) return emptySet()
    val letterIndices = secretWord.indices.filter { secretWord[it].isLetterOrDigit() }
    if (letterIndices.size <= 2) return emptySet()
    val progressSecondHalf = ((elapsedFraction - 0.5f) / 0.45f).coerceIn(0f, 1f)
    val hintsToShow = ((progressSecondHalf * maxHints).toInt() + 1).coerceAtMost(maxHints)
      .coerceAtMost((letterIndices.size - 1).coerceAtLeast(0))
    val step = (letterIndices.size.toFloat() / (maxHints + 1)).coerceAtLeast(1f)
    return (0 until hintsToShow).map { i ->
      letterIndices[((i + 1) * step).toInt().coerceIn(0, letterIndices.lastIndex)]
    }.toSet()
  }

  private val PROFANITY_LIST = setOf(
    "badword", "slurword", "explicit", "profane", "offensive"
  )

  fun filterProfanityAndSecret(text: String, secretWord: String, blockSecret: Boolean): String {
    var result = text.take(140)
    // Block URLs per §6
    result = result.replace(Regex("https?://\\S+|www\\.\\S+", RegexOption.IGNORE_CASE), "[link blocked]")
    for (bad in PROFANITY_LIST) {
      result = result.replace(Regex(bad, RegexOption.IGNORE_CASE), "***")
    }
    if (blockSecret && secretWord.isNotBlank()) {
      result = result.replace(Regex(Regex.escape(secretWord.trim()), RegexOption.IGNORE_CASE), "***")
    }
    return result
  }
}
