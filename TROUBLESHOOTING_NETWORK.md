# Network Troubleshooting for APK Build

## Issue: Cannot Access dl.google.com

If you encounter errors like:
```
Could not GET 'https://dl.google.com/dl/android/maven2/...'
> dl.google.com: No address associated with hostname
```

or

```
Could not GET 'https://dl.google.com/...'
> Received status code 403 from server: Forbidden
```

This indicates network connectivity issues with Google's Maven repository.

## Solutions

### For Local Development

#### Solution 1: Check Internet Connection
Ensure you have a working internet connection and can access:
- `https://maven.google.com`
- `https://dl.google.com`

Test with:
```bash
curl -I https://maven.google.com
curl -I https://dl.google.com/dl/android/maven2/
```

#### Solution 2: Use VPN or Proxy
If `dl.google.com` is blocked in your region or network:

1. **Use a VPN** to access Google services
2. **Configure HTTP Proxy** in `gradle.properties`:
   ```properties
   systemProp.http.proxyHost=your.proxy.host
   systemProp.http.proxyPort=8080
   systemProp.https.proxyHost=your.proxy.host
   systemProp.https.proxyPort=8080
   ```

#### Solution 3: Use Repository Manager (Enterprise)
For corporate environments with restricted access:

1. Set up **Nexus** or **Artifactory** as a proxy
2. Configure `build.gradle` to use your proxy:
   ```groovy
   allprojects {
       repositories {
           maven {
               url 'http://your-nexus-server:8081/repository/google-proxy/'
           }
           mavenCentral()
       }
   }
   ```

#### Solution 4: Offline Build (If you have SDK installed)
If you have Android Studio installed with all dependencies cached:

1. Build once with internet to cache dependencies
2. Subsequent builds can use `--offline` flag:
   ```bash
   ./gradlew assembleDebug --offline
   ```

### For GitHub Actions

The GitHub Actions workflow (`.github/workflows/build-apk.yml`) should work correctly in the GitHub Actions environment because:

1. GitHub Actions runners have unrestricted access to Google services
2. The workflow uses Gradle caching to speed up builds
3. All necessary dependencies will be automatically downloaded

If you're still experiencing issues in GitHub Actions:

1. Check if your repository has any restrictions
2. Verify the workflow file is using the latest actions:
   - `actions/checkout@v4`
   - `actions/setup-java@v4`
   - `actions/upload-artifact@v4`

3. Manual workflow dispatch:
   - Go to your repository on GitHub
   - Click "Actions" tab
   - Select "Android CI - Build APK" workflow
   - Click "Run workflow"

## DNS Resolution Issues

If DNS cannot resolve `dl.google.com`:

### Linux/Mac:
```bash
# Check DNS resolution
nslookup dl.google.com

# Try alternate DNS servers
echo "nameserver 8.8.8.8" | sudo tee /etc/resolv.conf
echo "nameserver 8.8.4.4" | sudo tee -a /etc/resolv.conf
```

### Windows:
```cmd
# Check DNS resolution
nslookup dl.google.com

# Change DNS to Google DNS
# Go to Network Settings > Change Adapter Options > 
# Properties > Internet Protocol Version 4 > Use the following DNS server addresses:
# Preferred: 8.8.8.8
# Alternate: 8.8.4.4
```

## Firewall/Corporate Network

If you're behind a corporate firewall:

1. **Request whitelist** for:
   - `*.google.com`
   - `*.maven.org`
   - `jitpack.io`
   - `*.gradle.org`

2. **Ask IT for proxy settings** and configure as shown in Solution 2

3. **Use Android Studio** which can handle proxy authentication automatically

## Still Having Issues?

If none of the above solutions work:

1. **Use GitHub Actions**: The workflow is configured correctly and will build successfully in GitHub Actions environment

2. **Download APK from GitHub Actions**:
   - Go to Actions tab
   - Click on latest successful workflow run
   - Download the `app-debug` artifact

3. **Contact Network Administrator**: If you're in a restricted environment, you may need IT assistance to access required repositories

## Verification Steps

After applying a solution, verify with:

```bash
# Clean build
./gradlew clean

# Build with stacktrace to see detailed errors
./gradlew assembleDebug --stacktrace

# Check if APK was generated
ls -la app/build/outputs/apk/debug/app-debug.apk
```

## Additional Resources

- [Android Build Configuration](https://developer.android.com/build)
- [Gradle Proxy Configuration](https://docs.gradle.org/current/userguide/build_environment.html#sec:accessing_the_web_via_a_proxy)
- [GitHub Actions Documentation](https://docs.github.com/en/actions)
