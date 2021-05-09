# DiscordIntegration

Добавляет **LauncherServer'у** интеграцию с *Discord'ом*. То есть станет доступным удаленное управление лаунчером из
дискорд канала, а также логируется авторизация через лаунчер.

#### Установка модуля

1. Скопировать модуль **DiscordIntegration_module.jar** в папку **/LaunchServer/modules/**
2. Скачать библиотеку *[jda]* и положить в папку **/LaunchServer/libraries/**:
3. Выполнить настройку конфигурации:
    - */LaunchServer/config/DiscordIntegration/Config.json*

- `"logAuth": true` - Логировать авторизацию пользователей.
- `"profilesEnable": true` - Отображение профилей. // Не работает на 5.2.0+
- `"avatarEnable": true` - Отображение головы в логах.
- `"prefix": "!"` - Префикс команд.
- `"url": "https://minotar.net/cube/user/%s.png"` - ссылка на API для возвращения картинки головы.
- `"token": "MY_TOKEN"` - токен бота.
- `"channelID": "CHANNEL_ID"` - ID канала для работы с ботом.
- `"adminOnly": true` - выполнять команды только от пользователей с правами Администратора.
- `"colorRun": 1` - Цвет Embed запуска лаунчера [0 - 10]
- `"colorAuth": 8` - Цвет Embed авторизации в лаунчере [0 - 10]

```json
{
  "logAuth": true,
  "profilesEnable": true,
  "avatarEnable": true,
  "prefix": "!",
  "url": "https://minotar.net/cube/user/%s.png",
  "token": "MY_TOKEN",
  "channelID": "CHANNEL_ID",
  "adminOnly": true,
  "colorRun": 1,
  "colorAuth": 8
}
```

#### Example:

#### Log:

![Log](img/log.png)

#### Commands:

![Command](img/command.png)

[jda]: https://github.com/DV8FromTheWorld/JDA/releases/download/v4.2.0/JDA-4.2.0_168-withDependencies-min.jar
