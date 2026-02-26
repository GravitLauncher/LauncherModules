# SystemdNotifer

Служит для уведомления **LaunchServer** о готовности системному менеджеру **systemd**.

#### Установка модуля

1. Установить модуль командой `modules load SystemdNotifer`.
2. Для работы *unit* потребуется так же установленная утилита **screen**.
    - Debian подобные системы `sudo apt install screen`
    - CentOS `sudo yum install screen`
3. Создаём новый файл **Launcher.service** по пути: `/etc/systemd/system/Launcher.service`.
4. Копируем конфигурацию указанную ниже с внесением изменений в созданный файл.
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
     Loaded: loaded (/etc/systemd/system/Launcher.service; enabled; preset: enabled)
     Active: active (running) since Thu 2026-01-29 00:25:20 UTC; 3min 50s ago
    Process: 1024358 ExecStartPre=/usr/bin/screen -S launchserver -X quit (code=exited, status=1/FAILURE)
   Main PID: 1024359 (screen)
      Tasks: 33 (limit: 76910)
     Memory: 233.3M (peak: 706.4M)
        CPU: 4.564s
     CGroup: /system.slice/Launcher.service
             ├─1024359 /usr/bin/SCREEN -DmS launchserver /home/launcher/launchserver/src/components/launchserver/build/install/launchserver/bin/launchserver
             └─1024362 java --add-modules ALL-MODULE-PATH --add-modules java.net.http --add-opens java.base/java.lang.invoke=launchserver -Dlauncher.useSlf4j=true -Dio.netty.noUnsafe=true -Xmx512M -classpath /home/launcher/launchserver/src/components/launchserver/build/install/la>

Jan 29 00:25:18 ubuntu2404lts systemd[1]: Starting Launcher.service - LaunchServer...
Jan 29 00:25:18 ubuntu2404lts screen[1024358]: No screen session found.
Jan 29 00:25:20 ubuntu2404lts systemd[1]: Started Launcher.service - LaunchServer.
```

#### Конфигурация

```bash
[Unit]
Description=LaunchServer
After=network-online.target
Wants=network-online.target

[Service]
WorkingDirectory=/home/launcher/launchserver
User=launcher
Group=launcher

Type=notify
NotifyAccess=all

Environment=APP_HOME=app
Environment=JAVA_OPTS=-Xmx512M

ExecStartPre=-/usr/bin/screen -S launchserver -X quit
ExecStart=/usr/bin/screen -DmS launchserver /home/launcher/launchserver/src/components/launchserver/build/install/launchserver/bin/launchserver
ExecStop=/usr/bin/screen -S launchserver -p 0 -X stuff "stop$(printf '\r')"

StandardOutput=journal
StandardError=journal

Restart=always
RestartSec=5
TimeoutStopSec=60
KillMode=mixed
KillSignal=SIGTERM

[Install]
WantedBy=multi-user.target
```
