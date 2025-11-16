#!/bin/bash

# APK Build Validation Script
# This script validates that the build configuration is correct and can build APKs

set -e  # Exit on error

echo "=================================================="
echo "  Image Text Editor Pro - Build Validation"
echo "=================================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if we're in the right directory
if [ ! -f "build.gradle" ] || [ ! -f "settings.gradle" ]; then
    echo -e "${RED}Error: Please run this script from the project root directory${NC}"
    exit 1
fi

echo "Step 1: Checking prerequisites..."
echo "---"

# Check for Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
    echo -e "${GREEN}✓${NC} Java found: $JAVA_VERSION"
else
    echo -e "${RED}✗${NC} Java not found. Please install JDK 11 or higher"
    exit 1
fi

# Check for ANDROID_HOME
if [ -z "$ANDROID_HOME" ]; then
    echo -e "${YELLOW}⚠${NC} ANDROID_HOME not set. This may cause issues."
    echo "  Set it with: export ANDROID_HOME=/path/to/android/sdk"
else
    echo -e "${GREEN}✓${NC} ANDROID_HOME: $ANDROID_HOME"
fi

# Check for gradlew
if [ -x "./gradlew" ]; then
    echo -e "${GREEN}✓${NC} Gradle wrapper is executable"
else
    echo -e "${YELLOW}⚠${NC} Making gradlew executable..."
    chmod +x gradlew
    echo -e "${GREEN}✓${NC} Done"
fi

echo ""
echo "Step 2: Checking network connectivity..."
echo "---"

# Check Google Maven
if curl -s -I https://maven.google.com | grep -q "HTTP/[12] [23]"; then
    echo -e "${GREEN}✓${NC} Can access maven.google.com"
else
    echo -e "${RED}✗${NC} Cannot access maven.google.com"
    echo "  See TROUBLESHOOTING_NETWORK.md for solutions"
fi

# Check dl.google.com
if curl -s -I https://dl.google.com/dl/android/maven2/ | grep -q "HTTP/[12] [23]"; then
    echo -e "${GREEN}✓${NC} Can access dl.google.com"
else
    echo -e "${RED}✗${NC} Cannot access dl.google.com"
    echo "  This may cause build failures. See TROUBLESHOOTING_NETWORK.md"
fi

# Check Maven Central
if curl -s -I https://repo1.maven.org/maven2/ | grep -q "HTTP/[12] [23]"; then
    echo -e "${GREEN}✓${NC} Can access Maven Central"
else
    echo -e "${YELLOW}⚠${NC} Cannot access Maven Central"
fi

# Check JitPack
if curl -s -I https://jitpack.io | grep -q "HTTP/[12] [23]"; then
    echo -e "${GREEN}✓${NC} Can access JitPack"
else
    echo -e "${YELLOW}⚠${NC} Cannot access JitPack"
fi

echo ""
echo "Step 3: Validating Gradle configuration..."
echo "---"

# Test Gradle configuration
if ./gradlew projects &> /dev/null; then
    echo -e "${GREEN}✓${NC} Gradle configuration is valid"
else
    echo -e "${RED}✗${NC} Gradle configuration has errors"
    exit 1
fi

echo ""
echo "Step 4: Building debug APK..."
echo "---"
echo "This may take a few minutes on first build..."
echo ""

# Clean build
./gradlew clean

# Build debug APK
if ./gradlew assembleDebug --stacktrace; then
    echo ""
    echo -e "${GREEN}✓${NC} Debug APK built successfully!"
    
    # Check if APK exists
    APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        echo -e "${GREEN}✓${NC} APK generated: $APK_PATH ($APK_SIZE)"
        
        # Get APK info if aapt is available
        if command -v aapt &> /dev/null && [ -n "$ANDROID_HOME" ]; then
            echo ""
            echo "APK Information:"
            echo "---"
            if [ -f "$ANDROID_HOME/build-tools/"*/aapt ]; then
                AAPT=$(find "$ANDROID_HOME/build-tools/" -name aapt | head -n 1)
                $AAPT dump badging "$APK_PATH" | grep -E "package:|sdkVersion:|targetSdkVersion:" || true
            fi
        fi
    else
        echo -e "${RED}✗${NC} APK not found at expected location"
        exit 1
    fi
else
    echo ""
    echo -e "${RED}✗${NC} Build failed!"
    echo ""
    echo "Common solutions:"
    echo "1. Check your internet connection"
    echo "2. See TROUBLESHOOTING_NETWORK.md for network issues"
    echo "3. Try: ./gradlew clean assembleDebug --stacktrace"
    echo "4. Use GitHub Actions to build (see README.md)"
    exit 1
fi

echo ""
echo "=================================================="
echo -e "${GREEN}  ✓ All validation checks passed!${NC}"
echo "=================================================="
echo ""
echo "Your build environment is correctly configured."
echo "You can now:"
echo "  • Build release APK: ./gradlew assembleRelease"
echo "  • Install on device: adb install $APK_PATH"
echo "  • Open in Android Studio"
echo ""
