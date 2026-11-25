"""Exercise guest login, navigation, font settings and persisted color choices on Android 15."""

import argparse
from collections import Counter
import json
from pathlib import Path
import re
import struct
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


def wait_for_match(description, lookup, timeout=45):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        try:
            tree = hierarchy()
        except (subprocess.TimeoutExpired, subprocess.CalledProcessError, ElementTree.ParseError):
            time.sleep(0.4)
            continue
        result = lookup(tree)
        if result is not None:
            return result
    raise RuntimeError(f"Could not find {description} in the foreground app")


def labelled_nodes(tree, label):
    for node in tree.iter("node"):
        if node.get("package") == PACKAGE and center(node):
            values = (node.get("text", ""), node.get("content-desc", ""))
            if any(label in re.split(r"[,，\n]\s*", value) for value in values):
                yield node


def find_label(label, timeout=45, recover_launcher=False):
    def position(tree):
        nonlocal recover_launcher
        if recover_launcher:
            system = [node for node in tree.iter("node") if node.get("package") == "android"]
            launcher_anr = any(node.get("resource-id") == "android:id/alertTitle" and
                               node.get("text") == "Pixel Launcher isn't responding" for node in system)
            close = next((center(node) for node in system if node.get("resource-id") == "android:id/aerr_close"), None)
            if launcher_anr and close and adb("shell", "getprop", "ro.kernel.qemu").strip() == b"1":
                recover_launcher = False
                print("Recovering from the emulator's Pixel Launcher boot ANR", flush=True)
                adb("shell", "input", "tap", *(str(v) for v in close))
                adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivityV2", timeout=60)
                return None
        return next((center(node) for node in labelled_nodes(tree, label)), None)

    return wait_for_match(repr(label), position, timeout)


def reveal(label):
    # Theme options span more than one viewport when the user increases the font size.
    for direction in ("up", "down"):
        for _ in range(4):
            try:
                return find_label(label, timeout=3)
            except RuntimeError:
                start, end = (1720, 800) if direction == "up" else (800, 1720)
                adb("shell", "input", "swipe", "500", str(start), "500", str(end), "320")
    return find_label(label)


def tap_option(label):
    x, y = reveal(label)
    adb("shell", "input", "tap", str(x), str(y))


def confirm_option(label, checked):
    def matches(tree):
        parents = {child: parent for parent in tree.iter() for child in parent}
        for row in labelled_nodes(tree, label):
            while row is not None:
                if row.get("clickable") == "true" or row.get("checkable") == "true":
                    controls = [node for node in row.iter()
                                if node.get("checkable") == "true" or node.get("selected") == "true"]
                    active = any(node.get("checked") == "true" or node.get("selected") == "true"
                                 for node in controls)
                    if controls and active == checked:
                        return True
                    break
                row = parents.get(row)
        return None

    reveal(label)
    wait_for_match(f"{label!r} checked={checked}", matches)


def tap(label):
    x, y = find_label(label)
    adb("shell", "input", "tap", str(x), str(y))


def back_to(label):
    adb("shell", "input", "keyevent", "KEYCODE_BACK")
    find_label(label)


def capture(output, name, label):
    find_label(label)
    image = adb("exec-out", "screencap", "-p")
    if not image.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError("screencap returned invalid PNG bytes")
    (output / (name + ".png")).write_bytes(image)
    print(f"Captured {name}", flush=True)


def restart_home():
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivityV2", timeout=60)
    find_label("随便看看")


def disconnect_emulator():
    if adb("shell", "getprop", "ro.kernel.qemu").strip() != b"1":
        raise RuntimeError("The offline login check requires a disposable emulator")
    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    adb("shell", "svc", "wifi", "disable")
    end = time.monotonic() + 30
    while time.monotonic() < end:
        status = adb("shell", "dumpsys", "connectivity")
        if re.search(rb"(?mi)^\s*Active default network:\s*none\s*$", status):
            return
        time.sleep(0.5)
    raise RuntimeError("Android still has a default network or could not confirm disconnection")


def review_guest_login(output):
    tap("去登录")
    find_label("登录百度账号")
    find_label("无法打开百度登录页")
    tap("重试")
    capture(output, "01a-login-offline", "无法打开百度登录页")
    tap("返回")
    find_label("去登录")

    tap("我")
    tap("请先登录")
    find_label("登录百度账号")
    find_label("无法打开百度登录页")
    back_to("请先登录")
    restart_home()
    find_label("去登录")
    tap("我")
    capture(output, "02-profile", "请先登录")


def open_themes():
    tap("我")
    tap("主题选择")
    find_label("跟随系统壁纸")


def login_button_color():
    _, y = find_label("去登录")
    pixels = adb("exec-out", "screencap")
    width, height, pixel_format = struct.unpack_from("<III", pixels)
    header = len(pixels) - width * height * 4
    if pixel_format not in (1, 2) or header not in (12, 16) or not 0 <= y < height:
        raise RuntimeError("Unexpected Android screencap pixel format")
    row = header + width * y * 4
    samples = Counter(pixels[offset:offset + 3]
                      for offset in range(row + width // 6 * 4, row + width * 5 // 6 * 4, 16))
    fill, count = samples.most_common(1)[0]
    if count < samples.total() / 2:
        raise RuntimeError("Could not identify a solid fill inside the guest login button")
    return "#" + fill.hex().upper()


def review_colors(output):
    # Read Android's wallpaper-derived primary role as the independent color reference.
    resource = adb("shell", "cmd", "overlay", "lookup", "android", "android:color/system_primary_light").decode()
    matches = re.findall(r"#([0-9a-fA-F]{6,8})\b", resource)
    if not matches:
        raise RuntimeError(f"No resolved system primary color in {resource!r}")
    colors = {"system_primary_light": "#" + matches[-1][-6:].upper()}

    tap_option("跟随系统壁纸")
    confirm_option("跟随系统壁纸", True)
    restart_home()
    colors["wallpaper_after_restart"] = login_button_color()
    if colors["wallpaper_after_restart"] != colors["system_primary_light"]:
        raise RuntimeError(f"Home did not use Android's wallpaper color: {colors}")
    capture(output, "theme-wallpaper-home", "随便看看")
    open_themes()
    confirm_option("跟随系统壁纸", True)
    capture(output, "theme-sources", "跟随系统壁纸")
    tap_option("琥珀橙")
    confirm_option("琥珀橙", True)
    confirm_option("跟随系统壁纸", False)
    restart_home()
    colors["amber_after_restart"] = login_button_color()
    capture(output, "theme-amber-home", "随便看看")
    open_themes()
    confirm_option("琥珀橙", True)
    confirm_option("跟随系统壁纸", False)
    tap_option("自定义颜色")
    find_label("设置自定义主题")
    def hex_field(tree):
        for node in tree.iter("node"):
            if node.get("package") == PACKAGE and node.get("class") == "android.widget.EditText" and center(node):
                return node
        return None

    x, y = center(wait_for_match("the HEX text field", hex_field))
    adb("shell", "input", "tap", str(x), str(y))
    adb("shell", "input", "keycombination", "KEYCODE_CTRL_LEFT", "KEYCODE_A")
    adb("shell", "input", "text", "'#087F5B'")
    find_label("#087F5B")
    if re.search(rb"\b(?:mInputShown|inputShown)=true\b", adb("shell", "dumpsys", "input_method")):
        back_to("#087F5B")
    tap("完成")
    find_label("主题选择")
    restart_home()
    colors["custom_after_restart"] = login_button_color()
    # SchemeFidelity's light primary for the amber seed #C67417 and user seed #087F5B.
    if colors["amber_after_restart"] != "#8A4D00" or colors["custom_after_restart"] != "#006446":
        raise RuntimeError(f"Saved colors did not reach the home screen: {colors}")
    capture(output, "theme-custom-home", "随便看看")
    open_themes()
    confirm_option("跟随系统壁纸", False)
    tap_option("自定义颜色")
    capture(output, "theme-custom-hex-restored", "#087F5B")
    back_to("主题选择")
    return colors


def run(apk, output):
    output.mkdir(parents=True, exist_ok=True)
    disconnect_emulator()
    adb("install", "-r", "-g", str(apk), timeout=90)
    adb("shell", "wm", "size", "1080x2340")
    adb("shell", "wm", "density", "420")
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "wm", "dismiss-keyguard")
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivityV2", timeout=60)
    find_label("随便看看", recover_launcher=True)
    capture(output, "01-home", "随便看看")
    review_guest_login(output)
    tap("设置")
    capture(output, "03-settings", "个性化")
    tap("个性化")
    capture(output, "04-appearance", "字体大小")
    tap("字体大小")
    capture(output, "05-font-preview", "阅读预览")
    tap("放大一级")
    capture(output, "06-font-larger", "105%")
    tap("保存字号")
    find_label("夜间模式主题")
    back_to("个性化")
    back_to("主题选择")
    tap("主题选择")
    tap_option("自定义颜色")
    capture(output, "06a-custom-theme", "设置自定义主题")
    back_to("主题选择")
    tap_option("透明主题")
    x, y = find_label("界面设置")
    adb("shell", "input", "swipe", str(x), str(y), str(x), str(y // 3), "500")
    capture(output, "06b-wallpaper-controls", "模糊度")
    back_to("主题选择")
    colors = review_colors(output)
    back_to("设置")
    tap("设置")
    tap("个性化")
    tap("字体大小")
    find_label("105%")
    for _ in range(5):
        tap("放大一级")
    find_label("130%")
    tap("保存字号")
    find_label("夜间模式主题")

    # Relaunch with the system in night mode on a 320dp-wide display.
    adb("shell", "am", "force-stop", PACKAGE)
    adb("shell", "cmd", "uimode", "night", "yes")
    adb("shell", "wm", "size", "840x2058")
    adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivityV2", timeout=60)
    capture(output, "07-home-compact-dark-130", "随便看看")
    colors["custom_after_night_mode"] = login_button_color()
    if colors["custom_after_night_mode"] != "#78D9AF":
        raise RuntimeError(f"Night mode lost the saved custom color: {colors}")
    (output / "theme-colors.json").write_text(json.dumps(colors, indent=2) + "\n")
    tap("我")
    tap("设置")
    tap("个性化")
    tap("字体大小")
    capture(output, "08-font-compact-dark-130", "阅读预览")


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
