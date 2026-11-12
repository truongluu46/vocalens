# Firebase Authentication — VocabLens

## Configuration

- Android application ID: `com.luudev.vocalens`.
- Download the matching Android app configuration from Firebase Console and put it at `app/google-services.json`.
- This configuration file is ignored by Git; each checkout needs its own copy.
- In Firebase Console, open Authentication → Sign-in method and enable Email/Password.
- Sync Gradle, then run the app with Internet access.
- Firebase BoM is pinned to 34.7.0 (Auth 24.0.1) for the current Kotlin 2.2 toolchain. BoM 35.1.0 / Auth 25.0.0 failed compilation with Kotlin metadata 2.4; review compatibility before upgrading.

## Architecture

Screens → ViewModels → auth use cases → `AuthRepository` → `FirebaseAuthRepositoryImpl`.
Firebase SDK types and error mapping stay in the data layer. All data repository implementations use the `Impl` suffix.

Registration creates the account and saves its display name in Firebase Authentication, then opens Home.
If saving the display name fails after account creation, the app signs out and explains that the account exists and can be used to sign in.
Login only opens Home after Firebase succeeds. Startup checks Firebase's persisted user, not a saved UI navigation flag.
Logout signs out of Firebase. Password reset sends Firebase's reset email; passwords are never stored in SavedStateHandle.

This integrates Authentication only. Dashboard and practice content are still demo data, and profile/vocabulary storage remains local. Profile edits do not update the Firebase account email or display name.

## Verification

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Manual checks against your Firebase project:

1. Register a new email and confirm the user and display name appear in Authentication → Users.
2. Log out; incorrect credentials must stay on Login and show an error.
3. Log in successfully, restart the app, and confirm Home opens with the persisted session.
4. Log out, restart, and confirm Home is no longer accessible without login.
5. Use Forgot password with a valid email and check the inbox.
6. Test duplicate email, offline requests, and repeated submit taps.

Official references:
- https://firebase.google.com/docs/android/setup
- https://firebase.google.com/docs/auth/android/password-auth
- https://firebase.google.com/docs/auth/android/manage-users
