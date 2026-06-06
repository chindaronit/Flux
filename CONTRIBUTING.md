# Contributing to Flux

Thank you for your interest in contributing to Flux! Your support means a lot.

---

## Table of Contents

- [Getting Started](#getting-started)
- [How to Contribute](#how-to-contribute)
- [Branch Naming](#branch-naming)
- [Commit Messages](#commit-messages)
- [Pull Request Guidelines](#pull-request-guidelines)
- [Architecture Overview](#architecture-overview)
- [Module Structure](#module-structure)
- [Code Style & Conventions](#code-style--conventions)
- [Tech Stack](#tech-stack)

---

## Getting Started

1. Install the latest stable version of [Android Studio](https://developer.android.com/studio).
2. Clone the repository:
   ```bash
   git clone git@github.com:chindaronit/Flux.git
   ```
3. Open the project in Android Studio and let Gradle sync complete.
4. Select `Run > Run 'app'` to build and launch the app on a device or emulator.

**Requirements:**
- Android SDK target: **36**, minimum: **29** (Android 10+)
- Kotlin: **2.2.10**
- JVM Target: **11**
- Build tool: **Gradle with Kotlin DSL**

---

## How to Contribute

1. **Fork the Repository** — Fork to your own GitHub account.
2. **Create a Branch** — Branch off from `main` using the naming conventions below.
3. **Make Your Changes** — Follow the architecture and code style described in this guide.
4. **Open an Issue First** — For non-trivial changes, create an issue before opening a PR so the approach can be discussed.
5. **Submit a Pull Request** — Target the `dev` branch. Reference the related issue in the PR description.

---

## Branch Naming

Use descriptive, hyphenated branch names prefixed by type:

| Type | Pattern | Example |
|------|---------|---------|
| Feature | `feat/<short-description>` | `feat/add-calendar-widget` |
| Bug fix | `fix/<issue-or-description>` | `fix/resolve-issue-123` |
| Refactor | `refactor/<description>` | `refactor/viewmodel-cleanup` |
| Translation | `i18n/<language>` | `i18n/add-japanese` |
| Documentation | `docs/<description>` | `docs/update-contributing` |

---

## Commit Messages

Write concise, present-tense commit messages that describe *what* the change does:

```
feat: add biometric lock toggle in settings
fix: resolve crash when deleting last workspace
refactor: extract reminder logic into ReminderReceiver
i18n: add Spanish translation strings
```

Avoid vague messages like `fix stuff`, `WIP`, or `update`.

---

## Pull Request Guidelines

- Target the **`dev`** branch, not `master`/`main`.
- Reference the issue your PR addresses (e.g., `Closes #42`).
- Keep PRs focused — one feature or fix per PR.
- Ensure the app builds and runs without errors before submitting.
- Add or update string resources in `res/values/strings.xml` for any user-facing text.
- If adding a new screen, register it in the navigation graph (`navigation/`).
- If adding a new data entity, update the relevant DAO, model, repository, and DI module.
- For new languages, add a corresponding `res/values-<lang>/strings.xml` file.

I'll review your pull request as soon as possible. Thank you for your contribution!

---

## Architecture Overview

Flux follows **MVI (Model–View–Intent)** architecture with **Jetpack Compose** for the UI layer. The data flow is strictly unidirectional:

```
User Interaction (Intent / Event)
        ↓
   ViewModel
        ↓
  State Update
        ↓
  Composable UI (re-renders from State)
```

- **Model** — Data layer: Room entities, DAOs, repositories.
- **View** — Composable screens that observe `State` objects and emit `Events`.
- **Intent/ViewModel** — Processes events, calls repository methods, and emits new `State`.

Side effects (navigation, toasts, etc.) are handled via `Effects` (one-shot flows) separate from persistent UI state.

**Dependency injection** is provided by **Hilt**, keeping ViewModels and repositories decoupled and testable.

---

## Module Structure

All source code lives under `app/src/main/java/com/flux/`.

```
app/src/main/java/com/flux/
│
├── data/                        # Data layer
│   ├── dao/                     # Room DAO interfaces (one per entity)
│   ├── models/                  # Room @Entity data classes and related data models
│   ├── repository/              # Repository implementations (single source of truth)
│   └── database/                # Room database definition and migrations
│
├── di/                          # Hilt dependency injection modules
│   ├── DataModules.kt           # Provides database and DAO instances
│   ├── RepositoryModules.kt     # Binds repository interfaces to implementations
│   └── Flux.kt                  # @HiltAndroidApp application class
│
├── navigation/                  # Compose Navigation graph and route definitions
│
├── other/                       # Utility and system-integration classes
│   ├── utils/                   # General-purpose helper functions and extensions
│   ├── BackupManager.kt         # Handles data backup and restore logic
│   ├── ReminderReceiver.kt      # BroadcastReceiver for scheduled reminders
│   ├── BootReceiver.kt          # Reschedules reminders after device reboot
│   ├── workspaceIcons/          # Icon assets and mapping utilities for workspaces
│   ├── BiometricAuthentication.kt  # Biometric lock integration
│   └── [other app-wide utilities]
│
├── ui/                          # UI layer (Jetpack Compose)
│   ├── common/                  # Shared, reusable composables (buttons, dialogs, cards, etc.)
│   ├── events/                  # Sealed classes / data classes representing user intents per screen
│   ├── states/                  # Data classes holding observable UI state per screen
│   ├── effects/                 # One-shot side-effect flows (navigation, snackbars, etc.)
│   ├── screens/                 # Full-screen composables, one file per screen
│   ├── viewModels/              # Hilt-injected ViewModels; process events, expose state
│   └── theme/                   # Material 3 color scheme, typography, and shape definitions
│
└── MainActivity.kt              # Single-activity entry point; hosts NavHost
```

```
app/src/main/res/
│
├── drawable/                    # Vector drawables and icons
└── values/                      # Resource files
    ├── strings.xml              # Default (English) strings
    └── values-<lang>/           # Translations (hi, fr, pt-BR, ru, de, es, nl, zh-rCN, …)
```

### Adding a New Feature — Checklist

When building a new screen or feature, create/update files in this order:

1. **`data/models/`** — Define the Room entity or data class.
2. **`data/dao/`** — Write the DAO interface with required queries.
3. **`data/database/`** — Add the entity to the database and increment the version with a migration.
4. **`data/repository/`** — Implement the repository exposing `Flow`s and suspend functions.
5. **`di/`** — Bind the new DAO/repository in the appropriate Hilt module.
6. **`ui/events/`** — Define a sealed class for all user actions on the new screen.
7. **`ui/states/`** — Define the data class representing the full UI state for the screen.
8. **`ui/viewModels/`** — Implement the ViewModel; inject the repository via Hilt.
9. **`ui/screens/`** — Build the Composable screen; collect state, dispatch events.
10. **`navigation/`** — Register the new route and composable in the nav graph.
11. **`res/values/strings.xml`** — Add all user-facing strings; keep zero hardcoded text in composables.

---

## Code Style & Conventions

- **Language**: Kotlin only. No Java files.
- **Formatting**: Follow standard Kotlin style (4-space indent, no wildcard imports).
- **Naming**:
    - ViewModels: `<Feature>ViewModel` (e.g., `NoteViewModel`)
    - States: `<Feature>State` (e.g., `NoteState`)
    - Events: `<Feature>Event` (e.g., `NoteEvent`)
    - Effects: `<Feature>Effect` (e.g., `NoteEffect`)
    - Screens: `<Feature>Screen` (e.g., `NoteScreen`)
    - DAOs: `<Entity>Dao` (e.g., `NoteDao`)
- **State hoisting**: Keep composables stateless where possible; hoist state to the ViewModel.
- **No business logic in composables**: Composables should only render state and forward events.
- **Strings**: All user-visible text must live in `strings.xml`. Never hardcode strings in composables.
- **Markdown content**: Flux uses CommonMark + GitHub Flavored Markdown (GFM) with LaTeX math support. Refer to `Guide.md` for supported syntax.

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.2.10 |
| UI | Jetpack Compose + Material 3 (Material You) |
| Architecture | MVI + ViewModel |
| DI | Hilt |
| Database | Room (SQLite abstraction) |
| Navigation | Compose Navigation |
| Markdown | CommonMark + Flexmark (HTML→MD) |
| Math | LaTeX via inline renderer |
| Async | Kotlin Coroutines + Flow |
| Build | Gradle Kotlin DSL |

---

## Questions?

Open a [Discussion](https://github.com/chindaronit/Flux/discussions) or file an [Issue](https://github.com/chindaronit/Flux/issues) — happy to help you get oriented.