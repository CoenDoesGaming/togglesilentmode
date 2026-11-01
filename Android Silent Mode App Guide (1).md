# Silent Mode Toggle (for Samsung Devices)

A lightweight, purely functional Android application designed to seamlessly toggle your phone's ringer between **Normal**, **Vibrate**, and **Silent** modes. 

While this app features a clean Material 3 UI for manual toggling, its true purpose is to provide a background **App Shortcut** that can be mapped to your physical hardware buttons using Samsung's Good Lock.

## 🤖 AI Generation Disclaimer
**Please note:** The code in this repository was generated with the assistance of AI. This project is provided as-is, and **will not be receiving future updates, active maintenance, or bug fixes.** Feel free to fork the repository if you would like to make your own modifications or improvements!

## 📥 Installation (No PC Required)
You do not need to build this app from source! You can download and install it directly to your phone:
1. Go to the **[Releases](../../releases)** tab on the right side of this GitHub page.
2. Download the latest `.apk` file to your Samsung device.
3. Open the downloaded file to install it. *(Note: Your phone may ask you to allow installing unknown apps from your browser—this is normal for apps outside the Play Store).*

## ✨ Features
*   **Background Toggle Shortcut:** Includes an App Shortcut that rotates through Normal -> Vibrate -> Silent. *(Note: Due to how Samsung's One UI launcher handles App Shortcuts, you will see a brief visual flash of the app opening and immediately closing when triggered).*
*   **Custom Haptic Feedback:** Feel which mode you just switched to without looking at your screen:
    *   *Normal Mode:* Single, quiet tap.
    *   *Vibrate Mode:* Double pulse.
    *   *Silent Mode:* Triple quick pulse.
*   **Material 3 UI:** Automatically adapts to your system's light/dark mode if you choose to use the in-app buttons.

## ⚠️ Important Context: The "Do Not Disturb" Limitation
Starting in Android 6.0, Google completely removed the classic "Silent Mode" from stock Android (AOSP), merging it into the "Do Not Disturb" (DND) system. On a standard Google Pixel or Android emulator, requesting Silent Mode will forcefully activate DND.

**The Samsung Exception:** 
Samsung's One UI custom software bypasses this strict limitation. On Samsung Galaxy devices, the classic "true" Silent mode still exists separate from DND. This app is specifically designed and tested for Samsung devices to take advantage of this, giving you a proper Silent mode without messing up your notification filters!

*(Note: The app still requires the "Do Not Disturb" permission to be granted in Android settings to legally modify the ringer state, even though it achieves true silent mode on Samsung).*

## 🚀 The Ultimate Setup: Physical Mute Switch via Good Lock
By combining this app's background shortcut with Samsung's Good Lock, you can turn your phone's Power/Side button into a physical mute switch (similar to an iPhone's alert slider).

### Step-by-Step Guide
1. **Install the App:** Download the APK from the Releases tab and install it. Open the app once, grant the required permission, and close it. (Opening it once registers the shortcut with your launcher).
2. **Get Good Lock:** Download the **Good Lock** app from the Samsung Galaxy Store.
3. **Get Routines+:** Inside Good Lock, download the **Routines+** module.
4. **Create the Routine:**
    * Open your phone's standard **Settings** > **Modes and Routines** (or access it via Routines+).
    * Click the **+** at the top to create a new routine.
    * Under **If**, select **Button action** (provided by Routines+). Choose **Side button** and set the interaction to **Press and hold** (or Double Press, whichever you prefer).
    * Under **Then**, go to **App** > **Launch app shortcut** > select **Toggle Mode** (with the custom icon) from this app.
    * Save the routine.

Now, whenever you press and hold your side button, your phone will instantly rotate to the next ringer mode and give you custom haptic feedback!

## 🛠️ Building from Source
1. Clone this repository.
2. Open the project in Android Studio.
3. Sync Gradle and hit the green Play button to install it on your connected Samsung device.