# ARTS - Accident Report and Tracking System

ARTS lets drivers report road accidents and get help fast. A driver reports an incident, or presses the panic button, and ARTS sends the location and details to the emergency services it works with: ambulance, police, fire, traffic and towing. Administrators follow every incident on a live dashboard. The system has two parts that share one database:

- an **Android app** for drivers (this repository), written in Kotlin
- a **website** for drivers and administrators, written in HTML, CSS and JavaScript (folder `arts_web` in the project hand-in)

> Demonstration video: `ADD YOUR VIDEO LINK HERE`

## What the app does

| Feature | How it works |
|---|---|
| Sign in and registration | Email and password through Firebase Authentication. Passwords are never stored by the app. |
| Single sign-on | Continue with Google, through Android Credential Manager and Firebase. |
| Biometric unlock | Optional fingerprint or face check before an already signed-in account opens. |
| Two-factor login | Optional authenticator-app code (TOTP), compatible with Google Authenticator. |
| Report an incident | Type, priority, location, description, photo, and a pin on an OpenStreetMap map. The pin and street address fill in from the phone's live location, and you can tap the map to move the pin. |
| AI Assist | Gemini tidies the description and suggests the type and priority. Without a key, or without signal, the built-in rules do the same job, so the button always works. |
| Panic button | One confirmed action: gets the phone's location and sends a High priority emergency. |
| Automatic triage | Urgent reports (High priority, or two or more warning words) are sent to the matching active services without waiting for an administrator. |
| Live tracking | Reports and alerts update on screen as administrators work on them. |
| Weather and QR code | Local weather for each report (Open-Meteo) and a QR code that opens the report. |
| Medical profile | Blood type, allergies, medication, medical aid and emergency contact, shown to administrators when the person reports an incident. |
| Settings | Profile name, phone and photo, light or dark theme, English, Afrikaans or isiZulu menu. |
| Help | In-app questions and answers. |
| Offline reports | A report sent without signal is stored on the phone and sent when the connection returns. |

## Architecture

```
  Android app (Kotlin, MVVM)        Website (HTML, CSS, JavaScript)
  Fragments -> ViewModels             Pages -> data layer (storage.js)
        |                                   |
        +-------- Firebase SDKs ------------+
                       |
        Firebase Authentication  (email, Google)
        Cloud Firestore          (shared database)
        Firestore security rules (who can read what)
        Firebase Hosting         (the website)
```

The app follows MVVM: fragments show the screen, view models hold the state, and repositories talk to Firebase.

```
app/src/main/java/com/example/arts/
  activities/   MainActivity, SplashActivity
  fragments/    one class per screen
  viewmodels/   AuthViewModel, IncidentViewModel, ProfileViewModel, NotificationViewModel, WeatherViewModel
  repository/   UserRepository, IncidentRepository, AlertRepository
  data/         Incident, UserProfile and other models, Firestore mappers and live-data wrappers
  network/      Open-Meteo weather API client (Retrofit)
  utils/        triage rules, validators, time formatting, biometric, TOTP, QR, image helpers
app/src/test/   unit tests
```

The database design, relationships and field list are in [DATABASE.md](DATABASE.md).

## Run it

1. Install Android Studio and JDK 17.
2. Open this folder in Android Studio and let Gradle sync.
3. `app/google-services.json` is already included for the `arts-477f0` Firebase project. To use your own project, replace it with the file from your Firebase console, and add your debug SHA-1 to the Android app there (needed for Google sign-in).
4. Optional, for Gemini-powered AI Assist: get a free key at https://aistudio.google.com/apikey and put `GEMINI_API_KEY=your key` in `local.properties`. Add `GEMINI_MODEL=model-name` only if Google retires the default (`gemini-3.5-flash`). Without a key, AI Assist uses its built-in rules and everything else works.
5. Run on a phone or emulator that has Google Play services.

Command line: `./gradlew assembleDebug` (Windows: `gradlew assembleDebug`).

## Tests and automated builds

Unit tests cover the triage rules, input validation, time formatting, the incident model, QR codes and two-factor codes.

```
./gradlew testDebugUnitTest
```

GitHub Actions (`.github/workflows/android.yml`) runs the tests and builds the app on every push and pull request, then uploads the test report and the debug APK as downloadable results. This proves the project builds on a clean machine and not just on one computer.

## Security

- Passwords are handled only by Firebase Authentication.
- Firestore rules (`firestore.rules`) make every collection private to signed-in people. A person can read only their own profile and reports; administrators are accounts listed in the `admins` collection, which can only be edited in the Firebase console.
- Two-factor secrets are stored where only their owner can read them.
- Known limits: automatic dispatch is decided on the device (doing it on the server needs a paid Firebase plan), and the Gemini key sits in the app, so for a public release move AI Assist to Firebase AI Logic with App Check.

## Release notes

| Version | Changes |
|---|---|
| v1.0 | Prototype: registration, login, report an incident, incident list, alerts, local database. |
| v1.1 | Biometric login and authenticator-app two-factor login. |
| v1.2 | Map with a location pin (MapLibre), weather at the incident location, incident QR codes and scanner. |
| v1.3 | Gemini AI Assist, photo upload, panic button, medical profile, settings (theme, language, profile photo). |
| v1.4 | Help screen, Gradle wrapper fixed. |
| v2.0 | Moved to Firebase: shared online database with the website, real Firebase sign-in, Google single sign-on, live updates, automatic triage and dispatch, offline reports, unit tests and GitHub Actions. |
| v2.1 | Green, red and white theme to match the website, new launcher icon, OpenStreetMap maps with a live location dot, panic button waits for a fresh location, AI Assist falls back to built-in rules. |

Innovative features: the one-confirmation panic button, automatic triage that notifies the right emergency services, and the medical profile that responders see when a person reports an incident.

## Team

`ADD TEAM MEMBERS AND THEIR RESPONSIBILITIES HERE`
