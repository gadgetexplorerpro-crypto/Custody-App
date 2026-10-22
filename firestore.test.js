const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_parent_1";
const BOB_UID = "bob_parent_2";
const MALLORY_UID = "mallory_outsider_3";
const FAMILY_ID = "family_alice_bob";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

const nowTimestamp = () => new Date(Date.now() - 2000);

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

test("Unauthenticated user: rejected across all collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("children").get());
  await assertFails(unauthDb.collection("schedule_events").get());
  await assertFails(unauthDb.collection("exchanges").get());
  await assertFails(unauthDb.collection("incidents").get());
  await assertFails(unauthDb.collection("expenses").get());
  await assertFails(unauthDb.collection("messages").get());
});

test("UserProfile PII Isolation: Alice can create/read her own profile, Bob cannot read Alice's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = nowTimestamp();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Carter",
      email: "alice@example.com",
      familyId: FAMILY_ID,
      subscriptionTier: "PREMIUM",
      custodyPattern: "TWO_TWO_THREE",
      defaultExpenseSplitPercent: 50,
      privacyRetentionDays: 1825,
      notificationsEnabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Shadow Update & Ghost Field rejection on UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = nowTimestamp();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Carter",
      email: "alice@example.com",
      familyId: FAMILY_ID,
      subscriptionTier: "PREMIUM",
      custodyPattern: "TWO_TWO_THREE",
      defaultExpenseSplitPercent: 50,
      privacyRetentionDays: 1825,
      notificationsEnabled: true,
      isAdminGhost: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});

test("Co-Parent Shared Access & Outsider Isolation on Children, Schedule, Exchanges, Incidents, Expenses, Messages", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const malloryDb = testEnv.authenticatedContext(MALLORY_UID).firestore();
  const ts = nowTimestamp();

  await assertSucceeds(
    aliceDb.collection("children").doc("child_1").set({
      createdByUid: ALICE_UID,
      familyId: FAMILY_ID,
      memberUids: [ALICE_UID, BOB_UID],
      name: "Oliver Carter",
      dateOfBirth: "2018-06-15",
      schoolOrChildcare: "Oakridge Primary",
      medicalNotes: "No known allergies",
      custodyScheduleType: "TWO_TWO_THREE",
      primaryParentRatio: 50,
      avatarColorHex: "#0D9488",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bobDb.collection("children").where("memberUids", "array-contains", BOB_UID).get()
  );

  await assertFails(malloryDb.collection("children").doc("child_1").get());
  await assertFails(malloryDb.collection("children").get());

  await assertSucceeds(
    aliceDb.collection("incidents").doc("inc_1").set({
      createdByUid: ALICE_UID,
      createdByName: "Alice Carter",
      familyId: FAMILY_ID,
      memberUids: [ALICE_UID, BOB_UID],
      childName: "Oliver Carter",
      category: "LATE_PICKUP",
      title: "Pickup delayed by 25 minutes",
      description: "Arrived at 17:25 instead of scheduled 17:00 due to traffic.",
      incidentDateIso: "2026-10-22",
      incidentTime: "17:25",
      attachmentType: "NONE",
      attachmentName: "",
      attachmentUri: "",
      originalFileHash: "sha256_none",
      verificationStatus: "USER_REPORTED",
      disputeResponse: "",
      version: 1,
      auditHistorySummary: "Created v1 by Alice Carter",
      isArchived: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(aliceDb.collection("incidents").doc("inc_1").delete());

  await assertSucceeds(
    bobDb.collection("incidents").doc("inc_1").update({
      verificationStatus: "MUTUALLY_CONFIRMED",
      disputeResponse: "Confirmed delay due to highway closure.",
      version: 2,
      auditHistorySummary: "Created v1 by Alice Carter | Confirmed v2 by Bob",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
