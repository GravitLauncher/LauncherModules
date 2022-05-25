# DiscordGame

Добавляет **Launcher'у** интеграцию с *Discord'ом*. То есть, при наличии *Discord'а* на компьютере игрока, запустившего
один из ваших игровых клиентов, в его аккаунте *Discord'а* будет показывать, что он играет именно у вас

---

## Установка модуля

1. Скопировать модуль **DiscordGame_lmodule.jar** в папку **/LaunchServer/launcher-modules/**
2. Скачать последнюю версию библиотеки **[discord-game-sdk4j-0.5.x.jar]**
3. Добавить в **discord-game-sdk4j-0.5.x.jar** следующие файлы из архива **[Discord Game SDK 2.5.6 ZIP]**:
- Файл `lib/x86/discord_game_sdk.dll` в `natives/windows/x86/discord_game_sdk.dll`
- Файл `lib/x86_64/discord_game_sdk.dll` в `natives/windows/amd64/discord_game_sdk.dll`
- Файл `lib/x86_64/discord_game_sdk.so` в `natives/linux/amd64/discord_game_sdk.so`
- Файл `lib/x86_64/discord_game_sdk.dylib` в `natives/macos/amd64/discord_game_sdk.dylib`
4. Библиотеку **discord-game-sdk4j-0.5.x.jar** поместить в папку **/LaunchServer/launcher-libraries/**

---

## Настройка модуля

#### Конфигурационный файл с комментариями:
```json
{
  "enable": true,
  "appId": 810913859371532298, //APPLICATION ID
  "launcherDetails": "Лучший проект Minecraft", //Текст при запущенном лаунчере (не авторизован)
  "launcherState": "В лаунчере", //Вторичный текст при запущенном лаунчере (не авторизован)
  "largeKey": "large", //Имя главной картинки
  "smallKey": "small", //Имя вторичной картинки (миниатюры)
  "largeText": "Everything", //Текст большой картинки
  "smallText": "Everything", //Текст вторичной картинки (миниатюры)
  "clientDetails": "Лучший проект Minecraft", //Текст при запущенном клиенте
  "clientState": "Играет на %profileName%", //Вторичный текст при запущенном клиенте
  "authorizedDetails": "Лучший проект Minecraft", //Текст при запущенном лаунчере (авторизован)
  "authorizedState": "Выбирает сервер", //Вторичный текст при запущенном лаунчере (авторизован)
  "clientLargeKey": "large", //Имя главной картинки клиента
  "clientSmallKey": "small", //Имя вторичной картинки клиента (миниатюры)
  "clientLargeText": "Everything", //Текст большой картинки
  "clientSmallText": "Everything", //Текст вторичной картинки (миниатюры)
  "profileNameKeyMappings": {} //Не работает! Ранее, должен был применять изображения к профилям
}
```
**Копировать не рекомендуется!**

### Немного пояснений:
- Имя приложения, отображается как название игры!
- `%profileName%` - это плейсхолдер, заменяющий собой, имя запущенного клиента (больше плейсхолдеров ниже)

### Получаем "APPLICATION ID":
1. Заходим на сайт: https://discord.com/developers/applications
2. Создаём новое приложение
3. Копируем `APPLICATION ID` в `appId` конфигурации модуля

### Своя картинка, под каждый профиль:
1. В `clientLargeKey` или `clientSmallKey` вставляем один из плейсхолдеров: `%profileUUID%` или `%profileHash%`
2. Называем изображение профиля, соответствующему ему UUID или Hash

### Как загрузить необходимое изображение:
1. Переходим в настройки вашего приложения на сайте: https://discord.com/developers/applications
2. Переходим в раздел "Rich Presence" и попадаем сразу на "Art Assets"
3. Нажимаем на кнопку "Add Image(s)" и загружаем необходимые вам изображения
4. Переименовываем загруженные изображения в более удобный вид (Не обязательно)

**Модуль игнорирует аватар вашего приложения!**

---

## Все плейсхолдеры:

- `%uuid%` - UUID пользователя
- `%profileVersion%` - Версия профиля
- `%profileName%` - Имя профиля (title)
- `%profileUUID%` - UUID профиля
- `%profileHash%` - Hash профиля (UUID без `-`)
- `%username%` - Имя пользователя
- `%skinurl%` - URL скина
- `%cloakurl%` - URL плаща
- `%launcherVersion%` - Версия лаунчера
- `%javaVersion%` - Версия используемой Java
- `%javaBits%` - Разрядность используемой Java
- `%os%` - Операционная система

[discord-game-sdk4j-0.5.x.jar]: https://github.com/JnCrMx/discord-game-sdk4j/releases

[Discord Game SDK 2.5.6 ZIP]: https://dl-game-sdk.discordapp.net/2.5.6/discord_game_sdk.zip
