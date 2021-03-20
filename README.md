# LauncherModules

Набор стандартных, публичных модулей для [GravitLauncher]. Подробное описание и конфигурацию каждого из них можно найти
в папке с этим модулем.

- Все скомпилированные модули находятся в папке **modules** архива **Launcher.zip**, когда вы скачиваете
  Артефакт [GravitLauncher].
- Если вы компилируете [GravitLauncher] скриптом, модули так же собираются и находятся в своих папках:
  */LaunchServer/src/modules/ModuleName_module/build/libs/ModuleName_module.jar*

| Module | Description |
| ------ | ------ |
| [AdditionalHash] | Добавляет новый тип **AuthProvider**. Позволяет проверять хеш паролей *bcrypt* и *phpass*.  |
| [AutoReHash]\* | При любом изменении в папке **
updates** модуль автоматически будет выполнять синхронизацию (syncUpdates). |
| [FileAuthSystem]\* | Система авторизации с хранением базы пользователей в файле json |
| [GenerateCertificate] | Создает сертификаты для подписи **Launcher.**(*jar\|exe*). |
| [MojangSupport] | Добавляет *[GravitLauncher]* поддержку **mojang**. |
| [OneLauncher] | Запрещает запуск двух и более копий лаунчера |
| [OpenSSLSignCode] | Позволяет подписывать **exe** файлы своим сертификатом. |
| [SashokSupport] | Позволяет лаунчеру **sashok724's v3** автоматически обновиться до *[GravitLauncher]*. |
| [Sentry LaunchServer] | Интеграция *[GravitLauncher]* с Sentry. |
| [ServerScriptEngine] | Позволяет выполнять **javascript** код на стороне сервера. |
| [SystemdNotifer] |  Служит для правильного порядка загрузки **LaunchServer** утилитой **systemd**. |
| [UnsafeCommandPack] | Добавляет дополнительные команды в *[GravitLauncher]*. |
| [DiscordRPC] | *(launcher-modules)* Добавляет *Launcher'у* интеграцию с **Discord**. |
| [LauncherStartScreen] | *(launcher-modules)* Добавляет окно загрузки *Launcher'а* до полной инициализации **
runtime**. |
| [Sentry Launcher] | *(launcher-modules)* Интеграция *Launcher'a* с Sentry. |

**рекомендуется к использованию.*

[GravitLauncher]: https://github.com/GravitLauncher/Launcher

[AdditionalHash]: https://github.com/GravitLauncher/LauncherModules/tree/master/AdditionalHash_module

[AutoReHash]: https://github.com/GravitLauncher/LauncherModules/tree/master/AutoReHash_module

[FileAuthSystem]: https://github.com/GravitLauncher/LauncherModules/tree/master/FileAuthSystem_module

[GenerateCertificate]: https://github.com/GravitLauncher/LauncherModules/tree/master/GenerateCertificate_module

[MojangSupport]: https://github.com/GravitLauncher/LauncherModules/tree/master/MojangSupport_module

[OneLauncher]: https://github.com/GravitLauncher/LauncherModules/tree/master/OneLauncher_module

[OpenSSLSignCode]: https://github.com/GravitLauncher/LauncherModules/tree/master/OpenSSLSignCode_module

[SashokSupport]: https://github.com/GravitLauncher/LauncherModules/tree/master/SashokSupport_module

[Sentry LaunchServer]: https://github.com/GravitLauncher/LauncherModules/tree/master/Sentry_module

[ServerScriptEngine]: https://github.com/GravitLauncher/LauncherModules/tree/master/ServerScriptEngine_module

[SystemdNotifer]: https://github.com/GravitLauncher/LauncherModules/tree/master/SystemdNotifer_module

[UnsafeCommandPack]: https://github.com/GravitLauncher/LauncherModules/tree/master/UnsafeCommandPack_module

[DiscordRPC]: https://github.com/GravitLauncher/LauncherModules/tree/master/DiscordRPC_lmodule

[LauncherStartScreen]: https://github.com/GravitLauncher/LauncherModules/tree/master/LauncherStartScreen_lmodule

[Sentry Launcher]: https://github.com/GravitLauncher/LauncherModules/tree/master/Sentry_lmodule
