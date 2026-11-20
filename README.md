# 챔의 달력

눈부시지 않은 파스텔톤 달력. 날짜마다 **일정**(시간·색)과 **할 일**을 적어요. 달력을 옆으로 밀면 달이 넘어가요.

## 안드로이드 앱 설치
휴대폰에서 이 링크를 열어 APK를 받고 설치해요.
**https://github.com/thsalstjr5-prog/cham-calendar/releases/latest/download/cham-calendar.apk**

- 처음 설치할 때 "출처를 알 수 없는 앱" 설치를 허용해야 해요.
- 새 버전도 같은 링크에서 받아 덮어 설치하면 돼요. 기록은 그대로 남아요.
- 기록은 폰 안에만 저장돼요. 폰을 바꾸기 전에 맨 아래 "백업 저장"으로 파일을 보관하세요.

## 웹 버전
https://thsalstjr5-prog.github.io/cham-calendar/ (크롬 메뉴 → 홈 화면에 추가)

## 고치는 법
- 화면은 `index.html` 하나에 있어요. 고친 뒤 `main`에 올리면 웹 버전이 바로 바뀌고, GitHub Actions가 새 APK를 만들어 위 링크에 올려요.
- 앱 버전을 올릴 때는 `android/app/build.gradle`의 `versionCode`를 1씩 올려요.
- 로컬 빌드: `android/app/cham-calendar.keystore`, `android/keystore.properties`를 넣고 `npm ci && npm run apk` (Android SDK, JDK 21 필요)

## 참고
- 제목 글씨: 그리운 체리한스푼 (그리운, 무료·상업 이용 가능, 폰트 파일 판매·개작 금지)
- 본문 글씨: Gothic A1 (Google Fonts, OFL)
- 앱 서명 열쇠는 저장소에 없어요. 따로 보관한 `cham-calendar.keystore`와 `keystore.properties`가 있어야 같은 앱으로 업데이트돼요. 열쇠를 잃어버리면 앱을 지우고 새로 설치해야 해요(백업 파일로 기록은 옮길 수 있어요).
- 자동 빌드를 쓰려면 저장소 Settings → Secrets and variables → Actions에 `CHAM_KEYSTORE_BASE64`(열쇠 파일을 base64로 바꾼 값)와 `CHAM_KEYSTORE_PASSWORD`를 등록해요. 없으면 빌드를 건너뛰어요.
- `sw.js`의 `VERSION`을 올리면 웹 버전 보관본이 새로 바뀌어요.
