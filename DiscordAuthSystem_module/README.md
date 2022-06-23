# DiscordAuthSystem

Позволяет входить в лаунчер через Discord.
Модуль использует библиотеку [JSOUP](https://jsoup.org/download).

#### Установка модуля

1. Скопировать модуль **DiscordAuthSystem_module.jar** в папку **/LaunchServer/modules/**
2. Создать приложение в панели управления разработчика https://discord.com/developers/applications и скопировать его id, и секретный токен.
3. В настройках приложение discord oauth добавить redirect_url. Он должен состоять из пути до webapi + /auth/discord. Пример: http://127.0.0.1:9274/webapi/auth/discord
4. Настроить конфигурацию модуля
5. Добавить авторизацию в LaunchServer
6. [Опционально] Обновить Runtime

#### Конфигурация модуля

```json
{
  "clientId": "сюда вставляется id",
  "clientSecret": "сюда вставляется секрет",
  "redirectUrl": "это редирект, который вы указали",
  "discordAuthorizeUrl": "https://discord.com/oauth2/authorize",
  "discordApiEndpointVersion": "https://discord.com/api/v10",
  "discordApiEndpoint": "https://discord.com/api"
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

## [Опционально] Обновить Runtime

Если вы хотите, чтобы окно открывалось в браузере, а также авторизация у
пользователя сохранялась, то необходимо будет отредактировать и пересобрать runtime.
Модуль будет работать и без этого, но не так красиво.

#### Изменение для открытия в окне браузера авторизации Дискорда (а не webview)

```java
// /srcRuntime/src/main/java/pro/gravit/launcher/client/gui/scenes/login/methods/WebAuthMethod

// Эти библиотеки нужно добавить
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

// А этот модуль переписать
@Override
public CompletableFuture<LoginScene.LoginAndPasswordResult> auth(AuthWebViewDetails details) {
    overlay.future = new CompletableFuture<>();
    if (details.onlyBrowser) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(new URI(details.url));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
        overlay.disable();
    } else {
        overlay.follow(details.url, details.redirectUrl, (r) -> {
            String code = r;
            LogHelper.debug("Code: %s", code);
            if(code.startsWith("?code=")) {
                code = r.substring("?code=".length(), r.indexOf("&"));
            }
            LogHelper.debug("Code: %s", code);
            overlay.future.complete(new LoginScene.LoginAndPasswordResult(null, new AuthCodePassword(code)));
        });
    }
    return overlay.future;
}
```

#### Изменение для сохранения состояния авторизации 

```java
// /srcRuntime/src/main/java/pro/gravit/launcher/client/gui/impl/GuiEventHandler

// Эти библиотеки нужно добавить
import pro.gravit.launcher.events.request.AdditionalDataRequestEvent;
import java.util.Map;

// А этот модуль переписать
@Override
public <T extends WebSocketEvent> boolean eventHandle(T event) {
    LogHelper.dev("Processing event %s", event.getType());
        if (event instanceof RequestEvent) {
            if (!((RequestEvent) event).requestUUID.equals(RequestEvent.eventUUID))
            return false;
        }
    try {
        if (event instanceof AuthRequestEvent) {
            boolean isNextScene = application.getCurrentScene() instanceof LoginScene;
            ((LoginScene) application.getCurrentScene()).isLoginStarted = true;
            LogHelper.dev("Receive auth event. Send next scene %s", isNextScene ? "true" : "false");
            application.stateService.setAuthResult(null, (AuthRequestEvent) event);
            if (isNextScene && ((LoginScene) application.getCurrentScene()).isLoginStarted)
            ((LoginScene) application.getCurrentScene()).onGetProfiles();
        }
        if (event instanceof AdditionalDataRequestEvent) {
            AdditionalDataRequestEvent dataRequest = (AdditionalDataRequestEvent) event;
            Map<String, String> data = dataRequest.data;

            String type = data.get("type");

            if (type != null && type.equals("ChangeRuntimeSettings")) {
                application.runtimeSettings.login = data.get("login");
                application.runtimeSettings.oauthAccessToken = data.get("oauthAccessToken");
                application.runtimeSettings.oauthRefreshToken = data.get("oauthRefreshToken");
                application.runtimeSettings.oauthExpire = System.currentTimeMillis() + Integer.parseInt(data.get("oauthExpire"));
                application.runtimeSettings.lastAuth = ((LoginScene) application.getCurrentScene()).getAuthAvailability();
            }
        }
    } catch (Throwable e) {
        LogHelper.error(e);
    }
    return false;
}
```

```java
// /srcRuntime/src/main/java/pro/gravit/launcher/client/gui/scenes/login/LoginScene

// Просто добавить модуль
public GetAvailabilityAuthRequestEvent.AuthAvailability getAuthAvailability() {
    return this.authAvailability;
}
```

Если вам впадлу делать все эти изменения, то я приложил готовы билд рантайма. Он лежит рядом с билдом модуля.