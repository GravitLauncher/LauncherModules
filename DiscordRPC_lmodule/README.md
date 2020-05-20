+ Описание: Добавляет лаунчеру интеграцию с Discord'ом. Тоесть, при наличии Discord'а на компьютере человека, запустившего один из ваших игровых клиентов, в его аккауте Discord'а будет показывать, что он играет именно у вас.
+ Так же для работы RPC требуются libraries. 
+ https://jcenter.bintray.com/club/minnced/java-discord-rpc/2.0.2/java-discord-rpc-2.0.2.jar
+ https://jcenter.bintray.com/club/minnced/discord-rpc-release/v3.4.0/discord-rpc-release-v3.4.0.jar
+ Папка в которую необходимо загрузить libraries: launcher-libraries
+ Конфигурация:

      "appId": 617731283404980249,    /--- Секция ClientID у дискорд-бота ---/
      "firstLine": "Играет на сервере %profile%",
      "secondLine": "Ник: %user%",
      "largeKey": "icon",     /--- Название главной картинки у дискорд-бота ---/
      "smallKey": "small",    /--- Название дополнительной картинки у дискорд-бота  ---/
      "largeText": "projectname.ml",    /--- Основной текст ---/
      "smallText": "servername"   /--- Дополнительный текст ---/
      
+ Дополнительные настройки: В конфиге прогуарда добавить club.minnced.discord.rpc.** в keeppackagenames и keep class
+ Альтернативная конфигурация(alt...) применяется при работе в лаунчере, тогда как основная конфигурация 
+ Так же необходимо настроить приложение на Discord Developer Portal
