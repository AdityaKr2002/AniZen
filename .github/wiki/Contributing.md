# 🤝 Contributing

Thanks for your interest in contributing to AniZen! Here's everything you need to get started.

---

## 🐛 Reporting Bugs

1. Check [existing issues](https://github.com/salmanbappi/AniZen/issues) to avoid duplicates.
2. Open a [new issue](https://github.com/salmanbappi/AniZen/issues/new) with:
   - AniZen version
   - Android version & device model
   - Steps to reproduce
   - Logcat or crash report (use **Settings → Diagnostics → Export Logs**)

---

## 💡 Feature Requests

Open an issue tagged `enhancement`. Describe the use case, not just the solution. Feature discussions also happen in the [Discord server](https://discord.gg/J2wmZqEJnS).

---

## 🔧 Code Contributions

Pull requests are welcome! No assignment or permission required — just pick an [open issue](https://github.com/salmanbappi/AniZen/issues) and comment on it so others know it's being worked on.

### Prerequisites

You need working knowledge of:

| Skill | Resource |
|:---|:---|
| **Kotlin** | [kotlinlang.org](https://kotlinlang.org/) |
| **Android Development** | [developer.android.com](https://developer.android.com/) |
| **Jetpack Compose** | [Compose docs](https://developer.android.com/compose) |

### Tools Required

- [Android Studio](https://developer.android.com/studio) (latest stable or Canary)
- An Android device or emulator with developer options enabled

### Setup

```bash
git clone https://github.com/salmanbappi/AniZen.git
cd AniZen
./gradlew assembleDebug
```

Install the debug APK to your device and you're ready to iterate.

### Code Style

- Run `./gradlew detekt` before submitting — all Detekt rules must pass
- Follow existing naming conventions and architecture patterns
- See [Architecture](Architecture) for module boundaries — respect them

---

## 🌐 Translations

Translations are managed externally via [**Weblate**](https://hosted.weblate.org/projects/salmanbappi/anizen/).

- No code changes required — just translate strings in Weblate
- Weblate auto-syncs to the repo via PRs

---

## 🍴 Forking

Forks are permitted under [Apache-2.0](https://github.com/salmanbappi/AniZen/blob/master/LICENSE). When forking:

| Requirement | Why |
|:---|:---|
| Change the app name | Avoid user confusion |
| Change the app icon | Avoid user confusion |
| Change `applicationId` in `build.gradle.kts` | Avoid install conflicts with AniZen |
| Disable/replace the update checker | Prevent your users from receiving AniZen updates |
| Replace ACRA endpoint (if using crash reporting) | Prevent your crash data polluting AniZen's reports |

---

## 💬 Getting Help

Join the [Discord server](https://discord.gg/J2wmZqEJnS) for live help while developing.
