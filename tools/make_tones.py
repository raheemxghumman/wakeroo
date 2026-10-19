"""Synthesises Wakeroo's bundled alarm tones (all original, no samples) and encodes them to OGG.

Usage: python tools/make_tones.py app/src/main/res/raw
Needs numpy and ffmpeg on PATH. Every tone loops cleanly: it ends with a short fade.
"""
import os
import subprocess
import sys
import tempfile
import wave

import numpy as np

RATE = 32000


def t_axis(dur):
    return np.arange(int(dur * RATE)) / RATE


def env(n, attack, decay):
    t = np.arange(n) / RATE
    return (1 - np.exp(-t / max(attack, 1e-4))) * np.exp(-t * decay)


def place(buf, start, sig, amp=1.0):
    i = int(start * RATE)
    end = min(len(buf), i + len(sig))
    buf[i:end] += amp * sig[: end - i]


def bell(freq, dur, decay=3.0, partials=((1, 1.0), (2.76, 0.45), (5.4, 0.25), (8.9, 0.12))):
    t = t_axis(dur)
    s = sum(a * np.sin(2 * np.pi * freq * p * t) * np.exp(-t * decay * (1 + p * 0.35)) for p, a in partials)
    return s * (1 - np.exp(-t * 300))


def marimba(freq, dur, decay=5.5):
    t = t_axis(dur)
    s = np.sin(2 * np.pi * freq * t) + 0.35 * np.sin(2 * np.pi * freq * 4 * t) * np.exp(-t * 18) + 0.15 * np.sin(2 * np.pi * freq * 2 * t)
    return s * (1 - np.exp(-t * 90)) * np.exp(-t * decay)


def steel_pan(freq, dur):
    t = t_axis(dur)
    s = np.sin(2 * np.pi * freq * t) + 0.5 * np.sin(2 * np.pi * freq * 2.02 * t) * np.exp(-t * 6) + 0.25 * np.sin(2 * np.pi * freq * 3.01 * t) * np.exp(-t * 9)
    return s * (1 - np.exp(-t * 400)) * np.exp(-t * 3.2)


def brass(freq, dur):
    t = t_axis(dur)
    vib = 1 + 0.004 * np.sin(2 * np.pi * 5.5 * t)
    s = sum((1 / k) * np.sin(2 * np.pi * freq * k * vib * t) for k in range(1, 9))
    a = np.minimum(1, t / 0.04) * np.exp(-np.maximum(0, t - dur + 0.12) * 30)
    return s * a


def soft_square(freq, dur, harmonics=7):
    t = t_axis(dur)
    return sum((1 / k) * np.sin(2 * np.pi * freq * k * t) for k in range(1, harmonics * 2, 2))


def reverb(sig, taps=((0.031, 0.35), (0.053, 0.25), (0.089, 0.18), (0.137, 0.12))):
    out = sig.copy()
    for delay, gain in taps:
        d = int(delay * RATE)
        out[d:] += gain * sig[:-d]
    return out


def chirp(f0, f1, dur):
    t = t_axis(dur)
    f = f0 + (f1 - f0) * (t / dur) ** 0.6
    phase = 2 * np.pi * np.cumsum(f) / RATE
    return np.sin(phase) * np.sin(np.pi * t / dur) ** 2


# ---- Gentle -------------------------------------------------------------------------------

def dawn_chimes():
    buf = np.zeros(int(5.6 * RATE))
    notes = [784.0, 659.3, 880.0, 587.3, 784.0, 1046.5, 880.0]
    for i, f in enumerate(notes):
        place(buf, i * 0.7, bell(f, 3.2, decay=1.6), 0.5)
    return reverb(buf)


def soft_marimba():
    buf = np.zeros(int(4.0 * RATE))
    seq = [(392.0, 0.0), (523.3, 0.25), (659.3, 0.5), (523.3, 1.0), (587.3, 1.5), (493.9, 1.75), (392.0, 2.25), (440.0, 2.75)]
    for f, st in seq:
        place(buf, st, marimba(f, 1.2, decay=4.0), 0.4)
    return reverb(buf)


def birdsong_bells():
    buf = np.zeros(int(5.0 * RATE))
    rng = np.random.default_rng(3)
    for k in range(9):
        st = 0.2 + k * 0.5 + rng.uniform(-0.08, 0.08)
        f0 = rng.uniform(2200, 3000)
        for j in range(rng.integers(2, 4)):
            place(buf, st + j * 0.09, chirp(f0, f0 * rng.uniform(1.25, 1.5), 0.07), 0.28)
    for f, st in [(1318.5, 0.0), (1568.0, 2.5)]:
        place(buf, st, bell(f, 2.4, decay=2.0), 0.35)
    return reverb(buf)


def warm_pads():
    dur = 6.0
    t = t_axis(dur)
    chords = [[261.6, 329.6, 392.0, 493.9], [220.0, 277.2, 329.6, 440.0]]
    buf = np.zeros_like(t)
    for ci, chord in enumerate(chords):
        seg = (t >= ci * 3) & (t < ci * 3 + 3)
        tt = t[seg] - ci * 3
        swell = np.sin(np.pi * tt / 3) ** 2
        for f in chord:
            for det in (-0.6, 0.6):
                buf[seg] += 0.12 * swell * (np.sin(2 * np.pi * (f + det) * tt) + 0.3 * np.sin(2 * np.pi * 2 * (f + det) * tt))
    return reverb(buf)


# ---- Energetic ----------------------------------------------------------------------------

def rise():
    buf = np.zeros(int(2.4 * RATE))
    seq = [(523.25, 0.00), (659.25, 0.15), (783.99, 0.30), (1046.5, 0.45), (783.99, 0.75), (1046.5, 0.90), (1318.5, 1.05), (1568.0, 1.20)]
    for f, st in seq:
        place(buf, st, marimba(f, 1.1), 0.3)
    return buf


def bright_pulse():
    buf = np.zeros(int(3.2 * RATE))
    arp = [523.3, 659.3, 784.0, 1046.5, 784.0, 659.3, 587.3, 740.0, 880.0, 1174.7, 880.0, 740.0]
    step = 3.2 / 16
    for i in range(16):
        f = arp[i % len(arp)]
        n = soft_square(f, step * 0.9, 5) * env(int(step * 0.9 * RATE), 0.003, 9)
        place(buf, i * step, n, 0.22)
    place(buf, 0, soft_square(130.8, 1.5, 3) * env(int(1.5 * RATE), 0.01, 2.5), 0.25)
    place(buf, 1.6, soft_square(146.8, 1.5, 3) * env(int(1.5 * RATE), 0.01, 2.5), 0.25)
    return buf


def steel_drum_run():
    buf = np.zeros(int(3.6 * RATE))
    seq = [(587.3, 0.0), (659.3, 0.2), (784.0, 0.4), (880.0, 0.6), (784.0, 0.9), (880.0, 1.1), (987.8, 1.3), (1174.7, 1.5),
           (987.8, 1.95), (880.0, 2.15), (784.0, 2.35), (880.0, 2.75)]
    for f, st in seq:
        place(buf, st, steel_pan(f, 1.0), 0.35)
    return reverb(buf)


def fanfare():
    buf = np.zeros(int(3.4 * RATE))
    seq = [(392.0, 0.0, 0.18), (392.0, 0.22, 0.18), (392.0, 0.44, 0.18), (523.3, 0.66, 0.9), (466.2, 1.7, 0.2), (523.3, 1.95, 1.2)]
    for f, st, d in seq:
        place(buf, st, brass(f, d), 0.18)
        place(buf, st, brass(f * 1.5, d), 0.08)
    return reverb(buf)


# ---- Extreme ------------------------------------------------------------------------------

def klaxon():
    dur = 1.6
    t = t_axis(dur)
    buf = np.zeros_like(t)
    for st in (0.0, 0.8):
        seg = (t >= st) & (t < st + 0.65)
        tt = t[seg] - st
        f = 320 + 260 * np.minimum(1, tt / 0.25)
        phase = 2 * np.pi * np.cumsum(f) / RATE
        saw = 2 * ((phase / (2 * np.pi)) % 1) - 1
        buf[seg] = np.tanh(3 * saw) * np.minimum(1, (0.65 - tt) / 0.03)
    return buf


def siren_sweep():
    dur = 3.0
    t = t_axis(dur)
    f = 700 + 650 * (0.5 - 0.5 * np.cos(2 * np.pi * t / 1.5))
    phase = 2 * np.pi * np.cumsum(f) / RATE
    return np.sin(phase) + 0.3 * np.sin(2 * phase) + 0.15 * np.sin(3 * phase)


def bell_storm():
    dur = 2.4
    t = t_axis(dur)
    hammer = (np.sin(2 * np.pi * 18 * t) > 0).astype(float)
    tone = np.sin(2 * np.pi * 2350 * t) + 0.6 * np.sin(2 * np.pi * 3610 * t) + 0.3 * np.sin(2 * np.pi * 5240 * t)
    gate = ((t % 1.2) < 0.95).astype(float)
    smooth = np.convolve(hammer * gate, np.ones(40) / 40, mode="same")
    return tone * smooth


def buzzer():
    dur = 2.0
    t = t_axis(dur)
    beep = soft_square(2400, dur, 4)
    pattern = np.zeros_like(t)
    for k in range(4):
        st = k * 0.16
        pattern += ((t >= st) & (t < st + 0.1)).astype(float)
    pattern += ((t >= 1.0) & (t < 1.6) & ((t % 0.16) < 0.1)).astype(float)
    return beep * np.convolve(pattern, np.ones(30) / 30, mode="same")


TONES = {
    "dawn_chimes": (dawn_chimes, 0.80),
    "soft_marimba": (soft_marimba, 0.80),
    "birdsong_bells": (birdsong_bells, 0.75),
    "warm_pads": (warm_pads, 0.80),
    "wakeroo_rise": (rise, 0.92),
    "bright_pulse": (bright_pulse, 0.90),
    "steel_drum_run": (steel_drum_run, 0.90),
    "fanfare": (fanfare, 0.90),
    "klaxon": (klaxon, 0.97),
    "siren_sweep": (siren_sweep, 0.97),
    "bell_storm": (bell_storm, 0.97),
    "buzzer": (buzzer, 0.95),
}


def write(name, sig, peak, out_dir):
    sig = sig / np.max(np.abs(sig)) * peak
    fade = int(0.06 * RATE)
    sig[-fade:] *= np.linspace(1, 0, fade)
    sig[:200] *= np.linspace(0, 1, 200)
    pcm = (sig * 32767).astype("<i2")
    with tempfile.TemporaryDirectory() as tmp:
        wav = os.path.join(tmp, name + ".wav")
        with wave.open(wav, "wb") as w:
            w.setnchannels(1)
            w.setsampwidth(2)
            w.setframerate(RATE)
            w.writeframes(pcm.tobytes())
        out = os.path.join(out_dir, "tone_" + name + ".ogg")
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", out], check=True)
        print(out, os.path.getsize(out) // 1024, "KB")


if __name__ == "__main__":
    out_dir = sys.argv[1]
    for name, (fn, peak) in TONES.items():
        write(name, fn(), peak, out_dir)
