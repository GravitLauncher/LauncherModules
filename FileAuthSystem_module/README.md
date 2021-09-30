# FileAuthSystem

Система пользователей с хранением данных в файле `.json`

#### Установка модуля

1. Скопировать модуль **FileAuthSystem_module.jar** в папку **/LaunchServer/modules/**
2. Запустить LaunchServer и прописать команду `fileauthsystem install`
3. Зарегистрируйте пользователей командой `config auth.std.core register [username] email [password]`
4. Всё!

#### Команды

Введите `config auth.std.core` и нажмите TAB для просмотра доступных команд

#### Конфигурация

- autoSave - автоматически сохранить базу данных в файл при остановке LaunchServer

```
{
   "autoSave": true
}
```

- Тип authCoreProvider `fileauthsystem`, конфигурация:

```json
{
  "type": "fileauthsystem"
}
```

- При установке через `fileauthsystem install` AuthCoreProvider будет установлен автоматически