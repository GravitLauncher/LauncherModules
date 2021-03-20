# OneLauncher

Позволяет запускать только один лаунчер и/или клиент майнкрафта одновременно

#### Установка модуля

1. Скопировать модуль **OneLauncher_lmodule.jar** в папку **/LaunchServer/launcher-modules/**
2. Перезапустить лаунчсервер или прописать команду `synclaunchermodules`
2. Собрать лаунчер командой `build`

#### Конфигурация

- `text` текст, который выведется при обнаружении уже запущенного лаунчера
- `launcherLock` запретить запуск двух лаунчеров одновременно ( `launcher.lock` )
- `clientLock` запретить запуск двух клиентов одновременно ( `client.lock` )
- `checkClientLock` запретить запуск лаунчера если запущен клиент майнкрафта (параметры `clientLock` и `launcherLock`
  должны быть включены, а `multipleProfilesAllow` выключен)
- `multipleProfilesAllow` вместо `client.lock` будет использоваться `{profileUUID}.lock`, что разрешает запускать **не
  более одного клиента на каждый профиль** вместо **не более одного клиента вообще** по умолчанию

```
{
  "text": "Launcher or minecraft is already running",
  "launcherLock": true,
  "checkClientLock": true,
  "clientLock": true,
  "multipleProfilesAllow": false
}
```