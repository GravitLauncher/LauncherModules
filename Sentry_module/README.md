+ Описание: Интеграция лаунчсервера с Sentry
+ Конфигурация:

      "dsn": "https://8b54292d477142c0a65c58fdbc0d1a1a@o123456.ingest.sentry.io/1234567",   /--- DSN с сайта sentry.io ---/
      "captureAll": false,    /--- Требуется ли собирать все логи, или же только исключения ---/
      "setThreadExcpectionHandler": false /--- Создать отдельный поток для сбора исключений ---/
