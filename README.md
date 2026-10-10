# NoshNote

먹은 음식의 **칼로리와 단백질을 간단히 기록하고 목표와 비교하는 Android 앱**입니다. 음식 템플릿을 미리 만들지 않아도 바로 기록할 수 있으며, 자주 먹는 음식은 템플릿으로 저장해 다음 기록에서 재사용할 수 있습니다.

## 주요 기능

- 음식명, 먹은 양, 칼로리, 단백질을 직접 입력해 바로 기록
- 자주 먹는 음식을 템플릿으로 저장하고, 기록할 때 수량을 입력해 재사용
- 날짜별 섭취 합계와 목표 확인
- 기록 날짜를 달력에서 확인하고 날짜별 기록으로 이동
- 설정에서 식단 기록·음식 템플릿·목표를 JSON 파일로 내보내고 가져오기
- CameraX와 ML Kit를 이용해 영양성분표의 칼로리·단백질을 읽어 입력을 보조
- 한국어·영어·중국어(간체)·일본어·프랑스어·스페인어·러시아어·독일어·브라질 포르투갈어·인도네시아어 리소스 제공

## 기술 구성

| 영역 | 사용 기술 |
|---|---|
| 플랫폼 | Android, minSdk 26 |
| 언어 및 UI | Kotlin, Jetpack Compose, Material 3 |
| 화면 상태 | ViewModel, Kotlin Coroutines, Flow / StateFlow |
| 화면 이동 | Navigation Compose |
| 로컬 데이터 | Room 3 (SQLite) |
| 카메라 및 OCR | CameraX, ML Kit Text Recognition 한국어 모델 |
| 앱 모듈 | 단일 `app` 모듈 |

현재 Gradle 설정의 `compileSdk`와 `targetSdk`는 37이며, 의존성 버전은 `gradle/libs.versions.toml`에서 관리합니다. 제품의 상세 요구사항과 결정된 구조는 아래 문서를 기준으로 합니다.

식단 기록·음식 템플릿·현재 목표에는 Room을 사용합니다. 작은 키-값 설정에 Preferences DataStore를 사용하는 것은 기술 스펙에 결정되어 있지만, 아직 현재 앱 의존성에는 추가되지 않았습니다. Android 자동 백업과 기기 간 전송은 비활성화되어 있습니다. 앱을 삭제하거나 기기를 바꾸기 전에 설정에서 JSON 백업을 직접 내보내야 데이터를 복원할 수 있습니다. JSON을 가져오면 현재 데이터가 교체되며, 실행 전에 확인 경고가 표시됩니다.

## 시작하기

1. Android Studio에서 저장소 루트 폴더를 엽니다.
2. Android SDK와 JDK 17을 사용할 수 있도록 설정하고 Gradle 동기화를 완료합니다.
3. `app` 실행 구성을 선택해 에뮬레이터 또는 Android 기기에서 실행합니다.

터미널에서 디버그 APK를 빌드하려면 저장소 루트에서 실행합니다.

```bash
./gradlew assembleDebug
```

생성되는 APK는 `app/build/outputs/apk/debug/app-debug.apk`에 있습니다.

## CI 및 APK 배포

- `main` 대상 PR과 `main` 푸시에서 GitHub Actions가 단위 테스트, Android Lint, Debug·Release APK 및 Release AAB 빌드를 확인합니다.
- `main`에 병합된 커밋에 `v<versionName>` 태그를 푸시하면 같은 검증을 통과한 뒤 서명된 APK·AAB를 만들고 **GitHub Release 초안**을 생성합니다.
- 초안의 파일을 확인한 뒤 직접 Publish하면 사용자가 APK를 다운로드할 수 있습니다. Play Store 출시는 같은 앱 서명 키를 등록하고 AAB를 업로드하는 흐름으로 준비합니다.
- 키 파일과 서명 비밀번호는 Repository Actions Secrets의 `KEY`, `KEY_PW`, `KEY_ALIAS_NAME`, `KEY_ALIAS_PW`에서 읽습니다.

버전 변경, 태그 발행, Secrets 설정 및 Play 준비 절차는 [배포 및 CI/CD 안내](doc/noshnote-배포-CICD.md)를 참고하세요.

## 저장소 구조

```text
app/src/main/java/com/hazuny/noshnote/
├── data/       # Room 데이터베이스와 Repository
├── ocr/        # 영양성분표 값 후보 파싱
├── ui/         # Compose 화면, 앱 구조, 테마
├── MealViewModel.kt
└── MainActivity.kt
doc/
├── noshnote-PRD.md
├── noshnote-기술스펙.md
├── noshnote-배포-CICD.md
└── noshnote-디자인-프로토타입.html
```

## 제품 문서

- [제품 요구사항 문서(PRD)](doc/noshnote-PRD.md)
- [기술 스펙](doc/noshnote-기술스펙.md)
- [배포 및 CI/CD 안내](doc/noshnote-배포-CICD.md)
- [디자인 프로토타입](doc/noshnote-디자인-프로토타입.html)
- [프로젝트 작업 지침](AGENTS.md)

주요 UX나 비즈니스 흐름을 바꾸면 관련 PRD 항목, 기술 스펙, 프로토타입을 함께 확인하고 영향을 받는 문서를 갱신합니다.

## 데이터 및 개인정보

현재 앱은 식단 기록과 음식 템플릿을 기기 내 로컬 데이터베이스에 저장하는 구조입니다. OCR은 카메라 프레임의 텍스트를 인식해 사용자가 입력값을 확인하도록 돕습니다. 계정이나 서버 동기화는 현재 제품 범위에 포함하지 않습니다.
