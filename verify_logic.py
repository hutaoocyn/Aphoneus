#!/usr/bin/env python3
import json

def test_range_ordering():
    def determine_write_sequence(policy, min_khz, max_khz, cur_max_khz):
        lo = min(min_khz, max_khz)
        hi = max(min_khz, max_khz)
        base = f"/sys/devices/system/cpu/cpufreq/{policy}"
        if hi >= cur_max_khz:
            return [
                (f"{base}/scaling_max_freq", str(hi)),
                (f"{base}/scaling_min_freq", str(lo))
            ]
        else:
            return [
                (f"{base}/scaling_min_freq", str(lo)),
                (f"{base}/scaling_max_freq", str(hi))
            ]

    # Test widening
    seq_w = determine_write_sequence("policy0", 600000, 1800000, 1200000)
    assert seq_w[0] == ("/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq", "1800000")
    assert seq_w[1] == ("/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq", "600000")

    # Test narrowing
    seq_n = determine_write_sequence("policy0", 400000, 1000000, 2400000)
    assert seq_n[0] == ("/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq", "400000")
    assert seq_n[1] == ("/sys/devices/system/cpu/cpufreq/policy0/scaling_max_freq", "1000000")
    print("✓ Range Ordering tests passed!")

def test_profile_serialization():
    profile = {
        "id": "profile_gaming_01",
        "name": "Sustained Gaming",
        "clusters": [
            {"policy": "policy0", "minFreqKHz": 1200000, "maxFreqKHz": 1800000, "governor": "schedutil"},
            {"policy": "policy4", "minFreqKHz": 1500000, "maxFreqKHz": 2400000, "governor": "performance"}
        ],
        "gpuMinFreqKHz": 600000,
        "gpuMaxFreqKHz": 900000,
        "gpuGovernor": "msm-adreno-tz",
        "uclampMin": 512,
        "uclampMax": 1024
    }
    json_str = json.dumps(profile)
    loaded = json.loads(json_str)
    assert loaded["id"] == profile["id"]
    assert len(loaded["clusters"]) == 2
    assert loaded["uclampMin"] == 512
    print("✓ Profile serialization tests passed!")

def test_cluster_parsing():
    avail_freqs = "300000 576000 748800 998400 1200000 1516800"
    freqs = sorted(list(set([int(x) for x in avail_freqs.split()])))
    assert len(freqs) == 6
    assert freqs[0] == 300000
    assert freqs[-1] == 1516800

    # Fallback when empty
    avail_empty = ""
    min_str = "800000"
    max_str = "2400000"
    fallback_freqs = sorted(list(set([int(x) for x in [min_str, max_str] if x])))
    assert fallback_freqs == [800000, 2400000]
    print("✓ Cluster parsing & fallback tests passed!")

def test_battery_power_calculation():
    raw_current = -650000.0  # uA discharge
    raw_voltage = 4150000.0  # uV
    status = "Discharging"

    current_ma = raw_current / 1000.0
    voltage_mv = raw_voltage / 1000.0
    if status == "Discharging" and current_ma > 0:
        current_ma = -current_ma
    
    power_mw = abs(current_ma) * (voltage_mv / 1000.0)
    assert current_ma == -650.0
    assert voltage_mv == 4150.0
    assert round(power_mw, 1) == round(650.0 * 4.15, 1)
    print("✓ Battery power & sign normalization tests passed!")

if __name__ == "__main__":
    test_range_ordering()
    test_profile_serialization()
    test_cluster_parsing()
    test_battery_power_calculation()
    print("\nALL LOGIC VERIFICATION TESTS PASSED SUCCESSFULLY!")
