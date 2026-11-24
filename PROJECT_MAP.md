# Project Map: NuvioDesktop (2-sa/Nuvio Fork — Native PC Adaptation)

## [TECH_STACK]
- **Runtime / Language:** Kotlin Multiplatform (JDK 17) / C++17 (`player_bridge`) / WebView2 (`player-ui/`)
- **Framework & Core Libraries:** Compose Multiplatform Desktop, Ktor, Supabase-kt, libmpv
- **Storage / External Services:** GitHub Releases (`2-sa/Nuvio`), Optional: Supabase, TMDB, Trakt, Simkl, MDBList, Sentry
- **Verification Commands (GitHub Actions ONLY — never run Gradle/tests locally on user PC):**
  - CI Desktop Tests: `.github/workflows/desktop-tests.yml` (`workflow_dispatch`, default filter `com.nuvio.app.core.ui.*`, or `all`)
  - CI Desktop Release: `.github/workflows/desktop-release.yml` (`workflow_dispatch`, `mode=publish`, `target=windows`)

## [DECISIONS & ASSUMPTIONS]
- Fork repository `2-sa/Nuvio` is live with automated Windows `.msi` GitHub Releases (`0.1.42-alpha` pushed).
- All Gradle builds and unit tests MUST run on GitHub Actions (`.github/workflows/desktop-tests.yml` / `.github/workflows/desktop-release.yml`), never locally on the user's PC.
- Target experience is a **native Desktop/PC media app** (mouse/trackpad/keyboard-first, desktop windowing, proper information density, native dialogs/context menus instead of mobile bottom sheets/touch-drag/ExoPlayer leftovers).
- Structured in 5 independent, verifiable milestones so each slice compiles and commits cleanly.

## [SYSTEM_FLOW]
1. **Milestone 1 (Desktop Settings & ExoPlayer/Mobile Cleanup):** Hide Android ExoPlayer settings (`DecoderPriority`, ExoPlayer-gated `Downmix`, phone audio delay) on Desktop, expose hidden P2P profile & cache size settings on Desktop, add Desktop UI Scale setting, and replace mobile `"Tap..."` / `"sp"` strings.
2. **Milestone 2 (Desktop Player Ergonomics — `player-ui/`):** Swap mouse wheel to Volume by default (and `Shift/Ctrl+Wheel` or seekbar hover for Seek), fix volume boost keyboard/wheel clamp (`>100%`), add hover time tooltip on seekbar + tooltips on icon buttons, add right-click context menu, and update window title during playback.
3. **Milestone 3 (Pointer, Hover, Carousels & Context Menus):** Add `PointerIcon.Hand` on interactive controls, add hover left/right navigation arrows & wheel-scroll to `NuvioShelfSection` / `HomeHeroSection` / `DetailSeriesContent` rows, replace right-click mobile zoom overlays with desktop context menus, and wrap metadata/hashes/errors in `SelectionContainer`.
4. **Milestone 4 (Shell, Navigation, Shortcuts & Windowing):** Remove 5s mobile splash delay & `Esc` exit-confirmation popup on Home, add Forward history (`Alt+Left/Right` & Mouse Forward button), add global shortcuts (`Ctrl+F` Search, `Ctrl+,` Settings, `Ctrl+1..4` tabs), set minimum window size, and upgrade `DesktopHoverSidebar`.
5. **Milestone 5 (Density, Modals & Multi-Pane Settings):** Replace forced `1.08f–1.18f` UI/font upscaling with crisp 1.0x default (configurable in Settings), convert `NuvioModalBottomSheet` to centered desktop dialogs/popovers on `isDesktop`, and make Settings a persistent two-pane master-detail view with an always-visible search box.

## [ARCHITECTURE]
- `composeApp/src/desktopMain/` — Desktop windowing, shortcuts, native player bridge, and `resources/player-ui/`.
- `composeApp/src/commonMain/kotlin/com/nuvio/app/core/ui/` — Shared theme, density, navigation, shelf carousels, dialogs/sheets, and pointer modifiers.
- `composeApp/src/commonMain/kotlin/com/nuvio/app/features/settings/` — Settings screens, playback/video/audio/P2P/layout options.

## [TEAM & OWNERSHIP]
- **Pod 1 (Core):**
  - `architect-scout` (`0b2d1390-0cdd-4a5d-ac12-9b1c2eae1edf`, read-only) — Full Desktop audit complete.
  - `backend-core` (`thread:delegated-task:command%3Amcp%3A07b76708-3a0c-49f3-b38f-f93f3ed0e4b1%3Adelegate-task%3Acbd9dc05-d386-49a4-ae89-f9bcef789c6c`) — Owns: `HeroStretchOverscroll.kt`, `PlatformKeyboardNavigation.desktop.kt`, `ControllerFocusEngine.kt`, `DisintegrationEffect.kt`, `PlayerScreenRuntimeUi.kt`.
- **Pod 2 (Delivery):**
  - `player-ui` (`thread:delegated-task:command%3Amcp%3Ab1e6249f-fbf4-4256-9d99-c966102af4ec%3Adelegate-task%3A020b9077-b5cb-45a6-9a2d-a07a21fb58ad`) — Owns: `composeApp/src/desktopMain/resources/player-ui/*`.
  - `frontend-ui` (`thread:delegated-task:command%3Amcp%3A3ffdf750-3122-48ac-b8b8-b0921830bedc%3Adelegate-task%3A7f0361f5-97ee-43f4-8189-38d2d60f5684`) — Owns: `StreamsTabletLayout.kt`, `DesktopDetailHero.kt`, `TabletDetailHero.kt`, `ProfileMeshBackground.kt`, `ShelfComponents.kt`, `DetailSeriesContent.kt`, `ProfileSelectionScreen.kt`, `DesktopNavigationBar.kt`, `HomeHeroSection.kt`.
  - `qa-reviewer` (`thread:delegated-task:command%3Amcp%3A71c25c6c-9ae0-47ec-b13d-9e095709dca7%3Adelegate-task%3Af9b1c223-d0cd-45d2-a37c-a510002ca681`) — Owns: `desktopTest/`, `commonTest/` + Gradle/Node/XML verification & diff audit.

## [ORPHANS & PENDING]
- [x] Slice 0: Retarget fork to `2-sa/Nuvio`, make CI secrets optional, and trigger Windows `.msi` release (`0.1.38-alpha`).
- [x] Milestone 1: Desktop Settings & ExoPlayer/Mobile Leftover Cleanup + `arabic-ui` localization across 226 Arabic strings.
- [x] Milestone 2: Desktop Player (`player-ui/`) Mouse/Keyboard, Seekbar Hover Tooltip & Volume Boost Overhaul.
- [x] Milestone 3: Instant Desktop Right-Click Context Menus (`DesktopContextMenuPopup`), Carousel Scroll Arrows & `PointerIcon.Hand`.
- [x] Milestone 4: Desktop Shell, Forward/Back History Navigation, Global Shortcuts (`Ctrl+F`, `Ctrl+,`) & Window Minimum Size.
- [x] Milestone 5: Desktop 1.0x Crisp Density, Centered Modal Dialogs (replacing `ModalBottomSheet`) & Always-Visible Settings Search.
- [x] 5-Agent Team Audit & Fixes (`0.1.43-alpha` prep): Fix Home Hero `NaN` crash (`HeroStretchOverscroll.kt`), dynamic button-anchored player popovers (`player-ui/`), RTL wheel/drag scroll & zero-dimension guards, arrow-key text input guard, `HeroStretchOverscrollTest.kt`, and `.github/workflows/desktop-tests.yml`.
- **Blockers:** None
