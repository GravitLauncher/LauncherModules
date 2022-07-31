# DiscordBotConsole

Позволяет управлять лаунчсервером удаленно через Discord бота.\
Модуль использует библиотеку [JDA](https://github.com/DV8FromTheWorld/JDA/releases/download/v5.0.0-alpha.17/JDA-5.0.0-alpha.17-withDependencies.jar).

#### Установка модуля

1. Скопировать модуль **DiscordBotConsole_module.jar** в папку **/LaunchServer/modules/**
2. Создать бота в панели управления разработчика https://discord.com/developers/applications и скопировать его токен
3. Настроить конфигурацию

#### Конфигурация

```json
{
   "token": "TOKEN",
   "prefix": "!",
   "color": "",
   "avatarEnable": "true",
   "avatar_url": "https://minotar.net/cube/user/%s.png",
   "eventChannelId": 111111,
   "allowUsers": [ 123456, 123457 ],
   "allowRoles": [ 7654321, 7654322 ],
   "events" : {
     "login": true,
     "selectProfile": "true",
     "checkServer": true
   }
}
```

- **token**: Ваш токен бота
- **prefix**: Префикс команд
- **color**: Цвет embed, по умолчанию - рандом
- **avatarEnable**: Включены ли аватарки в embed
- **avatar_url**: Ссылка на скрипт раздачи аватарок
- **eventChannelId**: ID канала для уведомлений от лаунчсервера
- **allowUsers**: ID пользователей, которым разрешено выполнять команды
- **allowRoles**: ID ролей, участникам которых разрешено выполнять команды
- **events**: События, о которых вас будет уведомлять бот
