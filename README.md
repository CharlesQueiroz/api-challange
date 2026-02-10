# E-Commerce CRUD API

REST API for managing Products, Orders, and Order Items with stock control, built as a backend technical challenge.

---

## Tech Stack

- **Java 21**
- **Spring Boot 4.0.2** (Web MVC, Data JPA, Validation, Actuator)
- **PostgreSQL 18** with Liquibase migrations
- **MapStruct** for compile-time DTO mapping
- **Lombok** for boilerplate reduction
- **SpringDoc OpenAPI** (Swagger UI)
- **Testcontainers** (PostgreSQL) for integration tests
- **JUnit 5 + Mockito** for unit tests
- **Docker** with multi-stage build and non-root runtime user

---

## Architecture and Design Decisions

### Layered Architecture

Standard Controller - Service - Repository layering. Controllers are thin delegates; business logic lives in the service layer and, where appropriate, in the domain entities themselves.

### Generic CRUD Abstractions

`AbstractCrudController<C, U, R>` and `MappedCrudService<E, C, U, R>` provide type-safe CRUD scaffolding. Concrete controllers and services inherit the full lifecycle (create, findAll,
findByCode, update, delete) and only override what they need.

### Optimistic Locking with Manual Version Check

All entities carry a JPA `@Version` field. However, Hibernate uses the version from its persistence context snapshot. To enforce true client-side optimistic locking, the service layer performs an **explicit version comparison** between the DTO and the loaded entity before applying the update.

### Pessimistic Locking for Stock Operations

Stock adjustments use `@Lock(PESSIMISTIC_WRITE)` via a dedicated `findByIdForStockUpdate` repository method. This acquires a row-level `SELECT ... FOR UPDATE`lock, preventing concurrent transactions from reading stale stock quantities.

### Domain Logic in Entities

Entities are not anemic data holders. `Product` owns `decreaseStock()` and `increaseStock()` with built-in validation.

### UUID Codes for Public API

Internal numeric IDs are never exposed. Each entity has a database-generated UUID `code` column (`@Generated` + `insertable = false`) that serves as the public identifier in all API
paths and responses.

### Order Status State Machine

Order status transitions are validated server-side: `PENDING -> PROCESSING | CANCELLED`, `PROCESSING -> COMPLETED | CANCELLED`. Terminal states (`COMPLETED`, `CANCELLED`) cannot transition further. Cancellation triggers automatic stock restoration for all line items.


## API Quick Reference

All endpoints return paginated results for list operations (via `page`, `size`, `sort` query params).

### Products `/api/products`


| Method   | Endpoint               | Description              |
| -------- | ---------------------- | ------------------------ |
| `GET`    | `/api/products`        | List all products        |
| `GET`    | `/api/products/{code}` | Get product by UUID code |
| `POST`   | `/api/products`        | Create a new product     |
| `PUT`    | `/api/products/{code}` | Update a product         |
| `DELETE` | `/api/products/{code}` | Delete a product         |

### Orders `/api/orders`


| Method   | Endpoint                         | Description                     |
| -------- | -------------------------------- | ------------------------------- |
| `GET`    | `/api/orders`                    | List all orders                 |
| `GET`    | `/api/orders/{code}`             | Get order by UUID code          |
| `POST`   | `/api/orders`                    | Create order with inline items  |
| `PUT`    | `/api/orders/{code}`             | Update order fields / status    |
| `DELETE` | `/api/orders/{code}`             | Delete order (restores stock)   |
| `GET`    | `/api/orders/{code}/order-items` | List items for a specific order |

### Order Items `/api/order-items`


| Method   | Endpoint                  | Description                 |
| -------- | ------------------------- | --------------------------- |
| `GET`    | `/api/order-items`        | List all order items        |
| `GET`    | `/api/order-items/{code}` | Get order item by UUID code |
| `POST`   | `/api/order-items`        | Add item to existing order  |
| `PUT`    | `/api/order-items/{code}` | Update order item quantity  |
| `DELETE` | `/api/order-items/{code}` | Delete order item           |

---

## Getting Started

### Prerequisites

- **Docker** and **Docker Compose**

### 1. Create the environment file (optional)

Create a `.env` file in the project root:

```bash
POSTGRES_DB=ecommerce
POSTGRES_USER=ecommerce
POSTGRES_PASSWORD=ecommerce
```

### 2. Build and run

```bash
docker compose up -d
```

This starts PostgreSQL 16 and the application container. Liquibase runs migrations automatically on startup.

### 3. Verify


| URL                                     | Description       |
| --------------------------------------- | ----------------- |
| `http://localhost:8080/actuator/health` | Health check      |
| `http://localhost:8080/swagger-ui.html` | Swagger UI        |
| `http://localhost:8080/v3/api-docs`     | OpenAPI JSON spec |

---

## Running Tests

Docker must be running -- Testcontainers spins up a disposable PostgreSQL instance for each test run.

```bash
./mvnw test
```

### Test coverage

- **Unit tests** (`*Test.java`): Service layer logic with Mockito mocks for repositories and dependencies.
- **Integration tests** (`*IT.java`): Full Spring Boot context with real PostgreSQL via Testcontainers.


## Postman Collection

Pre-built collection and environment files are provided in the `postman/` directory.

### Import instructions

1. Open Postman.
2. Import the collection: `postman/Ecommerce-API-Recruiter.postman_collection.json`.
3. Import the environment: `postman/Ecommerce-API-Local.postman_environment.json`.
4. Select the imported environment in the top-right dropdown.
5. Confirm `baseUrl` is set to `http://localhost:8080`.
6. Run requests in folder order: `00 - Health & Docs` -> `01 - Products` -> `02 - Orders & OrderItems`.



Collection scripts automatically capture and propagate variables (codes, versions) between requests.
