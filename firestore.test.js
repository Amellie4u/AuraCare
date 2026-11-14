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

function pastTimestamp() {
  return new Date(Date.now() - 5000);
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

test("Unauthenticated user cannot read or write patient profile or seizures", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("seizures").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("seizures").doc("s1").set({
      seizureType: "Focal Aware",
      durationSeconds: 45,
      occurredAtMillis: 1700000000000,
      triggers: ["Stress"],
      medicationSideEffects: [],
      severity: 2,
      emergencyNotified: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});

test("Authenticated user can create, read, and update own PatientProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      fullName: "Alice Morgan",
      epilepsyType: "Focal Temporal",
      dietaryPreference: "Ketogenic",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(profileRef.get());
  await assertSucceeds(
    profileRef.update({
      primaryNeurologist: "Dr. Chen",
      updatedAt: pastTimestamp(),
    })
  );
});

test("Cross-user access is denied across profile, seizures, meds, and medical records", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore();
    await adminDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      fullName: "Bob Smith",
      dietaryPreference: "Balanced",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    });
    await adminDb.collection("users").doc(BOB_UID).collection("seizures").doc("bob_s1").set({
      seizureType: "Absence",
      durationSeconds: 15,
      occurredAtMillis: 1700000000000,
      triggers: [],
      medicationSideEffects: [],
      severity: 1,
      emergencyNotified: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    });
    await adminDb.collection("users").doc(BOB_UID).collection("medicalRecords").doc("bob_r1").set({
      title: "EEG Oct 2026",
      recordType: "EEG",
      fileSizeBytes: 1024,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("users").doc(BOB_UID).get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("seizures").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("medicalRecords").doc("bob_r1").get());
  await assertFails(aliceDb.collection("users").doc(BOB_UID).collection("seizures").doc("bob_s1").delete());
});

test("Owner can CRUD seizures, medications, doseLogs, dailyLogs, appointments, emergencyContacts, and medicalRecords", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(
    userDoc.collection("seizures").doc("s_1").set({
      seizureType: "Focal Impaired",
      durationSeconds: 90,
      occurredAtMillis: 1700000000000,
      triggers: ["Sleep Deprivation", "Stress"],
      symptomsAndAura: "Rising epigastric sensation",
      medicationSideEffects: ["Drowsiness"],
      postIctalNotes: "Fatigue for 20 min",
      severity: 3,
      emergencyNotified: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc.collection("medications").doc("m_1").set({
      name: "Levetiracetam",
      dosage: "500 mg",
      frequency: "Twice Daily",
      scheduleTimes: ["08:00", "20:00"],
      status: "current",
      prescribedBy: "Dr. Patel",
      startDate: "2026-01-10",
      sideEffectsNoted: ["Mild fatigue"],
      reminderEnabled: true,
      notes: "Take with food",
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc.collection("doseLogs").doc("d_1").set({
      medicationId: "m_1",
      medicationName: "Levetiracetam",
      dosage: "500 mg",
      scheduledTime: "08:00",
      dateKey: "2026-11-13",
      taken: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc.collection("dailyLogs").doc("dl_1").set({
      entryType: "food",
      title: "Salmon & Avocado Bowl",
      category: "Lunch",
      mealTime: "12:30",
      durationMinutes: 0,
      caloriesOrAmount: "480 kcal",
      dietTags: ["Ketogenic", "High-Omega3"],
      notes: "Felt energized",
      loggedAtMillis: 1700000000000,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc.collection("appointments").doc("a_1").set({
      title: "Neurology Review",
      doctorName: "Dr. Patel",
      clinicLocation: "Neurology Clinic Rm 402",
      providerEmail: "dr.patel@clinic.org",
      appointmentMillis: 1700500000000,
      notes: "Bring PDF summary",
      linkedToCalendar: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc.collection("emergencyContacts").doc("ec_1").set({
      name: "Sam Morgan",
      relationship: "Spouse",
      phone: "555-0199",
      email: "sam@example.com",
      isPrimary: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc.collection("medicalRecords").doc("mr_1").set({
      title: "Routine EEG",
      recordType: "EEG",
      recordDate: "2026-10-15",
      facilityOrDoctor: "Bay Area Neuro Lab",
      findingsSummary: "Mild focal slowing left temporal",
      fileName: "eeg_oct.pdf",
      mimeType: "application/pdf",
      localFileUri: "/data/user/0/app/files/eeg_oct.pdf",
      fileSizeBytes: 24500,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(userDoc.collection("seizures").get());
  await assertSucceeds(userDoc.collection("medications").get());
  await assertSucceeds(userDoc.collection("medicalRecords").get());
});

test("Dirty Dozen: rejects ghost fields, invalid enum status, negative duration, and id poisoning", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);

  // Ghost field rejection
  await assertFails(
    userDoc.collection("seizures").doc("s_bad1").set({
      seizureType: "Absence",
      durationSeconds: 30,
      occurredAtMillis: 1700000000000,
      triggers: [],
      medicationSideEffects: [],
      severity: 2,
      emergencyNotified: false,
      isVerified: true,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Invalid medication status enum
  await assertFails(
    userDoc.collection("medications").doc("m_bad1").set({
      name: "Lamotrigine",
      dosage: "100 mg",
      frequency: "Daily",
      scheduleTimes: ["09:00"],
      status: "archived",
      sideEffectsNoted: [],
      reminderEnabled: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Negative duration rejection
  await assertFails(
    userDoc.collection("seizures").doc("s_bad2").set({
      seizureType: "Absence",
      durationSeconds: -5,
      occurredAtMillis: 1700000000000,
      triggers: [],
      medicationSideEffects: [],
      severity: 2,
      emergencyNotified: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );

  // Invalid doc ID characters
  await assertFails(
    userDoc.collection("emergencyContacts").doc("bad id with spaces!").set({
      name: "Test",
      isPrimary: false,
      createdAt: pastTimestamp(),
      updatedAt: pastTimestamp(),
    })
  );
});
