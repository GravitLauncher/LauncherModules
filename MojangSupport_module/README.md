# MojangSupport

Добавляет [GravitLauncher] поддержку **mojang**

#### Установка модуля

1. Скопировать модуль **GenerateCertificate_module.jar** в папку **/LaunchServer/modules/**
2. Выполнить настройку `auth provider`, `auth handler` и `auth textureProvider`

```json
{
  "provider": {
    "type": "mojang"
  },
  "handler": {
    "type": "mojang"
  },
  "textureProvider": {
    "type": "mojang"
  }
}
```

[GravitLauncher]: https://github.com/GravitLauncher/Launcher
