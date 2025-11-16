# Image Text Editor Pro

A production-ready Android application for comprehensive image editing with advanced text detection and manipulation capabilities using ML Kit OCR and OpenCV.

## Features

### 🎨 Complete Photo Editing Suite
- **Crop & Rotate**: Professional cropping tools with custom aspect ratios
- **Filters**: 14 professional filters including grayscale, sepia, vintage, sketch, and more
- **Adjustments**: Fine-tune brightness, contrast, and saturation with real-time preview
- **Drawing Tools**: Freehand drawing with customizable brush sizes and colors
- **Text Overlay**: Add custom text with various fonts and styles
- **Stickers & Emojis**: Enhance images with fun stickers and emoji overlays
- **Undo/Redo**: Full history support for all editing operations

### 🔍 Advanced Text Detection (OCR)
- **ML Kit Integration**: Powered by Google's ML Kit for accurate text recognition
- **Automatic Detection**: Instantly detect all text in images
- **Text Removal**: Remove detected text with intelligent inpainting
- **Bounding Box Visualization**: See exactly what text was detected
- **Multi-language Support**: Recognizes text in multiple languages

### 🌙 Modern UI/UX
- **Material Design 3**: Beautiful, modern interface following Google's latest design guidelines
- **Dark Mode**: Default dark theme with seamless light/dark mode switching
- **Professional Layout**: Intuitive navigation and tool organization
- **Real-time Preview**: See changes instantly as you edit
- **Responsive Design**: Optimized for all Android screen sizes

### 💾 Storage & Permissions
- **Save to Gallery**: Direct save to device photo gallery
- **Scoped Storage**: Full Android 13+ scoped storage support
- **Permission Management**: Proper runtime permission handling
- **Image Quality**: High-quality JPEG compression (95%)

## Technology Stack

### Core Technologies
- **Language**: Kotlin
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Build System**: Gradle 8.0 with Kotlin DSL

### Key Libraries
- **ML Kit Text Recognition** (v16.0.0): Google's ML Kit for OCR
- **OpenCV Android** (v4.5.3.0): Computer vision and image processing
- **UCrop** (v2.2.8): Advanced image cropping
- **GPUImage** (v2.1.0): Real-time image filters
- **PhotoEditor SDK** (v3.0.2): Drawing, stickers, and text overlay
- **Material Components** (v1.10.0): Material Design 3 UI components
- **Glide** (v4.16.0): Efficient image loading and caching

## Installation

### Prerequisites
- Android Studio Hedgehog (2023.1.1) or later
- JDK 17 or higher
- Android SDK 34
- Minimum device: Android 7.0 (API 24)

### Building from Source

1. **Clone the repository**
   ```bash
   git clone https://github.com/mrfortune94/Image-Text-Editor-Pro.git
   cd Image-Text-Editor-Pro
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Click "OK" and wait for Gradle sync

3. **Build the APK**
   
   **Debug APK** (for testing):
   ```bash
   ./gradlew assembleDebug
   ```
   Output: `app/build/outputs/apk/debug/app-debug.apk`
   
   **Release APK** (for production):
   ```bash
   ./gradlew assembleRelease
   ```
   Output: `app/build/outputs/apk/release/app-release.apk`

4. **Install on Device**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

> **⚠️ Build Issues?** If you encounter network or repository access errors, see [TROUBLESHOOTING_NETWORK.md](./TROUBLESHOOTING_NETWORK.md) for detailed solutions.

### Using GitHub Actions

The project includes automated CI/CD with GitHub Actions:

1. **Automatic Builds**: Every push to `main`, `master`, or `develop` triggers a build
2. **APK Artifacts**: Both debug and release APKs are uploaded as artifacts
3. **Download**: Access built APKs from the "Actions" tab on GitHub

#### Setting up Release Signing (Optional)

To enable signed release builds in GitHub Actions:

1. Generate a keystore:
   ```bash
   keytool -genkey -v -keystore release-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias release-key
   ```

2. Add secrets to your GitHub repository (Settings → Secrets → Actions):
   - `KEYSTORE_FILE`: Base64-encoded keystore file
     ```bash
     base64 -i release-keystore.jks | pbcopy
     ```
   - `KEYSTORE_PASSWORD`: Your keystore password
   - `KEY_ALIAS`: Your key alias (e.g., "release-key")
   - `KEY_PASSWORD`: Your key password

## Usage Guide

### Getting Started

1. **Open Image**: Tap "Open Image" to select a photo from your gallery
2. **Choose Tool**: Select any editing tool from the toolbox
3. **Edit**: Make your desired changes with real-time preview
4. **Save**: Tap "Save Image" to export to your gallery

### Editing Tools

#### Crop
- Select crop tool
- Drag corners to adjust crop area
- Support for free-form and aspect ratio cropping
- Tap checkmark to apply

#### Filters
- Browse through 14 professional filters
- Tap any filter for instant preview
- Tap "Apply" to confirm changes

#### Text Detection (OCR)
- Automatically detects all text in image
- Red boxes highlight detected text
- Tap "Remove All Text" to erase detected text
- View detected text regions numbered for reference

#### Draw
- Select brush tool
- Choose color from palette
- Adjust brush size with slider
- Draw freehand on image
- Use eraser to remove mistakes
- Undo/Redo support

#### Stickers & Emojis
- Tap sticker/emoji tool
- Select from emoji picker
- Drag to position
- Pinch to resize
- Rotate with two fingers

#### Adjust
- Fine-tune brightness (-100 to +100)
- Adjust contrast (0 to 2x)
- Modify saturation (0 to 2x)
- Real-time preview of changes
- Reset button to restore original

#### Text Overlay
- Add custom text to images
- Choose from various fonts
- Adjust size and color
- Position anywhere on image
- Pinch to scale text

### Theme Switching

- Tap the moon/sun icon in the toolbar
- Instantly switch between light and dark modes
- Theme preference persists across app restarts

## Project Structure

```
Image-Text-Editor-Pro/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/imagetexteditor/pro/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── CropActivity.kt
│   │   │   │   ├── FilterActivity.kt
│   │   │   │   ├── TextDetectionActivity.kt
│   │   │   │   ├── PhotoEditorActivity.kt
│   │   │   │   └── AdjustActivity.kt
│   │   │   ├── res/
│   │   │   │   ├── layout/          # XML layouts
│   │   │   │   ├── drawable/        # Icons and graphics
│   │   │   │   ├── values/          # Strings, colors, themes
│   │   │   │   └── values-night/    # Dark mode themes
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/             # Instrumented tests
│   │   └── test/                    # Unit tests
│   ├── build.gradle                 # App module build config
│   └── proguard-rules.pro          # ProGuard rules
├── gradle/
│   └── wrapper/                     # Gradle wrapper
├── .github/
│   └── workflows/
│       └── build-apk.yml           # CI/CD workflow
├── build.gradle                     # Project build config
├── settings.gradle                  # Project settings
├── gradle.properties               # Gradle properties
├── .gitignore                      # Git ignore rules
└── README.md                       # This file
```

## Permissions

The app requests the following permissions:

- `READ_EXTERNAL_STORAGE`: Read images from gallery (Android 12 and below)
- `WRITE_EXTERNAL_STORAGE`: Save edited images (Android 9 and below)
- `READ_MEDIA_IMAGES`: Read images from gallery (Android 13+)
- `CAMERA`: Optional, for future camera feature
- `INTERNET`: For ML Kit models (on-device processing)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Google ML Kit for text recognition
- OpenCV for computer vision capabilities
- UCrop for professional cropping tools
- PhotoEditor SDK for drawing and overlay features
- GPUImage for real-time filter processing
- Material Design team for UI/UX guidelines

## Support

For issues, questions, or suggestions:
- Open an issue on GitHub
- Contact: [Your contact information]

## Roadmap

Future enhancements planned:
- [ ] Advanced text editing with font matching
- [ ] AI-powered background removal
- [ ] Batch image processing
- [ ] Cloud storage integration
- [ ] More filter presets
- [ ] Video editing capabilities
- [ ] Social media direct sharing
- [ ] Collage maker

---

Made with ❤️ by the Image Text Editor Pro team

