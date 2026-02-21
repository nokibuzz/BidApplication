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

### API Usage (on Swagger e.g.)
1. **Authentication**: Use the `/auth/login` endpoint to obtain a JWT token by providing valid credentials (e.g., `admin/password`).
2. **Authorization**: For protected endpoints, include the obtained JWT token in the `Authorization` header as `Bearer <token>` to access the resources.
3. **Testing Endpoints**: Use the Swagger UI to test various API endpoints, ensuring you have the necessary permissions based on your user role.
4. **Role-Based Access**: Depending on your role (e.g., ADMIN or USER), you will have access to different sets of endpoints. Make sure to test the functionality according to your assigned role.

### Creating admin users
To create admin users, you can use the `/auth/register` endpoint. Provide the necessary details such as username, password,
it is important to include `admin` word into the username to ensure the user is assigned the ADMIN role.
For example, you can create a user with the username `adminJohn` and password `password` to have admin privileges.

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
* Quartz Scheduler
