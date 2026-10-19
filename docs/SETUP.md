# SETUP.md — ResonanceDrift development environment

The Mac is the development machine. A Windows PC is an optional backup
(D19): step 2 is only for the day it is needed. If it is used, the project
lives on the **Windows side** of the PC, never inside WSL2, and both machines
install to the same Pixel 9, so both sign with the same debug key (handled by
the repo — see step 6).

Do the steps in order. Each ends with a check; don't move on until it passes.

---

## 0. What you are installing, and why

| Thing | What it is | Where it ends up |
|---|---|---|
| Android Studio | The IDE. Bundles its own JDK (JetBrains Runtime) and the Gradle wrapper runs from the repo — **do not install a separate JDK or Gradle** | Mac: `/Applications/Android Studio.app` · Win: `C:\Program Files\Android\Android Studio` |
| Android SDK | Platform, build tools, `adb`. Installed by Studio's first-run wizard | Mac: `~/Library/Android/sdk` · Win: `%LOCALAPPDATA%\Android\Sdk` |
| Git | Studio needs a `git` binary. Mac has one via Xcode CLT; Windows needs Git for Windows | — |
| Pixel 9 in developer mode | The device you run on. The emulator is optional and slower than the phone | — |

Nothing here needs admin rights beyond the installers themselves.

---

## 1. Android Studio — Mac

1. Download the current **stable** build from https://developer.android.com/studio.
   Pick the **Apple Silicon** `.dmg` unless the Mac is Intel.
2. Open the `.dmg`, drag Android Studio to Applications, launch it. Allow it
   through Gatekeeper if asked.
3. First-run wizard: **Do not import settings** → **Standard** install type →
   accept licences → Finish. It downloads ~1.5 GB (SDK platform, build tools,
   platform-tools, emulator). Let it finish.
4. Open **Settings → Languages & Frameworks → Android SDK**:
   - **SDK Platforms** tab: tick the latest stable Android (the one the repo's
     `compileSdk` names — check `gradle/libs.versions.toml`). Also tick
     **Android 14.0 (API 34)** — that is `minSdk`.
   - **SDK Tools** tab: make sure these are ticked — *Android SDK Build-Tools*,
     *Android SDK Platform-Tools*, *Android SDK Command-line Tools (latest)*,
     *Android Emulator* (optional).
   - Apply.
5. Put `adb` on your PATH. Add to `~/.zshrc`:
   ```sh
   export ANDROID_HOME="$HOME/Library/Android/sdk"
   export PATH="$PATH:$ANDROID_HOME/platform-tools"
   ```
   Open a new terminal.

**Check:** `adb version` prints a version. `git --version` works (if not:
`xcode-select --install`).

---

## 2. Android Studio — Windows (optional backup)

Everything below happens in **Windows**, not in a WSL2 terminal. Gradle over
the `\\wsl$` bridge is slow, file watching breaks, and Studio will mis-detect
paths. Keep this repo at something like `C:\Users\ErezSchatz\dev\ResonanceDrift`.

1. Install **Git for Windows** from https://git-scm.com/download/win. Defaults
   are fine except: when asked about line endings choose **"Checkout as-is,
   commit as-is"**. (The repo has a `.gitattributes` forcing LF anyway.)
2. Download the current **stable** Android Studio `.exe` installer from
   https://developer.android.com/studio and run it. Defaults.
3. First-run wizard: **Do not import settings** → **Standard** → accept
   licences → Finish. Same ~1.5 GB download.
4. If Windows Defender / Studio offers to **exclude the Gradle and project
   folders from real-time scanning**, accept — it halves build times.
5. **Settings → Languages & Frameworks → Android SDK**: same ticks as the Mac
   (latest stable platform + API 34; Build-Tools, Platform-Tools, Command-line
   Tools). In **SDK Tools** also tick **Google USB Driver** (needed for the
   Pixel over USB on Windows).
6. Add `%LOCALAPPDATA%\Android\Sdk\platform-tools` to your user PATH
   (Settings → System → About → Advanced system settings → Environment
   Variables → User → Path → New). Open a new terminal.

**Check:** `adb version` and `git --version` both work in a fresh PowerShell.

---

## 3. The build JDK comes from the repo

There is no per-machine JDK to pick. `gradle/gradle-daemon-jvm.properties`
(Gradle's *Daemon JVM criteria*) pins the JDK the build runs on — version 25 —
with download links per OS. Gradle uses a matching local JDK if it finds one
and otherwise downloads it, so both machines build with the same JDK.

In Studio this shows up as **Settings → Build, Execution, Deployment → Build
Tools → Gradle → Gradle JVM criteria** (it replaces the old *Gradle JDK*
dropdown). Leave it as the repo has it; changing it there rewrites the file,
and that change is a commit, not a local preference. Leave *Distribution* on
"Wrapper" — the repo's `gradlew` decides the Gradle version, not the machine.

---

## 4. The phone

Do this once on the Pixel 9.

1. **Settings → About phone → tap "Build number" seven times.** Developer
   options appear under **Settings → System**.
2. In **Developer options**, turn on **USB debugging** and **Wireless debugging**.
3. Also in Developer options, consider **Stay awake** (screen stays on while
   charging) — useful while testing, turn off afterwards.

### Connect over USB (simplest, works on both OSes)
Plug in. On the phone, choose **Allow** on the "Allow USB debugging?" prompt
and tick *Always allow from this computer*. On Windows, if the phone shows up
in Device Manager with a warning, point it at the Google USB Driver installed
in step 2.5.

**Check:** `adb devices` lists the phone as `device` (not `unauthorized`).

### Connect over Wi-Fi (no cable, same network)
In Studio: **Device Manager → ⋮ / "Pair Devices Using Wi-Fi"**. On the phone:
**Developer options → Wireless debugging → Pair device with QR code**. Scan.
Pairing persists per machine; reconnecting later is one click in Device
Manager. The phone must be on the same Wi-Fi as the computer.

---

## 5. The repo

1. Create an empty private GitHub repository `ResonanceDrift`. Claude generates
   the project skeleton (Gradle files, manifest, `AppGraph`, the empty package
   tree, `docs/`, this file), commits, and pushes.
2. On each machine: `git clone` it to the path above (Mac: wherever your other
   repos live; Windows: on `C:`, not WSL).
3. In Studio: **File → Open…** and pick the repo folder (the one containing
   `settings.gradle.kts`). Click **Trust Project**.
4. Gradle sync starts automatically. The **first** sync downloads Gradle itself
   plus all dependencies — several minutes and ~1 GB. Later syncs are seconds.
   Wait for "Gradle sync finished" in the bottom bar.
5. Studio writes `local.properties` with your SDK path. It is gitignored and
   machine-specific — never commit it.

**Check:** the project tree shows `app` with no red underlines, and **Build →
Make Project** succeeds. Then run the unit tests once (`./gradlew test`) —
the skeleton ships with one small test so the whole toolchain is proven
before any real code exists.

Honesty note: the skeleton was written from the current Android docs on a
machine without an Android SDK, so **this first sync is its first build**.
If it fails, paste the full Build-tab output — the fix is almost always a
one-line version bump in `gradle/libs.versions.toml`, not a rewrite.

---

## 6. Signing — why both machines must share a key

Android refuses to install an app over an existing one signed with a
different key (`INSTALL_FAILED_UPDATE_INCOMPATIBLE`). Each Studio install
generates its *own* debug key, so without this step every switch between Mac
and Windows would force an uninstall and lose the saved queue.

- **Debug builds:** the repo commits `keystore/debug.keystore` and
  `app/build.gradle.kts` points `signingConfigs.debug` at it. Both machines
  therefore sign identically. Nothing for you to do. (Committing a *debug*
  key to a private personal repo is fine; it can only sign debug builds.)
- **Release builds** (only needed when you want a smaller, faster APK to live
  with): generate the key **once**, on the Mac —
  **Build → Generate Signed App Bundle / APK → APK → Create new…** — and save
  it **outside the repo**, e.g. `~/.android/resonancedrift.jks`. Then create
  `keystore.properties` in the repo root (gitignored):
  ```properties
  storeFile=/Users/<you>/.android/resonancedrift.jks
  storePassword=…
  keyAlias=resonancedrift
  keyPassword=…
  ```
  Copy the `.jks` to the Windows machine at `C:\Users\ErezSchatz\.android\resonancedrift.jks`
  and write the same `keystore.properties` there with that path. Back the
  `.jks` up somewhere that is not either machine. Losing it means uninstalling
  on the phone to install the next release.

---

## 7. First run on the phone

1. Phone connected (step 4) and selected in the device dropdown in Studio's
   toolbar.
2. Press **Run ▶** (Shift+F10 / Ctrl+R). Studio builds a debug APK, installs
   it, launches it. First build: a minute or two. Subsequent: seconds.
3. Allow the **notification permission** prompt on first launch — that is
   what shows the playback notification / lock-screen controls.

**Check:** the auth screen appears. Log in. Play an album. Lock the phone.
Wait for the track boundary. It should just continue.

Install from the command line instead, if you prefer:
```sh
./gradlew installDebug          # Mac
.\gradlew.bat installDebug      # Windows
```

---

## 8. Day-to-day

| Task | Mac | Windows |
|---|---|---|
| Run unit tests | `./gradlew test` | `.\gradlew.bat test` |
| Run instrumented tests (phone connected) | `./gradlew connectedAndroidTest` | `.\gradlew.bat connectedAndroidTest` |
| Install debug build | Run ▶ or `./gradlew installDebug` | Run ▶ or `.\gradlew.bat installDebug` |
| Build release APK | Build → Generate Signed APK, or `./gradlew assembleRelease` | same |
| Read the app's logs | **Logcat** tab in Studio, filter `package:net.erezschatz.resonancedrift` | same |
| Pull the playback diagnostics | Now Playing → bug icon → paste anywhere | — |

Switching machines: `git pull`, open Studio, let it sync, Run. Because the
debug key is shared, the install on the phone updates in place.

---

## 9. When it doesn't work

| Symptom | Cause | Fix |
|---|---|---|
| Gradle sync: "SDK location not found" | No `local.properties` | **File → Sync Project with Gradle Files**; Studio regenerates it. Or create it with `sdk.dir=<SDK path from step 1/2>` |
| Sync: "Unsupported class file major version" / JDK errors | Daemon JVM criteria changed or the toolchain download failed | Step 3 — `git diff gradle/gradle-daemon-jvm.properties`; restore it, then sync again with network on |
| Sync: "The project is using an incompatible version of the Android Gradle plugin" / "requires Android Studio X" | Studio older than the `agp` version in `libs.versions.toml` | Update Studio (Help → Check for Updates) — preferred. Or lower `agp` to the newest version your Studio accepts; Studio's error names it |
| Sync: a dependency "could not be found" / "no matching variant" | A version pinned in `libs.versions.toml` doesn't exist | Studio underlines the line and offers the real latest version; take it. Paste the error if unsure |
| Sync hangs or times out on downloads | Network / proxy | Retry; corporate VPN off; `gradle.properties` must not have stale proxy lines |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | App on phone signed with another key | `adb uninstall net.erezschatz.resonancedrift`, run again. Should only happen once, before the shared debug key existed |
| Phone `unauthorized` in `adb devices` | Didn't accept the prompt | Unlock phone, re-plug, tap Allow |
| Phone not listed at all (Windows) | Driver | SDK Manager → SDK Tools → Google USB Driver; Device Manager → update driver → browse to `%LOCALAPPDATA%\Android\Sdk\extras\google\usb_driver` |
| Windows build is very slow / "file watching" warnings | Project is under `\\wsl$` or a synced folder | Move it to a plain `C:\` path |
| Line-ending churn in `git status` on Windows | autocrlf | `git config --global core.autocrlf false`; the repo's `.gitattributes` does the rest |
| Build: "Installed Build Tools revision X is corrupted" | Half-installed build tools | SDK Manager → SDK Tools → untick/retick Build-Tools, Apply |
| Compose preview broken | Irrelevant to the app | Ignore; run on the phone |
| Emulator won't start (Windows) | Virtualisation | Hyper-V is already on if WSL2 works; in SDK Tools install *Android Emulator hypervisor driver*. Or skip the emulator and use the phone |
| Out-of-memory during build | Gradle daemon heap | `gradle.properties`: `org.gradle.jvmargs=-Xmx4g` (the repo ships this) |

If something fails that isn't here, paste the **full** error from the Build
tab (not the one-line summary) — the real cause is usually ten lines down.
