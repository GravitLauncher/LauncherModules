# Sentry

Интеграция [GravitLauncher] с Sentry

#### Установка модуля

1. Скопировать модуль **Sentry_module.jar** в папку **/LaunchServer/modules/**
2. Скопировать библиотеки https://repo1.maven.org/maven2/io/sentry/sentry/5.0.0-beta.4/sentry-5.0.0-beta.4.jar
   и https://repo1.maven.org/maven2/io/sentry/sentry-log4j2/5.0.0-beta.4/sentry-log4j2-5.0.0-beta.4.jar в папку
   libraries
3. Запустить и остановить **LaunchServer.jar** для создания файла конфигурации.
4. Зарегистрироваться на сайте **Sentry.io** и создайте там проект.
5. После создания проекта потребуется скопировать **DSN** ключ.
    - *Настройки -> Проекты -> Созданный проект -> Client Keys (DSN)*
6. Изменить в файле конфигурации **DSN** на скопированный.

#### Конфигурация

- */LaunchServer/config/SentryServerModule/Config.json*

```json
{
  "dsn": "YOUR_DSN",
  "captureAll": false,
  "setThreadExcpectionHandler": false
}
```

[GravitLauncher]: https://github.com/GravitLauncher/Launcher
