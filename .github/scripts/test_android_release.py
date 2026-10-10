"""배포를 중단해야 하는 태그, 버전 및 키 처리 조건을 확인한다."""

import base64
import json
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

import android_release


class ReleaseGuardsTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.root_patch = patch.object(android_release, "ROOT", self.root)
        self.root_patch.start()
        self.addCleanup(self.root_patch.stop)
        self.environment_patch = patch.dict(os.environ, {
            "GITHUB_REF_NAME": "v1.0",
            "GITHUB_REF_TYPE": "tag",
        }, clear=True)
        self.environment_patch.start()
        self.addCleanup(self.environment_patch.stop)

    def git(self, *arguments):
        return subprocess.run([
            "git", "-c", "user.name=배포 검증", "-c", "user.email=release-test@example.invalid",
            *arguments,
        ], cwd=self.root, check=True, capture_output=True, text=True)

    def initialize_repository(self):
        self.git("init", "-b", "main")
        self.git("commit", "--allow-empty", "-m", "초기 커밋")
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")

    def write_metadata(self, **overrides):
        element = {"versionName": "1.0", "versionCode": 1, "outputFile": "app-release.apk"}
        element.update(overrides)
        directory = self.root / "app/build/outputs/apk/release"
        directory.mkdir(parents=True)
        (directory / "output-metadata.json").write_text(json.dumps({
            "applicationId": "com.hazuny.noshnote", "elements": [element],
        }))

    def test_accepts_current_main_tag(self):
        self.initialize_repository()
        self.git("tag", "-a", "v1.0", "-m", "첫 배포")
        android_release.check_tag()

    def test_rejects_unmerged_branch_tag(self):
        self.initialize_repository()
        self.git("checkout", "-b", "feature")
        self.git("commit", "--allow-empty", "-m", "미병합 변경")
        self.git("tag", "v1.0")
        with self.assertRaises(subprocess.CalledProcessError):
            android_release.check_tag()

    def test_rejects_checkout_different_from_tag(self):
        self.initialize_repository()
        self.git("tag", "v1.0")
        self.git("commit", "--allow-empty", "-m", "태그 이후 변경")
        self.git("update-ref", "refs/remotes/origin/main", "HEAD")
        with self.assertRaisesRegex(ValueError, "커밋이 다릅니다"):
            android_release.check_tag()

    def test_rejects_non_tag_and_unsafe_tag_names(self):
        for tag in ("main", "v1.0/other", "v1.0;command", "v1", "v1.0\n"):
            with self.subTest(tag=tag), patch.dict(os.environ, {"GITHUB_REF_NAME": tag}):
                with self.assertRaises(ValueError):
                    android_release.release_tag()
        with patch.dict(os.environ, {"GITHUB_REF_TYPE": "branch"}):
            with self.assertRaises(ValueError):
                android_release.release_tag()

    def test_rejects_version_mismatch_before_signing_verification(self):
        self.write_metadata(versionName="1.1")
        with patch.object(android_release, "verify_signatures") as verify:
            with self.assertRaisesRegex(ValueError, "versionName"):
                android_release.collect_assets("v1.0", self.root / "key.jks")
            verify.assert_not_called()
        self.assertFalse((self.root / "app/build/release-assets").exists())

    def test_rejects_invalid_version_code(self):
        self.write_metadata(versionCode=0)
        with self.assertRaisesRegex(ValueError, "versionCode"):
            android_release.collect_assets("v1.0", self.root / "key.jks")

    def test_missing_secrets_cannot_start_gradle(self):
        with patch.object(android_release, "run") as run:
            with self.assertRaisesRegex(ValueError, "Secret 누락"):
                android_release.build()
            run.assert_not_called()

    def test_invalid_base64_cannot_start_gradle(self):
        with patch.dict(os.environ, {
            "KEY": "not base64!", "KEY_PW": "store-password",
            "KEY_ALIAS_NAME": "release", "KEY_ALIAS_PW": "key-password",
        }), patch.object(android_release, "run") as run:
            with self.assertRaises(ValueError):
                android_release.build()
            run.assert_not_called()

    def test_failed_build_removes_key_and_keeps_passwords_out_of_arguments(self):
        observed_keys = []

        def fail_build(command, **kwargs):
            environment = kwargs["env"]
            keystore = Path(environment["NOSHNOTE_KEYSTORE_PATH"])
            observed_keys.append(keystore)
            self.assertEqual(keystore.read_bytes(), b"temporary-key")
            self.assertEqual(keystore.stat().st_mode & 0o777, 0o600)
            self.assertEqual(environment["NOSHNOTE_KEYSTORE_PASSWORD"], "store-password")
            self.assertEqual(environment["NOSHNOTE_KEY_PASSWORD"], "key-password")
            self.assertNotIn("store-password", " ".join(command))
            self.assertNotIn("key-password", " ".join(command))
            raise subprocess.CalledProcessError(1, command)

        with patch.dict(os.environ, {
            "KEY": base64.b64encode(b"temporary-key").decode(),
            "KEY_PW": "store-password", "KEY_ALIAS_NAME": "release",
            "KEY_ALIAS_PW": "key-password", "RUNNER_TEMP": str(self.root),
        }), patch.object(android_release, "run", side_effect=fail_build):
            with self.assertRaises(subprocess.CalledProcessError):
                android_release.build()
        self.assertEqual(len(observed_keys), 1)
        self.assertFalse(observed_keys[0].exists())
        self.assertFalse(observed_keys[0].parent.exists())


if __name__ == "__main__":
    unittest.main()
