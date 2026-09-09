# MultiversalCopy

A cross-platform clipboard copy application powered by PaddleOCR.

## Building the Android App

This project uses Gradle to build the Android application. 
The Android SDK is required to compile the app, but it is **not** tracked in this repository to save space and avoid cross-platform configuration issues.

### How to set up the Android SDK

1. **Download Android Studio / Command Line Tools**:
   If you do not have the Android SDK installed, the easiest way to get it is by downloading and installing [Android Studio](https://developer.android.com/studio). Alternatively, you can download the Command Line Tools from the same page if you prefer a headless setup.

2. **Locate your SDK Path**:
   - **Windows**: `C:\Users\YourUsername\AppData\Local\Android\Sdk`
   - **macOS**: `/Users/YourUsername/Library/Android/sdk`
   - **Linux**: `/home/YourUsername/Android/Sdk`

3. **Configure the Project**:
   Create a file named `local.properties` in the `frontend/` directory (where the `build.gradle` is located).
   
   Add the following line to `frontend/local.properties`, replacing the path with your actual SDK path:
   ```properties
   sdk.dir=/path/to/your/android/sdk
   ```

4. **Build and Install**:
   Navigate to the `frontend/` folder and run Gradle to build and install the app to an attached Android device or emulator.
   ```bash
   cd frontend
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Branches
- `master`: The default debug version of the app. Displays raw OCR boxes, includes images, and shows text layout debug labels.
- `release`: A clean production version. Drops image boxes, drops boxes overlapping the phone's system bars, and hides the debug labels.
