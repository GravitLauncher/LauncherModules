# LauncherGuard

Добавляет поддержку нативной защиты

#### Установка модуля

1. Скопировать модуль **LauncherGuard_lmodule.jar** в папку **/LaunchServer/launcher-modules/**
2. Положить файлы exe и dll вашей нативной защиты в папки вида **/LaunchServer/launcher-pack/guard/ARCH-OS/**(например */LaunchServer/launcher-pack/guard/x86-64-mustdie/* для Windows x64)
3. Настроить конфигурацию
4. Выполнить **build** Launcher.

#### Конфигурация

- `/LaunchServer/config/LauncherGuard/Config.json`

```json
{
  "files": {
    "x86-64-mustdie": [
      "GravitGuard2.exe",
      "GuardDLL.dll"
    ],
    "x86-mustdie": [
      "GravitGuard2.exe",
      "GuardDLL.dll"
    ]
  },
  "exeFile": {
    "x86-64-mustdie": "GravitGuard2.exe",
    "x86-mustdie": "GravitGuard2.exe"
  },
  "renameExeFile": true,
  "useClasspathProperty": true,
  "protectLauncher": false,
  "nativeAgent": {
    "x86-64-mustdie": "GuardDLL",
    "x86-mustdie": "GuardDLL"
  }
}
```
