# Build Instructions for Image Text Editor Pro

## Prerequisites

Before building this Android project, ensure you have the following installed:

### Required Software
1. **Android Studio** (Hedgehog 2023.1.1 or later)
   - Download from: https://developer.android.com/studio
   
2. **JDK 11 or higher**
   - Android Studio includes OpenJDK
   - Or install from: https://adoptium.net/

3. **Android SDK**
   - SDK Platform 33 (Android 13)
   - Android SDK Build-Tools 33.0.0 or higher
   - Android SDK Platform-Tools

## Building the Project

### Method 1: Using Android Studio (Recommended for First Build)

1. **Clone the Repository**
   ```bash
   git clone https://github.com/mrfortune94/Image-Text-Editor-Pro.git
   cd Image-Text-Editor-Pro
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Click "Open"
   - Navigate to the cloned directory
   - Click "OK"

3. **Sync Gradle**
   - Android Studio will automatically start syncing Gradle
   - Wait for "Gradle sync finished" message
   - If prompted, accept any SDK licenses

4. **Build the Project**
   - Click "Build" → "Make Project" (or press Ctrl+F9 / Cmd+F9)
   - Wait for build to complete

5. **Build APK**
   - For Debug APK: Click "Build" → "Build Bundle(s) / APK(s)" → "Build APK(s)"
   - For Release APK: Click "Build" → "Generate Signed Bundle / APK"
   
6. **Locate APK**
   - Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
   - Release APK: `app/build/outputs/apk/release/app-release.apk`

### Method 2: Using Command Line

1. **Clone the Repository**
   ```bash
   git clone https://github.com/mrfortune94/Image-Text-Editor-Pro.git
   cd Image-Text-Editor-Pro
   ```

2. **Set ANDROID_HOME Environment Variable**
   ```bash
   # Linux/Mac
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools
   
   # Windows (Command Prompt)
   set ANDROID_HOME=%USERPROFILE%\AppData\Local\Android\Sdk
   set PATH=%PATH%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools
   
   # Windows (PowerShell)
   $env:ANDROID_HOME = "$env:USERPROFILE\AppData\Local\Android\Sdk"
   $env:PATH += ";$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\tools"
   ```

3. **Make gradlew executable** (Linux/Mac only)
   ```bash
   chmod +x gradlew
   ```

4. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```
   
   **Output:** `app/build/outputs/apk/debug/app-debug.apk`

5. **Build Release APK (Unsigned)**
   ```bash
   ./gradlew assembleRelease
   ```
   
   **Output:** `app/build/outputs/apk/release/app-release-unsigned.apk`

## Signing Release APK

For production release, you need to sign the APK:

### Step 1: Generate Keystore

```bash
keytool -genkey -v -keystore release-keystore.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias release-key
```

Follow the prompts to set:
- Keystore password
- Key password
- Your name and organization details

### Step 2: Configure Signing in build.gradle

Add to `app/build.gradle`:

```groovy
android {
    ...
    signingConfigs {
        release {
            storeFile file("../release-keystore.jks")
            storePassword "YOUR_KEYSTORE_PASSWORD"
            keyAlias "release-key"
            keyPassword "YOUR_KEY_PASSWORD"
        }
    }
    
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### Step 3: Build Signed Release APK

```bash
./gradlew assembleRelease
```

**Output:** `app/build/outputs/apk/release/app-release.apk` (signed)

## Installing the APK

### On Physical Device

1. **Enable Developer Options**
   - Go to Settings → About Phone
   - Tap "Build Number" 7 times
   
2. **Enable USB Debugging**
   - Go to Settings → Developer Options
   - Enable "USB Debugging"
   
3. **Connect Device and Install**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### On Emulator

1. **Start Emulator** from Android Studio or command line
2. **Install APK**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### Manual Installation

1. Transfer APK to device (email, cloud storage, USB)
2. On device, open the APK file
3. Allow "Install from Unknown Sources" if prompted
4. Tap "Install"

## Troubleshooting

### Gradle Sync Failed

```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### License Acceptance Required

```bash
# Accept all licenses
yes | $ANDROID_HOME/tools/bin/sdkmanager --licenses
```

### Out of Memory Error

Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### Build Tools Version Error

Open `build.gradle` and update to your installed version:
```groovy
android {
    compileSdk 33
    buildToolsVersion "33.0.0"
}
```

## GitHub Actions CI/CD

The repository includes automated builds via GitHub Actions.

### Automatic Builds

- Triggered on push to `main`, `master`, or `develop` branches
- Also triggered on pull requests
- Can be manually triggered from Actions tab

### Accessing Built APKs

1. Go to repository on GitHub
2. Click "Actions" tab
3. Click on latest workflow run
4. Scroll to "Artifacts" section
5. Download `app-debug` or `app-release`

### Setting Up Signed Releases in CI

To enable automatic signed releases:

1. **Encode your keystore:**
   ```bash
   base64 -i release-keystore.jks | pbcopy
   ```

2. **Add GitHub Secrets:**
   - Go to repository Settings → Secrets → Actions
   - Add these secrets:
     - `KEYSTORE_FILE`: The base64-encoded keystore
     - `KEYSTORE_PASSWORD`: Your keystore password
     - `KEY_ALIAS`: Your key alias (e.g., "release-key")
     - `KEY_PASSWORD`: Your key password

3. Push code - GitHub Actions will automatically build signed APK

## Build Variants

The project supports multiple build variants:

- **Debug**: For development and testing
  - Debuggable
  - Not optimized
  - Larger APK size

- **Release**: For production
  - Not debuggable
  - Optimized with ProGuard
  - Smaller APK size
  - Must be signed

## Project Structure

```
Image-Text-Editor-Pro/
├── app/
│   ├── build.gradle                    # App module build configuration
│   ├── proguard-rules.pro             # ProGuard rules
│   └── src/
│       └── main/
│           ├── java/                   # Kotlin source files
│           ├── res/                    # Resources (layouts, drawables, etc.)
│           └── AndroidManifest.xml    # App manifest
├── build.gradle                        # Project build configuration
├── settings.gradle                     # Project settings
├── gradle.properties                   # Gradle properties
├── gradlew                            # Gradle wrapper script (Unix)
├── gradlew.bat                        # Gradle wrapper script (Windows)
└── gradle/wrapper/                    # Gradle wrapper files
```

## Dependencies

All dependencies are automatically downloaded by Gradle:

- AndroidX libraries
- Material Design 3 components
- ML Kit Text Recognition
- OpenCV for Android
- UCrop for cropping
- GPUImage for filters
- PhotoEditor SDK
- Glide for image loading

## Minimum Requirements

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 33 (Android 13)
- **Compile SDK**: 33
- **Gradle**: 7.5
- **Android Gradle Plugin**: 7.4.2
- **Kotlin**: 1.8.20

## Support

If you encounter issues:

1. Check this document for troubleshooting
2. Verify all prerequisites are installed
3. Try cleaning and rebuilding
4. Open an issue on GitHub with:
   - Error message
   - Build log
   - Your environment (OS, Android Studio version, etc.)

## Additional Resources

- [Android Developer Documentation](https://developer.android.com/docs)
- [Gradle Build Guide](https://developer.android.com/studio/build)
- [Android Studio User Guide](https://developer.android.com/studio/intro)

---

For questions or issues, please create an issue on GitHub or contact the development team.
