# Room storage — VocabLens

## Scope

The application uses Room over SQLite in the new on-device `vocab_lens_app.db` database, version 1, with all ten tables from the ERD. Firebase Authentication still manages sign-in. There is no cloud synchronization for the Room database.
As explicitly requested, there is no migration: the former `vocab_lens.db` and legacy SharedPreferences are not read, imported or deleted. Their data does not appear in the new application database.

## Structure

- `data/local/entity/`: User, UserSettings, Vocabulary, UserVocabulary, Collection, CollectionItem, Lesson, UserLessonProgress, StudySession and VocabularyReview entities.
- `data/local/dao/`: UserDao, VocabularyDao, CollectionDao, LessonDao, StudyDao and StatisticsDao.
- `data/local/database/VocabLensDatabase`: application-context singleton, database version 1.
- `data/mapper/VocabularyMapper`: conversion between database and domain models.
- `data/repository/LocalVocabularyRepositoryImpl`: implements the domain repository using Room.
- `GetSavedVocabulary`, `SaveVocabularyWord`, `DeleteVocabularyWord`: domain entry points.

`vocabulary` holds shared immutable content, while `user_vocabulary` has composite PK `(userId, vocabularyId)` and holds saved time, notes and learning state. Saving an already-saved word preserves its progress and original saved time. Reusing an ID for different content is rejected. All personal reads/deletes include the UID. Results are ordered by saved time, then vocabulary ID.
Composite foreign keys ensure collection items reference a collection and saved word with the same owner. Removing a collection removes its links only; unsaving a word removes its collection links but retains past review history. Words in an active review cannot be removed until that session closes.

## Authentication and existing screens

After successful authentication, `SynchronizeLocalUser` creates the local user/settings before any child records are used. Returning users keep their local display name and settings, while cached email is refreshed from Firebase. Email is read-only in the local profile editor; local profile changes do not update Firebase Authentication.
Profile, vocabulary, practice and dashboard screens observe account-scoped Room flows. Logout cancels observation and clears screen data. Bootstrap errors show a retry/logout screen; storage failures appear in the respective screens.
The production DemoDashboardRepositoryImpl and DemoPracticeRepositoryImpl have been removed. Lessons start empty. Names/emails come from the signed-in account, and learning counters start at zero and are computed from actual stored events. Default goals/settings are preferences, not sample achievements.

## Study data and accounting

Lesson content can be written through `LessonDao.saveLesson`; per-user progress through `saveProgress`. Existing practice screens read that content but still do not include a media player.
Collection persistence is available through CollectionDao; no collection-management UI is introduced in this database change.
StudyDao provides transactional start/finish/review operations, validates ownership and review modes, and prevents duplicate review IDs from incrementing counters twice. Only one active session per user is allowed, and backdated overlapping sessions are rejected. Resume or finish/abandon an interrupted active session before starting another.
Self-rated flashcards use AGAIN/HARD/GOOD/EASY and do not claim correctness. Quiz reviews require an actual correctness result. There is no automatic MASTERED threshold or spaced-repetition algorithm, and current reveal/next controls do not fabricate review results or XP. A future rating/player flow must call these persistence APIs.
XP is supplied explicitly on session completion, never inferred from button taps. Completion retries do not award it again. Abandoned sessions must have zero XP. Active duration cannot exceed the session wall-clock interval.
Dashboard/profile statistics observe actual library state, closed sessions, reviews and the clock. Daily figures attribute a closed session to its end date in the device timezone; minute totals round down. Streak uses completed sessions with positive activity, allowing the ongoing streak to end yesterday. Word goals count distinct reviewed words that day, not the number of reveal taps.

The Activity sets each screen's owner after local user synchronization and clears it immediately before logout. ViewModels cancel old jobs and clear account data when the owner changes. The UID is supplied through the domain boundary; these ViewModels do not depend on Firebase APIs.

## Existing prototype data

The old word bank contained two hard-coded examples; SharedPreferences only remembered their deleted IDs. Those records have no account owner. This integration does not assign them to any account or delete the old preferences. Each user's actual Room library starts empty.

The persistence API is ready for saving words from future camera/import features through `SaveVocabularyWord(uid, word)`. No add-word UI is introduced in this change.

## Schema changes

The generated schema for the fresh database is stored in `app/schemas/` and should be committed. No migration or destructive fallback is configured for this fresh start. Future changes that must retain data require a version increment and migration.

## Verification

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleDebugAndroidTest --console=plain
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain
```

Android database tests cover the ten-table schema, FK ownership, cascades, retained review history, idempotent review/XP updates, progress isolation, actual statistics and persistence after reopening. ViewModel tests cover reactive updates, filtering, review, account switches/logout and storage failures. Instrumentation tests use isolated databases, not the user's `vocab_lens_app.db`.

Inspect local rows in Android Studio via App Inspection → Database Inspector while the debug app is running and the vocabulary database is open.
