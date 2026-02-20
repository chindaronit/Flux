## Privacy Policy of Open Note

Flux is an open-source Android app developed by Ronit Chinda.  
The source code is available on GitHub under the GPL-3.0 license.

### Data Collection

Flux does not collect any personal or confidential information such as addresses, names, or email addresses.

### Permissions Requested

The app requires the following permissions, as listed in the `AndroidManifest.xml` file:

https://github.com/chindaronit/Flux/blob/master/app/src/main/AndroidManifest.xml

| Permission                      | Purpose                                                                                                       |
|---------------------------------|---------------------------------------------------------------------------------------------------------------|
| `android.permission.USE_BIOMETRIC` | Enables biometric authentication to provide secure access to protected notes                                  |
| `android.permission.INTERNET`   | Allows network connectivity for loading web content (images, videos, and other media) when rendering markdown |
| `android.permission.RECEIVE_BOOT_COMPLETED`           | Allows the app to receive the system broadcast after device boot, enabling it to restart scheduled tasks such as reminders, alarms, or background sync services |
| `android.permission.POST_NOTIFICATIONS`           | Grants permission to display notifications to the user (required on Android 13+), used for reminders, updates, or alerting about scheduled events |
| `android.permission.SCHEDULE_EXACT_ALARM`           | Allows the app to schedule precise alarms that trigger at exact times (e.g., note reminders, task deadlines), bypassing battery optimizations that delay inexact alarms |

### Dependencies

The app uses the following dependencies:

- **Room**: For local database management.
- **Hilt**: For dependency injection.
- **Compose**: For building the UI.
- **CommonMark**: For markdown rendering and parsing.

### Data Sharing
Flux does not share any personal or sensitive user data with third parties.

### Data Deletion
Usually all data is stored locally and can be cleared by the user at any time.

---

If you have any questions about this policy or personal information protection, please send your inquiries, opinions, or suggestions to: chindaronit04@gmail.com