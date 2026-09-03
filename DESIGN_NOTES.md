# Aphoneus — Design Notes & UI/UX Architecture

## 1. Architectural Philosophy & Product Job
**Aphoneus** is a deterministic, production-grade root utility engineered to expose and govern raw silicon topology, DVFS scheduling, thermal mitigation mechanics, and battery power states with zero daemon overhead and surgical sysfs precision.

### The Problem in Legacy Root Apps
Legacy root apps (e.g., Kernel Adiutor, Trickster MOD, old EXKM clones) suffered from three catastrophic UX failures:
1. **The "Overclocking" Lie**: Advertising frequency pinning as "overclocking", deceiving users into believing voltage or hardware frequency limits were being surpassed.
2. **Cognitive Overload & Border Soup**: Hundreds of raw, unorganized sliders with nested borders, neon glow cards, and erratic terminology that panicked everyday power users.
3. **Silent Reversion & Lack of Verification**: When vendor daemons (`perfd`, `thermal-engine`, `mi_thermald`) reverted sysfs nodes, the UI continued to display stale user values.

---

## 2. Navigation Architecture & Reference Image Alignment (2986.jpg)
The navigation architecture directly adapts the **Floating Segmented Capsule Dock** shown in the reference design (`2986.jpg`):

### Anatomical Breakdown
1. **Floating Capsule Container**:
   * Centered horizontally with `20.dp` screen gutters.
   * Elevated with an authentic 16dp depth shadow and a single 1dp boundary stroke (`#2B3036`), completely eliminating fuzzy neon glow halos.
   * Dynamically tracks `WindowInsets.navigationBars` to guarantee **zero collision** with the Android 3-button bar or modern 16dp Gesture Pill.
2. **5-Slot Segmented Layout**:
   * **Slot 0 (Home / Dashboard)**: Live SoC utilization, cluster telemetry, real-time battery drain (mA / mW), highest thermal zone reading.
   * **Slot 1 (Explore / Modes)**: High-level preset selection (Performance, Balanced, Battery Saver) designed for low cognitive load (ADHD accommodation).
   * **Slot 2 (Center Illuminated Action Pill)**: A dedicated 58x44dp capsule with vibrant accent coloring (`#6366F1`) housing the `SyncAlt` mode cycling action. Enables rapid 1-tap cyclic switching (*Balanced → Performance → Battery Saver → Balanced*) without entering menus.
   * **Slot 3 (Analyze / Custom Tuning)**: Granular per-cluster discrete sliders, governor pickers, and profile export/import.
   * **Slot 4 (Tools / Diagnostics)**: Verified capability report, root environment detector, and reboot persistence watchdog installer.
3. **Active-State Clarity**:
   * Selected tabs transition smoothly to an illuminated capsule background (`#262938`) with prominent accent icon and label tint (`#6366F1`), exactly matching the segmented active state clarity from `2986.jpg`.

---

## 3. Color & Surface Lighting System (Anti-Slop Compliance)
Adhering strictly to the APEX Design Slop Firewall:
* **No AMOLED Pitch Black (#000000)**: Replaced with a 3-tier deep-neutral slate foundation:
  * `Surface Canvas` (`#121417`): Base window background.
  * `Surface Container` (`#1A1D21`): Card modules and metrics containers.
  * `Surface Container Elevated` (`#22262B`): Floating dock and dialogs.
* **No Neon Haze or Cyan-Violet Gradients**:
  * Action Accent: Refined Indigo (`#6366F1`)
  * Nominal Status: Controlled Cyan (`#06B6D4`)
  * Warning Status: Amber (`#F59E0B`)
  * Critical / Destructive: Rust Red (`#EF4444`)
  * Success / Nominal: Emerald (`#10B981`)

---

## 4. Typography & Numeric Jitter Elimination
* **OpenType Tabular Figures**:
  * Every telemetry reading (`kHz`, `MHz`, `°C`, `mA`, `mV`, `mW`) uses `fontFeatureSettings = "tnum, lnum"`.
  * Digits take identical horizontal advance widths, completely eliminating the visual "dancing jitter" that plagues root telemetry apps during 1-second polling passes.
* **Typographic Contrast**:
  * Strong contrast ratio exceeding 1.25 between steps (Headline 24sp, Title 20sp/16sp, Body 15sp/14sp, Labels 13sp/11sp).

---

## 5. Accessibility, Insets & Motor Ergonomics (WCAG 2.2 AA)
* **Touch Target Primacy (SC 2.5.8)**:
  * Every interactive touch target has a minimum bounding box of `48.dp x 48.dp`.
  * Sliders are flanked by dual discrete stepper buttons `[-]` and `[+]` so users with motor tremors or one-handed grips can step frequencies without micro-dragging.
* **Progressive Disclosure (ADHD / Cognitive Pacing)**:
  * Users can manage all device performance using the 3 high-level modes on the Modes screen without ever viewing raw sysfs trees.
  * Granular per-cluster sliders are isolated to the Custom Tuning tab.
* **Safety & Panic Architecture**:
  * Global 1-tap Panic Button accessible from Dashboard and notification to restore the pristine stock boot snapshot in $\le 1000\text{ ms}$.
  * High-risk actions (Performance lock, thermal relaxation) require explicit two-step confirmation dialogs.
  * Non-disableable hard thermal watchdog constantly monitors silicon temperatures and automatically reverts to Balanced if thresholds ($\ge 85^\circ\text{C}$) are breached.
