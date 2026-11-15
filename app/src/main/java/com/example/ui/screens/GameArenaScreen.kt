package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BuiltInCatalog
import com.example.data.model.CanvasOpDoc
import com.example.data.model.ChatMessageDoc
import com.example.data.model.DrawingTool
import com.example.data.model.GameMode
import com.example.data.model.GamePhase
import com.example.data.model.RoomPlayer
import com.example.data.model.SymmetryMode
import com.example.ui.canvas.DoodleCanvasEngine
import com.example.ui.components.AvatarView
import com.example.ui.components.RoomCodeQrMatrix
import com.example.ui.components.StickerBadgeCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.ActiveRoomUiState
import com.example.viewmodel.DoodleDashViewModel
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun GameArenaScreen(
  viewModel: DoodleDashViewModel,
  onLeaveRoom: () -> Unit,
) {
  val state by viewModel.roomUiState.collectAsStateWithLifecycle()
  val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
  val room = state.room ?: return
  val context = LocalContext.current

  var showInviteDialog by remember { mutableStateOf(false) }
  var showClearConfirmDialog by remember { mutableStateOf(false) }
  var showCustomHexDialog by remember { mutableStateOf(false) }
  var showHostSettingsDialog by remember { mutableStateOf(false) }
  var selectedPlayerForCard by remember { mutableStateOf<RoomPlayer?>(null) }

  BackHandler {
    viewModel.leaveCurrentRoom()
    onLeaveRoom()
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
      // 1. Top Match Bar
      TopArenaBar(
        roomCode = room.roomCode,
        phase = room.phase,
        round = room.round.toInt(),
        maxRounds = room.maxRounds.toInt(),
        wordMask = if (state.isDrawer && room.phase == GamePhase.DRAWING.name) {
          "DRAW: ${room.secretWord.uppercase()}"
        } else {
          room.wordMask
        },
        timeRemainingSec = state.timeRemainingSec,
        isMuted = state.isMuted,
        onShowInvite = { showInviteDialog = true },
        onToggleMute = { viewModel.toggleMuteAudio() },
        onExportPng = { viewModel.exportDrawingToGallery(context, false) },
        onLeave = {
          viewModel.leaveCurrentRoom()
          onLeaveRoom()
        }
      )

      // Status Toast Banner
      AnimatedVisibility(
        visible = state.statusBanner != null || state.closeGuessToast != null || state.scorePopupText != null,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        val text = state.scorePopupText ?: state.closeGuessToast ?: state.statusBanner ?: ""
        Surface(
          color = if (state.scorePopupText != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(
            text = text,
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }

      // 2. Horizontal Scoreboard Strip
      PlayersScoreboardStrip(
        players = state.players,
        currentDrawerId = room.currentDrawerId,
        onPlayerClick = { selectedPlayerForCard = it }
      )

      // 3. Main Phase Content (Adaptive Portrait / Landscape via BoxWithConstraints)
      when (room.phase) {
        GamePhase.LOBBY.name -> {
          RoomLobbyContent(
            state = state,
            onStartMatch = { viewModel.startGameFromLobby() },
            onAddBot = { viewModel.addBotToLobby() },
            onOpenSettings = { showHostSettingsDialog = true },
            onShowInvite = { showInviteDialog = true },
            onSendMessage = { viewModel.submitChatOrGuess(it) },
            onSendSticker = { viewModel.sendSticker(it) },
            unlockedPackIds = userProfile.unlockedPacksJson.split(",").toSet()
          )
        }

        GamePhase.GAME_END.name -> {
          GameEndPodiumContent(
            players = state.players,
            onPlayAgain = { viewModel.startGameFromLobby() },
            onExportPng = { viewModel.exportDrawingToGallery(context, false) },
            onLeave = {
              viewModel.leaveCurrentRoom()
              onLeaveRoom()
            }
          )
        }

        else -> {
          // WORD_CHOICE, DRAWING, or TURN_END
          BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isLandscape = maxWidth > 680.dp
            if (isLandscape) {
              Row(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                Column(
                  modifier = Modifier
                    .weight(0.62f)
                    .fillMaxHeight(),
                  verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  ActiveCanvasStage(
                    state = state,
                    viewModel = viewModel,
                    onRequestClear = { showClearConfirmDialog = true },
                    onRequestCustomColor = { showCustomHexDialog = true },
                    modifier = Modifier.weight(1f)
                  )
                }
                ChatAndSocialPanel(
                  state = state,
                  unlockedPackIds = userProfile.unlockedPacksJson.split(",").toSet(),
                  onSendChatOrGuess = { viewModel.submitChatOrGuess(it) },
                  onSendSticker = { viewModel.sendSticker(it) },
                  onSendReaction = { viewModel.sendLiveReaction(it) },
                  modifier = Modifier
                    .weight(0.38f)
                    .fillMaxHeight()
                )
              }
            } else {
              Column(
                modifier = Modifier
                  .fillMaxSize()
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                ActiveCanvasStage(
                  state = state,
                  viewModel = viewModel,
                  onRequestClear = { showClearConfirmDialog = true },
                  onRequestCustomColor = { showCustomHexDialog = true },
                  modifier = Modifier.weight(if (state.isDrawer) 0.64f else 0.50f)
                )
                ChatAndSocialPanel(
                  state = state,
                  unlockedPackIds = userProfile.unlockedPacksJson.split(",").toSet(),
                  onSendChatOrGuess = { viewModel.submitChatOrGuess(it) },
                  onSendSticker = { viewModel.sendSticker(it) },
                  onSendReaction = { viewModel.sendLiveReaction(it) },
                  modifier = Modifier.weight(if (state.isDrawer) 0.36f else 0.50f)
                )
              }
            }
          }
        }
      }
    }
  }

  // Invite & QR Modal
  if (showInviteDialog) {
    RoomInviteDialog(
      roomCode = room.roomCode,
      onDismiss = { showInviteDialog = false }
    )
  }

  // Clear Canvas Confirmation Dialog (§5)
  if (showClearConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showClearConfirmDialog = false },
      title = { Text("Clear the entire canvas?") },
      text = { Text("This adds a CLEAR operation to the vector op-log. You can still Undo it if needed.") },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearCanvasOps()
            showClearConfirmDialog = false
          },
          modifier = Modifier.testTag("confirm_clear_canvas_button")
        ) {
          Text("Clear Canvas")
        }
      },
      dismissButton = {
        TextButton(onClick = { showClearConfirmDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Custom HSV / Hex Color Picker Dialog (§5)
  if (showCustomHexDialog) {
    CustomColorPickerDialog(
      initialHex = state.selectedColorHex,
      onSelectHex = { hex ->
        viewModel.selectColorHex(hex)
        showCustomHexDialog = false
      },
      onDismiss = { showCustomHexDialog = false }
    )
  }

  // Host Room Settings Dialog (§4)
  if (showHostSettingsDialog) {
    HostRoomSettingsDialog(
      room = room,
      onSave = { rounds, drawSec, choices, hints, mode, diff, cat, customCsv ->
        viewModel.updateLobbySettings(rounds, drawSec, choices, hints, mode, diff, cat, customCsv)
        showHostSettingsDialog = false
      },
      onDismiss = { showHostSettingsDialog = false }
    )
  }

  // Player Card Modal (Add Friend, Kick / Vote-Kick, Transfer Host, Report) (§6)
  selectedPlayerForCard?.let { targetPlayer ->
    PlayerSocialActionDialog(
      player = targetPlayer,
      isCurrentUserHost = state.isHost,
      isSelf = targetPlayer.uid == viewModel.currentUserId,
      onAddFriend = {
        viewModel.addFriendByUsername(targetPlayer.username, targetPlayer.displayName)
        selectedPlayerForCard = null
      },
      onKickOrVoteKick = {
        viewModel.kickOrVoteKickPlayer(targetPlayer.uid)
        selectedPlayerForCard = null
      },
      onTransferHost = {
        viewModel.transferHost(targetPlayer.uid)
        selectedPlayerForCard = null
      },
      onReport = { reason ->
        viewModel.submitPlayerReport(targetPlayer.uid, targetPlayer.username, reason)
        selectedPlayerForCard = null
      },
      onDismiss = { selectedPlayerForCard = null }
    )
  }
}

@Composable
private fun TopArenaBar(
  roomCode: String,
  phase: String,
  round: Int,
  maxRounds: Int,
  wordMask: String,
  timeRemainingSec: Int,
  isMuted: Boolean,
  onShowInvite: () -> Unit,
  onToggleMute: () -> Unit,
  onExportPng: () -> Unit,
  onLeave: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Room Code & Round Badge
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer,
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .clickable { onShowInvite() }
            .testTag("room_code_invite_chip")
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.QrCode2,
              contentDescription = "Room QR Code",
              modifier = Modifier.size(15.dp),
              tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              text = roomCode,
              fontFamily = JetBrainsMonoFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }

        Text(
          text = "R$round/$maxRounds",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }

      // Secret Word Mask + Countdown Timer
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
      ) {
        Text(
          text = wordMask.ifBlank { phase },
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis
        )
        if (phase != GamePhase.LOBBY.name) {
          val isLowTime = timeRemainingSec in 1..10
          Text(
            text = "${timeRemainingSec}s",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isLowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
          )
        }
      }

      // Action Icons
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleMute, modifier = Modifier.size(36.dp)) {
          Icon(
            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = if (isMuted) "Unmute Audio" else "Mute Audio",
            modifier = Modifier.size(18.dp)
          )
        }
        IconButton(
          onClick = onExportPng,
          modifier = Modifier.size(36.dp).testTag("export_png_button")
        ) {
          Icon(
            imageVector = Icons.Default.Download,
            contentDescription = "Save Drawing as PNG",
            modifier = Modifier.size(18.dp)
          )
        }
        IconButton(
          onClick = onLeave,
          modifier = Modifier.size(36.dp).testTag("leave_room_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Leave Room",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun PlayersScoreboardStrip(
  players: List<RoomPlayer>,
  currentDrawerId: String,
  onPlayerClick: (RoomPlayer) -> Unit,
) {
  LazyRow(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
      .padding(horizontal = 8.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    items(players, key = { it.uid }) { player ->
      val isDrawer = player.uid == currentDrawerId
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = when {
          player.hasGuessedThisTurn -> MaterialTheme.colorScheme.secondaryContainer
          isDrawer -> MaterialTheme.colorScheme.primaryContainer
          else -> MaterialTheme.colorScheme.surface
        },
        modifier = Modifier.clickable { onPlayerClick(player) }
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          AvatarView(avatarJson = player.avatarJson, size = 26.dp)
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
              Text(
                text = player.displayName.take(11),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 11.sp,
                maxLines = 1
              )
              if (isDrawer) {
                Icon(
                  imageVector = Icons.Default.Brush,
                  contentDescription = "Drawing",
                  modifier = Modifier.size(12.dp),
                  tint = MaterialTheme.colorScheme.primary
                )
              } else if (player.hasGuessedThisTurn) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = "Guessed",
                  modifier = Modifier.size(12.dp),
                  tint = MaterialTheme.colorScheme.secondary
                )
              }
            }
            Text(
              text = "${player.score} pts" + (if (player.lastTurnDelta > 0) " (+${player.lastTurnDelta})" else ""),
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveCanvasStage(
  state: ActiveRoomUiState,
  viewModel: DoodleDashViewModel,
  onRequestClear: () -> Unit,
  onRequestCustomColor: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val room = state.room ?: return
  val activeStrokePoints = remember { mutableStateListOf<DoodleCanvasEngine.TimedPoint>() }
  var cursorPosLogical by remember { mutableStateOf<Offset?>(null) }
  var lastPointTimestamp by remember { mutableLongStateOf(0L) }
  var showBrushSettingsDrawer by remember { mutableStateOf(false) }

  // Render committed bitmap from 20-op checkpoint cache (or partial slice during 3s TURN_END timelapse!)
  val committedBitmap = remember(state.effectiveOps, room.phase, state.timelapseProgressFraction) {
    val maxOps = if (room.phase == GamePhase.TURN_END.name) {
      (state.effectiveOps.size * state.timelapseProgressFraction).roundToInt().coerceAtLeast(1)
    } else null
    viewModel.renderCurrentCanvasBitmap(maxOps)
  }

  val isBlindModeForDrawer = state.isDrawer &&
    room.phase == GamePhase.DRAWING.name &&
    room.gameMode == GameMode.BLIND_DRAWER.name

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(4.dp)
  ) {
    // 1600x1000 Aspect-Ratio Fitted Canvas Stage
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFFF7F3EB))
        .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
      contentAlignment = Alignment.Center
    ) {
      val canDrawNow = state.isDrawer && room.phase == GamePhase.DRAWING.name

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .aspectRatio(1.6f, matchHeightConstraintsFirst = false)
          .testTag("doodle_main_canvas")
          .pointerInput(
            canDrawNow,
            state.selectedTool,
            state.selectedColorHex,
            state.brushSizePx,
            state.stabilizerStrength,
            state.symmetryMode
          ) {
            if (!canDrawNow) return@pointerInput
            if (state.selectedTool == DrawingTool.FILL_BUCKET || state.selectedTool == DrawingTool.EYEDROPPER) {
              detectTapGestures { offset ->
                val lx = (offset.x / size.width.coerceAtLeast(1)) * DoodleCanvasEngine.LOGICAL_WIDTH
                val ly = (offset.y / size.height.coerceAtLeast(1)) * DoodleCanvasEngine.LOGICAL_HEIGHT
                viewModel.commitUserStrokeOrShape(
                  listOf(DoodleCanvasEngine.TimedPoint(lx, ly, 0.8f, 16))
                )
              }
            } else {
              detectDragGestures(
                onDragStart = { offset ->
                  activeStrokePoints.clear()
                  val lx = (offset.x / size.width.coerceAtLeast(1)) * DoodleCanvasEngine.LOGICAL_WIDTH
                  val ly = (offset.y / size.height.coerceAtLeast(1)) * DoodleCanvasEngine.LOGICAL_HEIGHT
                  lastPointTimestamp = System.currentTimeMillis()
                  val pt = DoodleCanvasEngine.TimedPoint(lx, ly, 0.72f, 16)
                  activeStrokePoints.add(pt)
                  cursorPosLogical = Offset(lx, ly)
                },
                onDrag = { change, _ ->
                  change.consume()
                  val rawLx = (change.position.x / size.width.coerceAtLeast(1)) * DoodleCanvasEngine.LOGICAL_WIDTH
                  val rawLy = (change.position.y / size.height.coerceAtLeast(1)) * DoodleCanvasEngine.LOGICAL_HEIGHT
                  val prev = activeStrokePoints.lastOrNull()
                  val stab = state.stabilizerStrength.coerceIn(0f, 0.85f)
                  val smoothedLx = if (prev != null) prev.x * stab + rawLx * (1f - stab) else rawLx
                  val smoothedLy = if (prev != null) prev.y * stab + rawLy * (1f - stab) else rawLy

                  val now = System.currentTimeMillis()
                  val dt = (now - lastPointTimestamp).toInt().coerceIn(4, 250)
                  lastPointTimestamp = now

                  // Simulate pressure from drawing velocity (§5)
                  val dist = if (prev != null) hypot(smoothedLx - prev.x, smoothedLy - prev.y) else 0f
                  val speed = dist / dt.coerceAtLeast(1)
                  val simulatedPressure = (0.88f - speed * 0.14f).coerceIn(0.25f, 0.95f)

                  activeStrokePoints.add(
                    DoodleCanvasEngine.TimedPoint(
                      x = smoothedLx.coerceIn(0f, DoodleCanvasEngine.LOGICAL_WIDTH),
                      y = smoothedLy.coerceIn(0f, DoodleCanvasEngine.LOGICAL_HEIGHT),
                      pressure = simulatedPressure,
                      dtMs = dt
                    )
                  )
                  cursorPosLogical = Offset(smoothedLx, smoothedLy)
                },
                onDragEnd = {
                  if (activeStrokePoints.isNotEmpty()) {
                    viewModel.commitUserStrokeOrShape(activeStrokePoints.toList())
                    activeStrokePoints.clear()
                  }
                  cursorPosLogical = null
                },
                onDragCancel = {
                  activeStrokePoints.clear()
                  cursorPosLogical = null
                }
              )
            }
          }
      ) {
        if (!isBlindModeForDrawer) {
          // 1. Committed layer
          drawImage(
            image = committedBitmap.asImageBitmap(),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
          )

          // 2. Live stroke in progress + Symmetry branches
          if (activeStrokePoints.isNotEmpty()) {
            val scaleX = size.width / DoodleCanvasEngine.LOGICAL_WIDTH
            val scaleY = size.height / DoodleCanvasEngine.LOGICAL_HEIGHT
            val liveColor = Color(DoodleCanvasEngine.parseHexColor(state.selectedColorHex))
              .copy(alpha = (state.brushOpacity / 100f).coerceIn(0.1f, 1f))
            val branches = DoodleCanvasEngine.expandSymmetryPoints(activeStrokePoints, state.symmetryMode)
            val strokePx = (state.brushSizePx * scaleX).coerceAtLeast(2f)

            for (branch in branches) {
              if (state.selectedTool.isShape && branch.size >= 2) {
                val p0 = branch.first()
                val p1 = branch.last()
                drawLine(
                  color = liveColor,
                  start = Offset(p0.x * scaleX, p0.y * scaleY),
                  end = Offset(p1.x * scaleX, p1.y * scaleY),
                  strokeWidth = strokePx
                )
              } else {
                for (i in 1 until branch.size) {
                  val p0 = branch[i - 1]
                  val p1 = branch[i]
                  drawLine(
                    color = liveColor,
                    start = Offset(p0.x * scaleX, p0.y * scaleY),
                    end = Offset(p1.x * scaleX, p1.y * scaleY),
                    strokeWidth = strokePx
                  )
                }
              }
            }
          }
        }

        // 3. Brush Size Cursor Ring Overlay (§5)
        cursorPosLogical?.let { cur ->
          val scaleX = size.width / DoodleCanvasEngine.LOGICAL_WIDTH
          val scaleY = size.height / DoodleCanvasEngine.LOGICAL_HEIGHT
          drawCircle(
            color = Color(0xFF26231F).copy(alpha = 0.65f),
            radius = (state.brushSizePx * scaleX * 0.5f).coerceAtLeast(4f),
            center = Offset(cur.x * scaleX, cur.y * scaleY),
            style = Stroke(width = 1.5f)
          )
        }
      }

      if (isBlindModeForDrawer) {
        Surface(
          color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
        ) {
          Text(
            text = "🙈 Blind Ink Mode! Guessers can see your strokes, but you can't!",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
          )
        }
      }

      // Floating Live Reactions Overlay (§6)
      Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        state.floatingReactions.forEachIndexed { idx, reaction ->
          Text(
            text = reaction.emoji,
            fontSize = 26.sp,
            modifier = Modifier
              .align(Alignment.BottomStart)
              .offset(
                x = (reaction.xFraction * 240).dp,
                y = (-24 - (idx * 18)).dp
              )
          )
        }
      }

      // WORD_CHOICE Overlay (§4)
      if (room.phase == GamePhase.WORD_CHOICE.name) {
        Surface(
          color = Color(0xCC26231F),
          modifier = Modifier.fillMaxSize()
        ) {
          Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            if (state.isDrawer) {
              Text(
                text = "Pick Your Secret Word (${state.timeRemainingSec}s)",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFF7F3EB)
              )
              Spacer(modifier = Modifier.height(14.dp))
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
              ) {
                state.candidateWords.forEach { word ->
                  Button(
                    onClick = { viewModel.selectSecretWord(word) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("pick_word_${word.replace(" ", "_")}")
                  ) {
                    Text(
                      text = word.uppercase(),
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                  }
                }
              }
            } else {
              Text(
                text = "🎨 ${room.currentDrawerName} is choosing a word...",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFF7F3EB),
                textAlign = TextAlign.Center
              )
              Spacer(modifier = Modifier.height(8.dp))
              Text(
                text = "Get ready to type your guess in chat!",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFEFE9DC)
              )
            }
          }
        }
      }

      // TURN_END 3-Second Timelapse Replay Banner (§4 & §5)
      if (room.phase == GamePhase.TURN_END.name) {
        Surface(
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(10.dp)
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "The word was: ${room.secretWord.uppercase()}",
              style = MaterialTheme.typography.titleLarge,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              text = "⏪ Replaying 3-second vector timelapse...",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      }
    }

    // Pro Drawing Tool Dock (Visible when current user is the active drawer)
    if (state.isDrawer && room.phase == GamePhase.DRAWING.name) {
      ProDrawerToolDock(
        state = state,
        showAdvancedSliders = showBrushSettingsDrawer,
        onToggleAdvancedSliders = { showBrushSettingsDrawer = !showBrushSettingsDrawer },
        onSelectTool = { viewModel.selectTool(it) },
        onSelectColorHex = { viewModel.selectColorHex(it) },
        onOpenCustomHex = onRequestCustomColor,
        onBrushSizeChange = { viewModel.setBrushSize(it) },
        onBrushOpacityChange = { viewModel.setBrushOpacity(it) },
        onStabilizerChange = { viewModel.setStabilizerStrength(it) },
        onSymmetryChange = { viewModel.setSymmetryMode(it) },
        onToggleFillShape = { viewModel.toggleFillShape() },
        onToggleColorblind = { viewModel.toggleColorblindPalette() },
        onUndo = { viewModel.undoCanvasOp() },
        onRedo = { viewModel.redoCanvasOp() },
        onClear = onRequestClear
      )
    }
  }
}

@Composable
private fun ProDrawerToolDock(
  state: ActiveRoomUiState,
  showAdvancedSliders: Boolean,
  onToggleAdvancedSliders: () -> Unit,
  onSelectTool: (DrawingTool) -> Unit,
  onSelectColorHex: (String) -> Unit,
  onOpenCustomHex: () -> Unit,
  onBrushSizeChange: (Int) -> Unit,
  onBrushOpacityChange: (Int) -> Unit,
  onStabilizerChange: (Float) -> Unit,
  onSymmetryChange: (SymmetryMode) -> Unit,
  onToggleFillShape: () -> Unit,
  onToggleColorblind: () -> Unit,
  onUndo: () -> Unit,
  onRedo: () -> Unit,
  onClear: () -> Unit,
) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(16.dp),
    tonalElevation = 3.dp,
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Row 1: Brush & Shape Tools + Undo / Redo / Clear / Tune
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          modifier = Modifier
            .weight(1f)
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          DrawingTool.entries.forEach { tool ->
            FilterChip(
              selected = state.selectedTool == tool,
              onClick = { onSelectTool(tool) },
              label = { Text(tool.label, fontSize = 11.sp) }
            )
          }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = onUndo, modifier = Modifier.size(34.dp).testTag("canvas_undo_button")) {
            Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onRedo, modifier = Modifier.size(34.dp).testTag("canvas_redo_button")) {
            Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onClear, modifier = Modifier.size(34.dp).testTag("canvas_clear_button")) {
            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Canvas", modifier = Modifier.size(18.dp))
          }
          IconButton(onClick = onToggleAdvancedSliders, modifier = Modifier.size(34.dp).testTag("canvas_tune_button")) {
            Icon(
              Icons.Default.Tune,
              contentDescription = "Brush & Symmetry Settings",
              tint = if (showAdvancedSliders) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
          }
        }
      }

      // Row 2: 24 Curated Botanical / Colorblind Swatches + Custom Hex + Symmetry Quick Cycle
      val palette = if (state.colorblindPaletteEnabled) {
        DoodleCanvasEngine.COLORBLIND_SAFE_PALETTE
      } else {
        DoodleCanvasEngine.CURATED_PALETTE
      }
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        AssistChip(
          onClick = {
            val nextOrd = (state.symmetryMode.ordinal + 1) % SymmetryMode.entries.size
            onSymmetryChange(SymmetryMode.entries[nextOrd])
          },
          label = { Text("Sym: ${state.symmetryMode.shortLabel}", fontSize = 11.sp) }
        )

        AssistChip(
          onClick = onOpenCustomHex,
          label = { Text(state.selectedColorHex, fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp) }
        )

        palette.forEach { color ->
          val hex = DoodleCanvasEngine.colorToHex(color)
          val isSelected = hex.equals(state.selectedColorHex, ignoreCase = true)
          Box(
            modifier = Modifier
              .size(if (isSelected) 28.dp else 22.dp)
              .clip(CircleShape)
              .background(color)
              .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = CircleShape
              )
              .clickable { onSelectColorHex(hex) }
          )
        }
      }

      // Collapsible Sliders Drawer (Size 1-80px, Opacity, Stabilizer, Fill Shape, Colorblind Mode)
      AnimatedVisibility(visible = showAdvancedSliders) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("Size ${state.brushSizePx}px", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(72.dp))
            Slider(
              value = state.brushSizePx.toFloat(),
              onValueChange = { onBrushSizeChange(it.roundToInt()) },
              valueRange = 1f..80f,
              modifier = Modifier.weight(1f)
            )
            Text("Alpha ${state.brushOpacity}%", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(76.dp))
            Slider(
              value = state.brushOpacity.toFloat(),
              onValueChange = { onBrushOpacityChange(it.roundToInt()) },
              valueRange = 10f..100f,
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text("Stabilizer", style = MaterialTheme.typography.labelMedium)
              Slider(
                value = state.stabilizerStrength,
                onValueChange = onStabilizerChange,
                valueRange = 0f..0.85f,
                modifier = Modifier.weight(1f)
              )
            }
            FilterChip(
              selected = state.fillShapeEnabled,
              onClick = onToggleFillShape,
              label = { Text("Fill Shapes", fontSize = 11.sp) }
            )
            Spacer(modifier = Modifier.width(6.dp))
            FilterChip(
              selected = state.colorblindPaletteEnabled,
              onClick = onToggleColorblind,
              label = { Text("Colorblind Safe", fontSize = 11.sp) }
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ChatAndSocialPanel(
  state: ActiveRoomUiState,
  unlockedPackIds: Set<String>,
  onSendChatOrGuess: (String) -> Unit,
  onSendSticker: (String) -> Unit,
  onSendReaction: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var chatInput by remember { mutableStateOf("") }
  var showStickerDrawer by remember { mutableStateOf(false) }

  // Filter visible messages: players who haven't guessed yet do not see the "GUESSED" channel (§4)
  val canSeeGuessedChannel = state.isDrawer || state.hasGuessedCurrentTurn || state.room?.phase != GamePhase.DRAWING.name
  val visibleMessages = remember(state.messages, canSeeGuessedChannel) {
    state.messages.filter { msg ->
      msg.channel == "EVERYONE" || (msg.channel == "GUESSED" && canSeeGuessedChannel)
    }
  }

  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(8.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      // One-tap Live Reaction Bar (👏 🔥 😂 🤯 ❤️) + Sticker Picker Toggle (§6)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
          listOf("👏", "🔥", "😂", "🤯", "❤️").forEach { emoji ->
            Surface(
              shape = CircleShape,
              color = MaterialTheme.colorScheme.surfaceVariant,
              modifier = Modifier
                .clickable { onSendReaction(emoji) }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(text = emoji, fontSize = 16.sp)
            }
          }
        }

        AssistChip(
          onClick = { showStickerDrawer = !showStickerDrawer },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.EmojiEmotions,
              contentDescription = "Stickers",
              modifier = Modifier.size(16.dp)
            )
          },
          label = { Text(if (showStickerDrawer) "Hide Stickers" else "Stickers", fontSize = 11.sp) }
        )
      }

      // Collapsible 4-Pack Sticker Drawer (§6)
      AnimatedVisibility(visible = showStickerDrawer) {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          contentPadding = PaddingValues(vertical = 4.dp)
        ) {
          val availableStickers = BuiltInCatalog.STICKER_PACKS
            .filter { it.id in unlockedPackIds || it.requiredLevel <= 1 }
            .flatMap { it.stickers }
          items(availableStickers, key = { it.id }) { sticker ->
            StickerBadgeCard(
              sticker = sticker,
              compact = true,
              onClick = {
                onSendSticker(sticker.id)
                showStickerDrawer = false
              }
            )
          }
        }
      }

      // Chat & Guess Feed
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(
            MaterialTheme.colorScheme.background.copy(alpha = 0.65f),
            RoundedCornerShape(12.dp)
          )
          .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        if (visibleMessages.isEmpty()) {
          item {
            Text(
              text = if (state.isDrawer) {
                "You are drawing! Watch players guess here in real time."
              } else {
                "Type your guess below! Use :fire: :sprout: or tap Stickers."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
        items(visibleMessages, key = { it.id.ifBlank { "${it.senderId}_${it.content}_${it.createdAt}" } }) { msg ->
          ChatMessageRow(msg)
        }
      }

      // Guess / Chat Input Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        OutlinedTextField(
          value = chatInput,
          onValueChange = { chatInput = it.take(140) },
          placeholder = {
            Text(
              text = when {
                state.isDrawer -> "Chat in Guessed channel..."
                state.hasGuessedCurrentTurn -> "You guessed it! Chat with winners..."
                else -> "Type your guess or :shortcode:..."
              },
              fontSize = 12.sp
            )
          },
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("chat_guess_input"),
          shape = RoundedCornerShape(14.dp)
        )
        Button(
          onClick = {
            if (chatInput.isNotBlank()) {
              onSendChatOrGuess(chatInput)
              chatInput = ""
            }
          },
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
          modifier = Modifier.testTag("send_guess_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Send,
            contentDescription = "Send Guess or Chat"
          )
        }
      }
    }
  }
}

@Composable
private fun ChatMessageRow(msg: ChatMessageDoc) {
  when (msg.messageType) {
    "GUESS_RESULT" -> {
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = msg.content,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
      }
    }
    "STICKER" -> {
      val sticker = BuiltInCatalog.ALL_STICKERS[msg.stickerId]
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        Text(
          text = "${msg.senderName}:",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary
        )
        if (sticker != null) {
          StickerBadgeCard(sticker = sticker, compact = true)
        } else {
          Text(text = msg.content, style = MaterialTheme.typography.bodyMedium)
        }
      }
    }
    else -> {
      val isGuessedOnly = msg.channel == "GUESSED"
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        if (isGuessedOnly) {
          Text(
            text = "[Guessed]",
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.secondary
          )
        }
        Text(
          text = "${msg.senderName}:",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = msg.content,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
    }
  }
}

@Composable
private fun RoomLobbyContent(
  state: ActiveRoomUiState,
  onStartMatch: () -> Unit,
  onAddBot: () -> Unit,
  onOpenSettings: () -> Unit,
  onShowInvite: () -> Unit,
  onSendMessage: (String) -> Unit,
  onSendSticker: (String) -> Unit,
  unlockedPackIds: Set<String>,
) {
  val room = state.room ?: return
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(12.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Card(
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "Room Lobby • ${room.roomCode}",
              style = MaterialTheme.typography.headlineMedium
            )
            Text(
              text = "${room.gameMode} • ${room.maxRounds} Rounds • ${room.drawTimeSec}s Draw • ${room.wordPackCategory}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          IconButton(
            onClick = onOpenSettings,
            modifier = Modifier.testTag("lobby_settings_button")
          ) {
            Icon(Icons.Default.Settings, contentDescription = "Room Settings")
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = onStartMatch,
            modifier = Modifier
              .weight(1f)
              .height(48.dp)
              .testTag("start_game_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Start Match (${state.players.size} Players)", fontWeight = FontWeight.Bold)
          }

          OutlinedButton(
            onClick = onAddBot,
            modifier = Modifier.height(48.dp).testTag("add_bot_button"),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.SmartToy, contentDescription = "Add Bot", modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("+Bot")
          }

          OutlinedButton(
            onClick = onShowInvite,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(14.dp)
          ) {
            Icon(Icons.Default.QrCode2, contentDescription = "Invite QR", modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    ChatAndSocialPanel(
      state = state,
      unlockedPackIds = unlockedPackIds,
      onSendChatOrGuess = onSendMessage,
      onSendSticker = onSendSticker,
      onSendReaction = {},
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
private fun GameEndPodiumContent(
  players: List<RoomPlayer>,
  onPlayAgain: () -> Unit,
  onExportPng: () -> Unit,
  onLeave: () -> Unit,
) {
  val sorted = remember(players) {
    players.sortedWith(
      compareByDescending<RoomPlayer> { it.score }
        .thenByDescending { it.correctGuessesCount }
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Text(
      text = "🎉 Match Podium 🎉",
      style = MaterialTheme.typography.displayMedium,
      color = MaterialTheme.colorScheme.primary
    )

    Text(
      text = "XP, Level Progress & Achievements have been saved to your Studio Profile!",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center
    )

    sorted.forEachIndexed { idx, player ->
      val medal = when (idx) {
        0 -> "🥇 1st"
        1 -> "🥈 2nd"
        2 -> "🥉 3rd"
        else -> "#${idx + 1}"
      }
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (idx == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Text(text = medal, style = MaterialTheme.typography.titleLarge)
            AvatarView(avatarJson = player.avatarJson, size = 44.dp)
            Column {
              Text(text = player.displayName, style = MaterialTheme.typography.titleMedium)
              Text(
                text = "${player.correctGuessesCount} correct guesses",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Text(
            text = "${player.score} pts",
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.primary
          )
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onPlayAgain,
        modifier = Modifier.weight(1f).height(50.dp),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text("Play Another Match", fontWeight = FontWeight.Bold)
      }
      OutlinedButton(
        onClick = onExportPng,
        modifier = Modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text("Save PNG")
      }
      OutlinedButton(
        onClick = onLeave,
        modifier = Modifier.height(50.dp),
        shape = RoundedCornerShape(14.dp)
      ) {
        Text("Hub")
      }
    }
  }
}

@Composable
private fun RoomInviteDialog(
  roomCode: String,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val inviteUrl = "https://doodledash.app/r/$roomCode"
  var copied by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Invite Friends to Room $roomCode") },
    text = {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        RoomCodeQrMatrix(roomCode = roomCode, size = 148.dp)
        Text(
          text = roomCode,
          fontFamily = JetBrainsMonoFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 26.sp,
          color = MaterialTheme.colorScheme.primary
        )
        Text(
          text = inviteUrl,
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (copied) {
          Text(
            text = "✓ Copied room code & invite link!",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
          clipboard?.setPrimaryClip(ClipData.newPlainText("Doodle Dash Invite", inviteUrl))
          copied = true
        }
      ) {
        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Copy Invite")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
private fun CustomColorPickerDialog(
  initialHex: String,
  onSelectHex: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var hexText by remember { mutableStateOf(initialHex.removePrefix("#")) }
  var hue by remember { mutableFloatStateOf(18f) }
  var saturation by remember { mutableFloatStateOf(0.65f) }
  var value by remember { mutableFloatStateOf(0.78f) }

  val hsvColor = remember(hue, saturation, value) {
    Color.hsv(hue, saturation, value)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("HSV & Hex Studio Color Picker") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(hsvColor)
        )
        Text("Hue (${hue.roundToInt()}°)", style = MaterialTheme.typography.labelMedium)
        Slider(
          value = hue,
          onValueChange = {
            hue = it
            hexText = DoodleCanvasEngine.colorToHex(Color.hsv(hue, saturation, value)).removePrefix("#")
          },
          valueRange = 0f..360f
        )
        Text("Saturation (${(saturation * 100).roundToInt()}%)", style = MaterialTheme.typography.labelMedium)
        Slider(
          value = saturation,
          onValueChange = {
            saturation = it
            hexText = DoodleCanvasEngine.colorToHex(Color.hsv(hue, saturation, value)).removePrefix("#")
          },
          valueRange = 0f..1f
        )
        Text("Brightness (${(value * 100).roundToInt()}%)", style = MaterialTheme.typography.labelMedium)
        Slider(
          value = value,
          onValueChange = {
            value = it
            hexText = DoodleCanvasEngine.colorToHex(Color.hsv(hue, saturation, value)).removePrefix("#")
          },
          valueRange = 0.1f..1f
        )
        OutlinedTextField(
          value = hexText,
          onValueChange = { hexText = it.uppercase().take(6) },
          label = { Text("Hex (#RRGGBB)") },
          singleLine = true
        )
      }
    },
    confirmButton = {
      Button(onClick = { onSelectHex("#${hexText.padEnd(6, '0')}") }) {
        Text("Apply Color")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun HostRoomSettingsDialog(
  room: com.example.data.model.GameRoom,
  onSave: (Int, Int, Int, Int, GameMode, String, String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var rounds by remember { mutableIntStateOf(room.maxRounds.toInt()) }
  var drawSec by remember { mutableIntStateOf(room.drawTimeSec.toInt()) }
  var wordChoices by remember { mutableIntStateOf(room.wordChoicesCount.toInt()) }
  var hints by remember { mutableIntStateOf(room.hintsCount.toInt()) }
  var selectedMode by remember {
    mutableStateOf(runCatching { GameMode.valueOf(room.gameMode) }.getOrDefault(GameMode.CLASSIC))
  }
  var difficulty by remember { mutableStateOf(room.difficulty) }
  var category by remember { mutableStateOf(room.wordPackCategory) }
  var customCsv by remember { mutableStateOf(room.customWordsCsv) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Room & Game Mode Settings") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Rounds: $rounds", style = MaterialTheme.typography.labelLarge)
        Slider(value = rounds.toFloat(), onValueChange = { rounds = it.roundToInt() }, valueRange = 2f..10f)

        Text("Draw Time: ${drawSec}s", style = MaterialTheme.typography.labelLarge)
        Slider(value = drawSec.toFloat(), onValueChange = { drawSec = it.roundToInt() }, valueRange = 30f..180f)

        Text("Word Choices: $wordChoices • Letter Hints: $hints", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Slider(
            value = wordChoices.toFloat(),
            onValueChange = { wordChoices = it.roundToInt() },
            valueRange = 1f..5f,
            modifier = Modifier.weight(1f)
          )
          Slider(
            value = hints.toFloat(),
            onValueChange = { hints = it.roundToInt() },
            valueRange = 0f..3f,
            modifier = Modifier.weight(1f)
          )
        }

        Text("Game Mode", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          GameMode.entries.forEach { mode ->
            FilterChip(
              selected = selectedMode == mode,
              onClick = { selectedMode = mode },
              label = { Text(mode.title, fontSize = 11.sp) }
            )
          }
        }

        Text("Word Pack Category", style = MaterialTheme.typography.labelLarge)
        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          BuiltInCatalog.CATEGORIES.forEach { cat ->
            FilterChip(
              selected = category == cat,
              onClick = { category = cat },
              label = { Text(cat, fontSize = 11.sp) }
            )
          }
        }

        Text("Difficulty", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          listOf("MIXED", "EASY", "MEDIUM", "HARD").forEach { diff ->
            FilterChip(
              selected = difficulty == diff,
              onClick = { difficulty = diff },
              label = { Text(diff, fontSize = 11.sp) }
            )
          }
        }

        OutlinedTextField(
          value = customCsv,
          onValueChange = { customCsv = it },
          label = { Text("Optional Custom Words (comma-separated)") },
          modifier = Modifier.fillMaxWidth()
        )
      }
    },
    confirmButton = {
      Button(onClick = {
        onSave(rounds, drawSec, wordChoices, hints, selectedMode, difficulty, category, customCsv)
      }) {
        Text("Save Settings")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    }
  )
}

@Composable
private fun PlayerSocialActionDialog(
  player: RoomPlayer,
  isCurrentUserHost: Boolean,
  isSelf: Boolean,
  onAddFriend: () -> Unit,
  onKickOrVoteKick: () -> Unit,
  onTransferHost: () -> Unit,
  onReport: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var reportReason by remember { mutableStateOf("") }
  var showReportField by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        AvatarView(avatarJson = player.avatarJson, size = 42.dp)
        Column {
          Text(player.displayName, style = MaterialTheme.typography.titleMedium)
          Text("@${player.username} • ${player.score} pts", style = MaterialTheme.typography.bodyMedium)
        }
      }
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!isSelf) {
          Button(onClick = onAddFriend, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Studio Friend")
          }
          if (isCurrentUserHost && !player.isBot) {
            OutlinedButton(onClick = onTransferHost, modifier = Modifier.fillMaxWidth()) {
              Text("Transfer Room Host")
            }
          }
          OutlinedButton(onClick = onKickOrVoteKick, modifier = Modifier.fillMaxWidth()) {
            Text(if (isCurrentUserHost) "Remove from Room" else "Vote-Kick Player (${player.voteKickedBy.size} votes)")
          }
          OutlinedButton(
            onClick = { showReportField = !showReportField },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(Icons.Default.Flag, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Report Player (Saves 20-line Chat Snapshot)")
          }
          if (showReportField) {
            OutlinedTextField(
              value = reportReason,
              onValueChange = { reportReason = it },
              label = { Text("Reason for report") },
              modifier = Modifier.fillMaxWidth()
            )
            Button(
              onClick = { onReport(reportReason.ifBlank { "Unsportsmanlike drawing or chat" }) },
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Submit Report")
            }
          }
        } else {
          Text("This is your active seat in the room!")
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}
