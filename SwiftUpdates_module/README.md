# SwiftUpdates

---
Модуль позволяет синхронизировать папку `updates` с Хранилищами объектов похожими на S3 (Object Storage) основанными на OpenStack Swift.
Модуль может использоваться для поставки обновлений через хранилище, снимая нагрузку с лаунчсервера и nginx при обновлениях клиентов.
Потенциально совместим с CDN (Cloudflare и проч.)

---
## Установка модуля

1. Скопировать модуль **SwiftUpdates_module.jar** в папку **/LaunchServer/modules/**
2. Создать хранилище в панели управления провайдера
3. Взять у провайдера учетные данные пользователя для доступа к хранилищу и заполнить конфиг файл
4. Перезагрузить лаунчсервер и прописать `syncupdates` или `syncup`

---
## Конфигурация
* /LaunchServer/config/SwiftUpdates/Config.json
```json
{
  "openStackEndpoint": "https://auth.cloud.ovh.net/v3",
  "openStackUsername": "user",
  "openStackPassword": "password",
  "openStackRegion": "DE",
  "openStackContainer": "public-container",
  "openStackDomain": "Default",
  "behavior": {
    "forceUpload": false,
    "prefix": "launcher-updates/"
  }
}
```
- **openStackContainer** - название контейнера в которые будут загружаться объекты
- **forceUpload** - нужно ли модулю сверять ETag файла на хранилище и локального
- **prefix** - префикс для всех загружаемых объектов (псевдо-директория)

Все остальные данные поставляются провайдером, и могут разниться от провайдера к провайдеру

## Проверенные провайдеры
- [OVH Гайд (GravitLauncher Discord)](https://discord.com/channels/853340557522370561/853340558328070164/1026257836105269319)

# Важное замечание
Несмотря на то, что Swift имеет поддержку S3 API, данный модуль использует именно OpenStack Swift API ввиду его
легковесности и относительной простоты (Реализациия на AWS SDK весит 20мб, данный модуль весит ~8мб с учетом использования Java11+ HttpClient).
Подавляющее большинство провайдеров используют OpenStack Swift, если на сайте провайдера написано
"S3 Compatible API" то оно скорее всего совместимо с этим модулем.
