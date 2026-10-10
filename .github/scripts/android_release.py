"""태그 배포의 버전, 서명 및 산출물을 검증한다."""

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile


ROOT = Path(__file__).resolve().parents[2]
TAG_PATTERN = r"v[0-9]+(?:\.[0-9]+){1,2}(?:-[A-Za-z0-9]+(?:[.-][A-Za-z0-9]+)*)?"


def release_tag():
    tag = os.environ.get("GITHUB_REF_NAME", "")
    if os.environ.get("GITHUB_REF_TYPE") != "tag" or not re.fullmatch(TAG_PATTERN, tag):
        raise ValueError("v1.0, v1.0.1 또는 v1.0.1-beta.1 형식의 버전 태그가 필요합니다.")
    return tag


def run(command, **kwargs):
    return subprocess.run(command, cwd=ROOT, check=True, **kwargs)


def check_tag():
    tag = release_tag()
    run(["git", "merge-base", "--is-ancestor", "HEAD", "origin/main"])
    tagged_commit = run(["git", "rev-parse", f"refs/tags/{tag}^{{commit}}"], capture_output=True, text=True).stdout.strip()
    current_commit = run(["git", "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip()
    if tagged_commit != current_commit:
        raise ValueError("현재 체크아웃과 배포 태그의 커밋이 다릅니다.")
    print(f"main에 포함된 배포 태그 확인: {tag}")


def verify_signatures(keystore, apk, bundle):
    # 비밀번호는 명령 인자가 아닌 환경변수로 전달해 프로세스 목록에 남기지 않는다.
    certificate = run([
        "keytool", "-exportcert", "-keystore", str(keystore),
        "-alias", os.environ["KEY_ALIAS_NAME"], "-storepass:env", "KEY_PW",
    ], capture_output=True).stdout
    expected_digest = hashlib.sha256(certificate).hexdigest()

    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not sdk:
        raise ValueError("Android SDK 경로 환경변수가 필요합니다.")
    apksigner = Path(sdk) / "build-tools/36.0.0/apksigner"
    verification = run([
        str(apksigner), "verify", "--verbose", "--print-certs", str(apk),
    ], capture_output=True, text=True).stdout
    apk_digests = re.findall(r"Signer #\d+ certificate SHA-256 digest: ([0-9a-fA-F:]+)", verification)
    if not apk_digests or {digest.replace(":", "").lower() for digest in apk_digests} != {expected_digest}:
        raise ValueError("APK의 서명 인증서가 선택한 키와 다릅니다.")

    # 저장소의 인증서를 신뢰 기준으로 삼아 AAB의 미서명 항목과 다른 서명자를 거부한다.
    bundle_verification = run([
        "jarsigner", "-verify", "-strict", "-keystore", str(keystore),
        "-storepass:env", "KEY_PW", str(bundle), os.environ["KEY_ALIAS_NAME"],
    ], capture_output=True, text=True).stdout
    if "jar verified." not in bundle_verification:
        raise ValueError("AAB가 정상적으로 서명되지 않았습니다.")
    print("APK와 AAB의 서명 검증 완료")
    return expected_digest


def collect_assets(tag, keystore):
    release_dir = ROOT / "app/build/outputs/apk/release"
    metadata = json.loads((release_dir / "output-metadata.json").read_text())
    if metadata["applicationId"] != "com.hazuny.noshnote" or len(metadata["elements"]) != 1:
        raise ValueError("NoshNote 단일 APK 산출물이 필요합니다.")
    version = metadata["elements"][0]
    if version["versionName"] != tag[1:]:
        raise ValueError("태그의 버전과 앱의 versionName이 다릅니다.")
    if not isinstance(version["versionCode"], int) or version["versionCode"] < 1:
        raise ValueError("양수 versionCode가 필요합니다.")

    apk = release_dir / version["outputFile"]
    bundle = ROOT / "app/build/outputs/bundle/release/app-release.aab"
    digest = verify_signatures(keystore, apk, bundle)
    assets = ROOT / "app/build/release-assets"
    if assets.exists():
        shutil.rmtree(assets)
    assets.mkdir(parents=True)
    shutil.copyfile(apk, assets / f"NoshNote-{tag}.apk")
    shutil.copyfile(bundle, assets / f"NoshNote-{tag}.aab")
    mapping = ROOT / "app/build/outputs/mapping/release/mapping.txt"
    if mapping.exists():
        shutil.copyfile(mapping, assets / "mapping.txt")
    release_metadata = {
        "tag": tag,
        "commit": run(["git", "rev-parse", "HEAD"], capture_output=True, text=True).stdout.strip(),
        "applicationId": metadata["applicationId"],
        "versionName": version["versionName"],
        "versionCode": version["versionCode"],
        "signingCertificateSha256": digest,
    }
    (assets / "release-metadata.json").write_text(json.dumps(release_metadata, indent=2) + "\n")
    checksums = [f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n" for path in sorted(assets.iterdir())]
    (assets / "SHA256SUMS").write_text("".join(checksums))
    print(f"검증된 배포 파일 준비 완료: {tag}, versionCode={version['versionCode']}")


def build():
    tag = release_tag()
    required = ("KEY", "KEY_PW", "KEY_ALIAS_NAME", "KEY_ALIAS_PW")
    missing = [name for name in required if not os.environ.get(name)]
    if missing:
        raise ValueError("필수 Secret 누락: " + ", ".join(missing))

    key_bytes = base64.b64decode("".join(os.environ["KEY"].split()), validate=True)
    if not key_bytes:
        raise ValueError("KEY에 빈 파일을 등록할 수 없습니다.")
    # 키 파일은 캐시와 산출물 폴더 밖에 두고 실패 시에도 임시 폴더와 함께 제거한다.
    with tempfile.TemporaryDirectory(prefix="noshnote-signing-", dir=os.environ.get("RUNNER_TEMP")) as temporary:
        keystore = Path(temporary) / "release.jks"
        keystore.write_bytes(key_bytes)
        keystore.chmod(0o600)
        environment = os.environ.copy()
        environment.update({
            "NOSHNOTE_KEYSTORE_PATH": str(keystore),
            "NOSHNOTE_KEYSTORE_PASSWORD": os.environ["KEY_PW"],
            "NOSHNOTE_KEY_ALIAS": os.environ["KEY_ALIAS_NAME"],
            "NOSHNOTE_KEY_PASSWORD": os.environ["KEY_ALIAS_PW"],
        })
        run([
            "./gradlew", ":app:lintRelease", ":app:assembleRelease", ":app:bundleRelease",
            "--no-daemon", "--no-configuration-cache", "--stacktrace",
        ], env=environment)
        collect_assets(tag, keystore)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("check-tag", "build"))
    arguments = parser.parse_args()
    try:
        if arguments.command == "check-tag":
            check_tag()
        else:
            build()
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError) as error:
        print(f"릴리스 실패: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
