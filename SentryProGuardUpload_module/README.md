# SentryProGuardUpload

Снятие ProGuard маппингов с приходящих стактрейсов со стороны лаунчера

- `Устанавливается как модуль для лаунчсервера`
- `Является дополнением к модулю:` [\[Sentry_lmodule\]](https://github.com/GravitLauncher/LauncherModules/tree/master/Sentry_lmodule)
- `Отправляет файл маппингов на Sentry при компиляции лаунчера, что позволяет расшифровать код ошибок`
- `Использовать только при включённом ProGuard и активном параметре создания маппингов в конфигурации лаунчсервера`

![sentry-cli](https://user-images.githubusercontent.com/12544425/236628763-dbbf9a6d-f74c-40a8-a222-7a636c51c53f.png)

#### Предварительная настройка системы. Установка **[\[Sentry-CLI\]](https://docs.sentry.io/product/cli/installation/)**

- `Утилита для командной строки, которая будет отправлять маппинги на сервера Sentry`
  - Установка на Linux:
    ```bash
    curl -sL https://sentry.io/get-cli/ | sh
    ```
  - Яйцо для Pterodactyl (Содержит в себе `sentry-cli` с автообновлением): **[\[gravit-eggs by XJIuPa\]](https://github.com/gravit-core/gravit-eggs)**

#### Установка модуля

1. Скопировать модуль **SentryProGuardUpload_module.jar** в папку **/LaunchServer/modules/**
   - Либо установкой symlink:
     ```
     cd modules
     ln -s ../src/modules/SentryProGuardUpload_module/build/libs/SentryProGuardUpload_module.jar
     ```
2. Запустить и остановить **LaunchServer.jar** для создания файла конфигурации.
3. Создание токена доступа:
   ```
   https://<ИМЯ_ОРГАНИЗАЦИИ>.sentry.io/settings/account/api/auth-tokens/
   ```
   - Права токена:
     ```
     project:read
     project:releases
     org:read
     ```
4. Добавьте строку `-useuniqueclassmembernames` в файл **/LaunchServer/proguard/proguard.config**
5. Установите параметр `stripLineNumbers` на **`false`** в конфиге лаунчсервера **/LaunchServer/LaunchServer.json**
6. Измените в файле конфигурации `authToken` на созданный токен доступа
7. Измените в файле конфигурации `org` на ваше <ИМЯ_ОРГАНИЗАЦИИ>
8. Измените в файле конфигурации `project` на ваше <ИМЯ_ПРОЕКТА_ДЛЯ_ЛАУНЧЕРА>

#### Конфигурация

- **/LaunchServer/config/SentryProGuardUpload/Config.json**

```json
{
  "sentryCliPath": "sentry-cli",
  "authToken": "<ВАШ_ТОКЕН>",
  "org": "<ИМЯ_ОРГАНИЗАЦИИ>",
  "project": "<ИМЯ_ПРОЕКТА_ДЛЯ_ЛАУНЧЕРА>",
  "url": "",
  "mappingPath": "proguard/mappings.pro",
  "customArgs": [],
  "customArgsBefore": []
}
```
