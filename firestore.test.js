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

function validProfile(uid) {
  const now = new Date();
  return {
    userId: uid,
    displayName: "Alice Quant",
    email: "alice@example.com",
    role: "trader",
    accountBalance: 100000,
    riskPerTradePct: 1.0,
    dailyLossLimitPct: 3.0,
    weeklyLossLimitPct: 6.0,
    maxDrawdownPct: 12.0,
    liveTradingEnabled: false,
    preferredSymbol: "BTCUSDT",
    preferredTimeframe: "H1",
    createdAt: now,
    updatedAt: now,
  };
}

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

test("Unauthenticated user cannot read or list user profiles or setups", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("setups").get());
});

test("Authenticated user can create and update their own profile with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      riskPerTradePct: 0.75,
      updatedAt: new Date(),
    })
  );
});

test("Cross-user access: Bob cannot read or write Alice's profile or indicator configs", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile(ALICE_UID)));

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("setups").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ riskPerTradePct: 5.0, updatedAt: new Date() }));
});

test("Shadow update & privilege escalation: rejects ghost fields and role self-escalation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const payload = validProfile(ALICE_UID);
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set({ ...payload, isAdmin: true }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set({ ...payload, role: "admin" }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).set({ ...payload, liveTradingEnabled: true }));
});

test("Approval request terminal state locking and AuditLog append-only immutability", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const reqRef = aliceDb.collection("users").doc(ALICE_UID).collection("approvalRequests").doc("req_1");
  const now = new Date();
  await assertSucceeds(
    reqRef.set({
      userId: ALICE_UID,
      actionType: "DEPLOY_MODEL_VERSION",
      targetId: "ind_1",
      summary: "Promote Adaptive Trend DNA v2.1",
      status: "PENDING",
      requestedBy: "Alice Quant",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(reqRef.update({ status: "APPROVED", updatedAt: new Date() }));
  // Terminal state locked: cannot mutate after APPROVED
  await assertFails(reqRef.update({ status: "REJECTED", updatedAt: new Date() }));

  const logRef = aliceDb.collection("users").doc(ALICE_UID).collection("auditLogs").doc("log_1");
  await assertSucceeds(
    logRef.set({
      userId: ALICE_UID,
      action: "APPROVAL_GRANTED",
      actor: "Alice Quant",
      details: "Approved model promotion",
      severity: "INFO",
      createdAt: new Date(),
    })
  );
  await assertFails(logRef.update({ details: "tampered" }));
  await assertFails(logRef.delete());
});

test("Indicator Forge projects are strictly user-isolated and download logs are append-only", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const projRef = aliceDb.collection("users").doc(ALICE_UID).collection("indicatorProjects").doc("proj_1");
  await assertSucceeds(
    projRef.set({
      userId: ALICE_UID,
      name: "XAUUSD Liquidity Sniper",
      description: "M15/H1 sweep with Entry, SL, TP1-TP3",
      category: "Smart Money",
      platform: "DUAL",
      complexity: "PRO",
      devStatus: "VALIDATED",
      currentVersion: "1.0.0",
      visibility: "PRIVATE",
      isFavorite: true,
      mtfTimeframes: ["H1", "M15", "M5"],
      setupPattern: "Liquidity Sweep",
      closedCandleOnly: true,
      entryColorHex: "#D4AF37",
      slColorHex: "#EF4444",
      tpColorHex: "#10B981",
      equivalenceNotes: "Uses closed bar confirmation on both MT5 and Pine v6.",
      downloadCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("indicatorProjects").doc("proj_1").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("indicatorProjects").doc("proj_1").update({ name: "Stolen", updatedAt: new Date() }));

  // Append-only download record
  const dlRef = aliceDb.collection("users").doc(ALICE_UID).collection("indicatorDownloads").doc("dl_1");
  await assertSucceeds(
    dlRef.set({
      userId: ALICE_UID,
      projectId: "proj_1",
      projectName: "XAUUSD Liquidity Sniper",
      version: "1.0.0",
      platform: "MT5",
      fileType: "MQ5",
      fileName: "JAX_XAUUSD_Sniper.mq5",
      byteSize: 4096,
      createdAt: now,
    })
  );
  await assertFails(dlRef.update({ byteSize: 9999 }));
  await assertFails(dlRef.delete());
});

test("Marketplace listings require admin privilege to approve or reject", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com" }).firestore();
  const adminDb = testEnv.authenticatedContext("admin_uid", { email: "impuriumai@gmail.com" }).firestore();
  const now = new Date();

  const listingRef = aliceDb.collection("marketplaceListings").doc("list_1");
  // Normal user cannot directly publish APPROVED
  await assertFails(
    listingRef.set({
      creatorUid: ALICE_UID,
      creatorName: "Alice Quant",
      projectId: "proj_1",
      name: "BOS & FVG Matrix",
      description: "Validates FVG mitigation.",
      category: "Smart Money",
      platform: "TRADINGVIEW",
      priceUsd: 0.0,
      listingStatus: "APPROVED",
      version: "1.0.0",
      mql5Source: "",
      pineSource: "//@version=6\nindicator('Test')",
      releaseNotes: "Initial release",
      downloadCount: 0,
      avgRating: 0.0,
      reviewCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Normal user submits PENDING_REVIEW
  await assertSucceeds(
    listingRef.set({
      creatorUid: ALICE_UID,
      creatorName: "Alice Quant",
      projectId: "proj_1",
      name: "BOS & FVG Matrix",
      description: "Validates FVG mitigation.",
      category: "Smart Money",
      platform: "TRADINGVIEW",
      priceUsd: 0.0,
      listingStatus: "PENDING_REVIEW",
      version: "1.0.0",
      mql5Source: "",
      pineSource: "//@version=6\nindicator('Test')",
      releaseNotes: "Initial release",
      downloadCount: 0,
      avgRating: 0.0,
      reviewCount: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Non-admin creator cannot approve own listing
  await assertFails(listingRef.update({ listingStatus: "APPROVED", updatedAt: new Date() }));

  // Admin can approve
  await assertSucceeds(
    adminDb.collection("marketplaceListings").doc("list_1").update({ listingStatus: "APPROVED", updatedAt: new Date() })
  );
});

