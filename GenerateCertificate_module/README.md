# GenerateCertificate

Создает сертификаты для подписи **Launcher.**(*jar\|exe*).

#### Установка модуля

1. Скопировать модуль **GenerateCertificate_module.jar** в папку **/LaunchServer/modules/**
2. Запустите **LaunchServer.jar** и выполните команду в консоли: `generatecertificate`
3. Остановите *LaunchServer* командой `stop`
4. Скопируйте сгенерированую конфигурацию.
   *Из лог файла или же из терминала выделив его*
5. Замените блок конфигурации в файле **LaunchServerConfig.json** тем, что вы скопировали в п.4.

#### Замечания

- Модуль **GenerateCertificate_module.jar** можно удалить после генерации сертификатов.
