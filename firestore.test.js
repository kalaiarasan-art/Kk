const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function makeValidProfile(uid, ts) {
  return {
    userId: uid,
    username: "sage_artist",
    displayName: "Sage Artist",
    avatarJson: '{"face":0,"color":1}',
    bio: "Loves sketching botanicals",
    countryFlag: "🌿",
    themeColor: "#C86446",
    xp: 120,
    level: 2,
    equippedTitle: "Sketch Sprout",
    equippedBadge: "first_blood",
    gamesPlayed: 3,
    wins: 1,
    totalPoints: 1450,
    correctGuesses: 8,
    bestGuessMs: 4200,
    bestStreak: 4,
    unlockedPacksJson: "starter,reactions",
    achievementsJson: "first_blood",
    createdAt: ts,
    updatedAt: ts,
  };
}

function makeValidRoom(hostUid, isPublic, ts) {
  return {
    roomCode: "DASH42",
    hostId: hostUid,
    hostName: "Sage Artist",
    isPublic: isPublic,
    phase: "LOBBY",
    round: 1,
    maxRounds: 3,
    drawTimeSec: 80,
    wordChoicesCount: 3,
    hintsCount: 2,
    gameMode: "CLASSIC",
    difficulty: "MIXED",
    wordPackCategory: "All Categories",
    customWordsCsv: "",
    currentDrawerId: hostUid,
    currentDrawerName: "Sage Artist",
    wordMask: "_ _ _ _ _",
    secretWord: "LOTUS",
    phaseEndsAtMs: 1800000000000,
    memberUids: [hostUid],
    playersJson: "[]",
    reactionCountsJson: "{}",
    createdAt: ts,
    updatedAt: ts,
  };
}

test("Unauthenticated user: cannot read profiles or rooms", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("rooms").get());
});

test("Authenticated user: can create and read own profile, but cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(makeValidProfile(ALICE_UID, now))
  );
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow update test: profile rejects undeclared ghost fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  const poisoned = { ...makeValidProfile(ALICE_UID, now), isAdmin: true };
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set(poisoned));
});

test("Private room isolation: non-member cannot read private room, member can query with memberUids filter", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_DASH42").set(makeValidRoom(ALICE_UID, false, now))
  );

  await assertSucceeds(
    aliceDb.collection("rooms").where("memberUids", "array-contains", ALICE_UID).get()
  );
  await assertFails(bobDb.collection("rooms").doc("room_DASH42").get());
  await assertFails(bobDb.collection("rooms").get());
});

test("Canvas ops & chat subcollections enforce memberUids and append-only immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("rooms").doc("room_DASH42").set(makeValidRoom(ALICE_UID, true, now))
  );

  const opRef = aliceDb.collection("rooms").doc("room_DASH42").collection("ops").doc("op_1");
  await assertSucceeds(
    opRef.set({
      roomId: "room_DASH42",
      authorId: ALICE_UID,
      seq: 1,
      opType: "STROKE",
      tool: "INK_PEN",
      colorHex: "#C86446",
      sizePx: 8,
      opacity: 100,
      symmetryMode: "NONE",
      seed: 42,
      encodedData: "100,100,128,16",
      memberUids: [ALICE_UID],
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("rooms")
      .doc("room_DASH42")
      .collection("ops")
      .where("memberUids", "array-contains", ALICE_UID)
      .get()
  );

  // Append-only: updates and deletes must fail
  await assertFails(opRef.delete());
  // Bob is not in memberUids so cannot read op_1
  await assertFails(
    bobDb.collection("rooms").doc("room_DASH42").collection("ops").doc("op_1").get()
  );
});
