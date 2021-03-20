# DiscordGame

Добавляет **Launcher'у** интеграцию с *Discord'ом*. То есть, при наличии *Discord'а* на компьютере игрока, запустившего
один из ваших игровых клиентов, в его аккаунте *Discord'а* будет показывать, что он играет именно у вас.  
Альтернативная, более современная реализация модуля DiscordRPC. **В настоящее время находится в разработке. Инструкция и
функционал может быть неполным**

#### Установка модуля

1. Скопировать модуль **DiscordGame_lmodule.jar** в папку **/LaunchServer/launcher-modules/**
2. Скачать библиотеку **discord-game-sdk4j**  и положить в папку **/LaunchServer/launcher-libraries/**:

```sh
wget https://github.com/JnCrMx/discord-game-sdk4j/releases/download/v0.5/discord-game-sdk4j-0.5.jar
mv discord-game-sdk4j-0.5.jar /LaunchServer/launcher-libraries/
```

3. Добавить в **discord-game-sdk4j-0.5.jar** файлы discord_game_sdk.dll и discord_game_sdk.so из архива DiscordSDK (  )
3. Выполнить настройку конфигурациии:
    - */LaunchServer/config/DiscordRPC/Config.json*

- `"appId": "123456789012345678"` - Секция ClientID у дискорд-бота
- `"largeKey": "icon"` - Название главной картинки у дискорд-бота
- `"smallKey": "small"` - Название дополнительной картинки у дискорд-бота
- `"largeText": "Играю"` - Основной текст
- `"smallText": "projectname.ml"` - Дополнительный текст
- `"profileNameKeyMappings": {"ServerName":"asset1"}` - Набор названий серверов и картинок к ним

```json
{
  "appId": "123456789012345678",
  "firstLine": "Играет на %profileName%",
  "secondLine": "Ник: %username%",
  "largeKey": "icon",
  "smallKey": "small",
  "largeText": "Играю",
  "smallText": "projectname.ml",
  "useAlt": true,
  "altAppId": "123456789012345678",
  "altFirstLine": "В лаунчере",
  "altSecondLine": "Авторизируется",
  "altAuthorizedFirstLine": "В лаунчере",
  "altAuthorizedSecondLine": "Ник: %username%",
  "altLargeKey": "home",
  "altSmallKey": "small",
  "altLargeText": "Дома",
  "altSmallText": "projectname.ml",
  "profileNameKeyMappings": {
    "ServerName1":"asset1",
    "ServerName2":"asset2"
  }
}
```

#### Заметки

- Дополнительные настройки: В конфиге *ProGuard* добавить **club.minnced.discord.rpc.\**** в *keeppackagenames* и *keep
  class*.
- Так же необходимо настроить приложение на *Discord Developer Portal*.
    - Все изображения должны быть с названием нижнего регистра, а пробелы заменены на "_".
- Для отображения статуса у игрока - *Игровая активность* в настройках discord должна быть включена.
- Альтернативная конфигурация `"alt..."` отображает статус открытого **Launcher'а**, тогда как основная конфигурация
  показывает статус запущеного *игрового клиента*.
- Картинка к изображения сервера берется из ассетцов приложения в дискорде (`Rich Presence -> Art Assets`)

[java-discord-rpc]: https://jcenter.bintray.com/club/minnced/java-discord-rpc/2.0.2/java-discord-rpc-2.0.2.jar

[discord-rpc-release]: https://jcenter.bintray.com/club/minnced/discord-rpc-release/v3.4.0/discord-rpc-release-v3.4.0.jar
