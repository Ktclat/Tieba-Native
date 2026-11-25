"""Measure rendered frames while using two offline destinations on a disposable Android emulator."""

import argparse
import csv
import hashlib
import io
import json
import math
from pathlib import Path
import subprocess
import time
from xml.etree import ElementTree

from capture_android_ui import PACKAGE, adb, center, find_label, hierarchy, labelled_nodes


def frame_rows(dump):
    rows = {}
    for block in dump.split("---PROFILEDATA---")[1::2]:
        for fields in csv.DictReader(io.StringIO(block.strip())):
            if fields.get("Flags") != "0":
                continue
            start = int(fields["IntendedVsync"])
            end = int(fields["FrameCompleted"])
            deadline = int(fields["FrameDeadline"])
            if 0 < start < end < 2**63 - 1 and start < deadline < 2**63 - 1:
                rows[start] = (end - start, end - deadline)
    return rows


def dump_frames():
    return adb("shell", "dumpsys", "gfxinfo", PACKAGE, "framestats").decode()


def measure(directory, scenario, repetition, action):
    adb("shell", "dumpsys", "gfxinfo", PACKAGE, "reset")
    previous = frame_rows(dump_frames())
    action()
    dump = dump_frames()
    name = f"{scenario}-{repetition}"
    (directory / f"{name}.txt").write_text(dump)
    rows = [row for start, row in frame_rows(dump).items() if start not in previous]
    if not rows:
        raise RuntimeError(f"No new completed app frames for {name}")
    return rows


def summarize(rows):
    durations = sorted(duration / 1_000_000 for duration, _ in rows)
    missed = sum(overrun > 0 for _, overrun in rows)
    return {
        "frames": len(rows),
        "median_ms": round(durations[math.ceil(len(rows) * 0.50) - 1], 2),
        "p95_ms": round(durations[math.ceil(len(rows) * 0.95) - 1], 2),
        "missed_deadline": missed,
        "missed_deadline_pct": round(100 * missed / len(rows), 1),
    }


def launch():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivityV2", timeout=60)


def press(point):
    adb("shell", "input", "tap", *(str(v) for v in point))


def swipe(reverse):
    # Coordinates are within the scrollable guest profile at 840 × 735 px.
    start, end = (95, 475) if reverse else (475, 95)
    adb("shell", "input", "swipe", "385", str(start), "385", str(end), "360")
    time.sleep(0.32)


def verify_navigation(visible, output):
    tree = hierarchy()
    shown = next(labelled_nodes(tree, "动态"), None) is not None
    output.with_suffix(".xml").write_bytes(ElementTree.tostring(tree))
    output.with_suffix(".png").write_bytes(adb("exec-out", "screencap", "-p"))
    if shown != visible:
        raise RuntimeError(f"Expected bottom navigation visible={visible} at {output.name}")
    positions = {}
    for label in ("浏览记录", "主题选择", "设置", "关于"):
        row = next(labelled_nodes(tree, label), None)
        if row is not None:
            positions[label] = center(row)[1]
    if not positions:
        raise RuntimeError(f"No guest profile menu items at {output.name}")
    return positions


def check_scroll(before, after, direction):
    shared = before.keys() & after.keys()
    if not any(direction * (after[label] - before[label]) > 12 for label in shared):
        raise RuntimeError(f"No visible menu row moved in the expected direction: {before} -> {after}")


def run_block(apk, directory):
    directory.mkdir(parents=True, exist_ok=True)
    if "package:" + PACKAGE in adb("shell", "pm", "list", "packages", PACKAGE).decode().splitlines():
        adb("uninstall", PACKAGE, timeout=60)
    adb("install", "-g", str(apk), timeout=120)
    adb("shell", "wm", "size", "1080x2340")
    launch()
    find_label("随便看看", recover_launcher=True)
    profile = find_label("我")
    press(profile)
    find_label("主题选择")
    home = find_label("首页")
    if min(home[1], profile[1]) < 2340 * 0.75:
        raise RuntimeError("Expected both tab targets in the bottom navigation")
    press(home)
    find_label("随便看看")

    def switch_tabs():
        press(profile)
        time.sleep(0.36)
        press(home)
        time.sleep(0.36)

    for _ in range(3):
        switch_tabs()
    readings = {"switch_tabs": [], "profile_scroll": []}
    for repetition in range(1, 4):
        readings["switch_tabs"].append(measure(directory, "switch_tabs", repetition, switch_tabs))
    find_label("随便看看")

    # The read-only guest profile has only four menu rows. Use a short, compact
    # window so these real rows can scroll far enough to hide the bottom bar.
    adb("shell", "wm", "size", "840x735")
    launch()
    press(find_label("我", recover_launcher=True))
    find_label("浏览记录")
    start = verify_navigation(True, directory / "profile-before")
    swipe(False)
    hidden = verify_navigation(False, directory / "profile-hidden")
    check_scroll(start, hidden, -1)
    swipe(True)
    restored = verify_navigation(True, directory / "profile-restored")
    check_scroll(hidden, restored, 1)

    def scroll_profile():
        swipe(False)
        swipe(True)

    for _ in range(2):
        scroll_profile()
    for repetition in range(1, 4):
        readings["profile_scroll"].append(measure(directory, "profile_scroll", repetition, scroll_profile))
    verify_navigation(True, directory / "profile-end")
    return readings


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--baseline-dir", type=Path)
    parser.add_argument("--baseline-sha", default="")
    parser.add_argument("--candidate-sha", required=True)
    parser.add_argument("--reset-emulator-app-data", required=True, action="store_true",
                        help="Uninstall and reinstall the app before each measurement block.")
    args = parser.parse_args()
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != b"1":
        raise RuntimeError("This measurement requires a disposable emulator")
    args.output.mkdir(parents=True, exist_ok=True)
    apks = {"candidate": args.apk}
    baseline = list(args.baseline_dir.glob("*.apk")) if args.baseline_dir else []
    if len(baseline) > 1:
        raise RuntimeError("Expected at most one baseline APK")
    if baseline:
        apks["baseline"] = baseline[0]
    order = ["baseline", "candidate", "candidate", "baseline"] if baseline else ["candidate", "candidate"]
    adb("shell", "wm", "density", "420")
    adb("shell", "cmd", "uimode", "night", "no")
    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    adb("shell", "svc", "wifi", "disable")
    for setting in ("animator_duration_scale", "transition_animation_scale", "window_animation_scale"):
        adb("shell", "settings", "put", "global", setting, "1")
    for setting in ("min_refresh_rate", "peak_refresh_rate"):
        adb("shell", "settings", "put", "system", setting, "60.0")
    (args.output / "display.txt").write_bytes(adb("shell", "dumpsys", "display"))
    result = {
        "candidate_sha": args.candidate_sha,
        "baseline_sha": args.baseline_sha if baseline else None,
        "apk_sha256": {key: hashlib.sha256(apk.read_bytes()).hexdigest() for key, apk in apks.items()},
        "device": adb("shell", "getprop", "ro.build.fingerprint").decode().strip(),
        "animation_scales": {key: adb("shell", "settings", "get", "global", key).decode().strip()
                             for key in ("animator_duration_scale", "transition_animation_scale", "window_animation_scale")},
        "order": order,
        "blocks": [],
    }
    combined = {version: {"switch_tabs": [], "profile_scroll": []} for version in apks}
    for index, version in enumerate(order):
        directory = args.output / f"{index + 1}-{version}"
        print(f"Profiling {directory.name}", flush=True)
        try:
            readings = run_block(apks[version], directory)
        except Exception:
            for filename, command in {
                "failure.png": ["exec-out", "screencap", "-p"],
                "last-window.xml": ["exec-out", "cat", "/sdcard/window.xml"],
                "gfxinfo.txt": ["shell", "dumpsys", "gfxinfo", PACKAGE, "framestats"],
            }.items():
                diagnostic = subprocess.run(["adb", *command], capture_output=True, timeout=30)
                if diagnostic.returncode == 0:
                    (directory / filename).write_bytes(diagnostic.stdout)
            raise
        block = {"version": version}
        for scenario, batches in readings.items():
            block[scenario] = [summarize(batch) for batch in batches]
            for batch in batches:
                combined[version][scenario].extend(batch)
        result["blocks"].append(block)
        (args.output / "measurements.json").write_text(json.dumps(result, indent=2) + "\n")
    result["summary"] = {version: {scenario: summarize(rows) for scenario, rows in readings.items()}
                         for version, readings in combined.items()}
    (args.output / "measurements.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result["summary"], indent=2), flush=True)


if __name__ == "__main__":
    main()
