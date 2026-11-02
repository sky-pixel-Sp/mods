import json
import tempfile
import unittest
from pathlib import Path
from skydesk_core import DataStore, default_data

class DataStoreTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.path = Path(self.tmp.name) / "data.json"
        self.store = DataStore(self.path)

    def tearDown(self):
        self.tmp.cleanup()

    def test_default_data_is_created(self):
        self.assertTrue(self.path.exists())
        self.assertEqual(json.loads(self.path.read_text(encoding="utf-8"))["schema_version"], 1)

    def test_task_create_toggle_delete_persist(self):
        item = self.store.add_task("Belajar Python", "Tinggi")
        self.assertTrue(self.store.toggle_task(item["id"], True))
        loaded = DataStore(self.path)
        self.assertTrue(loaded.data["tasks"][0]["done"])
        self.assertTrue(loaded.delete_task(item["id"]))
        self.assertEqual(loaded.data["tasks"], [])

    def test_empty_task_rejected(self):
        with self.assertRaises(ValueError):
            self.store.add_task("  ")

    def test_note_create_update_delete(self):
        item = self.store.add_note("Ide")
        self.assertTrue(self.store.update_note(item["id"], "Proyek", "Catatan penting"))
        loaded = DataStore(self.path)
        self.assertEqual(loaded.data["notes"][0]["body"], "Catatan penting")
        self.assertTrue(loaded.delete_note(item["id"]))

    def test_focus_total(self):
        self.store.record_focus_session(25)
        self.store.record_focus_session(50)
        self.assertEqual(self.store.total_focus_minutes(), 75)

    def test_backup_export_import(self):
        self.store.add_task("Backup")
        note = self.store.add_note("Memo")
        self.store.update_note(note["id"], "Memo", "Konten")
        backup = Path(self.tmp.name) / "backup.json"
        self.store.export_to(backup)
        other = DataStore(Path(self.tmp.name) / "other.json")
        other.import_from(backup)
        self.assertEqual(other.data["tasks"][0]["text"], "Backup")
        self.assertEqual(other.data["notes"][0]["body"], "Konten")

    def test_corrupt_file_is_backed_up(self):
        self.path.write_text("not json", encoding="utf-8")
        recovered = DataStore(self.path)
        self.assertEqual(recovered.data, default_data())
        copies = list(self.path.parent.glob("data.corrupt-*.json"))
        self.assertTrue(copies)
        self.assertEqual(copies[0].read_text(encoding="utf-8"), "not json")

if __name__ == "__main__":
    unittest.main()
