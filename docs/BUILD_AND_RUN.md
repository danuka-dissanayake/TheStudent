# Build the APK and run it on a USB device (terminal)

Everything here runs on **your PC**, from the project root.

* Gradle **9.3.1**, Android Gradle Plugin **9.1.1** → you need **JDK 17 or newer** (JDK 21 is safest).
* `applicationId` = `com.thestudent.sl`.
* Debug builds are signed with the committed `debug.keystore`, so `installDebug` just works.
  Release builds need `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_PASSWORD` (see §5).

---

## 1. One-time setup

### 1.1 Point Gradle at your SDK

Create `local.properties` in the project root (it is git-ignored). Use your own path:

```properties
# macOS
sdk.dir=/Users/YOUR_NAME/Library/Android/sdk
# Linux
# sdk.dir=/home/YOUR_NAME/Android/Sdk
# Windows (note the escaped backslashes)
# sdk.dir=C\:\\Users\\YOUR_NAME\\AppData\\Local\\Android\\Sdk
```

*If you forget this*, set `ANDROID_HOME` instead — Gradle accepts either.

### 1.2 API keys

```bash
cp .env.example .env
```

Edit `.env` and fill in `GEMINI_API_KEY`, `ZAI_API_KEY`, `Z_AI_API_KEY`, `HF_API_TOKEN`,
`GOOGLE_WEB_CLIENT_ID`. Without a `.env` the build still starts, but every AI feature reports
"not configured on this build". **Editing `.env` requires a rebuild** — it is compiled into
`BuildConfig`, it is not read at runtime.

### 1.3 Check the toolchain and the phone

```bash
java -version          # must print 17.x or 21.x
adb devices            # phone must appear with status "device"
```

If the phone shows `unauthorized`, unlock it and tap **Allow** on the "Allow USB debugging" dialog.
On the phone: **Settings → About phone → tap Build number 7×** → **Developer options → USB debugging = On**.

### 1.4 Optional but recommended: reset old installs

If a previous build was installed with a different signing key, the install fails with
`INSTALL_FAILED_UPDATE_INCOMPATIBLE`. Clean that up once:

```bash
adb uninstall com.thestudent.sl
```

---

## 2. Build + install + launch (the short version)

macOS / Linux:

```bash
./gradlew :app:installDebug && \
adb shell monkey -p com.thestudent.sl -c android.intent.category.LAUNCHER 1
```

Windows (PowerShell / cmd):

```bat
gradlew.bat :app:installDebug && adb shell monkey -p com.thestudent.sl -c android.intent.category.LAUNCHER 1
```

`monkey` launches whatever the app's launcher activity is, so it cannot get the class name wrong.
Expected output ends with `Events injected: 1` and the app opens on the phone.

> **First build is slow** (Gradle downloads dependencies, KSP generates Room code). 5–15 minutes is
> normal; later builds are seconds.

---

## 3. Or: build the APK, then install it by hand

```bash
./gradlew :app:assembleDebug
```

APK lands at:

```
app/build/outputs/apk/debug/app-debug.apk
```

Install and launch:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

```bash
adb shell am start -n com.thestudent.sl/com.example.MainActivity
```

> Why `com.example.MainActivity`? `namespace = "com.example"` in `app/build.gradle.kts` expands the
> manifest's relative `.MainActivity`, while `applicationId` is `com.thestudent.sl` — so the full
> component is `<applicationId>/<namespace>.MainActivity`.
> If that errors with `Activity class does not exist`, ask Android for the truth:

```bash
adb shell cmd package resolve-activity --brief com.thestudent.sl | tail -n 1
```

…then `adb shell am start -n <what it printed>`.

---

## 4. Watch the logs while you test sign-in

```bash
adb logcat -c                                                     # clear buffer
adb logcat -s TheStudentAuthClient TheStudentViewModel AndroidRuntime OkHttp
```

* App crash → `AndroidRuntime` shows the stack trace.
* Auth problems → `TheStudentAuthClient` / `TheStudentViewModel` log the API outcome.

Useful extras:

```bash
adb devices -l                                    # serial + model
adb -s <SERIAL> install -r app-debug.apk          # two devices connected
adb shell dumpsys package com.thestudent.sl | grep versionName
adb logcat -d > device.log                        # dump to a file
```

---

## 5. Release build (only when publishing)

The release config reads the keystore from the environment — never hardcode it:

```bash
export KEYSTORE_PATH="$HOME/keys/thestudent-upload.jks"
export STORE_PASSWORD='your-store-password'
export KEY_PASSWORD='your-key-password'

./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk  (also assembleBundle for Play)
```

Windows PowerShell: `$env:KEYSTORE_PATH="C:\keys\thestudent-upload.jks"` etc.

Install a release build on the phone over the debug one? Signatures differ — uninstall first:

```bash
adb uninstall com.thestudent.sl
adb install -r app/build/outputs/apk/release/app-release.apk
```

---

## 6. Wireless (no cable)

Android 11+ — **Developer options → Wireless debugging → Pair device with pairing code**:

```bash
adb pair 192.168.1.50:37123        # IP:port + the 6-digit code shown on the phone
adb connect 192.168.1.50:5555      # the IP:port shown under "Wireless debugging"
adb devices                        # now appears as 192.168.1.50:5555
```

Then run the same `installDebug` command.

---

## 7. Troubleshooting

| Symptom | Cause | Fix |
|---|---|---|
| `JAVA_HOME is set to an invalid directory` / `Unsupported class file major version` | old JDK | Install JDK 17+ and point `JAVA_HOME` at it |
| `SDK location not found` | no `local.properties` | §1.1 |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | different signing key | `adb uninstall com.thestudent.sl` |
| `INSTALL_FAILED_INSUFFICIENT_STORAGE` | phone full | free space; or `adb install -r -d` |
| `device unauthorized` | USB debugging prompt not accepted | unlock phone, tap Allow, `adb kill-server && adb start-server` |
| `no devices/emulators found` | cable/mode | use a data cable, set USB mode to **File transfer**, try another port |
| `Activity class {...} does not exist` | wrong component name | §3 `resolve-activity` |
| Build fails inside Compose Material3 after the BOM bump | dependency mismatch | revert `composeBom` in `gradle/libs.versions.toml` to `2024.09.00` and delete the `autofillType = …` arguments |
| `google-services.json` warning | no Firebase config | harmless — `googleServices.missing.passthrough=true` already set |
| App installs but "not configured on this build" | `.env` missing/empty | §1.2, then `./gradlew :app:clean :app:installDebug` |

---

## 8. Handy aliases

macOS / Linux (`~/.zshrc` or `~/.bashrc`):

```bash
alias ts-build='cd ~/path/to/TheStudent && ./gradlew :app:assembleDebug'
alias ts-run='cd ~/path/to/TheStudent && ./gradlew :app:installDebug && adb shell monkey -p com.thestudent.sl -c android.intent.category.LAUNCHER 1'
alias ts-log='adb logcat -s TheStudentAuthClient TheStudentViewModel AndroidRuntime'
alias ts-shot='adb exec-out screencap -p > ~/Desktop/device-$(date +%s).png'
```
