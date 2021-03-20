# FileAuthSystem

Система пользователей с хранением данных в файле `.json`

#### Установка модуля

1. Скопировать модуль **FileAuthSystem_module.jar** в папку **/LaunchServer/modules/**
2. Запустить LaunchServer и прописать команду `fileauthsystem install`
3. Зарегистрируйте пользователей командой `fileauthsystem register [username] [password]`
4. Всё!

#### Команды

Выполните `help fileauthsystem` для просмотра доступных команд:

```
fileauthsystem changepassword [username] [password] - сменить пароль пользователя
fileauthsystem reload (path) - загрузить базу данных из файла
fileauthsystem getuser [username] - просмотр пользователя
fileauthsystem getusers  - просмотр всех пользователей
fileauthsystem install (authid) - установка FileAuthSystem
fileauthsystem save (path) - сохранить базу данных в файл
fileauthsystem givepermission [username] [permission] [true/false] - выдача прав пользователю
fileauthsystem register [username] [password] - зарегистрировать пользователя
fileauthsystem giveflag [username] [flag] [true/false] - выдача флагов пользователю
```

#### Конфигурация

- autoSave - автоматически сохранить базу данных в файл при остановке LaunchServer

```
{
   "autoSave": true
}
```

- Тип authProvider `fileauthsystem`, конфигурация:

```json
{
  "type": "fileauthsystem",
  "errorMessage": "Login or password incorrect"
}
```

- Тип authHandler `fileauthsystem`, конфигурация:

```json
{
  "type": "fileauthsystem"
}
```

- При установке через `fileauthsystem install` provider и handler будут установлены автоматически