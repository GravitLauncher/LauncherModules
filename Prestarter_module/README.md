# Sentry ServerModule

Интеграция **[LaunchServer](https://github.com/GravitLauncher/Launcher)** с Sentry

- `Этот модуль позволяет отслеживать ошибки возникающие на стороне вашего лаунчсервера`

![sentry-java](https://user-images.githubusercontent.com/12544425/236633515-88d3d837-35c0-47e4-a4c1-765d9cb152bc.png)

#### Установка модуля

1. Скопировать модуль **Sentry_module.jar** в папку **/LaunchServer/modules/**
   - Либо установкой symlink:
     ```
     cd modules
     ln -s ../src/modules/Sentry_module/build/libs/Sentry_module.jar
     ```
2. Запустить и остановить **LaunchServer.jar** для создания файла конфигурации.
3. Зарегистрироваться на сайте **Sentry.io** и создайте там Java проект.
4. После создания проекта потребуется скопировать **DSN** ключ:
    - `https://<ОРГАНИЗАЦИЯ>.sentry.io/settings/sentry/projects/<ИМЯ_ПРОЕКТА_ДЛЯ_ЛАУНЧСЕРВЕРА>/keys/ => DSN`
    - **Settings -> Projects -> [Выбрать созданный проект] -> Client Keys (DSN)**
    - **Настройки -> Проекты -> [Выбрать созданный проект] -> Клиентские ключи (DSN)**
5. Изменить в файле конфигурации **YOUR_DSN** на скопированный.

#### Конфигурация

- **/LaunchServer/config/SentryServerModule/Config.json**

  ```json
  {
    "dsn": "YOUR_DSN",
    "sampleRate": 1.0,
    "enableTracing": false,
    "tracingSampleRate": 1.0,
    "addSentryAppender": true,
    "filterExceptions": true,
    "requestTracker": true,
    "captureRequestData": false,
    "captureRequestError": false,
    "appenderLogLevel": "ERROR",
    "ignoreErrors": [
      "auth.wrongpassword", "auth.require2fa", "auth.usernotfound", "auth.require.factor."
    ]
  }
  ```

#### P.S.

- Настройка в вашей учётной записи: языка, часового пояса и темы по ссылке [\[Account Details\]](https://sentry.io/settings/account/details/)
