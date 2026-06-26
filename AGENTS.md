# Flux — Agent Guide

## Overview

**Flux** is a lightweight Android productivity app built with **Jetpack Compose** and **Material 3**. It manages notes, tasks, journals, habits, events, and progress boards in customizable workspaces.

- **License:** GPL-3.0
- **Min SDK:** 29 (Android 10)
- **Target SDK:** 37 (Android 14+)
- **Version:** 3.1.8 (versionCode 14)
- **Package:** `com.flux`

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| DI | Dagger Hilt (with KSP) |
| Database | Room (with KSP) |
| Navigation | Compose Navigation |
| Markdown | CommonMark (rendering) + Flexmark (HTML→MD) |
| Image loading | Coil |
| Serialization | Gson + kotlinx.serialization |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle KTS + Version Catalog |

---

## Project Structure

```
Flux/
├── app/
│   ├── build.gradle.kts          # App module build config
│   ├── proguard-rules.pro        # ProGuard rules
│   └── src/main/
│       ├── AndroidManifest.xml   # Permissions, Activity, Receivers
│       ├── assets/               # KaTeX, Mermaid, Prism, template.html
│       ├── res/                  # Drawables, fonts, layouts, strings (multi-lang)
│       └── java/com/flux/
│           ├── MainActivity.kt
│           ├── data/             # Data layer (models, DAOs, DB, repos)
│           ├── di/               # Hilt DI modules
│           ├── navigation/       # Navigation routes & host
│           ├── other/            # Utilities (recorders, backup, constants, etc.)
│           └── ui/               # UI layer (screens, state, theme, ViewModels)
├── build.gradle.kts              # Root build config
├── settings.gradle.kts           # Module includes
├── gradle.properties
├── gradle/libs.versions.toml     # Version catalog
├── metadata/                     # F-Droid metadata (descriptions, screenshots, changelogs)
├── .github/workflows/build.yml   # CI build
├── CONTRIBUTING.md
├── Guide.md
└── PRIVACY_POLICY.md
```

---

## Key Files & Their Purposes

### Entry Point

```text
app/src/main/java/com/flux/MainActivity.kt
```
- Single Activity architecture (`@AndroidEntryPoint`)
- Installs splash screen, enables edge-to-edge
- Creates all ViewModels, collects states, passes to `AppNavHost`
- Side-effect handler shows Snackbar messages globally

```text
app/src/main/AndroidManifest.xml
```
- Defines `MainActivity` as launcher activity
- Registers `BootReceiver` (boot-completed reminders) and `ReminderReceiver`
- Declares permissions: boot, exact alarm, notifications, audio recording
- Application class: `com.flux.di.Flux` (Hilt entry point)

### Navigation

```text
app/src/main/java/com/flux/navigation/
├── NavRoutes.kt       # Sealed class of all routes + screen maps
├── NavHost.kt         # AppNavHost composable (NavHost wiring)
├── AnimatedComposable.kt  # Shared transition animations
└── Preloader.kt       # Loading/splash placeholder
```
- Routes are structured as `/workspace/{id}/...` for workspace-scoped screens
- Navigation params: workspaceId, noteId, habitId, eventId, etc.

### Data Layer (`data/`)

#### Models (`data/model/`)
| File | Purpose |
|---|---|
| `WorkspaceModel.kt` | Workspace entity — container for notes, tasks, etc. |
| `NotesModel.kt` | Rich-text note with markdown content |
| `TodoModel.kt` | Todo list (checklist container) |
| `TodoInstance.kt` (via TodoDao) | Individual todo items within a list |
| `JournalModel.kt` | Journal entry |
| `HabitModel.kt` | Habit tracker definition |
| `HabitConfig.kt` | Habit recurrence/schedule configuration |
| `EventModel.kt` | Calendar event definition |
| `RecurrenceRule.kt` | RRULE for recurring events/habits |
| `ScheduleRequest.kt` | Alarm scheduling data |
| `LabelModel.kt` | Labels/tags for notes |
| `ProgressBoardModel.kt` | Kanban/board for progress tracking |
| `SettingsModel.kt` | App-wide settings state |
| `Converter.kt` | Room type converters |

#### DAOs (`data/dao/`)
12 DAOs — one per core entity: `WorkspaceDao`, `NotesDao`, `TodoDao`, `TodoInstanceDao`, `JournalDao`, `HabitsDao`, `HabitInstanceDao`, `EventDao`, `EventInstanceDao`, `LabelDao`, `ProgressBoardDao`, `SettingsDao`.

- All DAOs use Kotlin Coroutines (`suspend` functions)
- Flow-based observe patterns for reactive UI

#### Database (`data/database/`)
| File | Purpose |
|---|---|
| `FluxDatabase.kt` | Room database class (all DAOs registered) |
| `FluxBackup.kt` | JSON-based backup/restore logic |

#### Repositories (`data/repository/`)
- Interface + Implementation pattern for each entity
- 8 repositories: Workspace, Note, Todo, Journal, Habit, Event, Label, ProgressBoard, Settings

### DI Layer (`di/`)

| File | Purpose |
|---|---|
| `Flux.kt` | Application class (`@HiltAndroidApp`) |
| `DataModule.kt` | Provides database, DAOs, and backup manager |
| `RepositoryModule.kt` | Binds repository interfaces to implementations |

### UI Layer (`ui/`)

#### Screens (`ui/screens/`)
Each feature has its own package:

| Package | Screens |
|---|---|
| `workspaces/` | Home screen, workspace details, new/edit workspace, empty state |
| `notes/` | Note list, detail/editor, text editor base, find&replace, read view, standard text field |
| `todo/` | Todo list, detail, new/edit checklist |
| `journal/` | Journal entries list, editor |
| `habits/` | Habit list, details, new/edit habit |
| `events/` | Calendar events, details, new/edit event |
| `analytics/` | Analytics charts page |
| `labels/` | Label management |
| `progressBoard/` | Kanban-style progress board |
| `search/` | Global search across all entities |
| `auth/` | Biometric authentication screen |
| `settings/` | ~15 settings screens (theme, editor, privacy, languages, backup, etc.) |

#### State (`ui/state/`)
- `States.kt` — Aggregate data class holding all UI states
- Per-feature state files: `NotesState.kt`, `TodoState.kt`, `WorkspaceState.kt`, etc.
- `TextState.kt` — Handles text editor state (undo/redo, cursor position)
- Each feature state defines a sealed `Event` class for UI actions

#### ViewModels (`ui/viewModel/`)
- 10 ViewModels via `ViewModels.kt` aggregate data class
- Each exposes `state` (StateFlow) and `effect` (SharedFlow for side-effects)
- `SettingsViewModel` is persistent (survives config changes)

#### Theme (`ui/theme/`)
- `Theme.kt` — Material 3 dynamic color setup with 6 palette options
- 6 palette files (`Palette1.kt` – `Palette6.kt`)
- `Type.kt` — Typography definitions

#### Common Components (`ui/common/`)
Reusable composables: app bars, bottom sheets, buttons, dialogs, dropdown menus, empty state, filter chips, search bar, scaffold, animations.

#### Effects (`ui/effects/`)
- `ScreenEffect.kt` — Sealed class for side-effects (Snackbar messages)

### Utilities (`other/`)

| File | Purpose |
|---|---|
| `Constants.kt` | App-wide constants |
| `Utils.kt` | General utility functions |
| `AudioRecorder.kt` | Audio recording support |
| `BackupManger.kt` | File-based backup manager |
| `BackupWorker.kt` | WorkManager-based periodic backup worker |
| `BiometricAuthenticator.kt` | Biometric auth helper |
| `BootReceiver.kt` | Triggers alarms on device boot |
| `ReminderReceiver.kt` | Shows notification reminders |
| `GetOccurrences.kt` | Calculates recurring event/habit dates |
| `MarkdownLint.kt` | Markdown validation/lint |
| `MarkdownSegment.kt` | Markdown text segment parsing |
| `MediaCache.kt` | Cached image/media loading |
| `WorkspaceIcons.kt` | Workspace icon picker data |
| `highlight/` | Custom CommonMark highlight extension (``==highlight==`` syntax) |

### Assets (`assets/`)
- `template.html` — HTML template for rendered markdown preview
- `katex/` — KaTeX for LaTeX rendering in markdown
- `mermaid.min.js` — Mermaid diagram rendering
- `prism/` — Prism.js syntax highlighting

---

## Architecture Pattern

**MVVM + Repository + Single Activity**

```
UI (Composable) → ViewModel (state + effects) → Repository → DAO → Room DB
                                  ↕
                            Hilt DI (modules)
```

- ViewModels hold `StateFlow<XxxState>` for UI state and `SharedFlow<ScreenEffect>` for one-shot side effects
- Data flows one way: DB → DAO → Repository → ViewModel → UI
- UI events flow: User action → ViewModel event → Repository → DAO

### Workspace Layout (Global Top Bar)

There is **no separate workspace selection screen**. Instead:
- On app launch, the last-opened workspace opens directly (auto-saved to `SettingsModel.defaultWorkspaceId`).
- A **global workspace dropdown** is available on all pages:
  - **Workspace detail screens** (Notes, Todo, Events, etc.): `SpaceTopBar` shows workspace dropdown + actions.
  - **Settings & Search**: `WorkspaceDropdownTopBar` (from `WorkspaceScaffold`) provides the dropdown.
- Switching workspaces via dropdown navigates to `WorkspaceHome/{newId}`.
- The bottom bar has Home (↔ workspace detail), Search, and Settings.
- The old workspace selection grid (`WorkspaceHomeScreen`) still exists as fallback but auto-redirects to the last/default workspace.

---

## Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires signing config)
./gradlew assembleRelease

# Clean
./gradlew clean
```

Signing properties (optional, from `gradle.properties` or env):
- `signingKeyAlias`, `signingKeyPassword`, `signingStoreFile`, `signingStorePassword`

---

## Agent Behavior Guidelines

### 1. AGENTS.md First — Always Keep It Updated

- **Before any modification** to the codebase, this file is the single source of truth for project understanding.
- **After every meaningful change** (new file, new screen, structural refactor, dependency change), update AGENTS.md to reflect it.
- If an agent reads the project and finds AGENTS.md outdated, fix it immediately before proceeding.

### 2. Git Workflow

- **Branching** — Never commit directly to `master`. Create a feature branch for every change:
  ```bash
  git checkout -b feat/description-of-change
  ```
  Branch naming: `feat/`, `fix/`, `refactor/`, `docs/` prefixes.
- **Commits** — Small, atomic, descriptive commits:
  ```bash
  git commit -m "feat: add note archiving to workspace detail"
  ```
  Use [Conventional Commits](https://www.conventionalcommits.org/) format: `type: short description`.
- **Pull** before branching:
  ```bash
  git checkout master && git pull origin master
  ```
- **Push & PR** — Push your branch and open a PR (or ask the user how they want to merge).
  ```bash
  git push origin feat/description
  ```
- **Never force push** to shared branches.

### 3. Safety Rules

- **Read before write** — Understand existing patterns before adding new ones. Don't duplicate functionality.
- **No silent changes** — Every modification is explained. Don't delete or overwrite files without checking dependencies.
- **Lint & build** — After any code change, run the build to verify nothing is broken:
  ```bash
  ./gradlew assembleDebug
  ```
- **Backup** — Before major refactors, `git stash` or commit current workfirst.

### 4. Communication Style

- Report what changed, why, and any side effects.
- If a task is ambiguous, ask the user before guessing.
- If a change would affect multiple files or cross-cutting concerns, mention it before proceeding.

### 5. Repo-Specific Conventions

1. **Language** — All code is Kotlin. Follow Kotlin idiom (no Java-style patterns).
2. **Project files** — Edit `.kt` (not `.java`). Resources in `res/`.
3. **Data flow** — Always go through ViewModel events → Repository → DAO. Never access DAO from UI directly.
4. **State management** — Use `StateFlow` in ViewModels, `collectAsStateWithLifecycle()` in Composables.
5. **Navigation** — All routes defined in `NavRoutes.kt`. New screens must be registered there and in `NavHost.kt`.
6. **DI** — New ViewModels/repos get added to Hilt modules in `di/`.
7. **Assets** — Web assets (KaTeX, Mermaid, Prism) are bundled locally under `assets/`.
8. **Multi-language** — String resources exist for DE, ES, FR, HI, NL, PT, RU, ZH, EN. Always add strings to all locale files.
9. **Version catalog** — Dependencies in `gradle/libs.versions.toml`, not inline.
10. **Labels** — Labels (`LabelModel`) are workspace-scoped and apply to notes, journals, and todos.

### 6. .hermes/ Directory

- `.hermes/` is the **private agent workspace**. It is gitignored and never pushed to GitHub.
- `AGENTS.md` lives here and is maintained by agents for agents.
- Other files in `.hermes/` are temporary or agent-only tooling.
- **Never store secrets, API keys, or credentials** anywhere in the repo — even in `.hermes/`. Use environment variables or the Hermes memory tool.
