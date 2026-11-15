package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GameRulesEngine
import com.example.data.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DoodleDashRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun saveAndGetProfile_authenticatedOwner_succeedsAndBlocksCrossUser() = runBlocking {
    val aliceUid = signInTestUser("alice_${UUID.randomUUID()}@test.com")
    val repo = DoodleDashRepository(firestore)

    val saveResult = withTimeout(5000L) {
      repo.ensureOrSaveProfile(
        UserProfile(
          userId = aliceUid,
          username = "alice_art",
          displayName = "Alice Botanical"
        )
      )
    }
    assertTrue("Expected profile save to succeed: ${saveResult.exceptionOrNull()}", saveResult.isSuccess)

    // Switch to Bob and verify Bob cannot read Alice's profile
    signInTestUser("bob_${UUID.randomUUID()}@test.com")
    val crossRead = withTimeout(5000L) { repo.getUserProfile(aliceUid) }
    assertTrue("Expected cross-user profile read to fail", crossRead.isFailure)
    val ex = crossRead.exceptionOrNull()?.unwrapFirestoreException()
    assertNotNull(ex)
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex?.code)
  }

  @Test
  fun createPrivateRoom_andAppendCanvasOp_enforcesMemberIsolation() = runBlocking {
    val aliceUid = signInTestUser("alice_room_${UUID.randomUUID()}@test.com")
    val repo = DoodleDashRepository(firestore)
    val code = GameRulesEngine.generateRoomCode(UUID.randomUUID().mostSignificantBits)

    val roomResult = withTimeout(5000L) {
      repo.createRoom(
        roomCode = code,
        hostName = "Alice Host",
        hostAvatarJson = "{}",
        isPublic = false
      )
    }
    assertTrue("Expected private room creation to succeed", roomResult.isSuccess)
    val roomId = roomResult.getOrThrow()

    val opResult = withTimeout(5000L) {
      repo.appendCanvasOp(
        roomId = roomId,
        seq = 1L,
        opType = "STROKE",
        tool = "INK_PEN",
        colorHex = "#C86446",
        sizePx = 10,
        opacity = 100,
        symmetryMode = "NONE",
        seed = 99,
        encodedData = "100,100,128,16",
        memberUids = listOf(aliceUid)
      )
    }
    assertTrue("Expected appendCanvasOp to succeed: ${opResult.exceptionOrNull()}", opResult.isSuccess)

    val ops = withTimeout(4000L) {
      repo.observeCanvasOps(roomId, aliceUid).first { it.isNotEmpty() }
    }
    assertEquals(1, ops.size)

    // Sign in as Bob (not in memberUids) -> reading private room must fail with PERMISSION_DENIED
    signInTestUser("bob_room_${UUID.randomUUID()}@test.com")
    val bobRead = withTimeout(5000L) { repo.getRoomById(roomId) }
    assertTrue("Expected non-member private room read to fail", bobRead.isFailure)
    val firestoreEx = bobRead.exceptionOrNull()?.unwrapFirestoreException()
    assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
  }

  @Test
  fun observePublicRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = DoodleDashRepository(firestore)
    try {
      withTimeout(4000L) {
        repo.observePublicRooms().first()
      }
      fail("Expected unauthenticated query to throw PERMISSION_DENIED")
    } catch (e: Exception) {
      val firestoreException = checkNotNull(e.unwrapFirestoreException()) {
        "Expected nested FirebaseFirestoreException, got $e"
      }
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException.code)
    }
  }

  @Test
  fun gameRulesEngine_scoringAndGuessNormalization_matchesSpec() {
    // NFKD normalization + diacritic stripping + case-insensitive alphanumeric matching
    assertEquals(
      GameRulesEngine.GuessEvaluation.EXACT_MATCH,
      GameRulesEngine.evaluateGuess("  Café-Au-Lait! ", "cafeaulait")
    )
    // Edit distance 1 on 5+ letter words -> CLOSE_MATCH ("So close!")
    assertEquals(
      GameRulesEngine.GuessEvaluation.CLOSE_MATCH,
      GameRulesEngine.evaluateGuess("sunflowr", "sunflower")
    )
    // Guesser score: round(500 * (0.2 + 0.8 * timeLeft / drawTime)) - 25 * (rank - 1), min 50
    assertEquals(500, GameRulesEngine.calculateGuesserScore(80.0, 80.0, 1))
    assertEquals(475, GameRulesEngine.calculateGuesserScore(80.0, 80.0, 2))
    assertEquals(50, GameRulesEngine.calculateGuesserScore(0.0, 80.0, 10))
    // Drawer score: 50 per correct guesser + 100 bonus if all guessed, capped at 400
    assertEquals(250, GameRulesEngine.calculateDrawerScore(3, 3))
    assertEquals(400, GameRulesEngine.calculateDrawerScore(7, 7))
  }
}
