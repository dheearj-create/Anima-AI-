# Anima AI Desk Robot - Android Installation & Source Package Guide

## 1. Quick Android Device Installation (.apk)
The compiled, ready-to-install Android package has been generated:

- **Path:** `/install_package/Anima_AI_Desk_Robot.apk`
- **Original Build Path:** `/app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** ~26 MB
- **Package Name:** `com.aistudio.anima.vkrxql`
- **Minimum Android Version:** Android 7.0 (API 24)+
- **Target Android Version:** Android 15 / 16 (API 36)

### How to Install on your Android Phone:
1. **Download:** In the AI Studio file explorer on the left sidebar, navigate to `install_package/Anima_AI_Desk_Robot.apk` and right-click -> **Download** (or use the top-right AI Studio **Settings / Export -> Download APK** button).
2. **Transfer:** If downloaded onto a PC/Mac, send the `.apk` to your phone via USB cable, Google Drive, Bluetooth, or Quick Share.
3. **Open & Install:** On your Android phone, tap on `Anima_AI_Desk_Robot.apk`.
4. If prompted with *"Install unknown apps"*, tap **Settings** and toggle **"Allow from this source"**.
5. Tap **Install** and then **Open**.

---

## 2. All Source Code Files (Organized & Separated)

Every component is modularized into distinct, dedicated directories:

### Core Ocular & Camera System:
- **`com.example.ui.components.GazeEye`** (`app/src/main/java/com/example/ui/components/GazeEye.kt`): Reusable Compose component rendering real-time OLED robot eyes with pupil tracking, eyelid cuts, and specular gleam.
- **`com.example.camera.CameraGazeTracker`** (`app/src/main/java/com/example/camera/CameraGazeTracker.kt`): Camera2 front-facing sensor hardware face and eye detection with low-pass exponential smoothing.
- **`com.example.ui.avatar.ExpressiveAvatarView`** (`app/src/main/java/com/example/ui/avatar/ExpressiveAvatarView.kt`): Xiaozhi/Cozmo-style OLED animated eyes with breath animation, emotion morphing, and voice reactivity.

### User Interface & Screens:
- **`RobotEyesDeskScreen`** (`app/src/main/java/com/example/ui/screens/RobotEyesDeskScreen.kt`): Flagship desk robot "Only Eyes" mode with wake word detection, audio wave visualizer, and clean kiosk display.
- **`ChatScreen`** (`app/src/main/java/com/example/ui/screens/ChatScreen.kt`): Interactive conversation interface.
- **`StudioScreen`** (`app/src/main/java/com/example/ui/screens/StudioScreen.kt`): Offline generative digital artwork studio.
- **`DeepSearchScreen`** (`app/src/main/java/com/example/ui/screens/DeepSearchScreen.kt`): Deep search investigation dossier viewer.
- **`SettingsScreen`** (`app/src/main/java/com/example/ui/screens/SettingsScreen.kt`): System settings, calibration card, and camera tracking toggle.

### Engine, Speech & Data Layer:
- **`OfflineKnowledgeEngine`** (`app/src/main/java/com/example/domain/engine/OfflineKnowledgeEngine.kt`): 100% offline knowledge retrieval engine.
- **`GenerativeArtEngine`** (`app/src/main/java/com/example/domain/engine/GenerativeArtEngine.kt`): Procedural artwork generator.
- **`SpeechManager`** (`app/src/main/java/com/example/speech/SpeechManager.kt`): Microphone input, wake word listener, and audio amplitude telemetry.
- **`ElevenLabsVoiceService`** (`app/src/main/java/com/example/speech/ElevenLabsVoiceService.kt`): High-fidelity AI speech synthesis (`MzqUf1HbJ8UmQ0wUsx2p`).
- **`AppDatabase`** (`app/src/main/java/com/example/data/local/AppDatabase.kt`): Room SQLite database for chat history, generated artworks, and deep search dossiers.

### Build & Platform Configurations:
- **`app/build.gradle.kts`**: Android application module build scripts with Jetpack CameraX, Compose M3, and Room.
- **`gradle/libs.versions.toml`**: Centralized Version Catalog.
- **`AndroidManifest.xml`**: Hardware camera, microphone, and internet permission declarations.

---

## 3. How to Export the Full Project ZIP (All Separate Files)
To download the complete source code tree to open in Android Studio:
1. In the top-right toolbar of Google AI Studio, click on the **Project Menu (⋮ or Settings icon)**.
2. Select **Export as ZIP**.
3. Unzip on your computer and open the folder in **Android Studio** (File -> Open).
