# NoshNote 배포 및 CI/CD

## 1. 배포 방식

GitHub Actions로 앱을 검증하고, 개발자가 버전 태그를 발행할 때 서명된 APK와 AAB를 생성한다. GitHub Release는 초안으로 만들고 파일을 확인한 뒤 개발자가 직접 공개한다. APK는 GitHub에서 직접 설치하는 용도이며, AAB는 향후 Play Console에 업로드할 파일이다.

GitHub APK와 미래 Play 배포 앱의 앱 서명 키는 동일하게 유지한다. 현재는 이 키를 AAB 업로드에도 사용한다. 별도 업로드 키는 Play의 필수 정책이 아니며 나중에 분리할 수 있다. Play App Signing 설정에서는 Google이 다른 앱 서명 키를 생성하도록 두지 말고 **기존 앱 서명 키를 제공하는 옵션**을 선택한다.

## 2. 파이프라인

| 실행 조건 | 워크플로 | 처리 |
|---|---|---|
| `main` 대상 PR 생성·커밋 갱신 | `.github/workflows/ci.yml` | 배포 중단 조건 테스트, 앱 단위 테스트, Debug·Release Lint, Debug·Release APK 및 Release AAB 빌드 |
| `main` 푸시 또는 병합 | 같은 CI | 통합된 코드 재검증 |
| Actions에서 Android CI 수동 실행 | 같은 CI | 선택한 브랜치 검증 |
| `v*` 태그 푸시 | `.github/workflows/release.yml` | 태그 확인 → 같은 CI → 서명 빌드 및 검증 → 산출물 보관 → Release 초안 생성 |

- 태그는 `v1.0`, `v1.0.1`, `v1.0.1-beta.1` 같은 형식을 사용하며 `v` 다음 값이 앱의 `versionName`과 같아야 한다.
- 태그 커밋이 원격 `main` 이력에 포함되어 있지 않으면 배포를 중단한다.
- PR의 CI에는 Secrets를 전달하지 않는다. Release 빌드는 서명 Secret을 해당 빌드 단계에서만 전달받는다.
- CI·서명 빌드는 저장소 읽기 권한만 사용하고 Release 초안 생성 작업에만 `contents: write`를 부여한다. 별도 PAT 또는 Deploy key는 필요하지 않다.
- 버전·서명·체크섬 검증이 실패하면 이후 배포 단계를 실행하지 않는다. 초안 생성 중 통신 장애로 파일 첨부가 일부 실패해도 공개되지 않으며 같은 태그의 실행을 다시 시도할 수 있다.
- 같은 태그를 재실행하면 기존 **초안**의 산출물을 갱신한다. 이미 공개한 Release는 덮어쓰지 않는다.
- 기존 Compose 기기 테스트 및 실제 카메라 OCR 품질 확인은 이 자동 검증에 포함되지 않는다. 공개 전 기기에서 주요 화면, 저장·조회 및 OCR을 확인한다.

## 3. 빌드 환경

`.github/actions/android-build/action.yml`에서 공통 도구를 준비한다.

- GitHub 호스팅 Ubuntu 24.04 실행 환경
- Temurin JDK 17
- Android SDK `platforms;android-37.0`, Build Tools `36.0.0`
- 저장소의 Gradle Wrapper 및 Version Catalog에 정의된 의존성
- Gradle 의존성 캐시 사용, 구성 캐시는 사용하지 않음
- 외부 Actions는 확인한 커밋 SHA로 고정

Android SDK나 JDK 기준을 바꾸면 앱 Gradle 설정과 이 공통 빌드 환경도 함께 갱신한다. 빌드에 쓰이는 Gradle Wrapper 배포 파일은 저장소에 기록된 SHA-256으로 검증한다.

## 4. Secrets

저장소 Settings → Secrets and variables → Actions → Repository secrets에 다음 값을 등록한다.

| 이름 | 값 |
|---|---|
| `KEY` | 서명 키 저장소 파일 전체를 Base64로 변환한 텍스트 |
| `KEY_PW` | 키 저장소 비밀번호 |
| `KEY_ALIAS_NAME` | 저장소 안에서 사용할 키의 Alias |
| `KEY_ALIAS_PW` | 선택한 키의 비밀번호. 저장소 비밀번호와 달라도 됨 |

Mac에서 키 파일 폴더의 터미널을 열고 실제 파일명을 넣어 실행하면 `KEY`에 붙여 넣을 내용이 복사된다.

```bash
base64 -i "$PWD/키파일.jks" | tr -d '\n' | pbcopy
```

Base64는 파일의 텍스트 표현이며 암호화가 아니다. 이 명령은 비밀번호나 Alias를 별도로 복사하지 않으므로 나머지 세 항목은 생성 시 정한 값을 직접 입력한다. Secrets는 값을 암호화해 보관하고 실행 중 필요한 작업에 원래 값을 전달한다.

`.jks` 하나에 여러 앱의 키가 있다면, GitHub에 등록하기 전에 NoshNote에 사용할 키 항목만 별도 저장소 파일로 내보낸다. 전체 파일을 Secret에 등록하면 다른 Alias의 키 항목도 함께 포함된다.

빌드 작업은 `KEY`를 실행 환경의 임시 폴더에 복원하고 파일 접근 권한을 제한한다. Gradle에는 다음 환경변수로 서명 정보를 전달한다.

| Gradle 환경변수 | 원본 |
|---|---|
| `NOSHNOTE_KEYSTORE_PATH` | 복원한 임시 키 파일 경로 |
| `NOSHNOTE_KEYSTORE_PASSWORD` | `KEY_PW` |
| `NOSHNOTE_KEY_ALIAS` | `KEY_ALIAS_NAME` |
| `NOSHNOTE_KEY_PASSWORD` | `KEY_ALIAS_PW` |

네 환경변수가 모두 없으면 로컬·CI Release 빌드는 서명 없이 수행한다. 일부만 설정되면 실패한다. 배포 스크립트는 네 Secret이 모두 있어야 빌드를 시작한다. 키 파일은 성공·실패 시 임시 폴더와 함께 제거하며 캐시나 산출물에 포함하지 않는다. 실행 환경 자체가 강제 종료된 경우에는 GitHub 호스팅 실행 환경 폐기 시 정리된다.

키 파일과 비밀번호는 GitHub 외에도 별도로 백업한다. Secret은 설정 화면에서 원래 값을 다시 조회하는 보관소로 사용할 수 없다.

## 5. 개발 및 첫 배포 순서

1. 작업 브랜치에서 변경하고 로컬에서 필요한 검증을 수행한다.
2. 브랜치를 푸시하고 `main` 대상 PR을 만든다. CI의 `Android 검증` 결과와 변경 내용을 확인한다.
3. PR을 병합하고 `main` CI 결과를 확인한다.
4. `app/build.gradle.kts`의 `versionName`과 `versionCode`를 정리한다. 버전 변경도 PR로 병합한다. 현재 값은 `1.0`과 `1`이며 첫 태그는 `v1.0`으로 만들 수 있다. 이후 배포마다 `versionCode`를 증가시킨다.
5. 검증된 `main` 커밋에 버전과 일치하는 태그를 만들어 푸시한다.

```bash
git switch main
git pull --ff-only origin main
git tag -a v1.0 -m "NoshNote 1.0 배포"
git push origin v1.0
```

6. Actions의 Android Release 실행 결과와 Releases의 초안을 확인한다.
7. APK를 기기에 설치해 기능을 확인한다. 이후 버전에서는 이전 Release APK 위에 업데이트하고 데이터가 유지되는지도 확인한다. Debug APK는 Release 키와 서명이 달라 직접 업데이트되지 않을 수 있으므로 필요한 기록은 JSON으로 내보낸 뒤 설치를 전환한다.
8. 릴리스 설명과 파일을 확인한 뒤 초안에서 Publish release를 누른다. 베타 태그는 prerelease로 표시된다.

태그를 푸시하면 Secrets를 사용하는 실제 서명 빌드가 시작된다. 단순 CI 확인에는 PR 또는 Android CI의 수동 실행을 사용한다. 이미 공개한 버전을 수정할 때는 새 `versionName`, 증가한 `versionCode`, 새 태그를 사용한다. 코드가 아닌 일시적인 빌드 장애나 Secret 설정 오류를 고친 경우에는 Actions에서 같은 태그의 실행을 재시도할 수 있다.

파이프라인은 태그·`versionName` 일치와 양수 `versionCode`를 확인한다. 이전 Play 업로드의 코드 값은 조회하지 않으므로 개발자가 `versionCode` 증가를 관리해야 한다.

## 6. 산출물과 실패 확인

| 산출물 | 위치 / 보관 |
|---|---|
| 단위 테스트·Lint 보고서 | CI 실행의 `ci-reports-*` Artifact, 14일 |
| Debug APK | CI 실행의 `debug-apk-*` Artifact, 7일. 개발 확인용 |
| 서명된 `NoshNote-v<버전>.apk` | Release 실행의 Artifact 및 GitHub Release |
| 서명된 `NoshNote-v<버전>.aab` | 같은 위치. Play 업로드용 |
| `mapping.txt` | 최적화 매핑이 생성되면 같은 위치에 보관 |
| `release-metadata.json` | 커밋, 앱 식별자, 버전 이름·코드, 서명 인증서 SHA-256 |
| `SHA256SUMS` | 위 배포 파일의 SHA-256 체크섬 |

Release Artifact는 30일 보관한다. GitHub Release에 첨부한 파일은 Artifact의 기간 만료와 별개로 유지된다. 임시 키 파일과 비밀번호는 첨부하지 않는다.

서명 검증은 APK의 서명이 유효하며 선택한 키의 인증서와 일치하는지 확인하고, AAB는 같은 키를 기준으로 JAR 서명 및 미서명 항목을 확인한다. 초안 생성 작업은 Artifact를 받은 뒤 체크섬을 다시 확인한다.

실패 시 Actions에서 실패한 단계와 검증 보고서를 확인한다. 버전이 다르면 코드와 태그를 맞추고, Secret 오류면 해당 Secret을 수정한다. 공개된 앱에서 발견한 오류는 기존 릴리스 덮어쓰기 대신 새 버전으로 배포한다.

## 7. 병합 전 CI 통과 요구

CI 실행과 병합 제한은 별도 설정이다. CI가 실제로 한 번 실행된 뒤 저장소 Settings → Rules → Rulesets에서 `main`에 적용할 규칙을 만들고, PR 병합 전 상태 검사로 **Android 검증**을 요구하도록 설정할 수 있다. 혼자 개발하는 경우에도 코드 변경과 CI 결과를 직접 확인한 뒤 병합한다.

## 8. Play 출시 준비

1. Play Console 개발자 계정과 앱을 준비하고 `com.hazuny.noshnote`를 유지한다.
2. Play App Signing에서 직접 보유한 앱 서명 키를 제공한다. Console이 제공하는 PEPK 도구와 명령을 사용해 NoshNote의 Alias에 해당하는 키만 암호화해 제출한다.
3. Play의 앱 서명 인증서 SHA-256과 `release-metadata.json`의 `signingCertificateSha256`이 일치하는지 확인한다.
4. 서명된 AAB를 내부 테스트 트랙에 업로드하고, GitHub의 이전 APK에서 Play 버전으로 업데이트되는지 확인한다. Play 버전의 `versionCode`는 이전 설치본보다 높아야 한다.
5. 출시 정보와 스토어 요구사항을 준비한 뒤 공개 트랙으로 진행한다. Play 자동 업로드는 별도 작업에서 서비스 계정과 권한을 준비해 파이프라인 뒤에 추가한다.

Play App Signing 등록 후 로컬 키를 유실하면 새 업로드 키를 만들고 재설정을 요청해 Play 업데이트를 이어갈 수 있다. Google이 보관한 앱 서명 개인키는 다시 내려받을 수 없다. GitHub에서 독립적으로 서명하려면 기존 키 백업이 필요하며, 백업이 없다면 Play에서 서명된 범용 APK를 받아 외부 배포하는 흐름을 사용한다.

## 9. 공식 자료

- [Android 앱 서명 및 Play App Signing](https://developer.android.com/studio/publish/app-signing)
- [Play App Signing 키 등록·재설정·외부 배포](https://support.google.com/googleplay/android-developer/answer/9842756)
- [GitHub Actions Secrets](https://docs.github.com/en/actions/how-tos/write-workflows/choose-what-workflows-do/use-secrets)
- [GitHub Rulesets](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets/about-rulesets)
