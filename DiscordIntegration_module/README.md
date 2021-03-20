# DiscordIntegration

Добавляет **LauncherServer'у** интеграцию с *Discord'ом*. То есть станет доступным удаленное управление лаунчером из
дискорд канала, а также логируется авторизация через лаунчер.

#### Установка модуля

1. Скопировать модуль **DiscordIntegration_module.jar** в папку **/LaunchServer/modules/**
2. Скачать библиотеку *[jda]* и положить в папку **/LaunchServer/libraries/**:
3. Выполнить настройку конфигурации:
    - */LaunchServer/config/DiscordIntegration/Config.json*

- `"logAuth": true` - Логировать авторизацию пользователей.
- `"avatarEnable": true` - Отображение головы в логах.
- `"prefix": "!"` - Префикс команд.
- `"url": "https://minotar.net/cube/user/%s.png"` - ссылка на API для возвращения картинки головы.
- `"webhook": "https://discord.com/api/webhooks/{YOUR_WEBHOOK}"` - ссылка на веб-хук для логирования авторизации, а
  также результата выполнения команды.
- `"bot": true` - бот для управления *LaunchServer'ом* через канал.
- `"token": "MY_TOKEN"` - токен бота.
- `"channelID": "CHANNEL_ID"` - ID канала для работы с ботом.
- `"adminOnly": true` - выполнять команды только от пользователей с правами Администратора.

```json
{
  "logAuth": true,
  "avatarEnable": true,
  "prefix": "!",
  "url": "https://minotar.net/cube/user/%s.png",
  "webhook": "https://discord.com/api/webhooks/{YOUR_WEBHOOK}",
  "bot": true,
  "token": "MY_TOKEN",
  "channelID": "CHANNEL_ID",
  "adminOnly": true
}
```

#### Example:

#### Log:

![Log](img/log.png)

#### Commands:

![Command](img/command.png)

[jda]: https://github.com/DV8FromTheWorld/JDA/releases/download/v4.2.0/JDA-4.2.0_168-withDependencies-min.jar
