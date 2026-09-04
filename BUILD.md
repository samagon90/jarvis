# 📦 Как получить APK-файл

Проект полностью готов к сборке. APK собирается **автоматически в облаке GitHub Actions**
(там есть интернет и Android SDK — на вашем компьютере ничего устанавливать не нужно).
Есть и способ локальной сборки. Выберите любой вариант.

---

## Вариант 1 (самый простой) — сборка в облаке GitHub Actions

APK соберётся бесплатно на серверах GitHub, без установки чего-либо локально.

### Шаг 1. Добавьте workflow-файл в репозиторий
Создайте в корне репозитория файл **`.github/workflows/build-apk.yml`** с этим содержимым
(ваша учётка на GitHub имеет право на workflow — в отличие от служебного токена бота):

```yaml
name: Build APK

on:
  workflow_dispatch:   # запуск вручную: вкладка Actions → Build APK → Run workflow
  push:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.9'
      - name: Install Android SDK 35 / Build Tools
        run: |
          S="${ANDROID_HOME:-/usr/local/lib/android/sdk}/cmdline-tools/latest/bin/sdkmanager"
          [ -f "$S" ] || S="$(command -v sdkmanager)"
          yes | "$S" --licenses >/dev/null 2>&1 || true
          "$S" --install "platforms;android-35" "build-tools;34.0.0"
      - name: Build debug APK
        env:
          ANDROID_HOME: /usr/local/lib/android/sdk
        run: gradle --no-daemon :app:assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: jarvis-master-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```

### Шаг 2. Запустите сборку
1. Перейдите в репозиторий на GitHub → вкладка **Actions**.
2. Слева выберите **Build APK** → кнопка **Run workflow** → подтвердите.
3. Через 2–5 минут сборка завершится ✅.

### Шаг 3. Скачайте APK
Внизу на странице workflow кликните на артефакт **`jarvis-master-debug-apk`** —
скачается файл **`app-debug.apk`**, который можно установить на телефон.

> Как добавить файл в репозиторий с телефона/компьютера: откройте репозиторий →
> «Add file» → «Create new file» → вставьте путь `.github/workflows/build-apk.yml`
> и содержимое выше → «Commit changes».

---

## Вариант 2 — сборка на своём компьютере (Android Studio)

1. Установите **Android Studio** (https://developer.android.com/studio).
2. Откройте в нём **эту папку проекта** (File → Open).
3. Дождитесь синхронизации Gradle (Android Studio сам скачает JDK, SDK, зависимости).
4. Выберите **Build → Build Bundle(s)/APK(s) → Build APK(s)**.
5. Готовый файл появится в `app/build/outputs/apk/debug/app-debug.apk`.

Либо из терминала (нужны JDK 17 и Android SDK):

```bash
export ANDROID_HOME=/path/to/android-sdk
gradle wrapper --gradle-version 8.9   # один раз, чтобы появились gradlew
./gradlew :app:assembleDebug
# результат: app/build/outputs/apk/debug/app-debug.apk
```

---

## Замечания

- **`gradle/wrapper/gradle-wrapper.jar` отсутствует** (не мог быть скачан в песочнице).
  Это нормально: Android Studio и команда `gradle wrapper` создают его автоматически.
- APK **debug** подписан стандартным debug-ключом и ставится на любой телефон
  с включённой установкой из неизвестных источников.
- Для публикации в Google Play/магазины нужен релизный APK со своей подписью
  (команда `:app:assembleRelease` + файл подписи в `keystore.properties`).
