"""Run with Pixel Duo installed and accessibility enabled on a Pixel Fold emulator.

Uses the real service, hinge sensor and window manager. No mocks.
"""
import re
import os
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    serial = os.environ.get("ANDROID_SERIAL", "emulator-5554")
    assert serial.startswith("emulator-"), "This check changes emulator settings and must not target a phone"
    return subprocess.check_output(["adb", "-s", serial, *map(str, args)], text=True)


def nodes():
    adb("shell", "uiautomator", "dump", "/sdcard/pixelduo-test.xml")
    return list(ET.fromstring(adb("shell", "cat", "/sdcard/pixelduo-test.xml")).iter("node"))


def switch():
    found = [node for node in nodes() if node.get("package") == "org.pixelduo" and node.get("checkable") == "true"]
    assert len(found) == 1, "The app must have exactly one switch"
    return found[0]


def tap_switch():
    node = switch()
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", (x1 + x2) // 2, (y1 + y2) // 2)
    time.sleep(.25)


def listening():
    current = adb("shell", "dumpsys", "sensorservice").split("Previous Registrations:")[0]
    return "org.pixelduo.FoldService" in current


def overlay():
    return "Pixel Duo fold animation" in adb("shell", "dumpsys", "window", "windows")


def hinge(angle):
    adb("emu", "sensor", "set", "hinge-angle0", angle)


def wait_for(condition, message):
    deadline = time.monotonic() + 3
    while time.monotonic() < deadline:
        if condition():
            return
        time.sleep(.1)
    raise AssertionError(message)


if __name__ == "__main__":
    original_scale = adb("shell", "settings", "get", "global", "animator_duration_scale").strip()
    try:
        adb("shell", "settings", "put", "global", "animator_duration_scale", "1")
        adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        adb("shell", "wm", "dismiss-keyguard")
        adb("shell", "am", "start", "--activity-clear-top", "-n", "org.pixelduo/.MainActivity")
        time.sleep(.5)
        if switch().get("checked") == "true":
            tap_switch()
        assert not listening() and not overlay(), "Off must release the hinge sensor and overlay"
        tap_switch()
        wait_for(listening, "On must register the public hinge sensor")
        hinge(180)
        time.sleep(.2)
        hinge(130)
        time.sleep(.2)
        assert overlay(), "Hinge movement must produce an accessibility overlay"
        time.sleep(1.4)
        assert not overlay(), "A settled hinge must release the overlay and captured image"
        print("PASS: one switch; real sensor and shader overlay; idle cleanup")

        node = switch()
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
        hinge(120)
        time.sleep(.2)
        assert overlay()
        adb("shell", "input", "tap", (x1 + x2) // 2, (y1 + y2) // 2)
        time.sleep(.3)
        assert not listening() and not overlay(), "Overlay must pass input to the switch below"
        tap_switch()
        print("PASS: input passes through overlay; Off cleans up immediately")

        adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
        wait_for(lambda: not listening() and not overlay(), "Screen off must release the sensor and overlay")
        adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        adb("shell", "wm", "dismiss-keyguard")
        wait_for(listening, "Screen on must restore the enabled service")
        adb("shell", "settings", "put", "global", "animator_duration_scale", "0")
        wait_for(lambda: not listening() and not overlay(), "System animations off must pause the service")
        adb("shell", "settings", "put", "global", "animator_duration_scale", original_scale)
        wait_for(listening, "System animations on must restore the service")
        print("PASS: screen-off sensor cleanup; wake recovery; reduced-motion pause")

        adb("shell", "dumpsys", "battery", "unplug")
        adb("shell", "cmd", "power", "set-mode", "1")
        wait_for(lambda: not listening() and not overlay(), "Battery Saver must pause the service")
        adb("shell", "cmd", "power", "set-mode", "0")
        wait_for(listening, "Battery Saver off must restore the service")
        print("PASS: Battery Saver pause and recovery")
    finally:
        adb("shell", "settings", "put", "global", "animator_duration_scale", original_scale)
        adb("shell", "cmd", "power", "set-mode", "0")
        adb("shell", "dumpsys", "battery", "reset")
        adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        adb("shell", "wm", "dismiss-keyguard")
        hinge(180)
