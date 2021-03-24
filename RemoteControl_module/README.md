# RemoteControl

Позволяет выполнять команды лаунчсервера удаленно используя HTTP запросы

#### Установка модуля

1. Скопировать модуль **RemoteControl_module.jar** в папку **/LaunchServer/modules/**
2. Перезапустить LaunchServer
3. Настроить конфигурацию модуля через команды или отредактировав файл конфигурации

#### Выполнение запроса

Запрос можно выполнить с помощью curl, браузера или любого скрипта.  
Например:

```bash
curl http://YOUR_ADDRESS:9274/webapi/remotecontrol/command?token=YOUR_TOKEN&command=build&log=true
```

Или используя проксирование:

```bash
curl https://YOUR_ADDRESS/webapi/remotecontrol/command?token=YOUR_TOKEN&command=build&log=true
```

#### Конфигурация

```

```