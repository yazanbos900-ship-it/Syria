# How to Download the Installable APK

If you previously downloaded a file that was around 3.5 MB, that was the **Project Source Code ZIP**, not the actual APK! This is why it did not install on your device.

An actual Android application file (.apk) built with Jetpack Compose will be much larger (usually 15MB to 30MB).

## Steps to download the true APK for testers:

1. Look at the **File Explorer** (workspace tree) on the left side of the screen.
2. Open the following folders in order:
   - `app/`
   - `build/`
   - `outputs/`
   - `apk/`
   - `debug/`
3. Inside the `debug` folder, you will see a file named `app-debug.apk`.
4. **Right-click** on `app-debug.apk` and select **Download**.
5. Once downloaded, you can send this file via WhatsApp, Telegram, or Google Drive, and it will install successfully on any Android device.
