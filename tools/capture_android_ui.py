"""Capture the installed app's guest navigation and font settings on an Android emulator."""

import argparse
from pathlib import Path
import re
import subprocess
import time
from xml.etree import ElementTree

PACKAGE = "com.huanchengfly.tieba.post"


def adb(*args, timeout=30):
    return subprocess.run(["adb", *args], check=True, capture_output=True, timeout=timeout).stdout


def hierarchy():
    adb("shell", "rm", "-f", "/sdcard/window.xml")
    adb("shell", "uiautomator", "dump", "/sdcard/window.xml", timeout=18)
    return ElementTree.fromstring(adb("exec-out", "cat", "/sdcard/window.xml"))


def center(node):
    match = re.fullmatch(r"\[(\d+),(\d+)]\[(\d+),(\d+)]", node.get("bounds", ""))
    if match:
        left, top, right, bottom = map(int, match.groups())
        if right > left and bottom > top:
            return (left + right) // 2, (top + bottom) // 2
    return None


def find_label(label, timeout=45):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        try:
            tree = hierarchy()
        except (subprocess.TimeoutExpired, subprocess.CalledProcessError, ElementTree.ParseError):
            time.sleep(0.4)
            continue
        for node in tree.iter("node"):
            if node.get("package") != PACKAGE:
                continue
            labels = (node.get("text", ""), node.get("content-desc", ""))
            if any(label in re.split(r"[,，\n]\s*", value) for value in labels):
                position = center(node)
                if position:
                    return position
    raise RuntimeError(f"Could not find {label!r} in the foreground app")


def tap(label):
    x, y = find_label(label)
    adb("shell", "input", "tap", str(x), str(y))


def capture(output, name, label):
    find_label(label)
    image = adb("exec-out", "screencap", "-p")
    if not image.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError("screencap returned invalid PNG bytes")
    (output / (name + ".png")).write_bytes(image)
    print(f"Captured {name}", flush=True)


def run(apk, output):
    output.mkdir(parents=True, exist_ok=True)
    adb("install", "-r", "-g", str(apk), timeout=90)
    adb("shell", "wm", "size", "1080x2340")
    adb("shell", "wm", "density", "420")
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "wm", "dismiss-keyguard")
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivityV2", timeout=60)
    capture(output, "01-home", "随便看看")
    tap("我")
    capture(output, "02-profile", "设置")
    tap("设置")
    capture(output, "03-settings", "个性化")
    tap("个性化")
    capture(output, "04-appearance", "字体大小")
    tap("字体大小")
    capture(output, "05-font-preview", "阅读预览")
    tap("放大一级")
    capture(output, "06-font-larger", "105%")
    tap("保存字号")
    capture(output, "07-saved", "夜间模式主题")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("output", type=Path)
    args = parser.parse_args()
    try:
        run(args.apk, args.output)
    except Exception:
        args.output.mkdir(parents=True, exist_ok=True)
        for name, command in {
            "failure.png": ["exec-out", "screencap", "-p"],
            "last-window.xml": ["exec-out", "cat", "/sdcard/window.xml"],
            "crashes.txt": ["logcat", "-b", "crash", "-d", "-t", "300"],
        }.items():
            result = subprocess.run(["adb", *command], capture_output=True, timeout=30)
            if result.returncode == 0:
                (args.output / name).write_bytes(result.stdout)
        raise
