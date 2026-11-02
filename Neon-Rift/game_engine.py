"""Deterministic helpers for Neon Rift."""
from __future__ import annotations
import json
import math
import os
import tempfile
from pathlib import Path

def clamp(value: float, low: float, high: float) -> float:
    return max(low, min(high, value))

def distance_squared(ax: float, ay: float, bx: float, by: float) -> float:
    dx, dy = ax - bx, ay - by
    return dx * dx + dy * dy

def collides(ax: float, ay: float, ar: float, bx: float, by: float, br: float) -> bool:
    radius = ar + br
    return distance_squared(ax, ay, bx, by) <= radius * radius

def wave_for_time(seconds: float) -> int:
    return max(1, int(max(0.0, seconds) // 18) + 1)

def enemy_profile(kind: str, wave: int) -> dict:
    wave = max(1, int(wave))
    profiles = {
        "drone": {"hp": 1 + wave // 4, "speed": 118 + min(62, wave * 2), "radius": 10, "score": 10, "damage": 8},
        "hunter": {"hp": 2 + wave // 3, "speed": 78 + min(50, wave * 1.5), "radius": 15, "score": 22, "damage": 13},
        "brute": {"hp": 5 + wave // 2, "speed": 43 + min(30, wave), "radius": 22, "score": 45, "damage": 22},
        "boss": {"hp": 28 + wave * 7, "speed": 34 + min(25, wave * 0.5), "radius": 38, "score": 500, "damage": 28},
    }
    if kind not in profiles:
        raise ValueError(f"Unknown enemy type: {kind}")
    result = profiles[kind].copy()
    if kind == "boss":
        result["hp"] = 28 + wave * 7
    return result

def load_high_score(path: str | Path) -> int:
    try:
        raw = json.loads(Path(path).read_text(encoding="utf-8"))
        if isinstance(raw, dict):
            value = raw.get("high_score", 0)
            return max(0, int(value))
    except (OSError, ValueError, TypeError, json.JSONDecodeError):
        pass
    return 0

def save_high_score(path: str | Path, score: int) -> None:
    target = Path(path)
    target.parent.mkdir(parents=True, exist_ok=True)
    payload = json.dumps({"high_score": max(0, int(score))}, indent=2)
    fd, temp_name = tempfile.mkstemp(prefix="neon-rift-", suffix=".tmp", dir=target.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as handle:
            handle.write(payload)
            handle.flush()
            os.fsync(handle.fileno())
        os.replace(temp_name, target)
    finally:
        try:
            if os.path.exists(temp_name):
                os.unlink(temp_name)
        except OSError:
            pass
