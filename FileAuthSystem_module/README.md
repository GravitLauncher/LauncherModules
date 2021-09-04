# FileAuthSystem

Система авторизации пользователей, с хранением данных в формате `.json`

<h2 align="center">
<br>
Установка
</h2>

1. Скопировать модуль **FileAuthSystem_module.jar** в папку **modules/**
2. Запустить LaunchServer и прописать команду: `fileauthsystem install`
- При установке через `fileauthsystem install`, AuthCoreProvider будет настроен автоматически
- Все другие способы авторизацию будут заменены. Делайте БЕКАП `LaunchServer.json`
3. Регистрация пользователя командой: `fileauthsystem register [username] [password]`

<h2 align="center">
<br>
Команды
</h2>

Для просмотра доступных команд модуля, используйте: `help fileauthsystem`

Список команд:
```boo
fileauthsystem changepassword [username] [password] - сменить пароль пользователя
fileauthsystem reload (path) - загрузить базу данных из файла
fileauthsystem getuser [username] - просмотр пользователя
fileauthsystem getusers - просмотр всех пользователей
fileauthsystem install (authid) - установка FileAuthSystem
fileauthsystem save (path) - сохранить базу данных в файл
fileauthsystem givepermission [username] [permission] [true/false] - выдача прав пользователю
fileauthsystem register [username] [password] - зарегистрировать пользователя
fileauthsystem giveflag [username] [flag] [true/false] - выдача флагов пользователю
```

<h2 align="center">
<br>
Конфигурация
</h2>

Настройка `fileauthsystem`, конфигурация в `LaunchServer.json`:
```json
{
  "type": "fileauthsystem"
}
```
Структура вложенности раздела авторизации `auth`:
> "auth": {
>> "std": {
>>> "isDefault":true,
>>>
>>> "core": {
>>>
>>>> "type": "fileauthsystem"
>>>
>>> },
>>>
>>> "textureProvider": {...},
>>>
>>> "displayName": "Default"
>>
>> }
> 
> },

<h2 align="center">
<br>
Стандартные настройки модуля
</h2>

- `autoSave` - автоматически сохранить базу данных в файл, при остановке **LaunchServer**
- `oauthTokenExpire` - время жизни Токена авторизации
> Config.json:
```
{
   "autoSave": true,
   "oauthTokenExpire": 3600000
}
```

<h2 align="center">
<br>
Файловая структура модуля
</h2>

> LaunchServer/
>> config/
>>> FileAuthSystem/
>>>> Config.json
>>>>
>>>> Database.json
>>>>
>>>> Sessions.json
>>
>> modules/
>>> FileAuthSystem_module.jar