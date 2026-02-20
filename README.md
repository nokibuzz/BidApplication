# Bidding Auction Application

Simple Spring Boot application for managing bidding auctions.

## 🚀 Getting Started

Build the project using Gradle:

```bash
gradle clean build
```

Run the application:

```bash
gradle bootRun
```

Application will start on:

```
http://localhost:8080
```

---

## 🗄 Database (H2 Console)

In-memory H2 database is available at:

```
http://localhost:8080/h2-console
```

Use the default Spring Boot H2 configuration (check `application.yml` / `application.properties` if needed).

---

## 📚 API Documentation (Swagger)

Swagger UI is available at:

```
http://localhost:8080/swagger-ui.html
```

---

## 👤 Default Users

On application startup, the following users are created:

| Username | Password | Role  |
| -------- | -------- | ----- |
| admin    | password | ADMIN |
| noki     | password | USER  |
| blagoje  | password | USER  |

---

## 🏷 Sample Data

On startup, **3 example auctions** are automatically created for testing purposes.

---

## 🛠 Tech Stack

* Java (21)
* Spring Boot 3
* H2 Database
* Swagger (OpenAPI)
* Gradle
