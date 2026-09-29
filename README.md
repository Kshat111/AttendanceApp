# SmartAttendance — Android Hiring Assignment

A simple Android attendance app with two user roles (Admin and Staff), built around
selfie-based face recognition. Staff can only mark attendance if their live selfie
matches their previously enrolled face.

---

## Tech Stack & Architecture

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Architecture | MVVM (View → ViewModel → Repository → Room) |
| Navigation | Navigation Compose |
| Local database | Room (SQLite) |
| Session storage | DataStore (Preferences) |
| Camera | CameraX (front camera, live preview + capture) |
| Face detection | ML Kit Face Detection (on-device) |
| Face recognition | MobileFaceNet (TensorFlow Lite, on-device, 192-dim embeddings) |
| Location | FusedLocationProviderClient (Google Play Services) |
| Image loading | Coil |
| Dependency injection | None — manual DI via a single `AppContainer` created in a custom `Application` class |

### Why no DI framework (Hilt/Koin)?
The app is small enough that a hand-rolled `AppContainer` (created once in
`AttendanceApplication.onCreate()` and exposed to ViewModels via `Application`
context) keeps the dependency graph easy to read end-to-end in one file, without
the setup and build-time overhead of a DI framework. `AttendanceDatabase` is
instantiated as a thread-safe singleton (double-checked locking) so only one
Room instance ever exists.

### How face recognition works
1. **Detection** — ML Kit finds the face in a captured photo and returns a
   bounding box + head-pose angles (yaw/pitch/roll). Photos are rejected if
   there is no face, more than one face, the face is too small in frame, or
   the head is turned/tilted more than ~20°.
2. **Cropping** — the detected face region (with ~15% padding) is cropped from
   the EXIF-corrected, upright bitmap.
3. **Embedding** — the cropped face is resized/normalized to what the
   MobileFaceNet TFLite model expects and run through it to produce a 192‑dim,
   L2‑normalized embedding vector.
4. **Enrolment** — admin captures 3 selfies per staff member. Each is
   validated individually, then all 3 embeddings are pairwise-compared
   (cosine similarity) to catch a mismatched capture (e.g. two different
   people, or a bad photo) before saving. All 3 are stored per staff member.
5. **Matching** — when staff mark attendance, a new embedding is computed from
   their live selfie and compared (cosine similarity) against all 3 stored
   embeddings. The **highest** of the 3 scores is used. If it's at or above
   the threshold, attendance is recorded; otherwise it's rejected with the
   actual similarity shown.

**Face match threshold:** `0.65` (cosine similarity)
**Enrolment consistency threshold:** `0.60` (looser — used only to catch
grossly inconsistent enrolment photos, not to gate attendance)

These were tuned by manually testing same-person and different-person pairs
on-device using an in-app embedding/similarity test screen built specifically
for this purpose. Measured cosine similarity scores (as a percentage):

| Comparison | Score |
|---|---|
| Same person (different photos) | **92.12%** |
| Different, unrelated people | **44.64%** |
| Twin siblings (hardest case tested) | **58.55%** |

Even the twin-sibling comparison, the most visually similar pair available
for testing, stayed comfortably below the 65% threshold, while the
same-person score sits well above it. This gap gave confidence in 65% as a
safe cutoff without being so strict that ordinary lighting/angle variation
in genuine same-person attempts would cause false rejections. See
"Assumptions & Limitations" below for caveats on the size of this test (a
handful of manual comparisons, not a formal benchmark).

### Data model
- **Staff**: `id` (auto-generated PK), `name`, `employeeId` (unique, editable),
  `faceEmbeddings` (list of 3 float arrays, stored as JSON via a Room
  TypeConverter).
- **Attendance**: `id` (auto-generated PK), `staffId` (foreign key →
  `Staff.id`, **not** `employeeId`), `timestamp`, `selfiePath`, `latitude`,
  `longitude`.

**Why `Attendance.staffId` references the internal `Staff.id` and not
`employeeId`:** `employeeId` is a user-editable, human-facing string (admin
can correct typos or re-issue IDs). If attendance rows referenced it directly,
editing an employee ID would either orphan history or require rewriting every
attendance row. The internal auto-generated `id` never changes, so it's the
stable key for the relationship — and it enables a proper `CASCADE` delete
(deleting a staff member also removes their attendance history).

---

## How to Run

### Prerequisites
- Android Studio (recent stable version)
- An Android device or emulator running **Android 8.0 (API 26)** or higher
  (a physical device is strongly recommended, since camera/location testing
  on an emulator is limited)

### Steps
1. Clone the repository:
   ```
   git clone <your-repo-url>
   ```
2. Open the project in Android Studio and let Gradle sync (dependencies are
   managed via the version catalog in `gradle/libs.versions.toml`).
3. Connect a device (enable **USB debugging** in Developer Options, or use
   **Wireless debugging**) or start an emulator.
4. Click **Run ▶**. On first launch you'll be prompted for camera and
   location permissions when you first use those features.
5. Log in with the demo credentials below.

### Building a release APK
**Build → Generate App Bundles / APKs → Generate APKs** in Android Studio.
A signed release APK is also attached to this repository's
[Releases](../../releases) page.

---

## Demo Credentials

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Staff | the staff member's Employee ID (e.g. `2026003`) | `staff123` |

Each staff member logs in individually using their own Employee ID as the
username — there is no shared staff account, and no username/password works
unless that Employee ID has already been registered by an admin.

**There is no pre-seeded data in the APK.** To test the app end-to-end:

1. Log in as **admin** (`admin` / `admin123`).
2. Add a new staff member (name + Employee ID).
3. Open their profile and enrol their face (3 selfies).
4. Log out.
5. Log in as **staff**, using that Employee ID as the username and
   `staff123` as the password.
6. Mark attendance with a selfie, then check attendance history from both
   the staff and admin sides.

This was a deliberate choice — see "Assumptions & Limitations" below.

---

## Assumptions & Limitations

- **No backend / no real authentication.** Credentials are hardcoded/local as
  explicitly permitted by the assignment. A production app would authenticate
  against a server and never trust a client-stored role.
- **No liveness detection.** The app verifies that a live camera *capture*
  matches an enrolled face, but does not detect whether the input is a live
  person vs. a photo of a photo. A production system would add liveness
  checks (blink detection, head-turn challenge, etc.).
- **Face matching accuracy** depends on lighting, camera quality, and angle.
  Thresholds were tuned through manual on-device testing (see above), not a
  formal benchmark dataset — good enough for demo purposes but not validated
  at production scale.
- **Location is optional, not mandatory.** If location permission is denied
  or a GPS fix isn't obtained within a few seconds, attendance is still
  recorded with `latitude`/`longitude` as `null` and a visible "Location
  unavailable" note, rather than blocking the staff member from marking
  attendance. This favors availability over strict location enforcement,
  which seemed more appropriate for an indoor office context with potentially
  weak GPS signal.
- **5-minute cooldown between attendance marks.** This is a data-quality
  decision (prevents accidental duplicate taps), not a spec requirement. The
  assignment doesn't define check-in/check-out semantics, so a single cooldown
  window was used rather than building explicit clock-in/clock-out states.
- **Selfies are stored mirrored** (matching the front-camera preview, as a
  user would expect to see themselves). This has no effect on matching
  accuracy since both enrolment and attendance selfies go through the same
  camera pipeline and are compared consistently.
- **Deleting a staff member cascades to their attendance history** (it's
  removed too). An alternative (soft-delete / archive) would preserve
  historical records but was out of scope for a 2-day build.
- **Admin can edit a staff member's name and Employee ID** after creation
  (with the same uniqueness check as on creation). This is safe because
  attendance records reference the internal database ID, not the Employee ID
  (see Data Model above).
- **No pre-seeded/demo staff data ships in the APK.** A reviewer must first
  log in as admin, register a staff member, and enrol their face before the
  staff-side flow can be tested. This was a deliberate choice: since face
  recognition depends on the *reviewer's own face* matching what was
  enrolled, any pre-seeded staff/face data would either belong to the
  developer (making it impossible for a reviewer to pass the match) or be
  unusable filler data. Requiring live registration means every reviewer
  can test the actual face-matching behavior with their own face, which is
  more representative of the real flow than a canned demo account would be.
- **MobileFaceNet model source:** [MCarlomagno/FaceRecognitionAuth](https://github.com/MCarlomagno/FaceRecognitionAuth)
  (`assets/mobilefacenet.tflite`), used under its
  [BSD-3-Clause license](https://github.com/MCarlomagno/FaceRecognitionAuth/blob/master/LICENSE).
- **16 KB page-size alignment warning:** the build shows a warning that some
  native libraries (from TensorFlow Lite / ML Kit) aren't aligned for 16 KB
  memory pages, which newer Android devices may require. This doesn't affect
  functionality on standard 4 KB-page devices (including the device used for
  testing) and wasn't addressed given the project timeline.
- **Not tested across multiple device models/manufacturers** — developed and
  tested primarily on a single physical device (vivo X200 FE, Android,
  4G/5G/Wi-Fi). Camera behavior, permission dialogs, and USB debugging
  quirks in particular are known to vary by OEM (Android skin), so behavior
  on other manufacturers' devices (e.g. Samsung, Pixel) hasn't been verified.

---

## Project Structure

```
com.example.myapplication
├── data/
│   ├── local/          # Room database, DAOs, TypeConverters
│   ├── model/           # Entities (Staff, Attendance)
│   ├── preferences/      # DataStore session repository
│   └── repository/       # Repository interfaces + implementations
├── di/                   # AppContainer (manual dependency injection)
├── face/                 # ML Kit detection + TFLite embedding logic
├── navigation/            # NavHost + route definitions
├── ui/
│   ├── login/
│   ├── admin/            # Staff list, add/edit/delete, profile, enrolment
│   ├── staff/             # Staff home, mark attendance, history
│   ├── camera/            # Reusable camera capture screen
│   └── theme/
├── util/                  # Constants, location helper, file helpers
└── AttendanceApplication.kt
```
