package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private data class WarmupStroke(
  val points: List<Offset>,
  val color: Color,
  val width: Float,
)

/**
 * Silent Auto-Sign-In on App Startup using GetGoogleIdOption in a single-option request.
 */
fun attemptAutoSignIn(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onUnauthenticated: () -> Unit,
  scope: CoroutineScope,
) {
  if (Firebase.auth.currentUser != null) {
    onAuthSuccess()
    return
  }
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onUnauthenticated()
    return
  }

  val googleIdOption = GetGoogleIdOption.Builder()
    .setFilterByAuthorizedAccounts(true)
    .setServerClientId(clientId)
    .setAutoSelectEnabled(true)
    .build()

  val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onUnauthenticated()
      }
    } catch (e: Exception) {
      onUnauthenticated()
    }
  }
}

/**
 * Interactive Google Sign-In using GetSignInWithGoogleOption in a single-option request.
 */
fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {},
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e("Auth", "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signOut(
  credentialManager: CredentialManager,
  scope: CoroutineScope,
  onSignOutComplete: () -> Unit = {},
) {
  scope.launch {
    withContext(NonCancellable) {
      try {
        credentialManager.clearCredentialState(ClearCredentialStateRequest())
      } catch (e: Exception) {
        Log.w("Auth", "Failed to clear credential state: ${e.message}")
      } finally {
        Firebase.auth.signOut()
        onSignOutComplete()
      }
    }
  }
}

/**
 * Designed Cold-Start & Google Sign-In Screen (§2.3) featuring an interactive Warm-Up Doodle Pad.
 */
@Composable
fun AuthAndColdStartScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  // Warm-up doodle pad state (§2.3)
  val warmupColors = listOf(
    Color(0xFFC86446), // Terracotta
    Color(0xFF5F7A61), // Sage
    Color(0xFFDDA15E), // Ochre
    Color(0xFF3B6E4C), // Moss
    Color(0xFF26231F), // Espresso Ink
  )
  var selectedWarmupColor by remember { mutableStateOf(warmupColors[0]) }
  val strokes = remember { mutableStateListOf<WarmupStroke>() }
  val activePoints = remember { mutableStateListOf<Offset>() }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = onAuthSuccess,
      onUnauthenticated = {},
      scope = scope
    )
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
      // Studio Badge
      Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = "Botanical Studio",
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
          )
          Text(
            text = "REAL-TIME MULTIPLAYER SKETCH STUDIO",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
      }

      Text(
        text = "Doodle Dash",
        style = MaterialTheme.typography.displayLarge,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
      )

      Text(
        text = "Draw on a 1600×1000 botanical canvas, race to guess secret words, trade animated stickers, and climb the studio podium.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 460.dp)
      )

      // Interactive Cold-Start Warm-Up Doodle Pad (§2.3)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Brush,
                contentDescription = "Warm-up Doodle Pad",
                tint = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Warm-Up Doodle Pad",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
              )
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              warmupColors.forEach { swatch ->
                val isSelected = swatch == selectedWarmupColor
                Box(
                  modifier = Modifier
                    .size(if (isSelected) 28.dp else 22.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(
                      width = if (isSelected) 2.5.dp else 1.dp,
                      color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                      shape = CircleShape
                    )
                    .clickable { selectedWarmupColor = swatch }
                )
              }
              IconButton(
                onClick = {
                  strokes.clear()
                  activePoints.clear()
                },
                modifier = Modifier.testTag("clear_warmup_canvas_button")
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = "Clear Warm-Up Pad",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .height(210.dp)
              .clip(RoundedCornerShape(16.dp))
              .background(Color(0xFFF7F3EB))
              .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
              .pointerInput(selectedWarmupColor) {
                detectDragGestures(
                  onDragStart = { offset ->
                    activePoints.clear()
                    activePoints.add(offset)
                  },
                  onDrag = { change, _ ->
                    change.consume()
                    activePoints.add(change.position)
                  },
                  onDragEnd = {
                    if (activePoints.isNotEmpty()) {
                      strokes.add(
                        WarmupStroke(
                          points = activePoints.toList(),
                          color = selectedWarmupColor,
                          width = 10f
                        )
                      )
                      activePoints.clear()
                    }
                  }
                )
              }
          ) {
            if (strokes.isEmpty() && activePoints.isEmpty()) {
              Text(
                text = "Sketch here with your finger while connecting! 🌿✏️",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF635C51),
                modifier = Modifier.align(Alignment.Center)
              )
            }

            Canvas(modifier = Modifier.fillMaxSize()) {
              fun drawSmoothPoints(pts: List<Offset>, color: Color, strokeWidth: Float) {
                if (pts.isEmpty()) return
                if (pts.size == 1) {
                  drawCircle(color, radius = strokeWidth / 2f, center = pts[0])
                  return
                }
                val path = Path().apply {
                  moveTo(pts[0].x, pts[0].y)
                  for (i in 1 until pts.size) {
                    val prev = pts[i - 1]
                    val curr = pts[i]
                    quadraticTo(prev.x, prev.y, (prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
                  }
                  lineTo(pts.last().x, pts.last().y)
                }
                drawPath(
                  path = path,
                  color = color,
                  style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
              }

              strokes.forEach { s ->
                drawSmoothPoints(s.points, s.color, s.width)
              }
              if (activePoints.isNotEmpty()) {
                drawSmoothPoints(activePoints, selectedWarmupColor, 10f)
              }
            }
          }
        }
      }

      // Google Sign-In Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          Text(
            text = "Sign in to sync your Avatar, Stickers, Achievements & Live Rooms",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
          )

          if (errorMessage != null) {
            Surface(
              color = MaterialTheme.colorScheme.errorContainer,
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = errorMessage ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center
              )
            }
          }

          Button(
            onClick = {
              isLoading = true
              errorMessage = null
              onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                  isLoading = false
                  onAuthSuccess()
                },
                onAuthError = { msg ->
                  isLoading = false
                  errorMessage = msg
                },
                scope = scope,
                onAuthCancelled = {
                  isLoading = false
                }
              )
            },
            enabled = !isLoading,
            modifier = Modifier
              .fillMaxWidth()
              .height(54.dp)
              .testTag("google_sign_in_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.5.dp
              )
            } else {
              Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
  }
}
