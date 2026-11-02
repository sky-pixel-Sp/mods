import json
import tempfile
import unittest
from pathlib import Path
from game_engine import clamp, collides, distance_squared, enemy_profile, load_high_score, save_high_score, wave_for_time

class GameEngineTests(unittest.TestCase):
    def test_clamp_bounds(self):
        self.assertEqual(clamp(-5, 0, 10), 0)
        self.assertEqual(clamp(15, 0, 10), 10)
        self.assertEqual(clamp(5, 0, 10), 5)

    def test_collision_edge_and_separation(self):
        self.assertTrue(collides(0, 0, 5, 10, 0, 5))
        self.assertFalse(collides(0, 0, 4, 10, 0, 5))
        self.assertEqual(distance_squared(0, 0, 3, 4), 25)

    def test_wave_progression(self):
        self.assertEqual(wave_for_time(-20), 1)
        self.assertEqual(wave_for_time(17.99), 1)
        self.assertEqual(wave_for_time(18), 2)
        self.assertEqual(wave_for_time(90), 6)

    def test_enemy_profiles_scale(self):
        self.assertGreater(enemy_profile("boss", 5)["hp"], enemy_profile("boss", 1)["hp"])
        self.assertGreater(enemy_profile("drone", 8)["hp"], enemy_profile("drone", 1)["hp"])
        self.assertGreater(enemy_profile("brute", 4)["radius"], enemy_profile("drone", 4)["radius"])
        with self.assertRaises(ValueError):
            enemy_profile("unknown", 1)

    def test_high_score_roundtrip(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "profile.json"
            self.assertEqual(load_high_score(path), 0)
            save_high_score(path, 240)
            self.assertEqual(load_high_score(path), 240)
            save_high_score(path, 150)
            self.assertEqual(load_high_score(path), 150)

    def test_corrupt_score_recovery(self):
        with tempfile.TemporaryDirectory() as tmp:
            path=Path(tmp)/"profile.json"
            path.write_text("{bad json",encoding="utf-8")
            self.assertEqual(load_high_score(path),0)

if __name__ == "__main__":
    unittest.main()
