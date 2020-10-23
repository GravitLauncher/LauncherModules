# OpenSSLSignCode
Позволяет подписывать **Launcher.exe** своим сертификатом используя утилиту *osslsigncode*.
#### Установка модуля
1. Скопировать модуль **OpenSSLSignCode_module.jar** в папку **/LaunchServer/modules/**
2. Обязательно создать самоподписанный сертификат или же купить его (более подробное описание есть на [Wiki])
2.1 В конфигурации **LaunchServer.json** `"sign": { "enabled": true }`
3. Установленная программа **osslsigncode**
3.1 Debian-подобные системы: `sudo apt install osslsigncode`
3.2 Для CentOS 7:
```sh
cd /etc/yum.repos.d/
wget https://download.opensuse.org/repositories/home:danimo/CentOS_7/home:danimo.repo
yum install osslsigncode calc
```
4. Выполнить **build** в консоли *LaunchServer*, если всё сделали правильно, **exe** будет подписан сертификатом.

#### Конфигурация

```json
{
  "timestampServer": "http://timestamp.globalsign.com/scripts/timstamp.dll",
  "osslsigncodePath": "osslsigncode",
  "customArgs": [],
  "checkSignSize": true,
  "checkCorrectSign": true,
  "checkCorrectJar": true
}
```

#### Замечания
 - Иногда вы можете получать ошибку о несоответствии размера подписи. Это происходит из за *timestamp server*, так как нельзя заранее угадать совпадет ли размер подписи в первом вызове и во втором. Если это вас беспокоит вы можете отключить использование `timestampServer` путем удаление этой строчки из конфигурации.

[Wiki]: https://launcher.gravit.pro
