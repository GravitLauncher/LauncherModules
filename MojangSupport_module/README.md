# MojangSupport

Добавляет [GravitLauncher] поддержку **mojang**

#### Установка модуля

1. Скопировать модуль **GenerateCertificate_module.jar** в папку **/LaunchServer/modules/**
2. Выполнить настройку `auth core provider`:
Для авторизации через microsoft:
```json
  "auth": {
    "microsoft": {
      "core": {
        "type": "microsoft"
      }
    }
  },
```

[GravitLauncher]: https://github.com/GravitLauncher/Launcher
