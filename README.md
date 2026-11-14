# OmniPad

**A flexible Android workspace for notes, tasks, journals, habits, events, and progress tracking.**

OmniPad is an open-source productivity app for keeping everyday planning and writing together.
Organize work in customizable workspaces, write in Markdown, track routines, and manage events—all
from your Android device.

> OmniPad is a rebranded fork of [Flux](https://github.com/chindaronit/Flux). The app and its
> screenshots are being updated to reflect the new name; current OmniPad screenshots will be added
> when available.

## Features

- **Customizable workspaces** for organizing notes, tasks, journals, habits, events, and progress boards.
- **Markdown editor** with support for tables, code blocks, LaTeX, Mermaid diagrams, and more.
- **Tasks and reminders**, including recurring tasks and progress tracking.
- **Journals and notes** for daily entries and longer-form writing.
- **Calendar and events** for schedules and important dates.
- **Habit tracking and analytics** with progress insights.
- **Media attachments** for keeping images alongside your content.
- **Biometric app lock** and screen protection options.
- **Local-first storage** for working with your data on-device.
- **Themes and appearance settings** to personalize the experience.

## Screenshots

Updated OmniPad screenshots are not available yet.

## Build from source

OmniPad is an Android app built with Kotlin, Jetpack Compose, and Gradle. To build and run it:

1. Install Android Studio and the Android SDK.
2. Clone this repository and open it in Android Studio, or build from the command line:

   ```bash
   git clone https://github.com/SuprDupr/OmniPad.git
   cd OmniPad
   ./gradlew assembleDebug
   ```

3. In Android Studio, select **Run > Run 'app'** to install and launch the app on a device or emulator.

### Technical details

- **Minimum Android version:** Android 10 (API 29)
- **Compile/target SDK:** 37
- **Language:** Kotlin
- **UI:** Jetpack Compose with Material 3
- **JVM target:** 11
- **Architecture:** Model-View-ViewModel (MVI-style state and events)

## Translations

The app currently includes English, French, German, Hindi, Dutch, Brazilian Portuguese, Russian,
Spanish, and Simplified Chinese.

## Contributing

Bug reports, suggestions, and pull requests are welcome. Please open an issue in this repository to
report a problem or discuss a proposed change.

## Credits and license

OmniPad is based on [Flux](https://github.com/chindaronit/Flux), originally created by Ronit Chinda.
The original project is licensed under the GNU General Public License, version 3. See [LICENSE](LICENSE)
for the license text and retain the original copyright notices when redistributing this work.
