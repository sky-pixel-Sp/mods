"""SkyDesk local storage core. Standard library only."""
from __future__ import annotations
import json, os, shutil, tempfile, uuid
from datetime import datetime
from pathlib import Path
from typing import Any

APP_NAME = "SkyDesk"
PRIORITIES = ("Rendah", "Sedang", "Tinggi")

def now_iso() -> str:
    return datetime.now().astimezone().isoformat(timespec="seconds")

def default_data() -> dict[str, Any]:
    return {"schema_version": 1, "tasks": [], "notes": [], "focus_sessions": [],
            "settings": {"focus_minutes": 25, "break_minutes": 5}}

def default_data_path() -> Path:
    appdata = os.environ.get("APPDATA")
    return (Path(appdata) / APP_NAME / "data.json") if appdata else (Path.home() / ".skydesk" / "data.json")

def bounded(value: Any, default: int, minimum: int, maximum: int) -> int:
    try: return max(minimum, min(maximum, int(value)))
    except (TypeError, ValueError): return default

class DataStore:
    def __init__(self, path: str | Path | None = None):
        self.path = Path(path) if path else default_data_path()
        self.path.parent.mkdir(parents=True, exist_ok=True)
        self.data = self._load()
        self.save()

    def _load(self) -> dict[str, Any]:
        if not self.path.exists(): return default_data()
        try:
            raw = json.loads(self.path.read_text(encoding="utf-8"))
            if not isinstance(raw, dict): raise ValueError("Root is not an object")
            clean = default_data()
            for item in raw.get("tasks", []):
                if isinstance(item, dict):
                    text = str(item.get("text", "Tugas tanpa judul")).strip()[:500] or "Tugas tanpa judul"
                    priority = item.get("priority", "Sedang")
                    if priority not in PRIORITIES: priority = "Sedang"
                    clean["tasks"].append({"id": str(item.get("id") or uuid.uuid4().hex), "text": text,
                        "done": bool(item.get("done", False)), "priority": priority,
                        "created_at": str(item.get("created_at") or now_iso())})
            for item in raw.get("notes", []):
                if isinstance(item, dict):
                    clean["notes"].append({"id": str(item.get("id") or uuid.uuid4().hex),
                        "title": str(item.get("title", "Catatan baru")).strip()[:200] or "Catatan baru",
                        "body": str(item.get("body", ""))[:100000], "updated_at": str(item.get("updated_at") or now_iso())})
            for item in raw.get("focus_sessions", []):
                if isinstance(item, dict):
                    clean["focus_sessions"].append({"id": str(item.get("id") or uuid.uuid4().hex),
                        "minutes": bounded(item.get("minutes"), 25, 1, 180), "completed_at": str(item.get("completed_at") or now_iso())})
            if isinstance(raw.get("settings"), dict):
                clean["settings"]["focus_minutes"] = bounded(raw["settings"].get("focus_minutes"), 25, 1, 180)
                clean["settings"]["break_minutes"] = bounded(raw["settings"].get("break_minutes"), 5, 1, 60)
            return clean
        except (OSError, ValueError, TypeError, json.JSONDecodeError):
            stamp = datetime.now().strftime("%Y%m%d-%H%M%S")
            try: shutil.copy2(self.path, self.path.with_name(f"{self.path.stem}.corrupt-{stamp}{self.path.suffix}"))
            except OSError: pass
            return default_data()

    def save(self) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        fd, tmp = tempfile.mkstemp(prefix="skydesk-", suffix=".tmp", dir=self.path.parent)
        try:
            with os.fdopen(fd, "w", encoding="utf-8", newline="\n") as f:
                json.dump(self.data, f, ensure_ascii=False, indent=2)
                f.flush(); os.fsync(f.fileno())
            os.replace(tmp, self.path)
        finally:
            try:
                if os.path.exists(tmp): os.unlink(tmp)
            except OSError: pass

    def add_task(self, text: str, priority: str = "Sedang") -> dict[str, Any]:
        text = str(text).strip()
        if not text: raise ValueError("Teks tugas tidak boleh kosong.")
        if priority not in PRIORITIES: priority = "Sedang"
        item = {"id": uuid.uuid4().hex, "text": text[:500], "done": False, "priority": priority, "created_at": now_iso()}
        self.data["tasks"].insert(0, item); self.save(); return item

    def toggle_task(self, task_id: str, done: bool) -> bool:
        for item in self.data["tasks"]:
            if item["id"] == task_id:
                item["done"] = bool(done); self.save(); return True
        return False

    def delete_task(self, task_id: str) -> bool:
        before = len(self.data["tasks"])
        self.data["tasks"] = [t for t in self.data["tasks"] if t["id"] != task_id]
        if len(self.data["tasks"]) == before: return False
        self.save(); return True

    def add_note(self, title: str = "Catatan baru") -> dict[str, Any]:
        item = {"id": uuid.uuid4().hex, "title": str(title).strip()[:200] or "Catatan baru", "body": "", "updated_at": now_iso()}
        self.data["notes"].insert(0, item); self.save(); return item

    def update_note(self, note_id: str, title: str, body: str) -> bool:
        for item in self.data["notes"]:
            if item["id"] == note_id:
                item["title"] = str(title).strip()[:200] or "Catatan tanpa judul"
                item["body"] = str(body)[:100000]; item["updated_at"] = now_iso(); self.save(); return True
        return False

    def delete_note(self, note_id: str) -> bool:
        before = len(self.data["notes"])
        self.data["notes"] = [n for n in self.data["notes"] if n["id"] != note_id]
        if len(self.data["notes"]) == before: return False
        self.save(); return True

    def record_focus_session(self, minutes: int) -> None:
        self.data["focus_sessions"].insert(0, {"id": uuid.uuid4().hex, "minutes": bounded(minutes, 25, 1, 180), "completed_at": now_iso()})
        self.save()

    def total_focus_minutes(self) -> int:
        return sum(bounded(s.get("minutes"), 0, 0, 180) for s in self.data["focus_sessions"])

    def export_to(self, path: str | Path) -> None:
        target = Path(path); target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(self.data, ensure_ascii=False, indent=2), encoding="utf-8")

    def import_from(self, path: str | Path) -> None:
        raw = json.loads(Path(path).read_text(encoding="utf-8"))
        if not isinstance(raw, dict): raise ValueError("Backup tidak valid.")
        for key in ("tasks", "notes", "focus_sessions"):
            if key in raw and not isinstance(raw[key], list): raise ValueError(f"Field {key} harus berupa daftar.")
        previous = self.data
        try:
            temp = Path(path)
            candidate = DataStore(temp)
            clean = candidate.data
            self.data = clean
            self.save()
        except Exception:
            self.data = previous
            raise
