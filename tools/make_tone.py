"""Synthesises Wakeroo's default alarm tone: a rising marimba-like motif that loops cleanly."""
import math, struct, sys, wave

RATE = 22050
def note(freq, start, dur, amp, buf):
    n0, n = int(start * RATE), int(dur * RATE)
    for i in range(n):
        t = i / RATE
        env = (1 - math.exp(-t * 90)) * math.exp(-t * 5.5)       # fast attack, woody decay
        s = (math.sin(2 * math.pi * freq * t)
             + 0.35 * math.sin(2 * math.pi * freq * 4.0 * t) * math.exp(-t * 18)   # marimba overtone
             + 0.15 * math.sin(2 * math.pi * freq * 2.0 * t))
        if n0 + i < len(buf):
            buf[n0 + i] += amp * env * s

def build(path):
    length = 2.4
    buf = [0.0] * int(length * RATE)
    C5, E5, G5, C6, E6, G6 = 523.25, 659.25, 783.99, 1046.5, 1318.5, 1568.0
    seq = [(C5, 0.00), (E5, 0.15), (G5, 0.30), (C6, 0.45), (G5, 0.75), (C6, 0.90), (E6, 1.05), (G6, 1.20)]
    for f, st in seq:
        note(f, st, 1.1, 0.30, buf)
    peak = max(abs(x) for x in buf)
    fade = int(0.08 * RATE)
    with wave.open(path, "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        frames = bytearray()
        for i, x in enumerate(buf):
            g = min(1.0, (len(buf) - i) / fade)                  # soft tail so the loop does not click
            frames += struct.pack("<h", int(32000 * 0.92 * g * x / peak))
        w.writeframes(bytes(frames))

if __name__ == "__main__":
    build(sys.argv[1])
