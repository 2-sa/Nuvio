<div align="center">

  <img src="https://nuvio.tv/assets/nuvio-app-logo-wordmark.webp" alt="Nuvio" width="320" />

  <p>
    A free, open-source media app for Windows, macOS, and Linux.
    <br />
    Bring your own sources. Nuvio turns them into a library with artwork, ratings, subtitles, and your place saved on every screen.
  </p>

  [Website](https://nuvio.tv) · [GitHub Releases](https://github.com/2-sa/Nuvio/releases/latest) · [Support Nuvio](https://nuvio.tv/support)

</div>

## ⚠️ Alpha Software - Testers Only

Nuvio Desktop is currently in alpha and is intended only for testers. It is under active development and is not suitable for daily use.

Expect breaking changes with every update. Features, settings, stored data, and compatibility may change or stop working without notice. Do not rely on this build as your primary media app, and report any issues you encounter during testing.

> This repository (`2-sa/Nuvio`) is a desktop-focused fork of [NuvioMedia/NuvioDesktop](https://github.com/NuvioMedia/NuvioDesktop) focused on native PC ergonomics, desktop player controls, memory tuning, and Arabic (`ar`) localization.

## Desktop Highlights

- **Native Desktop UI & Themes:** Cinema Obsidian desktop palette, collapsible sidebar rail, centered modal dialogs and anchored dropdown menus, pointer hover states, and customizable themes.
- **Desktop Player (`libmpv` + Web UI):** Stremio-style bottom control bar, mouse-wheel volume and seek ergonomics, hover seekbar time previews, and right-click context menus.
- **Navigation & Shortcuts:** `Ctrl/Cmd + F` to jump to Search, `Ctrl/Cmd + ,` to open Settings, mouse Back/Forward history navigation, smooth wheel scrolling, and shelf carousel arrows.
- **Settings & Integrations:** Grouped, searchable Settings workspace with live API key validation, Addons/Plugins, Debrid, TMDB, Trakt, Simkl, MDBList, and optional Supabase sync.
- **Localization & Performance:** Full English and Arabic (`ar` with RTL layout and CLDR plural rules) UI support, bounded image/metadata LRU caches, and idle/minimize memory trimming.

## Get Nuvio Desktop

Download the latest release from [GitHub Releases](https://github.com/2-sa/Nuvio/releases/latest):

- **Windows (x64):** MSI installer (`Nuvio-Windows-x64-*.msi`)
- **macOS (Apple Silicon `arm64` & Intel `x86_64`):** DMG installers (`Nuvio-macOS-arm64-*.dmg`, `Nuvio-macOS-x86_64-*.dmg`)
- **Linux:** DEB, RPM, Flatpak, or AppImage (can be built from source or via CI workflow dispatch)

## Build from Source

### Prerequisites

- **JDK 17** (Temurin 17 is used in CI)
- **Node.js 22+** (optional, for validating `player-ui` JavaScript files)

```bash
git clone https://github.com/2-sa/Nuvio.git
cd Nuvio
```

### Run the app

```bash
./gradlew :composeApp:run
```

On Windows PowerShell:

```powershell
.\gradlew.bat :composeApp:run
```

### Run tests & checks

Compile the desktop target and run unit tests:

```bash
./gradlew :composeApp:desktopMainClasses :composeApp:desktopTest \
  -x :composeMediaPlayer:buildNativeLinux \
  -x :composeApp:buildLinuxPlayerBridge \
  -Pcompose.desktop.packaging.checkJdkVendor=false
```

Validate the player Web UI scripts:

```bash
node --check composeApp/src/desktopMain/resources/player-ui/controls.js
node --check composeApp/src/desktopMain/resources/player-ui/party.js
```

### Package the app

Build a release package for the current OS:

```bash
./gradlew :composeApp:packageReleaseDistributionForCurrentOS
```

Platform-specific packaging:

- **Windows PowerShell (MSI):**

  ```powershell
  .\gradlew.bat :composeApp:packageReleaseMsi --rerun-tasks
  ```

- **macOS (DMG):**

  ```bash
  ./scripts/build-macos-release-dmgs.sh --package-only
  ```

- **Linux (DEB):**

  ```bash
  ./gradlew :composeApp:packageReleaseDeb
  ```

### CI & Releases

- **Desktop Tests:** [`.github/workflows/desktop-tests.yml`](./.github/workflows/desktop-tests.yml) runs compilation, player UI syntax/XML checks, and `:composeApp:desktopTest` on pushes to `Dev`.
- **Desktop Releases:** [`.github/workflows/desktop-release.yml`](./.github/workflows/desktop-release.yml) builds and publishes platform installers using the version defined in [`composeApp/Configuration/DesktopVersion.properties`](./composeApp/Configuration/DesktopVersion.properties).

Built with Kotlin Multiplatform and Compose Multiplatform.

## Contributing

Please read [`CONTRIBUTING.md`](./CONTRIBUTING.md) before opening issues or pull requests.

## License

[GNU General Public License v3.0](./LICENSE)
