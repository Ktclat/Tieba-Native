"""Review the installed Android app on a Pixel 9-sized Android 15 gesture-navigation device."""

import argparse
from collections import Counter
import json
from pathlib import Path
import re
import struct
import subprocess
import time
from xml.etree import ElementTree
from capture_cold_start import record_cold_start

PACKAGE = "com.huanchengfly.tieba.post"
PHONE_WIDTH, PHONE_HEIGHT, PHONE_DPI = 1080, 2424, 420


def configure_phone():
    adb("shell", "wm", "size", f"{PHONE_WIDTH}x{PHONE_HEIGHT}")
    adb("shell", "wm", "density", str(PHONE_DPI))
    adb("shell", "settings", "put", "system", "font_scale", "1.0")
    adb("shell", "cmd", "uimode", "night", "no")
    # A fresh emulator otherwise places a SystemUI tutorial over the first full-screen gallery.
    adb("shell", "settings", "put", "secure", "immersive_mode_confirmations", "confirmed")
    if adb("shell", "settings", "get", "secure", "immersive_mode_confirmations").strip() != b"confirmed":
        raise RuntimeError("Could not confirm the emulator's first-use immersive tutorial")
    overlay = "com.android.internal.systemui.navbar.gestural"
    adb("shell", "cmd", "overlay", "enable-exclusive", "--category", overlay)
    for setting in ("animator_duration_scale", "transition_animation_scale", "window_animation_scale"):
        adb("shell", "settings", "put", "global", setting, "1")
    for setting in ("min_refresh_rate", "peak_refresh_rate"):
        adb("shell", "settings", "put", "system", setting, "60.0")
    # Leave system back animations enabled so the device verifies the app’s own opt-out.
    adb("shell", "settings", "put", "global", "enable_back_animation", "1")
    if "[x] " + overlay not in adb("shell", "cmd", "overlay", "list").decode():
        raise RuntimeError("Gesture navigation overlay did not become active")
    deadline = time.monotonic() + 15
    while time.monotonic() < deadline:
        if adb("shell", "settings", "get", "secure", "navigation_mode").strip() == b"2":
            return
        time.sleep(0.3)
    raise RuntimeError("SystemUI did not switch to gesture navigation")



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


def find_label(label, timeout=45, recover_launcher=False, *, alternate_labels=()):
    labels = (label, *alternate_labels)

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
        return next((center(node) for candidate in labels for node in labelled_nodes(tree, candidate)), None)

    return wait_for_match(" or ".join(map(repr, labels)), position, timeout)


def reveal(label):
    # Scroll to theme options that are below the viewport.
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


def capture(output, name, label, *, alternate_labels=()):
    find_label(label, alternate_labels=alternate_labels)
    image = adb("exec-out", "screencap", "-p")
    if not image.startswith(b"\x89PNG\r\n\x1a\n"):
        raise RuntimeError("screencap returned invalid PNG bytes")
    (output / (name + ".png")).write_bytes(image)
    print(f"Captured {name}", flush=True)


def save_diagnostic(output, name, *command):
    try:
        (output / name).write_bytes(adb(*command, timeout=5))
    except (OSError, subprocess.SubprocessError) as error:
        print(f"Could not capture {name} ({type(error).__name__})", flush=True)


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

    def login_page(tree):
        if next(labelled_nodes(tree, "登录百度账号"), None) is not None:
            return tree
        return None

    tree = wait_for_match("the login title", login_page)
    (output / "login-entry.xml").write_bytes(ElementTree.tostring(tree, encoding="utf-8"))
    save_diagnostic(output, "login-entry.png", "exec-out", "screencap", "-p")
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


def run(apk, output, baseline):
    output.mkdir(parents=True, exist_ok=True)
    disconnect_emulator()
    configure_phone()
    adb("install", "-r", "-g", str(apk), timeout=90)
    adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
    adb("shell", "wm", "dismiss-keyguard")
    if baseline:
        adb("shell", "am", "start", "-W", "-n", PACKAGE + "/.MainActivityV2", timeout=60)
        find_label("随便看看", recover_launcher=True)
    else:
        record_cold_start(output / "cold-start", PACKAGE, adb, lambda: find_label("随便看看", recover_launcher=True))
    capture(output, "01-home", "随便看看")
    tap("动态")
    find_label("热榜")
    time.sleep(3.5)  # Let the transient network toast clear before reviewing the persistent error state.
    capture(output, "01-explore-offline", "热榜")
    if baseline:
        # Capture the old installed APK at its real top-right search entry.
        tap("搜索")
        capture(output, "search-empty", "返回")
        tap("返回")
        find_label("热榜")
    tap("消息")
    capture(output, "01-notifications", "登录后查看消息", alternate_labels=("回复我的",) if baseline else ())
    tap("首页")
    if baseline:
        tap("我")
        capture(output, "02-profile", "请先登录")
    else:
        review_guest_login(output)
    tap("设置")
    capture(output, "03-settings", "个性化")
    tap("个性化")
    capture(output, "04-appearance", "字体大小")
    if baseline:
        return
    tap("字体大小")
    capture(output, "05-font-preview", "100%")
    back_to("夜间模式主题")
    back_to("个性化")
    back_to("主题选择")
    tap("主题选择")
    tap_option("自定义颜色")
    capture(output, "06-custom-theme", "设置自定义主题")
    back_to("主题选择")
    colors = review_colors(output)
    back_to("设置")
    restart_home()
    adb("shell", "cmd", "uimode", "night", "yes")
    restart_home()
    capture(output, "07-home-dark", "随便看看")
    colors["custom_after_night_mode"] = login_button_color()
    if colors["custom_after_night_mode"] != "#78D9AF":
        raise RuntimeError(f"Night mode lost the saved custom color: {colors}")
    (output / "theme-colors.json").write_text(json.dumps(colors, indent=2) + "\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("output", type=Path)
    parser.add_argument("--baseline", action="store_true", help="Capture the former home, profile and settings.")
    args = parser.parse_args()
    try:
        run(args.apk, args.output, args.baseline)
    except Exception:
        save_diagnostic(args.output, "failure.png", "exec-out", "screencap", "-p")
        save_diagnostic(args.output, "last-window.xml", "exec-out", "cat", "/sdcard/window.xml")
        save_diagnostic(args.output, "crashes.txt", "logcat", "-b", "crash", "-d", "-t", "300", "-s", "AndroidRuntime:E")
        raise
