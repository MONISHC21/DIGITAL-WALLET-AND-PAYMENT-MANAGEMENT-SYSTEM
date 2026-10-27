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

test("Unauthenticated user: cannot read user profile or transactions", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("transactions").get());
});

test("Authenticated user: can create and read own profile, cannot read another user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Monish",
      email: "monish@gmail.com",
      upiHandle: "monish@digiwallet",
      demoBalancePaise: 1245000,
      currency: "INR",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Authenticated user: rejects ghost fields and negative balance on profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Monish",
      email: "monish@gmail.com",
      upiHandle: "monish@digiwallet",
      demoBalancePaise: -100,
      currency: "INR",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Monish",
      email: "monish@gmail.com",
      upiHandle: "monish@digiwallet",
      demoBalancePaise: 1000,
      currency: "INR",
      isAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Authenticated user: can create and query own transactions, expenses, and budgets", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      ownerId: ALICE_UID,
      amountPaise: 45000,
      currency: "INR",
      transactionType: "DEBIT",
      category: "Food & Dining",
      title: "Grocery Store",
      counterparty: "store@upi",
      description: "Monthly grocery shopping",
      paymentMethod: "UPI",
      status: "COMPLETED",
      referenceId: "TXN123456789",
      isDemo: true,
      timestampMillis: 1730000000000,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("transactions")
      .where("ownerId", "==", ALICE_UID)
      .get()
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("expenses").doc("exp_1").set({
      ownerId: ALICE_UID,
      amountPaise: 25000,
      category: "Food & Dining",
      merchant: "Cafe Coffee",
      note: "Lunch with friends",
      dateMillis: 1730000000000,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("budgets").doc("bud_1").set({
      ownerId: ALICE_UID,
      category: "Food & Dining",
      limitPaise: 500000,
      monthYear: "2026-10",
      createdAt: now,
      updatedAt: now,
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").get()
  );
});
