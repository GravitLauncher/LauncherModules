# LauncherModules

РЕПОЗИТОРИЙ ЯВЛЯЕТСЯ ПОДМОДУЛЕМ!!! Скомпилированные файлы находятся в релизах главного репозитория [GravitLauncher]

Набор стандартных, публичных модулей для [GravitLauncher]. Подробное описание и конфигурацию каждого из них можно найти в папке с этим модулем. Некоторые инструкции модулей могут быть устаревшими, первостепенным источником является [Wiki]

- Модули для LaunchServer, именуемые далее как **\_module**, находятся в архиве **LaunchServerModules.zip** и помещаются в папку `modules/` [Releases]
- Модули для LauncherRuntime, именуемые далее как **\_lmodule**, находятся в архиве **LauncherModules.zip** и помещаются в папку `launcher-modules/` [Releases]
- Если вы компилируете [GravitLauncher] скриптом, модули так же собираются и находятся в своих папках:
  */LaunchServer/src/modules/ModuleServerName_module/build/libs/ModuleServerName_module.jar*
  */LaunchServer/src/modules/ModuleRuntimeName_lmodule/build/libs/ModuleRuntimeName_lmodule.jar*


| Module | Description |
| ------ | ------ |
| [AdditionalHash] | Добавляет новый тип **AuthProvider**. Позволяет проверять хеш паролей *bcrypt* и *phpass*.  |
| [FileAuthSystem]\* | Система авторизации с хранением базы пользователей в файле json |
| [GenerateCertificate] | Создает сертификаты для подписи **Launcher.**(*jar\|exe*). |
| [MojangSupport] | Добавляет *[GravitLauncher]* поддержку **mojang**. |
| [OneLauncher] | Запрещает запуск двух и более копий лаунчера |
| [OpenSSLSignCode] | Позволяет подписывать **exe** файлы своим сертификатом. |
| [Sentry LaunchServer] | Интеграция *[GravitLauncher]* с Sentry. |
| [SystemdNotifer] |  Служит для правильного порядка загрузки **LaunchServer** утилитой **systemd**. |
| [UnsafeCommandPack] | Добавляет дополнительные команды в *[GravitLauncher]*. |
| [DiscordRPC] | *(launcher-modules)* Добавляет *Launcher'у* интеграцию с **Discord**. |
| [LauncherStartScreen] | *(launcher-modules)* Добавляет окно загрузки *Launcher'а* до полной инициализации **
runtime**. |
| [Sentry Launcher] | *(launcher-modules)* Интеграция *Launcher'a* с Sentry. |

**рекомендуется к использованию.*

[GravitLauncher]: https://github.com/GravitLauncher/Launcher

[AdditionalHash]: https://github.com/GravitLauncher/LauncherModules/tree/master/AdditionalHash_module

[FileAuthSystem]: https://github.com/GravitLauncher/LauncherModules/tree/master/FileAuthSystem_module

[GenerateCertificate]: https://github.com/GravitLauncher/LauncherModules/tree/master/GenerateCertificate_module

[MojangSupport]: https://github.com/GravitLauncher/LauncherModules/tree/master/MojangSupport_module

[OneLauncher]: https://github.com/GravitLauncher/LauncherModules/tree/master/OneLauncher_module

[OpenSSLSignCode]: https://github.com/GravitLauncher/LauncherModules/tree/master/OpenSSLSignCode_module

[Sentry LaunchServer]: https://github.com/GravitLauncher/LauncherModules/tree/master/Sentry_module

[SystemdNotifer]: https://github.com/GravitLauncher/LauncherModules/tree/master/SystemdNotifer_module

[UnsafeCommandPack]: https://github.com/GravitLauncher/LauncherModules/tree/master/UnsafeCommandPack_module

[LauncherStartScreen]: https://github.com/GravitLauncher/LauncherModules/tree/master/LauncherStartScreen_lmodule

[Sentry Launcher]: https://github.com/GravitLauncher/LauncherModules/tree/master/Sentry_lmodule

[Wiki]: https://launcher.gravit.pro

[Releases]: https://github.com/GravitLauncher/Launcher/releases
