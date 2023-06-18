# LauncherModules

РЕПОЗИТОРИЙ ЯВЛЯЕТСЯ ПОДМОДУЛЕМ!!! Скомпилированные файлы находятся в релизах главного репозитория [GravitLauncher]

Набор публичных модулей, различного назначения для [GravitLauncher]
- Подробное описание и конфигурацию каждого из них можно найти в папке с конкретным модулем
- Некоторые инструкции модулей могут быть устаревшими на GitHub, первостепенным источником является [Wiki]

## Модули для LauncherRuntime
- Именуемые далее как **`_lmodule`**
- Устанавливать в папку **`launcher-modules/`**
- Где найти?
  - В архиве **LauncherModules.zip** из [Releases]
  - При установке скриптом из исходников, модули доступны по пути: 
  ```java{1}:no-line-numbers
  src/modules/<ModuleName>_lmodule/build/libs/<ModuleName>_lmodule.jar
  ```
| Модуль | Описание |
| ------ | ------ |
| [DiscordGame] | Добавляет игровую активность в **Discord** |
| [LauncherGuard] | Добавляет поддержку нативной защиты |
| [LauncherStartScreen] | Добавляет окно загрузки [Launcher]'а до полной инициализации **runtime** |
| [Sentry] | Интеграция [Launcher] с системой отслеживания ошибок **Sentry** |


## Модули для LaunchServer
- Именуемые далее как **`_module`**
- Устанавливать в папку **`modules/`**
- Где найти?
  - В архиве **LaunchServerModules.zip** из [Releases]
  - При установке скриптом из исходников, модули доступны по пути: 
  ```java{1}:no-line-numbers
  src/modules/<ModuleName>_module/build/libs/<ModuleName>_module.jar
  ```
| Модуль | Описание |
| ------ | ------ |
| [AdditionalHash] | Необходим для хеша пароля PHPASS<br>[\[Конфигурация PasswordVerifier\]](https://gravitlauncher.com/auth/#%D0%BA%D0%BE%D0%BD%D1%84%D0%B8%D0%B3%D1%83%D1%80%D0%B0%D1%86%D0%B8%D1%8F-passwordverifier) |
| [DiscordBotConsole] | Модуль позволяет взаимодействовать с [LaunchServer] через Discord клиент |
| [FileAuthSystem] **\*** | Система авторизации с хранением базы пользователей в файле json<br>[\[Метод FileAuthSystem\]](https://gravitlauncher.com/auth/#%D0%BC%D0%B5%D1%82%D0%BE%D0%B4-fileauthsystem) |
| [GenerateCertificate] | Создает сертификаты для подписи бинарных файлов лаунчера (.jar/.exe) |
| [MirrorHelper] | Сборка клиентов с патчем authlib для [GravitLauncher] |
| [MojangSupport] | Добавляет поддержку лицензионных аккаунтов **Mojang/Microsoft** |
| [OpenSSLSignCode] | Позволяет подписывать бинарные файлы своим сертификатом |
| [RemoteControl] | Позволяет выполнять консольные команды [LaunchServer] при помощи HTTP протокола |
| [S3Updates] | Синхронизация папки `updates` с Хранилищем объектов S3 |
| [SentryProGuardUpload] | Снятие ProGuard маппингов с приходящих стактрейсов со стороны лаунчера |
| [Sentry LaunchServer] | Интеграция [LaunchServer] с системой отслеживания ошибок **Sentry** |
| [SystemdNotifer] | Служит для правильного порядка загрузки [LaunchServer] через службу **systemd** |
| [TelegramBotConsole] | Модуль позволяет взаимодействовать с [LaunchServer] через Telegram клиент. |
| [UnsafeCommandPack] | Добавляет дополнительные команды в [GravitLauncher] |

**\*** - `рекомендовано для разработки`

[GravitLauncher]: https://github.com/GravitLauncher/Launcher
[Launcher]: https://github.com/GravitLauncher/LauncherRuntime
[LaunchServer]: https://github.com/GravitLauncher/Launcher
[Wiki]: https://gravitlauncher.com
[Releases]: https://github.com/GravitLauncher/Launcher/releases

[AdditionalHash]: https://github.com/GravitLauncher/LauncherModules/tree/master/AdditionalHash_module
[DiscordBotConsole]: https://github.com/GravitLauncher/LauncherModules/tree/master/DiscordBotConsole_module
[DiscordGame]: https://github.com/GravitLauncher/LauncherModules/tree/master/DiscordGame_lmodule
[FileAuthSystem]: https://github.com/GravitLauncher/LauncherModules/tree/master/FileAuthSystem_module
[GenerateCertificate]: https://github.com/GravitLauncher/LauncherModules/tree/master/GenerateCertificate_module
[LauncherGuard]: https://github.com/GravitLauncher/LauncherModules/tree/master/LauncherGuard_lmodule
[LauncherStartScreen]: https://github.com/GravitLauncher/LauncherModules/tree/master/LauncherStartScreen_lmodule
[MirrorHelper]: https://github.com/GravitLauncher/LauncherModules/tree/master/MirrorHelper_module
[MojangSupport]: https://github.com/GravitLauncher/LauncherModules/tree/master/MojangSupport_module
[OpenSSLSignCode]: https://github.com/GravitLauncher/LauncherModules/tree/master/OpenSSLSignCode_module
[RemoteControl]: https://github.com/GravitLauncher/LauncherModules/tree/master/RemoteControl_module
[S3Updates]: https://github.com/GravitLauncher/LauncherModules/tree/master/S3Updates_module
[SentryProGuardUpload]: https://github.com/GravitLauncher/LauncherModules/tree/master/SentryProGuardUpload_module
[Sentry]: https://github.com/GravitLauncher/LauncherModules/tree/master/Sentry_lmodule
[Sentry LaunchServer]: https://github.com/GravitLauncher/LauncherModules/tree/master/Sentry_module
[SystemdNotifer]: https://github.com/GravitLauncher/LauncherModules/tree/master/SystemdNotifer_module
[TelegramBotConsole]: https://github.com/GravitLauncher/LauncherModules/tree/master/TelegramBotConsole_module
[UnsafeCommandPack]: https://github.com/GravitLauncher/LauncherModules/tree/master/UnsafeCommandPack_module
