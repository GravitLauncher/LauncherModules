# MirrorHelper

Полу-автоматическая сборка Forge, Fabric и Vanilla с патчем authlib. **Требует установки модуля UnsafeCommandsPack**

#### Установка модуля

1. Установить модуль UnsafeCommandsPack
2. Скопировать модуль **MirrorHelper_module.jar** в папку **/LaunchServer/modules/**
3. После первого запуска открыть конфигурацию модуля и скопировать туда API ключ Curseforge
4. Настроить окружение
5. Собрать клиент командой `installclient`

#### Описание окружения

Рабочая папка модуля находится в **/LaunchServer/config/MirrorHelper/workspace**  
1. В папке authlib должны находится LauncherAuthlib всех версий:
- LauncherAuthlib1.jar
- LauncherAuthlib2.jar
- LauncherAuthlib3.jar
- LauncherAuthlib3-1.19.jar
- LauncherAuthlib3-1.19.1.jar
2. В папке installers должны находится установщики forge и fabric:
- fabric-installer.jar
- forge-{ВЕРСИЯ}-installer.jar (например `forge-1.16.5-installer.jar`)
**Для установки Forge необходим GUI**
3. В папке workdir должны хранится файлы, которые будут скопированы в клиент в указанном порядке
- ALL/
- {FABRIC/FORGE/VANILLA}/
- lwjgl{2/3}/
- java{17/8}/
- {ВЕРСИЯ}/ALL/
- {ВЕРСИЯ}/{FABRIC/FORGE/VANILLA}/

#### Пример работы для Fabric 1.19.2
1. Модуль скачивает клиент и профиль в локальный кеш (при необходимости)
2. Копирует всё содержимое оригинального клиента в указанную папку в updates
3. Патчит authlib используя `LauncherAuthlib3-1.19.1.jar`
4. Устанавливает fabric
5. Копирует содежимое следующих папок в клиент в таком порядке:
- ALL/
- FABRIC/
- lwjgl3/
- java17/
- 1.18.2/ALL/
- 1.18.2/FABRIC/
6. Устанавливает моды с curseforge если это было указано
7. Выполняет `deduplibraries`
8. Создает профиль