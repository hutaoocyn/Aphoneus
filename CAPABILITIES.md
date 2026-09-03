# Aphoneus — Kernel Sysfs Capabilities & Hardware Contract

This document inventories every kernel sysfs and procfs node interacted with by Aphoneus, detailing its purpose, access mode, risk level, and revert strategy.

---

## 1. CPU Frequency & Topology (`cpufreq`)

| Sysfs Node Path | Purpose | Access | Risk Level | Revert Strategy |
| :--- | :--- | :--- | :--- | :--- |
| `/sys/devices/system/cpu/cpufreq/policy*/related_cpus` | Discovers CPU cores belonging to each clock cluster. | Read-only | None | N/A |
| `/sys/devices/system/cpu/cpufreq/policy*/scaling_available_frequencies` | Reads kernel OPP table (discrete frequency steps). | Read-only | None | N/A |
| `/sys/devices/system/cpu/cpufreq/policy*/cpuinfo_min_freq` & `cpuinfo_max_freq` | Fallback range boundaries when OPP table is stripped by OEM. | Read-only | None | N/A |
| `/sys/devices/system/cpu/cpufreq/policy*/scaling_available_governors` | Lists supported governors (`schedutil`, `performance`, `powersave`). | Read-only | None | N/A |
| `/sys/devices/system/cpu/cpufreq/policy*/scaling_governor` | Sets cluster CPU governor. | Write-verified | Low | Restored from pristine boot snapshot. |
| `/sys/devices/system/cpu/cpufreq/policy*/scaling_min_freq` | Lower frequency boundary for cluster DVFS scaling. | Write-verified | Moderate | Restored from pristine boot snapshot. Written in safe order to prevent `-EINVAL`. |
| `/sys/devices/system/cpu/cpufreq/policy*/scaling_max_freq` | Upper frequency boundary for cluster DVFS scaling. | Write-verified | Moderate | Restored from pristine boot snapshot. Written in safe order to prevent `-EINVAL`. |
| `/sys/devices/system/cpu/cpufreq/policy*/schedutil/*rate_limit_us` | Tunes responsiveness rate limits for schedutil governor. | Write-verified | Low | Restored from pristine boot snapshot. |
| `/sys/devices/system/cpu/cpu*/online` | CPU core hotplugging (brought online for Performance mode). | Write-verified | High | Soft-locked on Samsung/Exynos to prevent kernel panic. Default uses cpuset restriction. |

---

## 2. GPU Subsystem (Qualcomm KGSL & ARM Mali)

| Sysfs Node Path | Purpose | Access | Risk Level | Revert Strategy |
| :--- | :--- | :--- | :--- | :--- |
| `/sys/class/kgsl/kgsl-3d0/devfreq/available_frequencies` | Qualcomm Adreno GPU frequency steps. | Read-only | None | N/A |
| `/sys/class/kgsl/kgsl-3d0/devfreq/min_freq` & `max_freq` | Clamps GPU clock scaling boundaries. | Write-verified | Moderate | Restored to stock minimum/maximum from snapshot. |
| `/sys/class/kgsl/kgsl-3d0/devfreq/governor` | Adreno governor (`msm-adreno-tz`, `performance`, `powersave`). | Write-verified | Moderate | Restored to `msm-adreno-tz` or stock snapshot value. |
| `/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage` | Live GPU engine load metric. | Read-only | None | N/A |
| `/sys/class/kgsl/kgsl-3d0/force_clk_on` | Forces GPU clock rail on during performance lock. | Write-verified | High | Reverted to `0` on Balanced mode. |
| `/sys/class/devfreq/*.mali/*` | ARM Mali GPU devfreq nodes (MediaTek / Exynos). | Write-verified | Moderate | Restored to stock devfreq values. |

---

## 3. Thermal Mitigation Subsystem

| Sysfs Node Path | Purpose | Access | Risk Level | Revert Strategy |
| :--- | :--- | :--- | :--- | :--- |
| `/sys/class/thermal/thermal_zone*/type` | Sensor identifier (CPU, GPU, battery, skin). | Read-only | None | N/A |
| `/sys/class/thermal/thermal_zone*/temp` | Real-time sensor temperature reading (milli-Celsius). | Read-only | None | Monitored by hard watchdog; triggers emergency revert at $\ge 85^\circ\text{C}$. |
| `/sys/class/thermal/thermal_zone*/trip_point_*` | Kernel temperature trip boundaries for thermal throttling. | Read-only | Extreme | Read-only by default; software overrides strictly capped. |
| `/sys/class/thermal/cooling_device*/cur_state` | State of kernel cooling mitigators. | Read-only | None | Monitored during throttling telemetry. |
| `cmd thermalservice override-status` | Framework thermal status override. | Exec | High | Reverted with `cmd thermalservice reset`. |

---

## 4. Memory, Scheduler & Power Framework

| Sysfs / Command Node | Purpose | Access | Risk Level | Revert Strategy |
| :--- | :--- | :--- | :--- | :--- |
| `/proc/sys/kernel/sched_util_clamp_min` & `max` | Schedutil uclamp headroom and ceiling (0..1024). | Write-verified | Low | Restored to stock values (`0` and `1024`). |
| `/dev/cpuset/top-app/cpus` | Allocates cores available to foreground application. | Write-verified | Moderate | Restored to all online cores from boot snapshot. |
| `/sys/block/zram0/comp_algorithm` | ZRAM compression algorithm auto-selection (`lz4`, `zstd`). | Write-verified | Moderate | Restored to active default on boot. |
| `/proc/sys/vm/swappiness` | Kernel swap aggressiveness (30 for Perf, 100 for Saver). | Write-verified | Low | Restored to stock (`60` or `100`). |
| `/sys/class/power_supply/battery/current_now` | Battery instantaneous current (uA). OEM sign normalized. | Read-only | None | N/A |
| `/sys/class/power_supply/battery/voltage_now` | Battery terminal voltage (uV). | Read-only | None | N/A |
| `cmd power set-fixed-performance-mode-enabled` | Android framework fixed performance mode. | Exec | Low | Reset to `false` on Balanced mode. |
| `dumpsys deviceidle force-idle deep` | Forces deep Doze idle in Battery Saver mode. | Exec | Low | Restored on screen unlock. |

---

## 5. Vendor Overriders & Mitigation Protocols

| Vendor Mechanism | Detection Node | Neutralization Action | Reversion |
| :--- | :--- | :--- | :--- |
| **Qualcomm msm_performance** | `/sys/module/msm_performance/parameters/cpu_max_freq` | Write `4294967295` (unlimited). | Restored to stock per-cluster limits. |
| **Qualcomm core_ctl** | `/sys/devices/system/cpu/cpu*/core_ctl/enable` | Write `0` (disable automatic core offlining). | Restored to `1`. |
| **MediaTek PPM** | `/proc/ppm/policy_status` | Detected and logged in Capability Report. | Handled via generic cpufreq boundaries. |
| **Vendor Daemons** (`perfd`, `thermal-engine`, `mi_thermald`) | Process table scan (`ps -A`) | Optional termination gated behind multi-step confirmation. | Daemons restarted on Balanced mode or reboot. |
