# OpenSSLSignCode

Позволяет подписывать **Launcher.exe** своим сертификатом используя утилиту *osslsigncode*.

#### Установка модуля

1. Скопировать модуль **OpenSSLSignCode_module.jar** в папку **/LaunchServer/modules/**.
2. Обязательно создать самоподписанный сертификат или же купить его (более подробное описание есть на [Wiki]).
  - В конфигурации **LaunchServer.json** `"sign": { "enabled": true }`.
3. Установленная программа **osslsigncode**.
  - Debian-подобные системы: `sudo apt install osslsigncode`.
  - Для CentOS 7:
```sh
cd /etc/yum.repos.d/
wget https://download.opensuse.org/repositories/home:danimo/CentOS_7/home:danimo.repo
yum install osslsigncode calc
```
  - Для CentOS 8 Stream:
```sh
cd /etc/yum.repos.d/
wget -O VortexOBS.repo https://download.opensuse.org/repositories/home:VortexOBS/CentOS_8/home:VortexOBS.repo
dnf -y install osslsigncode
```


4. Выполнить **build** в консоли *LaunchServer*, если всё сделали правильно, **exe** будет подписан сертификатом.

#### Конфигурация

- */LaunchServer/config/OSSLSignCode/Config.json*

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

#### Команды

```
osslsignexe [path to input exe] [path to output exe] - подписать exe, созданный с помощью launch4j вручную
```

#### Замечания

- Иногда вы можете получать ошибку о несоответствии размера подписи. Это происходит из за *timestamp server*, так как
  нельзя заранее угадать совпадет ли размер подписи в первом вызове и во втором. Если это вас беспокоит вы можете
  отключить использование `timestampServer` путем удаление этой строчки из конфигурации.

[Wiki]: https://launcher.gravit.pro
