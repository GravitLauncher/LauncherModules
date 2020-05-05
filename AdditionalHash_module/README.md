+ Описание: Добавляет новый тип AuthProvider'a, который поддерживает стандартное шифрование паролей таких CMS как WorldPress и новые версии DLE. Вместо данного модуля почти всегда можно использовать AuthProvider типа request.
+ Конфигурация:

      "provider": {
        "type": "mysql-bcrypt",
        "mySQLHolder": {
          "address": "localhost",   /--- Адрес БД ---/
          "port": 3306,   /--- Порт БД ---/
          "username": "root",   /--- Пользователь БД ---/
          "password": "password",   /--- Пароль БД ---/
          "database": "database"    /--- Название БД ---/
        },
        "query": "SELECT password, name, permission FROM dle_users WHERE (email=? OR name=?)",    /--- Запрос к БД ---/
        "queryParams": [ "%login%", "%login%" ],
        "message": "Пароль неверный"
      },
