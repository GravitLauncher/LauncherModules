# LauncherStartScreen

Добавляет окно загрузки лаунчера сразу после его старта.

#### Установка модуля

1. Скопировать модуль **LauncherStartScreen_lmodule.jar** в папку **/LaunchServer/launcher-modules/**
2. Скопировать изображение *splash.png* в папку **/LaunchServer/runtime/splash.png**
3. Скопировать иконку *favicon.ico* в папку **/LaunchServer/runtime/favicon.ico**
4. Выполнить **build** Launcher.

#### Конфигурация

- `/LaunchServer/config/StartScreen/Config.json`

```json
{
  "imageURL": "runtime/splash.png",
  "faviconURL": "runtime/favicon.ico",
  "colorR": 1.0,
  "colorG": 1.0,
  "colorB": 1.0,
  "colorA": 0.0
}
```

#### Заметки

- Если при запуске **LaunchServer** возникает
  ошибка: `[ERROR] java.lang.ClassNotFoundException: pro.gravit.launchermodules.startscreen.TestConfig`.
    - Нужно изменить в файле **/META-INF/MANIFEST.MF** `TestConfig` на `Config`.
