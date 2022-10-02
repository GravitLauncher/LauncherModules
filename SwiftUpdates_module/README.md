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
  "openStackUsername": "user-BwgZjmXYBYET",
  "openStackPassword": "AbcdEfgHijKlMwMRsMb5UjsawDrWwWt",
  "openStackRegion": "DE",
  "openStackContainer": "public-container",
  "openStackDomain": "Default",
  "prefix": "updates/"
}
```
**prefix** - префикс для всех загружаемых объектов (псевдо-директория)
**openStackContainer** - название контейнера в которые будут загружаться объекты
Все остальные данные поставляются провайдером, и могут разнится от провайдера к провайдеру

## Проверенные провайдеры
- OVH Гайд (todo)

# Важное замечание
Несмотря на то, что Swift имеет поддержку S3 API, данный модуль использует именно OpenStack Swift API ввиду его
легковесности и относительной простоты (Реализациия на AWS SDK весит 20мб, данный модуль весит ~8мб с учетом использования Java11+ HttpClient).
Подавляющее большинство провайдеров используют OpenStack Swift, если на сайте провайдера написано
"S3 Compatible API" то оно скорее всего совместимо с этим модулем.
