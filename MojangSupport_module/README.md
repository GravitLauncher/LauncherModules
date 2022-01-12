# MojangSupport

Добавляет [GravitLauncher] поддержку **mojang**

#### Установка модуля

1. Скопировать модуль **GenerateCertificate_module.jar** в папку **/LaunchServer/modules/**
2. Выполнить настройку `auth core provider`:  
Для авторизации через mojang:  
```json
{
  "core": {
    "type": "mojang"
  }
}
```
Для авторизации через microsoft:
```json
{
  "core": {
    "type": "microsoft"
  }
}
```

[GravitLauncher]: https://github.com/GravitLauncher/Launcher
