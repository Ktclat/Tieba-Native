"""Run the installed app's Pixel 9 gesture and discussion checks and collect their screenshots."""

import argparse
from pathlib import Path
from subprocess import CalledProcessError

from capture_android_ui import PACKAGE, adb, configure_phone


def run(test_apk, output):
    output.mkdir(parents=True, exist_ok=True)
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != b"1":
        raise RuntimeError("This review requires a disposable emulator")
    configure_phone()
    adb("shell", "pm", "clear", PACKAGE)
    adb("install", "-r", "-g", str(test_apk), timeout=120)
    classes = [PACKAGE + ".ui.PixelBackGestureTest", PACKAGE + ".ui.page.thread.PixelDiscussionReviewTest"]
    report = adb("shell", "am", "instrument", "-w", "-r", "-e", "class", ",".join(classes),
                 PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner", timeout=300).decode()
    (output / "on-device-tests.txt").write_text(report)
    directory = "files/phone-review"
    # Preserve all completed gesture stages even when a later device assertion fails.
    try:
        files = adb("shell", "run-as", PACKAGE, "ls", directory).decode().splitlines()
    except CalledProcessError:
        files = []
    for name in files:
        if Path(name).name == name and (name.endswith(".png") or name == "gestures.tsv"):
            (output / name).write_bytes(adb("exec-out", "run-as", PACKAGE, "cat", directory + "/" + name))
    print(report, flush=True)
    if "OK (2 tests)" not in report:
        raise RuntimeError("Android gesture/discussion instrumentation failed")
    expected = {"discussion-topic.png", "discussion-preview.png", "discussion-expanded.png", "gestures.tsv"}
    expected.update(f"{name}-{stage}.png" for name in
                    ("tab-left-cancel", "tab-right-commit", "page-right-cancel", "page-left-commit")
                    for stage in ("before", "drag", "settled"))
    missing = expected.difference(files)
    if missing:
        raise RuntimeError(f"Missing installed screenshots: {sorted(missing)}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("test_apk", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    run(args.test_apk, args.output)
