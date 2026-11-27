"""Capture the Android 15 display from the launcher through the first usable home screen."""

import csv
import json
import re
import struct
import subprocess
import time


def frame_times(video, decoded_count):
    # Android 15 screenrecord includes the Winscope v2 clock mapping and each
    # actual video sample's elapsed time. Align frames to the device's WM log.
    payload = video.read_bytes()
    magic = b"#VV1NSC0PET1ME2#"
    marker = payload.find(magic)
    if marker >= 0:
        pos = marker + len(magic)
        version, wall_offset_ns, count = struct.unpack_from("<IqI", payload, pos)
        if version == 2 and count == decoded_count:
            elapsed = struct.unpack_from(f"<{count}Q", payload, pos + 16)
            return sorted((value + wall_offset_ns) / 1e9 for value in elapsed), True
    # Other Android encoders may omit Winscope metadata. Inspect their presentation
    # timestamps too, then repeat from a known light foreground to isolate the app.
    probe = subprocess.check_output([
        "ffprobe", "-v", "error", "-select_streams", "v:0", "-show_frames",
        "-show_entries", "frame=best_effort_timestamp_time", "-of", "json", str(video),
    ], timeout=40)
    times = [float(frame["best_effort_timestamp_time"]) for frame in json.loads(probe)["frames"]]
    if len(times) != decoded_count:
        raise ValueError("Unable to align decoded video frames and presentation timestamps")
    return times, False


def analyze(video, start, resumed, output, control=False):
    # Exclude status/navigation bars. Area scaling measures the whole content
    # instead of sampling a point that could fall inside the dark launch icon.
    width, height = 48, 96
    samples = subprocess.check_output([
        "ffmpeg", "-nostdin", "-loglevel", "error", "-i", str(video), "-map", "0:v:0",
        "-vf", f"crop=iw*0.9:ih*0.8,scale={width}:{height}:flags=area,format=gray",
        "-vsync", "0", "-f", "rawvideo", "-",
    ], timeout=40)
    size = width * height
    if len(samples) < size * 2 or len(samples) % size:
        raise ValueError("Incomplete cold-start video")
    times, device_clock = frame_times(video, len(samples) // size)
    aligned = device_clock and times[0] < start < times[-1]
    before = max(i for i, t in enumerate(times) if t < start) if aligned else 0
    launcher = samples[before * size:(before + 1) * size]
    dark_launcher = sum(value < 32 for value in launcher) >= size * 0.98
    if control and sum(value > 190 for value in launcher) < size / 2:
        raise ValueError("Control launch must start on the light Android Settings window")
    first_surface = None
    black_frames = []
    early_black = []
    with (output / "frames.csv").open("w", newline="") as stream:
        writer = csv.writer(stream)
        writer.writerow(["frame", "video_ms", "ms_from_intent", "phase", "near_black_percent", "light_percent"])
        for i, stamp in enumerate(times):
            frame = samples[i * size:(i + 1) * size]
            dark = sum(value < 32 for value in frame) / size
            light = sum(value > 219 for value in frame) / size
            if aligned and stamp < start:
                phase = "launcher"
            else:
                activity_resumed = aligned and resumed is not None and stamp >= resumed
                if first_surface is None and light >= 0.85:
                    difference = sum(abs(a - b) for a, b in zip(frame, launcher)) / size
                    if difference > 12 or activity_resumed or control:
                        first_surface = i
                phase = "app" if first_surface is not None or activity_resumed or control else "transition"
                if dark >= 0.98:
                    (black_frames if phase == "app" else early_black).append(i)
            writer.writerow([i, round((stamp - times[0]) * 1000), round((stamp - start) * 1000) if aligned else "",
                             phase, round(dark * 100, 1), round(light * 100, 1)])
    summary = {
        "captured_frames": len(times), "first_light_app_frame": first_surface,
        "aligned_to_intent": aligned, "activity_resumed_ms": round((resumed - start) * 1000) if resumed else None,
        "near_black_app_frames": black_frames, "near_black_transition_frames": early_black,
        "near_black_launch_origin": dark_launcher,
    }
    (output / "result.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(f"Cold start: {summary}", flush=True)
    if black_frames or (control and first_surface is None):
        raise RuntimeError("Light-theme cold start did not stay visible; see cold-start/frames.csv and video")
    return bool(early_black) or dark_launcher or not aligned or resumed is None or first_surface is None


def record_cold_start(output, package, adb, await_home, control=False):
    output.mkdir(parents=True, exist_ok=True)
    adb("shell", "am", "force-stop", package)
    if control:
        adb("shell", "am", "start", "-W", "--activity-no-animation", "-a", "android.settings.SETTINGS")
    else:
        adb("shell", "input", "keyevent", "KEYCODE_HOME")
    remote = "/data/local/tmp/tieba-review-cold-start.mp4"
    adb("shell", "rm", "-f", remote)
    command = f"echo $$; exec screenrecord --size 432x960 --bit-rate 4M --time-limit 90 {remote}"
    with subprocess.Popen(["adb", "shell", command], stdout=subprocess.PIPE) as recorder:
        pid = recorder.stdout.readline().decode().strip()
        if not pid.isdecimal():
            raise RuntimeError("Could not start the device screen recorder")
        try:
            deadline = time.monotonic() + 15
            while adb("shell", f"if [ -s {remote} ]; then echo ready; fi").strip() != b"ready":
                if recorder.poll() is not None or time.monotonic() >= deadline:
                    raise RuntimeError("Android did not start encoding the display")
                time.sleep(0.2)
            start = int(adb("shell", "date", "+%s%N").strip()) / 1e9
            flags = ["--activity-no-animation"] if control else []
            # Launchers send an explicit intent. An implicit startActivity would
            # require CATEGORY_DEFAULT, which the launcher's filter does not declare.
            try:
                launch = adb("shell", "am", "start", "-W", "-a", "android.intent.action.MAIN",
                             "-c", "android.intent.category.LAUNCHER", "-f", "0x10200000",
                             *flags, "-n", package + "/.MainActivityV2", timeout=60)
            except subprocess.CalledProcessError as error:
                (output / "launch.txt").write_bytes((error.stdout or b"") + (error.stderr or b""))
                raise
            (output / "launch.txt").write_bytes(launch)
            await_home()
        finally:
            if recorder.poll() is None:
                adb("shell", f"kill -2 {pid} 2>/dev/null || true")
            recorder.wait(timeout=12)
            video = output / "cold-start.mp4"
            if recorder.returncode == 0:
                adb("pull", remote, str(video))
                adb("shell", "rm", "-f", remote)
            else:
                raise RuntimeError(f"Android screenrecord exited with status {recorder.returncode}")
    events = adb("logcat", "-b", "events", "-v", "epoch", "-d", "-s", "wm_on_resume_called:I").decode()
    resumes = [float(stamp) for stamp, line in re.findall(r"(?m)^\s*(\d+\.\d+)([^\n]*)", events)
               if package + ".MainActivityV2" in line and float(stamp) >= start]
    check_transition = analyze(video, start, next(iter(resumes), None), output, control=control)
    if check_transition and not control:
        # A dark frame preceding the app surface could be the launcher itself.
        # Repeat from a verified light window without the system opening animation.
        # Here even a black frame before the splash becomes visible counts as failure.
        record_cold_start(output / "light-window-control", package, adb, await_home, control=True)
