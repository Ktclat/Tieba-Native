"""Run the installed app's Pixel 9 gesture, discussion, forum and gallery checks and collect their screenshots."""

import argparse
from pathlib import Path
from subprocess import CalledProcessError

from capture_android_ui import PACKAGE, adb, configure_phone, disconnect_emulator


def run(test_apk, output):
    output.mkdir(parents=True, exist_ok=True)
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != b"1":
        raise RuntimeError("This review requires a disposable emulator")
    configure_phone()
    disconnect_emulator()
    adb("shell", "pm", "clear", PACKAGE)
    adb("install", "-r", "-g", str(test_apk), timeout=120)
    classes = [PACKAGE + ".ui.PixelBackGestureTest", PACKAGE + ".ui.page.thread.PixelDiscussionReviewTest",
               PACKAGE + ".ui.PixelForumGalleryTest", PACKAGE + ".ui.PixelSearchNavigationTest",
               PACKAGE + ".ui.page.thread.PixelLocalReplyRulesTest"]
    report = adb("shell", "am", "instrument", "-w", "-r", "-e", "class", ",".join(classes),
                 PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner", timeout=360).decode()
    (output / "on-device-tests.txt").write_text(report)
    directory = "files/phone-review"
    # Preserve completed screenshots and the failing window even if a later assertion fails.
    try:
        files = adb("shell", "run-as", PACKAGE, "ls", directory).decode().splitlines()
    except CalledProcessError:
        files = []
    for name in files:
        if Path(name).name == name and (name.endswith(".png") or name in {"gestures.tsv", "gallery-controls.tsv"}):
            (output / name).write_bytes(adb("exec-out", "run-as", PACKAGE, "cat", directory + "/" + name))
    print(report, flush=True)
    if "OK (8 tests)" not in report:
        raise RuntimeError("Android phone review instrumentation failed")
    expected = {"discussion-topic.png", "discussion-preview.png", "discussion-expanded.png", "gestures.tsv",
                "gallery-first.png", "gallery-second.png", "gallery-share.png", "gallery-controls.tsv",
                "forum-normal.png", "forum-missing.png", "forum-good.png", "forum-sort.png", "forum-categories.png",
                "search-empty.png", "search-chinese.png", "navigation-standard.png", "navigation-setting.png",
                "navigation-floating-home.png", "navigation-floating-explore.png", "navigation-floating-profile.png",
                "local-block-settings.png", "local-block-comment.png", "local-block-invalid.png", "local-block-forum.png",
                "local-block-replies.png", "local-block-replies-restored.png"}
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
