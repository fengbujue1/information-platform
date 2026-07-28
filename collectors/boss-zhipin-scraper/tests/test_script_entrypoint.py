import os
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


COLLECTOR_ROOT = Path(__file__).resolve().parents[1]
SCRIPT_PATH = COLLECTOR_ROOT / "scripts" / "boss_cdp_raw.py"


class ScriptEntrypointTests(unittest.TestCase):
    def test_absolute_script_help_works_from_arbitrary_working_directory(self):
        with tempfile.TemporaryDirectory() as working_directory:
            result = subprocess.run(
                [sys.executable, str(SCRIPT_PATH), "--help"],
                cwd=working_directory,
                capture_output=True,
                text=True,
                timeout=10,
            )

        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertIn("--capture-raw-response", result.stdout)

    def test_absolute_script_loads_hub_integration_from_arbitrary_directory(
        self,
    ):
        with tempfile.TemporaryDirectory() as working_directory:
            config_path = Path(working_directory) / "collector.ini"
            config_path.write_text(
                """
[information_hub]
enabled = false
""".strip(),
                encoding="utf-8",
            )
            probe = (
                "import runpy, sys; "
                "module = runpy.run_path(sys.argv[1]); "
                "result = module['maybe_submit_saved_results_to_hub']("
                "{}, [], config_path=sys.argv[2]); "
                "raise SystemExit("
                "0 if result is not None and result.enabled is False else 1)"
            )
            environment = {
                key: value
                for key, value in os.environ.items()
                if not key.startswith("INFORMATION_HUB_")
            }
            environment.pop("PYTHONPATH", None)

            result = subprocess.run(
                [
                    sys.executable,
                    "-c",
                    probe,
                    str(SCRIPT_PATH),
                    str(config_path),
                ],
                cwd=working_directory,
                env=environment,
                capture_output=True,
                text=True,
                timeout=10,
            )

        self.assertEqual(
            result.returncode,
            0,
            f"stdout={result.stdout}\nstderr={result.stderr}",
        )
        self.assertNotIn("ModuleNotFoundError", result.stderr)


if __name__ == "__main__":
    unittest.main()
