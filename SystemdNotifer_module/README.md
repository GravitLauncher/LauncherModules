# SystemdNotifer

Служит для уведомления **LaunchServer** о готовности системному менеджеру **systemd**.

#### Установка модуля

1. Скопировать модуль **SystemdNotifer_module.jar** в папку **/LaunchServer/modules/**
2. Для работы *unit* потребуется так же установленная утилита **screen**.
    - Debian подобные системы `sudo apt install screen`
    - CentOS `sudo yum install screen`
3. Создаём новый файл **Launcher.service** по пути: `/etc/systemd/system/Launcher.service`.
4. Копируем конфигурацию указаную ниже с внесением изменений в созданный файл.
    - `Description` - Описание сервиса (unit'а).
    - `WorkingDirectory` - Полный путь до **LaunchServer**.
    - `User` - Имя пользователя, от имени которого будет запущена служба (LaunchServer.jar).
    - `Group` - Группа пользователя (обычно совпадает с именем пользователя).
5. Перезапускаем systemd `systemctl daemon-reload`
6. Включаем наш *unit* `systemctl enable Launcher`
7. Запускаем *unit* `systemctl start Launcher`
8. Проверяем что он запустился командой `systemctl status Launcher`

Вывод команды должен быть примерно такой:

```
● Launcher.service - LaunchServer
   Loaded: loaded (/etc/systemd/system/launcher.service; enabled; vendor preset: disabled)
   Active: active (running)
   CGroup: /system.slice/Launcher.service
           ├─36023 /usr/bin/SCREEN -DmS LaunchServer /usr/bin/java -Xmx128M -javaagent:LaunchServer.jar -jar LaunchServer.jar
           └─36024 /usr/bin/java -Xmx128M -javaagent:LaunchServer.jar -jar LaunchServer.jar
systemd[1]: Starting LaunchServer...
systemd[1]: Started LaunchServer.
```

#### Конфигурация

```bash
[Unit]
Description=LaunchServer
After=network.target

[Service]
WorkingDirectory=/home/launchserver/
Type=notify
User=launchserver
Group=servers
NotifyAccess=all
Restart=always    

ExecStart=/usr/bin/screen -DmS launchserver /usr/bin/java -Xmx128M -javaagent:LaunchServer.jar -jar LaunchServer.jar
ExecStop=/usr/bin/screen -p 0 -S launchserver -X eval 'stuff "stop"\015'

[Install]
WantedBy=multi-user.target
```
