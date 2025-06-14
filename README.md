
#  Сервис OTP-защиты  
Backend-приложение на Java для создания и проверки одноразовых временных паролей (OTP), с поддержкой отправки через Email, SMS (SMPP-эмулятор), Telegram, а также сохранением кодов в файл. Сервис предназначен для повышения безопасности действий пользователей с помощью одноразовых кодов.

## Основные возможности  
- Регистрация и авторизация пользователей с ролями ADMIN и USER  
- Генерация и отправка OTP:  
  - Email (через JavaMail)  
  - SMS (используя SMPP-эмулятор)  
  - Telegram Bot API  
- Логирование всех операций с использованием SLF4J/Logback  
- Сохранение сгенерированных OTP в файл  
- Проверка одноразовых кодов с учетом состояний: ACTIVE, USED, EXPIRED  
- Административные функции: настройка времени жизни OTP, длины кода, управление пользователями  
- Авторизация с использованием токенов и проверкой ролей  

##  Технологический стек  
- Java 17  
- PostgreSQL 17 (JDBC, без ORM)  
- Maven  
- JavaMail для отправки писем  
- OpenSMPP-core для SMPP (с эмулятором SMPPsim)  
- Telegram Bot API с Apache HttpClient  
- Встроенный HTTP сервер (`com.sun.net.httpserver`)  
- SLF4J + Logback для логирования  

##  Установка и запуск  
### 1. Подготовка окружения  
- Убедитесь, что установлены:  
  - Java 17  
  - PostgreSQL 17  
  - Maven  

## Сборка и запуск

- Создайте базу данных для сервиса:  
```sql
CREATE DATABASE otp_service;
```
---выполните из папки проекта
mvn clean package
java -jar target/otp-backend-1.0-SNAPSHOT.jar
---

###  Конфигурация  

- Настройте конфигурационные файлы в `src/main/resources`:  
  - `application.properties` — параметры подключения к базе данных  
  - `email.properties` — настройки SMTP сервера  
  - `sms.properties` — параметры SMPP эмулятора  
  - `telegram.properties` — токен бота и chatId  

- Пример `application.properties`:  
```
db.url=jdbc:postgresql://localhost:5432/otp_service
db.user=postgres
db.password=your_password
```


## Структура проекта  
```
otp-protection-service/
├── src/
│   └── main/
│       ├── java/
│       │   └── otp/
│       │       ├── api/         # HTTP контроллеры
│       │       ├── config/      # Загрузка конфигураций
│       │       ├── dao/         # Работа с БД (JDBC)
│       │       ├── model/       # Модели данных и DTO
│       │       ├── service/     # Бизнес-логика
│       │       └── util/        # Вспомогательные утилиты
│       └── resources/           # Конфигурационные файлы и ресурсы
│           ├── application.properties
│           ├── email.properties
│           ├── logback.xml
│           ├── sms.properties
│           └── telegram.properties
├── pom.xml                      # Конфигурация Maven
└── README.md                   # Документация проекта
```

##  Роли и безопасность  
- **ADMIN** — полный доступ: настройка параметров OTP, управление пользователями  
- **USER** — ограниченный доступ: генерация и проверка OTP  

- Аутентификация реализована через токены с ограниченным временем жизни (TTL). Токен передается в заголовке запроса:  
```
Authorization: Bearer <token>
```

##  Примеры запросов API  

### Регистрация пользователя  
```bash
curl -X POST http://localhost:8080/register \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password123","role":"USER"}'
```

### Вход в систему (получение токена)  
```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"user1","password":"password123"}'
```

### Генерация OTP  
```bash
curl -X POST http://localhost:8080/otp/generate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"operationId":"op123","channel":"EMAIL"}'
```

### Проверка OTP  
```bash
curl -X POST http://localhost:8080/otp/validate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -d '{"code":"123456"}'
```

### Админ-функции  

- Изменение параметров OTP:  
```bash
curl -X PATCH http://localhost:8080/admin/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ADMIN_TOKEN" \
  -d '{"length":6,"ttlSeconds":300}'
```

- Просмотр пользователей:  
```bash
curl -X GET http://localhost:8080/admin/users \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

- Удаление пользователя:  
```bash
curl -X DELETE http://localhost:8080/admin/users/2 \
  -H "Authorization: Bearer ADMIN_TOKEN"
```

##  Тестирование  
- Для проверки API используйте Postman или curl. Убедитесь, что корректно работают все ключевые функции:  
  - регистрация и вход пользователей  
  - генерация и отправка OTP  
  - валидация кодов  
  - административные операции  

  - административные операции  
