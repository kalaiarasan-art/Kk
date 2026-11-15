package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AvatarConfig
import com.example.data.model.BuiltInCatalog
import com.example.data.model.GameMode
import com.example.ui.components.AvatarView
import com.example.ui.components.StickerBadgeCard
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.DoodleDashViewModel

enum class HubTab(val label: String) {
  PLAY("Quick Play"),
  AVATAR("Avatar & Profile"),
  STICKERS("Awards & Stickers"),
  SOCIAL("Friends & Packs"),
}

@Composable
fun DoodleDashHubScreen(
  viewModel: DoodleDashViewModel,
  onEnterGameArena: () -> Unit,
  onSignedOut: () -> Unit,
) {
  var currentTab by remember { mutableStateOf(HubTab.PLAY) }
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val roomState by viewModel.roomUiState.collectAsStateWithLifecycle()

  Scaffold(
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
      ) {
        NavigationBarItem(
          selected = currentTab == HubTab.PLAY,
          onClick = { currentTab = HubTab.PLAY },
          icon = { Icon(Icons.Default.Brush, contentDescription = "Quick Play") },
          label = { Text("Play") },
          modifier = Modifier.testTag("nav_tab_play")
        )
        NavigationBarItem(
          selected = currentTab == HubTab.AVATAR,
          onClick = { currentTab = HubTab.AVATAR },
          icon = { Icon(Icons.Default.Person, contentDescription = "Avatar & Profile") },
          label = { Text("Profile") },
          modifier = Modifier.testTag("nav_tab_profile")
        )
        NavigationBarItem(
          selected = currentTab == HubTab.STICKERS,
          onClick = { currentTab = HubTab.STICKERS },
          icon = { Icon(Icons.Default.EmojiEvents, contentDescription = "Awards & Stickers") },
          label = { Text("Awards") },
          modifier = Modifier.testTag("nav_tab_awards")
        )
        NavigationBarItem(
          selected = currentTab == HubTab.SOCIAL,
          onClick = { currentTab = HubTab.SOCIAL },
          icon = { Icon(Icons.Default.Group, contentDescription = "Friends & Word Packs") },
          label = { Text("Social") },
          modifier = Modifier.testTag("nav_tab_social")
        )
      }
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
    ) {
      // Top User Banner
      Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            AvatarView(avatarJson = profile.avatarJson, size = 44.dp)
            Column {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = "${profile.countryFlag} ${profile.displayName}",
                  style = MaterialTheme.typography.titleMedium
                )
                Surface(
                  color = MaterialTheme.colorScheme.secondaryContainer,
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = "Lv.${profile.level}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }
              Text(
                text = "${profile.equippedTitle} • ${profile.xp} XP",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Surface(
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = "${profile.wins}W / ${profile.gamesPlayed}G",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onTertiaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      if (roomState.statusBanner != null) {
        Surface(
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = roomState.statusBanner ?: "",
            color = Color.White,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(10.dp)
          )
        }
      }

      when (currentTab) {
        HubTab.PLAY -> PlayHubTabContent(
          viewModel = viewModel,
          onEnterGameArena = onEnterGameArena
        )
        HubTab.AVATAR -> AvatarAndProfileStudioTab(
          viewModel = viewModel,
          onSignedOut = onSignedOut
        )
        HubTab.STICKERS -> AchievementsAndStickersTab(
          viewModel = viewModel
        )
        HubTab.SOCIAL -> FriendsAndWordPacksTab(
          viewModel = viewModel,
          onEnterGameArena = onEnterGameArena
        )
      }
    }
  }
}

@Composable
private fun PlayHubTabContent(
  viewModel: DoodleDashViewModel,
  onEnterGameArena: () -> Unit,
) {
  val publicRooms by viewModel.publicRooms.collectAsStateWithLifecycle()
  val customPacks by viewModel.customWordPacks.collectAsStateWithLifecycle()
  var joinCodeInput by remember { mutableStateOf("") }
  var selectedMode by remember { mutableStateOf(GameMode.CLASSIC) }
  var selectedCategory by remember { mutableStateOf("Botanical & Garden") }
  var isPublicRoom by remember { mutableStateOf(true) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Hero Quick-Launch Card
    item {
      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Sketch, Guess & Bloom",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )
          Text(
            text = "Jump into an instant 4-player match with smart studio bots or host a live room for friends with a 6-character code.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
          )

          // Game Mode Selector Pills
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            GameMode.entries.forEach { mode ->
              FilterChip(
                selected = selectedMode == mode,
                onClick = { selectedMode = mode },
                label = { Text(mode.title) }
              )
            }
          }

          // Word Category Selector Pills
          Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            BuiltInCatalog.CATEGORIES.forEach { cat ->
              FilterChip(
                selected = selectedCategory == cat,
                onClick = { selectedCategory = cat },
                label = { Text(cat, fontSize = 12.sp) }
              )
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = if (isPublicRoom) Icons.Default.Public else Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = if (isPublicRoom) "Public Quick Play Room" else "Private Code-Only Room",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
            }
            Switch(checked = isPublicRoom, onCheckedChange = { isPublicRoom = it })
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = {
                val customWords = customPacks.firstOrNull()?.wordsCsv ?: ""
                viewModel.createAndJoinRoom(
                  isPublic = isPublicRoom,
                  gameMode = selectedMode,
                  wordPackCategory = selectedCategory,
                  customWordsCsv = customWords,
                  botCount = 3,
                  onRoomReady = onEnterGameArena
                )
              },
              modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .testTag("create_room_button"),
              shape = RoundedCornerShape(16.dp),
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
              Icon(Icons.Default.PlayArrow, contentDescription = null)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Create & Play Now", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
              onClick = {
                viewModel.createAndJoinRoom(
                  isPublic = true,
                  gameMode = GameMode.DAILY_CHALLENGE,
                  wordPackCategory = "Botanical & Garden",
                  botCount = 3,
                  onRoomReady = onEnterGameArena
                )
              },
              modifier = Modifier
                .height(52.dp)
                .testTag("daily_challenge_button"),
              shape = RoundedCornerShape(16.dp)
            ) {
              Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Daily")
            }
          }
        }
      }
    }

    // Join by 6-Character Room Code Card
    item {
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
          Text("Join by 6-Character Room Code", style = MaterialTheme.typography.titleMedium)
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            OutlinedTextField(
              value = joinCodeInput,
              onValueChange = { joinCodeInput = it.uppercase().replace(Regex("[^A-Z0-9]"), "").take(6) },
              placeholder = { Text("e.g. DASH42", fontFamily = JetBrainsMonoFontFamily) },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("join_room_code_input"),
              shape = RoundedCornerShape(14.dp)
            )
            Button(
              onClick = {
                viewModel.joinRoomByCode(joinCodeInput, onJoined = onEnterGameArena)
              },
              enabled = joinCodeInput.length == 6,
              modifier = Modifier
                .height(52.dp)
                .testTag("join_room_code_button"),
              shape = RoundedCornerShape(14.dp)
            ) {
              Text("Join Room", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    // Free-Tier Egress Meter & Governor Status (§2.1)
    item {
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Speed,
              contentDescription = "Bandwidth Meter",
              tint = MaterialTheme.colorScheme.secondary
            )
            Column {
              Text(
                text = "Bandwidth Governor • Vector Op-Log Active",
                style = MaterialTheme.typography.labelLarge
              )
              Text(
                text = "Session Egress: ${(viewModel.egressUsedBytes / 1024)} KB / 5 GB Free-Tier Budget (${viewModel.egressUsagePercent}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = "100ms Batch",
              fontFamily = JetBrainsMonoFontFamily,
              fontSize = 10.sp,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }

    // Live Public Quick Play Rooms List
    item {
      Text(
        text = "Quick Play Public Rooms (${publicRooms.size})",
        style = MaterialTheme.typography.titleLarge
      )
    }

    if (publicRooms.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Palette,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(32.dp)
            )
            Text("No open public rooms yet!", style = MaterialTheme.typography.titleMedium)
            Text(
              "Tap 'Create & Play Now' above to launch a public room with instant bot challengers.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    } else {
      items(publicRooms, key = { it.id }) { room ->
        Card(
          shape = RoundedCornerShape(18.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              viewModel.joinRoomByCode(room.roomCode, onJoined = onEnterGameArena)
            }
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Surface(
                  color = MaterialTheme.colorScheme.primaryContainer,
                  shape = RoundedCornerShape(8.dp)
                ) {
                  Text(
                    text = room.roomCode,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                  )
                }
                Text(
                  text = "${room.hostName}'s Studio",
                  style = MaterialTheme.typography.titleMedium
                )
              }
              Text(
                text = "${room.wordPackCategory} • ${room.gameMode} • Round ${room.round}/${room.maxRounds}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Button(
              onClick = { viewModel.joinRoomByCode(room.roomCode, onJoined = onEnterGameArena) },
              shape = RoundedCornerShape(12.dp)
            ) {
              Text("Join")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun AvatarAndProfileStudioTab(
  viewModel: DoodleDashViewModel,
  onSignedOut: () -> Unit,
) {
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var avatarConfig by remember(profile.avatarJson) {
    mutableStateOf(AvatarConfig.fromJson(profile.avatarJson))
  }
  var username by remember(profile.username) { mutableStateOf(profile.username) }
  var displayName by remember(profile.displayName) { mutableStateOf(profile.displayName) }
  var bio by remember(profile.bio) { mutableStateOf(profile.bio) }
  var countryFlag by remember(profile.countryFlag) { mutableStateOf(profile.countryFlag) }
  var showPrivacyDialog by remember { mutableStateOf(false) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Layered Vector Avatar Studio Card (§6)
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Text("Layered Vector Avatar Builder", style = MaterialTheme.typography.headlineMedium)
        AvatarView(avatarJson = avatarConfig.toJson(), size = 112.dp)
        Text(
          text = "Payload size: ${avatarConfig.toJson().length} B (under 300 B budget)",
          fontFamily = JetBrainsMonoFontFamily,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.secondary
        )

        OutlinedButton(
          onClick = { avatarConfig = AvatarConfig.random(System.nanoTime().toInt()) },
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Randomize Botanical Look")
        }

        // Layer Cycling Controls
        AvatarFeatureRow("Face Shape", avatarConfig.faceShape, 4) { avatarConfig = avatarConfig.copy(faceShape = it) }
        AvatarFeatureRow("Skin / Dryad Tone", avatarConfig.skinToneIndex, 8) { avatarConfig = avatarConfig.copy(skinToneIndex = it) }
        AvatarFeatureRow("Eyes", avatarConfig.eyeStyle, 5) { avatarConfig = avatarConfig.copy(eyeStyle = it) }
        AvatarFeatureRow("Eyebrows", avatarConfig.browStyle, 4) { avatarConfig = avatarConfig.copy(browStyle = it) }
        AvatarFeatureRow("Mouth", avatarConfig.mouthStyle, 5) { avatarConfig = avatarConfig.copy(mouthStyle = it) }
        AvatarFeatureRow("Botanical Hat / Hair", avatarConfig.hairHatStyle, 6) { avatarConfig = avatarConfig.copy(hairHatStyle = it) }
        AvatarFeatureRow("Hair / Hat Color", avatarConfig.hairColorIndex, 8) { avatarConfig = avatarConfig.copy(hairColorIndex = it) }
        AvatarFeatureRow("Accessory", avatarConfig.accessoryStyle, 5) { avatarConfig = avatarConfig.copy(accessoryStyle = it) }
        AvatarFeatureRow("Backdrop Swatch", avatarConfig.bgColorIndex, 8) { avatarConfig = avatarConfig.copy(bgColorIndex = it) }
      }
    }

    // Profile Identity & Bio Card
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Text("Profile & Bio", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = countryFlag,
            onValueChange = { countryFlag = it.take(4) },
            label = { Text("Flag") },
            singleLine = true,
            modifier = Modifier.width(84.dp)
          )
          OutlinedTextField(
            value = displayName,
            onValueChange = { displayName = it.take(30) },
            label = { Text("Display Name") },
            singleLine = true,
            modifier = Modifier.weight(1f)
          )
        }
        OutlinedTextField(
          value = username,
          onValueChange = { username = it.lowercase().replace(Regex("[^a-z0-9_]"), "").take(16) },
          label = { Text("Unique Handle (3-16 chars)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
          value = bio,
          onValueChange = { bio = it.take(120) },
          label = { Text("Artist Bio (≤120 chars)") },
          modifier = Modifier.fillMaxWidth()
        )
        Button(
          onClick = {
            viewModel.saveProfileEdits(
              username = username,
              displayName = displayName,
              bio = bio,
              countryFlag = countryFlag,
              themeColor = profile.themeColor,
              equippedTitle = profile.equippedTitle,
              equippedBadge = profile.equippedBadge,
              avatarConfig = avatarConfig
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("save_profile_button"),
          shape = RoundedCornerShape(14.dp)
        ) {
          Text("Save Avatar & Profile", fontWeight = FontWeight.Bold)
        }
      }
    }

    // Lifetime Stats Card (§6)
    Card(
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Text("Studio Progression & Stats", style = MaterialTheme.typography.titleLarge)
        val xpInLevel = (profile.xp % 500L) / 500f
        LinearProgressIndicator(
          progress = { xpInLevel },
          modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
        )
        Text(
          text = "Level ${profile.level} • ${profile.xp} Total XP • Next level in ${500 - (profile.xp % 500)} XP",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          StatPill("Games", "${profile.gamesPlayed}")
          StatPill("Wins", "${profile.wins}")
          StatPill("Points", "${profile.totalPoints}")
          StatPill("Guesses", "${profile.correctGuesses}")
          StatPill("Best Streak", "${profile.bestStreak}")
        }
      }
    }

    // Account, Privacy Policy (13+) & Sign-Out Controls (§9)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      OutlinedButton(
        onClick = { showPrivacyDialog = true },
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Privacy & Terms (13+)")
      }
      OutlinedButton(
        onClick = {
          signOut(
            credentialManager = credentialManager,
            scope = scope,
            onSignOutComplete = onSignedOut
          )
        },
        modifier = Modifier.weight(1f).testTag("sign_out_button"),
        shape = RoundedCornerShape(12.dp)
      ) {
        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Sign Out")
      }
    }

    TextButton(
      onClick = { showDeleteConfirm = true },
      modifier = Modifier.align(Alignment.CenterHorizontally)
    ) {
      Text("Delete Profile & Stats Permanently", color = MaterialTheme.colorScheme.error)
    }
  }

  if (showPrivacyDialog) {
    AlertDialog(
      onDismissRequest = { showPrivacyDialog = false },
      title = { Text("Privacy Policy & Terms (13+)") },
      text = {
        Text(
          "Doodle Dash is designed for players aged 13+ with family-friendly word filtering enabled by default.\n\n" +
            "• Minimal Data: We store only your Google Sign-In UID, chosen display handle, vector avatar config, and aggregate game stats.\n" +
            "• Ephemeral Chat: Room chat is not logged permanently except a 20-line snapshot when a safety report is submitted.\n" +
            "• Account Deletion: You can permanently delete your profile and stats at any time from your Profile tab."
        )
      },
      confirmButton = {
        TextButton(onClick = { showPrivacyDialog = false }) {
          Text("Got It")
        }
      }
    )
  }

  if (showDeleteConfirm) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("Delete Profile & Stats?") },
      text = { Text("This immediately deletes your Firestore profile, avatar, and statistics, then signs you out.") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            viewModel.deleteAccountAndStats {
              signOut(credentialManager, scope, onSignedOut)
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text("Delete Everything")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
private fun AvatarFeatureRow(
  label: String,
  currentValue: Int,
  totalOptions: Int,
  onSelect: (Int) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(140.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
      for (i in 0 until totalOptions) {
        val selected = currentValue.mod(totalOptions) == i
        Surface(
          shape = CircleShape,
          color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .size(28.dp)
            .clickable { onSelect(i) }
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "${i + 1}",
              fontSize = 11.sp,
              color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StatPill(label: String, value: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = value,
      fontFamily = JetBrainsMonoFontFamily,
      fontWeight = FontWeight.Bold,
      fontSize = 16.sp,
      color = MaterialTheme.colorScheme.primary
    )
    Text(
      text = label,
      style = MaterialTheme.typography.bodyMedium,
      fontSize = 11.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AchievementsAndStickersTab(
  viewModel: DoodleDashViewModel,
) {
  val profile by viewModel.userProfile.collectAsStateWithLifecycle()
  val unlockedAchievements = remember(profile.achievementsJson) {
    profile.achievementsJson.split(",").filter { it.isNotBlank() }.toSet()
  }
  val unlockedPacks = remember(profile.unlockedPacksJson) {
    profile.unlockedPacksJson.split(",").filter { it.isNotBlank() }.toSet()
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(
        text = "Sticker Packs (40+ Bundled Vector Stickers)",
        style = MaterialTheme.typography.headlineMedium
      )
      Text(
        text = "Cosmetics are earned through XP & achievements — never bought!",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    items(BuiltInCatalog.STICKER_PACKS, key = { it.id }) { pack ->
      val isUnlocked = pack.id in unlockedPacks || profile.level >= pack.requiredLevel
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
              Text(pack.name, style = MaterialTheme.typography.titleLarge)
              Text(pack.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
              color = if (isUnlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                text = if (isUnlocked) "UNLOCKED" else pack.unlockRequirement,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            pack.stickers.forEach { sticker ->
              StickerBadgeCard(sticker = sticker, compact = true)
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(4.dp))
      Text(
        text = "Studio Achievements (${unlockedAchievements.size}/${BuiltInCatalog.ACHIEVEMENTS.size})",
        style = MaterialTheme.typography.headlineMedium
      )
    }

    items(BuiltInCatalog.ACHIEVEMENTS, key = { it.id }) { ach ->
      val unlocked = ach.id in unlockedAchievements
      Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
          containerColor = if (unlocked) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
          ) {
            Text(text = ach.iconEmoji, fontSize = 28.sp)
            Column {
              Text(
                text = "${ach.name} • Title: '${ach.unlockedTitle}'",
                style = MaterialTheme.typography.titleMedium
              )
              Text(
                text = ach.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Surface(
            color = if (unlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)
          ) {
            Text(
              text = if (unlocked) "✓ +${ach.xpReward} XP" else "+${ach.xpReward} XP",
              style = MaterialTheme.typography.labelMedium,
              color = if (unlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun FriendsAndWordPacksTab(
  viewModel: DoodleDashViewModel,
  onEnterGameArena: () -> Unit,
) {
  val friends by viewModel.friends.collectAsStateWithLifecycle()
  val customPacks by viewModel.customWordPacks.collectAsStateWithLifecycle()

  var friendUsernameInput by remember { mutableStateOf("") }
  var friendDisplayInput by remember { mutableStateOf("") }

  var packNameInput by remember { mutableStateOf("") }
  var packWordsInput by remember { mutableStateOf("monstera, terracotta pot, greenhouse, bonsai, watering can") }
  var packDifficulty by remember { mutableStateOf("MIXED") }
  var packFamilyFriendly by remember { mutableStateOf(true) }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Add Friend Card (§6)
    item {
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
          Text("Studio Friends", style = MaterialTheme.typography.titleLarge)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = friendUsernameInput,
              onValueChange = { friendUsernameInput = it.take(16) },
              label = { Text("Friend @username") },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("add_friend_username_input")
            )
            OutlinedTextField(
              value = friendDisplayInput,
              onValueChange = { friendDisplayInput = it.take(24) },
              label = { Text("Nickname") },
              singleLine = true,
              modifier = Modifier.weight(1f)
            )
          }
          Button(
            onClick = {
              if (friendUsernameInput.isNotBlank()) {
                viewModel.addFriendByUsername(friendUsernameInput, friendDisplayInput)
                friendUsernameInput = ""
                friendDisplayInput = ""
              }
            },
            modifier = Modifier.fillMaxWidth().testTag("add_friend_submit_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Add Friend")
          }
        }
      }
    }

    items(friends, key = { it.id }) { friend ->
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            AvatarView(avatarJson = friend.friendAvatarJson, size = 40.dp)
            Column {
              Text(friend.friendDisplayName, style = MaterialTheme.typography.titleMedium)
              Text(
                text = "@${friend.friendUsername} • Code: ${friend.friendCode}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (friend.roomCode.length == 6) {
              OutlinedButton(
                onClick = { viewModel.joinRoomByCode(friend.roomCode, onJoined = onEnterGameArena) }
              ) {
                Text("Join")
              }
            }
            IconButton(onClick = { viewModel.removeFriend(friend.id) }) {
              Icon(Icons.Default.Delete, contentDescription = "Remove Friend")
            }
          }
        }
      }
    }

    // Custom Word Pack Creator (§4 & §8)
    item {
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
          Text("Create Custom Word Pack (3–500 words)", style = MaterialTheme.typography.titleLarge)
          OutlinedTextField(
            value = packNameInput,
            onValueChange = { packNameInput = it.take(40) },
            label = { Text("Word Pack Title") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("word_pack_name_input")
          )
          OutlinedTextField(
            value = packWordsInput,
            onValueChange = { packWordsInput = it },
            label = { Text("Words (comma-separated, ≤32 chars each)") },
            modifier = Modifier.fillMaxWidth().testTag("word_pack_words_input")
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              listOf("MIXED", "EASY", "MEDIUM", "HARD").forEach { diff ->
                FilterChip(
                  selected = packDifficulty == diff,
                  onClick = { packDifficulty = diff },
                  label = { Text(diff, fontSize = 11.sp) }
                )
              }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("Family-Friendly", style = MaterialTheme.typography.bodyMedium)
              Spacer(modifier = Modifier.width(6.dp))
              Switch(checked = packFamilyFriendly, onCheckedChange = { packFamilyFriendly = it })
            }
          }
          Button(
            onClick = {
              viewModel.saveCustomWordPack(
                name = packNameInput.ifBlank { "My Botanical Pack" },
                difficulty = packDifficulty,
                isFamilyFriendly = packFamilyFriendly,
                rawWordsText = packWordsInput
              )
              packNameInput = ""
            },
            modifier = Modifier.fillMaxWidth().testTag("save_word_pack_button"),
            shape = RoundedCornerShape(12.dp)
          ) {
            Text("Save Custom Word Pack", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    items(customPacks, key = { it.id }) { pack ->
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("${pack.name} (${pack.wordCount} words • ${pack.difficulty})", style = MaterialTheme.typography.titleMedium)
            Text(
              text = pack.wordsCsv,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 2
            )
          }
          IconButton(onClick = { viewModel.deleteCustomWordPack(pack.id) }) {
            Icon(Icons.Default.Delete, contentDescription = "Delete Word Pack")
          }
        }
      }
    }
  }
}
