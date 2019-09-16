+ Описание: Служит для правильного порядка загрузки лаунчсервера с systemd. Необязателен, вместо systemd можно использовать cron.
+ Конфигурация:   

      [Unit]
      Description=LaunchServer    /--- Описание юнита ---/
      After=network.target
                  
      [Service]
      WorkingDirectory=/home/launchserver/    /--- Рабочий каталог ---/
      Type=notify
      User=launchserver   /--- Пользователь, от имени которого идет запуск ---/
      Group=servers   /--- Группа вышеупомянутого пользователя ---/
      NotifyAccess=all
      Restart=always    

      ExecStart=/usr/bin/screen -DmS launchserver /usr/bin/java -Xmx128M -javaagent:LaunchServer.jar -jar   LaunchServer.jar    /--- Команда для старта ---/
      ExecStop=/usr/bin/screen -p 0 -S launchserver -X eval 'stuff "stop"\015'    /--- Команда для остановки ---/
      [Install]
      WantedBy=multi-user.target
