# AdditionalHash

Добавляет новый тип **AuthProvider'a**, который поддерживает стандартное шифрование паролей таких CMS как *WorldPress* и
новые версии *DLE*.

- *Вместо данного модуля почти всегда можно использовать **AuthProvider** - `"type": "request"`.*

| CMS | type |
| ------ | ------ |
| DLE 11+ | mysql-bcrypt |
| Wordpress | mysql-phphash |

#### Установка модуля

1. Скопировать модуль **AdditionalHash_module.jar** в папку **/LaunchServer/modules/**
2. Скачать библиотеку *[jbcrypt-0.4.jar]* и положить в папку **/LaunchServer/libraries/**:

```sh
wget https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar
mv jbcrypt-0.4.jar /LaunchServer/libraries/
```

3. Выполнить настройку `auth provider`

Укажите нужный вам `"type": "mysql-bcrypt"` или `"mysql-phphash"`
Так же требуется заполнить подключение к вашей БД MySQL \| MariaDB.

`"address": "localhost"` - Адрес вашей БД.

`"port": 3306` - Порт БД.

`"username": "root"` - Имя пользователя от БД.

`"password": "db_password"` - Пароль от БД.

`"database": "database"` - Название БД.

`"query": "..."` - В запросе изменить название **dle_users** на вашу таблицу с учетными записями.

```json
{
  "provider": {
    "type": "mysql-bcrypt",
    "mySQLHolder": {
      "address": "localhost",
      "port": 3306,
      "username": "root",
      "password": "db_password",
      "database": "database?serverTimezone=UTC"
    },
    "query": "SELECT password, name, permission FROM dle_users WHERE (email=? OR name=?)",
    "queryParams": [ "%login%", "%login%" ],
    "usePermission": true,
    "message": "Пароль неверный!"
  }
}
```

*Обратите внимание на **permission** в запросе, в вашей таблице требуется создать столбец с таким именем, если его
нету!*

```sql
ALTER TABLE `dle_users` ADD `permission` TINYINT NOT NULL DEFAULT '0';
```

[jbcrypt-0.4.jar]: https://repo1.maven.org/maven2/org/mindrot/jbcrypt/0.4/jbcrypt-0.4.jar
