# ServerScriptEngine

Позволяет выполнять **JavaScript** код на стороне сервера. Отсутствует ограничение функций, как это было в **sashok724's
v3**.

#### Установка модуля

1. Скопировать модуль **ServerScriptEngine_module.jar** в папку **/LaunchServer/modules/**
2. Настройка не требуется.

#### Команды

```sh
scriptmappings [nothing] - Посмотреть все маппинги классов LaunchServer в JavaScript
eval [line] - Выполнить JavaScript код на стороне лаунчсервера
```

#### Предупреждение

Начиная с Java 15 Nashorn Script Engine удален из JRE и этот модуль работать не будет