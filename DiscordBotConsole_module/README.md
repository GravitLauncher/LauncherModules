# DiscordBotConsole

Позволяет управлять лаунчсервером удаленно через Discord бота.\
Модуль использует библиотеку [JDA](https://github.com/DV8FromTheWorld/JDA/releases/download/v5.0.0-alpha.9/JDA-5.0.0-alpha.9-withDependencies.jar).

#### Установка модуля

1. Скопировать модуль **DiscordBotConsole_module.jar** в папку **/LaunchServer/modules/**
2. Создать бота в панели управления разработчика https://discord.com/developers/applications и скопировать его токен
3. Настроить конфигурацию

#### Конфигурация

```json
{
   "token": "TOKEN",
   "prefix": "!",
   "eventGuildId": 1111111,
   "eventChannelId": 111111,
   "allowUsers": [ 123456, 123457 ],
   "allowRoles": [ 7654321, 7654322 ],
   "events" : {
     "login": true,
     "checkServer": true
   }
}
```

- **token**: ваш токен бота
- **prefix**: префикс команд
- **eventGuildId**: ID вашего Discord сервера
- **eventChannelId**: ID канала для уведомлений от лаунчсервера
- **allowUsers**: ID пользователей, которым разрешено выполнять команды
- **allowRoles**: ID ролей, участникам которых разрешено выполнять команды
- **events**: События, о которых вас будет уведомлять бот