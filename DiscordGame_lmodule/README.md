# DiscordGame

Добавляет **Launcher'у** интеграцию с *Discord'ом*. То есть, при наличии *Discord'а* на компьютере игрока, запустившего
один из ваших игровых клиентов, в его аккаунте *Discord'а* будет показывать, что он играет именно у вас.  
Альтернативная, более современная реализация модуля DiscordRPC. **В настоящее время находится в разработке. Инструкция и
функционал может быть неполным**

#### Установка модуля

1. Скопировать модуль **DiscordGame_lmodule.jar** в папку **/LaunchServer/launcher-modules/**
2. Скачать последнюю версию библиотеки **discord-game-sdk4j**(https://github.com/JnCrMx/discord-game-sdk4j/releases) и
   положить в папку **/LaunchServer/launcher-libraries/**:

```sh
wget https://github.com/JnCrMx/discord-game-sdk4j/releases/download/v0.5/discord-game-sdk4j-0.5.jar
mv discord-game-sdk4j-0.5.jar /LaunchServer/launcher-libraries/
```

3. Добавить в **discord-game-sdk4j-0.5.X.jar** файлы из архива
   DiscordSDK ( https://dl-game-sdk.discordapp.net/2.5.6/discord_game_sdk.zip ):

- Файл `lib/x86/discord_game_sdk.dll` в `natives/windows/x86/discord_game_sdk.dll`
- Файл `lib/x86_64/discord_game_sdk.dll` в `natives/windows/amd64/discord_game_sdk.dll`
- Файл `lib/x86_64/discord_game_sdk.so` в `natives/linux/x86_64/discord_game_sdk.so`
- Файл `lib/x86_64/discord_game_sdk.dylib` в `natives/macos/x86_64/discord_game_sdk.dylib`