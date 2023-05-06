# Sentry Launcher

Интеграция **[LauncherRuntime](https://github.com/GravitLauncher/LauncherRuntime)** с Sentry

- `Этот модуль позволяет отслеживать ошибки возникающие в лаунчере`
- `При возникновении ошибки будет отправлен стактрейс с подробной дополнительной информацией: для выявления причины, способа воспроизведения и дальнейшего её устранения`
- `Будьте властны над багами!`

![sentry](https://user-images.githubusercontent.com/12544425/236625413-5a7593f3-e5da-4f99-b1df-f1ffb86eb838.jpg)

#### Установка модуля

1. Скопировать модуль **Sentry_lmodule.jar** в папку **/LaunchServer/launcher-modules/**
   - Либо установкой symlink:
     ```
     cd launcher-modules
     ln -s ../src/modules/Sentry_lmodule/build/libs/Sentry_lmodule.jar
     ```
2. Запустить и остановить **LaunchServer.jar** для создания файла конфигурации.
3. Зарегистрироваться на сайте **Sentry.io** и создайте там Java проект.
4. После создания проекта потребуется скопировать **DSN** ключ:
    - `https://<ОРГАНИЗАЦИЯ>.sentry.io/settings/sentry/projects/<ИМЯ_ПРОЕКТА_ДЛЯ_ЛАУНЧЕРА>/keys/ => DSN`
    - **Settings -> Projects -> [Выбрать созданный проект] -> Client Keys (DSN)**
    - **Настройки -> Проекты -> [Выбрать созданный проект] -> Клиентские ключи (DSN)**
5. Изменить в файле конфигурации **DSN** на скопированный.

#### Конфигурация

- **/LaunchServer/config/Sentry/Config.json**

  ```json
  {
     "dsn": "YOUR_DSN",
     "collectSystemInfo": true,
     "collectMemoryInfo": true,
     "ignoreErrors": [
        "auth.wrongpassword"
     ]
  }
  ```

#### Для расшифровки стактрейсов с ProGuard, смотрите следующий модуль:
[\[SentryProGuardUpload_module\]](https://github.com/GravitLauncher/LauncherModules/tree/master/SentryProGuardUpload_module)


#### P.S.

- Настройка в вашей учётной записи: языка, часового пояса и темы по ссылке [\[Account Details\]](https://sentry.io/settings/account/details/)
