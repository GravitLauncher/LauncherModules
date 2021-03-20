# UnsafeCommandPack

Данный модуль добавляет дополнительные команды в [GravitLauncher]

#### Установка модуля

1. Скопировать модуль **UnsafeCommandPack_module.jar** в папку **/LaunchServer/modules/**
2. Настройка не требуется.

#### Список дополнительных команд

```sh
setsecuritymanager [allow, logger, system] - Вызов System.setSecurityManager для тестирования.
sendauth [connectUUID] [username] [auth_id] [client type] (permissions) (client uuid) - Ручная отправка события AuthEvent соеденению в обход AuthProvider.
newdownloadasset [version] [dir] - Скачать ассеты прямо с Mojang сайта, любой версии.
newdownloadclient [version] [dir] - Скачать клиент прямо с Mojang сайта, любой версии. Профиль придется создать самостоятельно.
patcher [patcher name or class] [path] [test mode(true/false)] (other args) - Запутсить патчер на основе ASM. Позволяет искать пакетхаки в модах (findPacketHack), RAT (findRemote/findDefineClass), UnsafeSunAPI (findSun), поиск и замена любых вызовов по опкоду INVOKESTATIC (pro.gravit.launchermodules.unsafecommands.patcher.StaticReplacerPatcher).
loadjar [jarfile] - Добавить в SystemClassLoader любой JAR (используя javaagent).
registercomponent [name] [classname] - Зарегистрировать компонент по классу.
```

[GravitLauncher]: https://github.com/GravitLauncher/Launcher
