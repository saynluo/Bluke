#!/usr/bin/env python3
"""Bluke LAN receiver: turns HID reports streamed by the Bluke app over Wi-Fi into local input.

Usage:  pip install pynput
        python bluke_lan_receiver.py <phone-ip> <pin>

Supports keyboard, touchpad/mouse and media keys. Gamepad mode is Bluetooth-only.
"""
import socket
import sys

from pynput.keyboard import Controller as Keyboard, Key, KeyCode
from pynput.mouse import Button, Controller as Mouse

PORT = 7393

MODIFIERS = [Key.ctrl_l, Key.shift_l, Key.alt_l, Key.cmd_l, Key.ctrl_r, Key.shift_r, Key.alt_r, Key.cmd_r]

USAGE = {u: KeyCode.from_char(c) for u, c in zip(range(0x04, 0x1E), "abcdefghijklmnopqrstuvwxyz")}
USAGE.update({u: KeyCode.from_char(c) for u, c in zip(range(0x1E, 0x28), "1234567890")})
USAGE.update({u: KeyCode.from_char(c) for u, c in zip(range(0x2D, 0x39), "-=[]\\#;'`,./")})
USAGE.update({u: KeyCode.from_char(c) for u, c in zip(range(0x54, 0x58), "/*-+")})
USAGE.update({u: KeyCode.from_char(c) for u, c in zip(range(0x59, 0x64), "1234567890.")})
USAGE.update({u: getattr(Key, f"f{i + 1}") for i, u in enumerate(range(0x3A, 0x46))})
USAGE.update({
    0x28: Key.enter, 0x29: Key.esc, 0x2A: Key.backspace, 0x2B: Key.tab, 0x2C: Key.space,
    0x39: Key.caps_lock, 0x46: Key.print_screen, 0x47: Key.scroll_lock, 0x48: Key.pause,
    0x49: Key.insert, 0x4A: Key.home, 0x4B: Key.page_up, 0x4C: Key.delete, 0x4D: Key.end,
    0x4E: Key.page_down, 0x4F: Key.right, 0x50: Key.left, 0x51: Key.down, 0x52: Key.up,
    0x53: Key.num_lock, 0x58: Key.enter, 0x65: Key.menu,
})
del USAGE[0x32]  # Non-US '#' has no portable key.

CONSUMER = {
    0x00E2: Key.media_volume_mute, 0x00EA: Key.media_volume_down, 0x00E9: Key.media_volume_up,
    0x00B6: Key.media_previous, 0x00CD: Key.media_play_pause, 0x00B5: Key.media_next,
}

MOUSE_BUTTONS = [Button.left, Button.right, Button.middle]


def signed(b):
    return b - 256 if b > 127 else b


class Injector:
    def __init__(self):
        self.kb = Keyboard()
        self.mouse = Mouse()
        self.keys = set()  # held keyboard pynput keys
        self.buttons = 0
        self.consumer = None

    def keyboard(self, data):
        held = {m for i, m in enumerate(MODIFIERS) if data[0] & (1 << i)}
        held |= {USAGE[u] for u in data[2:8] if u in USAGE}
        # Modifiers go down first and up last so Shift+A arrives as 'A'.
        order = lambda k: k in MODIFIERS
        for k in sorted(self.keys - held, key=order):
            self.kb.release(k)
        for k in sorted(held - self.keys, key=order, reverse=True):
            self.kb.press(k)
        self.keys = held

    def pointer(self, data):
        buttons, dx, dy, wheel = data[0], signed(data[1]), signed(data[2]), signed(data[3])
        for i, b in enumerate(MOUSE_BUTTONS):
            bit = 1 << i
            if buttons & bit and not self.buttons & bit:
                self.mouse.press(b)
            elif not buttons & bit and self.buttons & bit:
                self.mouse.release(b)
        self.buttons = buttons
        if dx or dy:
            self.mouse.move(dx, dy)
        if wheel:
            self.mouse.scroll(0, wheel)

    def media(self, data):
        key = CONSUMER.get(data[0] | (data[1] << 8))
        if self.consumer is not None:
            self.kb.release(self.consumer)
        if key is not None:
            self.kb.press(key)
        self.consumer = key

    def release_all(self):
        self.keyboard(bytes(8))
        self.pointer(bytes(4))
        self.media(bytes(2))


def read_exact(sock, n):
    buf = b""
    while len(buf) < n:
        chunk = sock.recv(n - len(buf))
        if not chunk:
            raise ConnectionError("phone disconnected")
        buf += chunk
    return buf


def main():
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    host, pin = sys.argv[1], sys.argv[2]
    sock = socket.create_connection((host, PORT), timeout=5)
    sock.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1)
    sock.sendall(f"BLUKE {pin}\n".encode())
    if read_exact(sock, 3) != b"OK\n":
        sys.exit("Handshake rejected")
    sock.settimeout(None)
    print(f"Connected to {host}. Ctrl+C to quit.")
    inj = Injector()
    handlers = {1: inj.keyboard, 2: inj.pointer, 4: inj.media}
    try:
        while True:
            length = read_exact(sock, 1)[0]
            frame = read_exact(sock, length)
            handler = handlers.get(frame[0])
            if handler:
                handler(frame[1:])
    except (ConnectionError, KeyboardInterrupt) as e:
        print(e or "Bye")
    finally:
        inj.release_all()
        sock.close()


if __name__ == "__main__":
    main()
