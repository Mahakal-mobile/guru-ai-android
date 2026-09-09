# Guru AI – Native Android (Nothing Phone 3a Lite)

Kotlin Android app with master password settings, Gemini chat, and Accessibility Service scaffold.

## Build APK without computer (GitHub Actions)

1. Create a new **empty** GitHub repo (e.g. `guru-ai-android`)
2. Upload **all files** from this folder (keep folder structure: `app/`, `.github/`, etc.)
3. Open repo on GitHub → **Actions** tab
4. Select workflow **Build Guru AI APK** → **Run workflow**
5. Wait 3–10 minutes
6. Open that run → **Artifacts** → download **guru-ai-debug-apk**
7. On phone: unzip → install `app-debug.apk`
   (Allow install from unknown sources)

### Notes
- First build may need Gradle wrapper; if it fails, read the Actions log and re-run.
- Best long-term: once open the project in Android Studio on any PC so `gradlew` is generated, then push — later builds are reliable from GitHub only.

## Master Settings
- Password: `Nikesh@12345`
- Box 1: Gemini API key
- Box 2: WhatsApp / Mail tokens
- Box 3: Lock status

## On phone after install
1. Open Guru AI → Settings → enter password → paste Gemini key → Save
2. Phone Settings → Accessibility → enable **Guru AI**
3. Optional: Battery → Unrestricted for Guru AI
4. Chat, or use **Read screen**

## Local build (PC)
Android Studio → Open this folder → Build → Build APK(s).

## Ethics
Use Accessibility only with clear user consent. Do not silent-spam messages.
