package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.repository.DoodleDashRepository
import com.example.ui.screens.AuthAndColdStartScreen
import com.example.ui.screens.DoodleDashHubScreen
import com.example.ui.screens.GameArenaScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.DoodleDashViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        AppRootContent()
      }
    }
  }
}

@Composable
fun AppRootContent() {
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose {
      Firebase.auth.removeAuthStateListener(listener)
    }
  }

  val user = currentUser
  if (user == null) {
    AuthAndColdStartScreen(
      onAuthSuccess = {
        currentUser = Firebase.auth.currentUser
      }
    )
  } else {
    val uid = user.uid
    val displayName = user.displayName ?: "Sage Sketcher"
    key(uid) {
      AuthenticatedDoodleDashApp(
        currentUserId = uid,
        initialDisplayName = displayName,
        onSignedOut = {
          currentUser = null
        }
      )
    }
  }
}

@Composable
private fun AuthenticatedDoodleDashApp(
  currentUserId: String,
  initialDisplayName: String,
  onSignedOut: () -> Unit,
) {
  val viewModel: DoodleDashViewModel = viewModel(
    key = currentUserId,
    factory = viewModelFactory {
      initializer {
        val app = checkNotNull(this[APPLICATION_KEY]) {
          "APPLICATION_KEY missing from CreationExtras"
        }
        val databaseId = app.getString(R.string.firestore_database_id)
        val db = FirebaseFirestore.getInstance(databaseId)
        DoodleDashViewModel(
          repository = DoodleDashRepository(db),
          currentUserId = currentUserId,
          initialDisplayName = initialDisplayName
        )
      }
    }
  )

  val roomState by viewModel.roomUiState.collectAsStateWithLifecycle()
  var inArena by remember { mutableStateOf(false) }

  if (inArena && roomState.room != null) {
    GameArenaScreen(
      viewModel = viewModel,
      onLeaveRoom = { inArena = false }
    )
  } else {
    DoodleDashHubScreen(
      viewModel = viewModel,
      onEnterGameArena = { inArena = true },
      onSignedOut = onSignedOut
    )
  }
}
