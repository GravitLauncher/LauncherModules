# S3Updates

---
Модуль позволяет синхронизировать папку `updates` с Хранилищем объектов S3
Модуль может использоваться для поставки обновлений через хранилище, снимая нагрузку с лаунчсервера и nginx при обновлениях клиентов.
Потенциально совместим с CDN (Cloudflare и проч.)

---
## Установка модуля

1. Скопировать модуль **S3Updates_module.jar** в папку **/LaunchServer/modules/**
2. Создать хранилище в панели управления провайдера
3. Взять у провайдера учетные данные пользователя для доступа к хранилищу и заполнить конфиг файл
4. Перезагрузить лаунчсервер и прописать `syncupdates` или `syncup`
5. Для удаления объектов на хранилище воспользуйтесь командой `s3cleanup`

---
## Конфигурация
* /LaunchServer/config/SwiftUpdates/Config.json
```json
{
  "s3Endpoint": "https://s3.gra.io.cloud.ovh.net/",
  "s3AccessKey": "accesskey",
  "s3SecretKey": "secretKey",
  "s3Region": "gra",
  "s3Bucket": "bucket",
  "behavior": {
    "forceUpload": false,
    "prefix": "updates/",
    "maxConcurrentConnections": 10,
    "maxPendingConnections": 2147483647,
    "connectionTimeout": 30,
    "connectionAcquisitionTimeout": 80
  }
}
```
- **forceUpload** - нужно ли модулю сверять ETag файла на хранилище и локального
- **prefix** - префикс для всех загружаемых объектов (псевдо-директория)

Все остальные данные поставляются провайдером, и могут разниться от провайдера к провайдеру


