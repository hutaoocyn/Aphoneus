# Aphoneus

> Production-grade, root-only Android system utility for CPU/GPU frequency scaling, DVFS power orchestration, and thermal safety management.

---

## Highlights

* **100% Dynamic Hardware Discovery**: Never assumes cluster counts (works on 2, 3, 4, 5+ cluster topologies). Probes OPP tables from `/sys/devices/system/cpu/cpufreq/policy*` at runtime.
* **Verified Write-Back Engine**: Every privileged sysfs mutation is read-back verified. Clamped or rejected writes are surfaced immediately.
* **Bit-for-Bit Pristine Snapshot & Panic Reset**: Captures a pristine boot snapshot before any mutation. Global Panic Reset restores stock state in $\le 1000\text{ ms}$ (2 taps maximum).
* **Non-Disableable Hard Thermal Guard**: Hard watchdog constantly polls thermal zones; auto-reverts to Balanced if SoC temperature exceeds $85^\circ\text{C}$ or skin exceeds $45^\circ\text{C}$.
* **Floating Segmented Capsule Dock (2986.jpg Reference)**: Custom Compose navigation dock with active-state clarity, center quick-cycle capsule, and complete clearance of `WindowInsets.navigationBars`.
* **Zero Network Permissions**: `android.permission.INTERNET` is completely omitted. Zero telemetry, zero analytics.
* **Modern Android 15/16 (TargetSdk 36)**: Compliant with edge-to-edge mandatory enforcement, 16KB memory page size alignment, and `specialUse` Foreground Service types.

---

## Project Structure

```
aphoneus/
├── app/
│   ├── build.gradle.kts          # TargetSdk 36, Compose BOM, libsu 6.0.0
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   # Zero-network, specialUse FGS
│       │   └── java/com/aphoneus/
│       │       ├── root/             # libsu RootService, FileSystemManager, verified writes
│       │       ├── discovery/        # Dynamic cluster, GPU, thermal, and battery probes
│       │       ├── model/            # Cluster, telemetry, snapshot, and profile models
│       │       ├── state/            # SnapshotManager, RevertEngine, DataStore repository
│       │       ├── modes/            # Performance, Balanced, Battery Saver, Custom strategies
│       │       ├── service/          # ModeForegroundService, ThermalWatchdog, TileService
│       │       └── ui/               # Jetpack Compose UI with floating capsule dock
│       └── test/java/com/aphoneus/   # Unit tests with FakeFileSystemManager
├── gradle/libs.versions.toml         # Gradle Version Catalog
├── DESIGN_NOTES.md                   # UI/UX rationale, anti-slop audit, ergonomics
└── CAPABILITIES.md                   # Exhaustive sysfs nodes & mitigation matrix
```

---

## Building the Project

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

## Requirements
* Rooted Android device running **KernelSU**, **APatch**, or **Magisk** (v26.0+).
* Android 8.0 (API 26) up to Android 16 (API 36).
