# Sentry Launcher

Интеграция **Launcher** с Sentry

#### Установка модуля

1. Скопировать модуль **Sentry_lmodule.jar** в папку **/LaunchServer/launcher-modules/**
2. Запустить и остановить **LaunchServer.jar** для создания файла конфигурации.
3. Зарегистрироваться на сайте **Sentry.io** и создайте там проект.
4. После создания проекта потребуется скопировать **DSN** ключ.
    - *Настройки -> Проекты -> Созданный проект -> Client Keys (DSN)*
5. Изменить в файле конфигурации **DSN** на скопированный.

#### Конфигурация

- */LaunchServer/config/SentryLauncher/Config.json*

```json
{
  "dsn": "YOUR_DSN",
  "captureAll": false,
  "setThreadExcpectionHandler": false
}
```

[GravitLauncher]: https://github.com/GravitLauncher/Launcher
