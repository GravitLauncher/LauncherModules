# SentryProGuardUpload

Служит для загрузки маппингов proguard в **Sentry** для модуля Sentry для лаунчера

#### Установка модуля

1. Скопировать модуль **SentryProGuardUpload.jar** в папку **/LaunchServer/modules/**
2. Установите `sentry-cli` по инструкции https://docs.sentry.io/product/cli/installation/
3. Укажите в конфигурации ваш auth-token

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
