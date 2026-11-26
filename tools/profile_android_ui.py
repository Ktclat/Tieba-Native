"""Compare warm tab switches and scrolling on a Pixel 9-sized offline Android emulator."""

import argparse
import csv
import hashlib
import io
import json
import math
from pathlib import Path
import re
import sqlite3
import tempfile
import time
from xml.etree import ElementTree

from capture_android_ui import PACKAGE, PHONE_HEIGHT, PHONE_WIDTH, adb, configure_phone, find_label, hierarchy, save_diagnostic, tap


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
    return adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivityV2", timeout=60).decode()


def press(point):
    adb("shell", "input", "tap", *(str(v) for v in point))


def swipe(reverse):
    top, bottom = PHONE_HEIGHT // 3, PHONE_HEIGHT * 3 // 4
    start, end = (top, bottom) if reverse else (bottom, top)
    x = str(PHONE_WIDTH // 2)
    adb("shell", "input", "swipe", x, str(start), x, str(end), "380")
    time.sleep(0.36)


def seed_history():
    # Only called on a freshly installed app in a disposable, offline emulator. Keep both builds'
    # production history list and pagination code on the same 180 local records and viewport.
    adb("shell", "am", "force-stop", PACKAGE)
    prefix = ["run-as", PACKAGE]
    data = "databases/tblite.db"
    names = adb("shell", *prefix, "ls", "databases").decode().split()
    with tempfile.TemporaryDirectory() as folder:
        path = Path(folder) / "tblite.db"
        for suffix in ("", "-wal", "-shm", "-journal"):
            if path.name + suffix in names:
                Path(str(path) + suffix).write_bytes(adb("exec-out", *prefix, "cat", data + suffix))
        with sqlite3.connect(path) as db:
            now = int(time.time() * 1000)
            db.executemany("INSERT INTO history(title,data,type,timestamp,count,username) VALUES(?,?,2,?,1,?)",
                           [(f"滚动测试 {i:03}", str(900000 + i), now - 60_000 * i, "离线示例")
                            for i in range(180)])
            db.commit()
            db.execute("PRAGMA wal_checkpoint(TRUNCATE)").fetchone()
            if db.execute("PRAGMA journal_mode=DELETE").fetchone()[0] != "delete":
                raise RuntimeError("History fixture still depends on a separate write-ahead log")
        db.close()
        remote = "/data/local/tmp/tieba-profile-history.db"
        private = "databases/benchmark-history.tmp"
        original = path.read_bytes()
        try:
            adb("push", str(path), remote)
            # The shell UID can read the pushed file. run-as owns the destination in app storage.
            # Wait for the full write and verify it before swapping the stopped app's database.
            adb("shell", f"cat {remote} | run-as {PACKAGE} sh -c 'cat > {private}'")
            staged = adb("exec-out", *prefix, "cat", private)
            if staged != original:
                raise RuntimeError(f"Transferred history DB differs: {len(staged)} / {len(original)} bytes")
            adb("shell", *prefix, "rm", "-f", data + "-wal", data + "-shm", data + "-journal")
            adb("shell", *prefix, "mv", private, data)
            check = Path(folder) / "installed.db"
            check.write_bytes(adb("exec-out", *prefix, "cat", data))
            with sqlite3.connect(check) as db:
                count = db.execute("SELECT COUNT(*) FROM history WHERE type=2 AND title LIKE '滚动测试 %'").fetchone()[0]
                integrity = db.execute("PRAGMA quick_check").fetchone()[0]
            if check.read_bytes() != original or count != 180 or integrity != "ok":
                raise RuntimeError(f"Installed history DB failed verification: count={count}, integrity={integrity}")
            print(f"Verified all {count} rows in the installed app's DB", flush=True)
        finally:
            adb("shell", "rm", "-f", remote)



def visible_history(output):
    tree = hierarchy()
    rows = []
    for node in tree.iter("node"):
        if node.get("package") == PACKAGE:
            # Clickable Compose rows may merge the headline with its author.
            label = node.get("text", "") + "\n" + node.get("content-desc", "")
            match = re.search(r"(?:^|[,，\n]\s*)滚动测试 (\d{3})(?:$|[,，\n])", label)
            if match:
                rows.append(int(match.group(1)))
    if not rows:
        raise RuntimeError("Could not find offline history rows")
    output.with_suffix(".xml").write_bytes(ElementTree.tostring(tree))
    output.with_suffix(".png").write_bytes(adb("exec-out", "screencap", "-p"))
    return min(rows)


def run_block(apk, directory, readings):
    directory.mkdir(parents=True, exist_ok=True)
    if "package:" + PACKAGE in adb("shell", "pm", "list", "packages", PACKAGE).decode().splitlines():
        adb("uninstall", PACKAGE, timeout=60)
    adb("install", "-g", str(apk), timeout=120)
    launch_report = launch()
    (directory / "cold-start.txt").write_text(launch_report)
    find_label("随便看看", recover_launcher=True)
    profile = find_label("我")
    press(profile)
    find_label("主题选择")
    home = find_label("首页")
    if min(home[1], profile[1]) < PHONE_HEIGHT * 0.75:
        raise RuntimeError("Expected both tab targets in the bottom navigation")
    press(home)
    find_label("随便看看")

    def switch_tabs():
        press(profile)
        time.sleep(0.15)
        press(home)
        time.sleep(0.32)

    for _ in range(3):
        switch_tabs()
    # An immediate page switch draws only a handful of frames; repeat to sample its tail.
    for repetition in range(1, 13):
        readings["switch_tabs"].append(measure(directory, "switch_tabs", repetition, switch_tabs))
    find_label("随便看看")
    (directory / "switch-tabs.json").write_text(json.dumps([summarize(batch) for batch in readings["switch_tabs"]]))

    seed_history()
    launch()
    tap("我")
    tap("浏览记录")
    find_label("滚动测试 000")
    top = visible_history(directory / "history-top")
    swipe(False)
    swipe(False)
    lower = visible_history(directory / "history-scrolled")
    if lower <= top:
        raise RuntimeError(f"History did not scroll: {top} -> {lower}")
    swipe(True)
    swipe(True)
    if visible_history(directory / "history-restored") >= lower:
        raise RuntimeError("History did not scroll back")
    for _ in range(2):
        swipe(False)
        swipe(True)
    for repetition in range(1, 4):
        up = measure(directory, "history_scroll_up", repetition, lambda: swipe(False))
        down = measure(directory, "history_scroll_down", repetition, lambda: swipe(True))
        readings["history_scroll"].append(up + down)


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
    configure_phone()
    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    adb("shell", "svc", "wifi", "disable")
    (args.output / "display.txt").write_bytes(adb("shell", "dumpsys", "display"))
    result = {
        "candidate_sha": args.candidate_sha,
        "baseline_sha": args.baseline_sha if baseline else None,
        "apk_sha256": {key: hashlib.sha256(apk.read_bytes()).hexdigest() for key, apk in apks.items()},
        "device": adb("shell", "getprop", "ro.build.fingerprint").decode().strip(),
        "screen": {"size": adb("shell", "wm", "size").decode().strip(),
                   "density": adb("shell", "wm", "density").decode().strip(),
                   "font_scale": adb("shell", "settings", "get", "system", "font_scale").decode().strip()},
        "history_source": "180 synthetic local visits with no remote media",
        "animation_scales": {key: adb("shell", "settings", "get", "global", key).decode().strip()
                             for key in ("animator_duration_scale", "transition_animation_scale", "window_animation_scale")},
        "order": order,
        "blocks": [],
    }
    combined = {version: {"switch_tabs": [], "history_scroll": []} for version in apks}
    errors = []
    for index, version in enumerate(order):
        directory = args.output / f"{index + 1}-{version}"
        print(f"Profiling {directory.name}", flush=True)
        readings = {"switch_tabs": [], "history_scroll": []}
        block = {"version": version}
        try:
            run_block(apks[version], directory, readings)
        except Exception as error:
            block["error"] = str(error)
            errors.append(f"{directory.name}: {error}")
            print(f"Measurement failed: {errors[-1]}", flush=True)
            for filename, command in {
                "failure.png": ["exec-out", "screencap", "-p"],
                "last-window.xml": ["exec-out", "cat", "/sdcard/window.xml"],
                "gfxinfo.txt": ["shell", "dumpsys", "gfxinfo", PACKAGE, "framestats"],
            }.items():
                save_diagnostic(directory, filename, *command)
        launch_file = directory / "cold-start.txt"
        if launch_file.exists():
            match = re.search(r"^TotalTime:\s*(\d+)", launch_file.read_text(), re.MULTILINE)
            block["cold_start_to_initial_display_ms"] = int(match.group(1)) if match else None
        for scenario, batches in readings.items():
            block[scenario] = [summarize(batch) for batch in batches]
            for batch in batches:
                combined[version][scenario].extend(batch)
        result["blocks"].append(block)
        (args.output / "measurements.json").write_text(json.dumps(result, indent=2) + "\n")
    result["summary"] = {version: {scenario: summarize(rows) for scenario, rows in readings.items() if rows}
                         for version, readings in combined.items()}
    (args.output / "measurements.json").write_text(json.dumps(result, indent=2) + "\n")
    print(json.dumps(result["summary"], indent=2), flush=True)
    if errors:
        raise RuntimeError(" | ".join(errors))


if __name__ == "__main__":
    main()
