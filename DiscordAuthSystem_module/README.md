# DiscordAuthSystem

Позволяет входить в лаунчер через Discord.
Модуль использует библиотеку [JSOUP](https://jsoup.org/download).

#### Установка модуля

1. Скопировать модуль **DiscordAuthSystem_module.jar** в папку **/LaunchServer/modules/**
2. Создать приложение в панели управления разработчика https://discord.com/developers/applications, и секретный токен.
Если вам нужно проверять находится ли пользователь в необходимых вам гильдиях, то опциональные пункты обязательны. 
     1. Скопировать его CLIENT ID
     2. Скопировать его CLIENT SECRET
     3. [Опционально] Создать бота из данного приложения
     4. [Опционально] Добавить его на необходимые вам сервера.
     5. [Опционально] В настройках бота включить пункт "SERVER MEMBERS INTENT". 
4. В настройках приложение discord oauth добавить redirect_url. Он должен состоять из пути до webapi + /auth/discord. Пример: http://127.0.0.1:9274/webapi/auth/discord
5. Настроить конфигурацию модуля
6. Добавить авторизацию в LaunchServer
7. [Опционально] Обновить Runtime

#### Конфигурация модуля

```json
{
  "clientId": "сюда вставляется id",
  "clientSecret": "сюда вставляется секрет",
  "redirectUrl": "это редирект, который вы указали",
  "discordAuthorizeUrl": "https://discord.com/oauth2/authorize",
  "discordApiEndpointVersion": "https://discord.com/api/v10",
  "discordApiEndpoint": "https://discord.com/api",
  "guildIdsJoined": [
    {
      "id": "id гильдии №1",
      "name": "наименование гильдии",
      "url": "ссылка для входа"
    },
    {
      "id": "id гильдии №2",
      "name": "наименование гильдии",
      "url": "ссылка для входа"
    }
  ],
  "guildIdGetNick": "id гильдии с которой будет браться ник. если не надо, то оставить пустым",
  "usernameRegex": "regex для валидации ника (если не нужно, то оставьте пустым)"
}
```

#### Конфигурация в LaunchServer

```json
{
  "std": {
    "isDefault": true,
    "core": {
      "type": "discordauthsystem",
      "mySQLHolder": {
        "address": "localhost",
        "port": 3306,
        "username": "root",
        "password": "root",
        "database": "test",
        "useHikari": false
      },
      "uuidColumn": "uuid",
      "usernameColumn": "username",
      "accessTokenColumn": "accessToken",
      "refreshTokenColumn": "refreshToken",
      "expiresInColumn": "expiresIn",
      "discordIdColumn": "discordId",
      "bannedAtColumn": "bannedAt",
      "hardwareIdColumn": "hwidId",
      "serverIDColumn": "serverID",
      "table": "users",
      "tableHwid": "hwids"
    },
    "textureProvider": {
      "skinURL": "http://example.com/skins/%username%.png",
      "cloakURL": "http://example.com/cloaks/%username%.png",
      "type": "request"
    },
    "displayName": "Default"
  }
}
```

- В mySQLHolder указывается коннект к mysql (данные аккаунтов хрантся там)
- \*\*\*\*Column - строки наименования колонок.
- tableHwid - таблица hwid юзеров.

#### Дефолтный запрос на создание таблицы

```mysql
-- Создаём таблицу пользователей
CREATE TABLE `users` (
    `uuid` CHAR(36) UNIQUE,
    `username` CHAR(32) UNIQUE,
    `accessToken` CHAR(32) DEFAULT NULL,
    `refreshToken` CHAR(32) DEFAULT NULL,
    `expiresIn` BIGINT DEFAULT NULL,
    `discordId` VARCHAR(32) DEFAULT NULL,
    `bannedAt` DATETIME DEFAULT NULL,
    `serverID` VARCHAR(41) DEFAULT NULL,
    `hwidId` BIGINT DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Создаём таблицу hwids данных
CREATE TABLE `hwids` (
     `id` bigint(20) NOT NULL,
     `publickey` blob,
     `hwDiskId` varchar(255) DEFAULT NULL,
     `baseboardSerialNumber` varchar(255) DEFAULT NULL,
     `graphicCard` varchar(255) DEFAULT NULL,
     `displayId` blob,
     `bitness` int(11) DEFAULT NULL,
     `totalMemory` bigint(20) DEFAULT NULL,
     `logicalProcessors` int(11) DEFAULT NULL,
     `physicalProcessors` int(11) DEFAULT NULL,
     `processorMaxFreq` bigint(11) DEFAULT NULL,
     `battery` tinyint(1) NOT NULL DEFAULT "0",
     `banned` tinyint(1) NOT NULL DEFAULT "0"
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- Добавляем модификаторы hwids таблицы
ALTER TABLE `hwids`
    ADD PRIMARY KEY (`id`),
    ADD UNIQUE KEY `publickey` (`publickey`(255));
ALTER TABLE `hwids`
    MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT;

-- Связываем пользователей и hwids
ALTER TABLE `users`
    ADD CONSTRAINT `users_hwidfk` FOREIGN KEY (`hwidId`) REFERENCES `hwids` (`id`);
```

#### [Опционально] Обновить Runtime

Если вы хотите, чтобы окно открывалось в браузере, а также авторизация у
пользователя сохранялась, то необходимо будет отредактировать (пропатчить) и пересобрать runtime.
Модуль будет работать и без этого, но не так красиво.

```shell
cd ./src/srcRuntime
git am DiscordAuthSystemRuntime.patch #Надеюсь не нужно объяснять,
# что тут нужен путь до файла DiscordAuthSystemRuntime.patch
gradlew build
```

Если вам впадлу делать все эти изменения, то я приложил готовы билд рантайма. Он лежит рядом с билдом модуля.