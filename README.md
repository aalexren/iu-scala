# URL Shortener
*Финальный проект по курсу "Программировние на Scala". Университет Иннополис 2023*

## Описание
Данный проект представляет из себя сервис-сокращатель ссылок. Реализует REST API концепцию, необходим для упрощённого хранения и отправки ссылок, для уменьшения объёма текста. Предлагает следующие возможности:
- создание сокращённой ссылки;
- получение информации (ключ, полный адрес, дата создания) по короткой ссылке-ключу;
- проверка доступности сервиса указанного в полном адресе.

## Глобальные зависимости проекта
- Scala 2.13.12
- sbt 1.9.4
- OpenJDK Runtime Environment Temurin-17.0.9+9
- Docker 24.0.7 (для сборки образа, опционально)
- Make 3.81 (опционально)

В качестве базы данных для операций хранения и доступа к информации о ссылках была выбрана `PostgreSQL`. Для миграций использовался docker-образ `Flyway`.

### Сборка и запуск
1. Перейти в корневую папку с проектом
2. Выполнить `make build`
3. Выполнить `make up`

Аналогичные действия можно провести вручную, для этого надо открыть `Makefile` и скопировать команды оттуда.

Для запуска без сборки docker образа необходимо выполнить команду `sbt run`; следует учесть, что необходимо определить переменные окружения для корректной работы приложения!

| VARIABLE | DEFAULT VALUE (should be provided)|
|---------|-------------|
| DB_NAME | shortenerdb |
| DB_USER | postgres    |
| DB_URL  | localhost   |
| DB_PASS | password    |


Сервис станет доступен локально на порту `1234`. Swagger документация будет доступна по адресу: **http://localhost:1234/docs**
