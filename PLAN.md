# План розробки: Трекер витрат

Навчальний проєкт: повний цикл від бази даних до сторінки в браузері.

**Стек:** Java 21 · Spring Boot 3.5 · Maven · Spring Data JPA (Hibernate) · PostgreSQL 16 (Docker) · HTML + CSS + JavaScript (без фреймворків)

**Як працюємо:** етап за етапом. Після кожного кроку перевіряємо результат і розбираємо, що зроблено. Тільки потім переходимо далі. Виконані кроки відмічаємо `[x]`.

---

## Загальна архітектура

```
Браузер (index.html + app.js)
   │  fetch() → HTTP + JSON
   ▼
ExpenseController   @RestController   — приймає HTTP-запити, повертає JSON
   ▼
ExpenseService      @Service          — бізнес-логіка, «не знайдено» → помилка
   ▼
ExpenseRepository   JpaRepository     — Spring Data генерує SQL
   ▼
PostgreSQL (Docker, порт 5433)        — таблиця expense
```

## Модель даних

Таблиця `expense`:

| Колонка        | Тип SQL         | Поле Java                | Правила                    |
|----------------|-----------------|--------------------------|----------------------------|
| `id`           | `BIGSERIAL` PK  | `Long id`                | генерується базою          |
| `amount`       | `NUMERIC(12,2)` | `BigDecimal amount`      | обов'язкове, > 0           |
| `category`     | `VARCHAR(50)`   | `String category`        | обов'язкове, до 50 символів |
| `description`  | `VARCHAR(255)`  | `String description`     | необов'язкове, до 255      |
| `expense_date` | `DATE`          | `LocalDate date`         | обов'язкове                |

Категорії (список на фронтенді): Їжа, Транспорт, Житло, Комунальні, Здоров'я, Розваги, Одяг, Інше.

> Чому `BigDecimal`, а не `double`? Гроші не можна рахувати з похибкою округлення: `0.1 + 0.2 != 0.3` для `double`.

## REST API

Базовий шлях: `/api/expenses`

| Метод    | URL                                      | Тіло запиту     | Відповідь                        |
|----------|------------------------------------------|-----------------|----------------------------------|
| `GET`    | `/api/expenses?category=&from=&to=`      | —               | `200` + масив витрат (нові згори) |
| `GET`    | `/api/expenses/{id}`                     | —               | `200` + витрата / `404`          |
| `POST`   | `/api/expenses`                          | JSON витрати    | `201` + створена витрата / `400` |
| `PUT`    | `/api/expenses/{id}`                     | JSON витрати    | `200` + оновлена / `400` / `404` |
| `DELETE` | `/api/expenses/{id}`                     | —               | `204` / `404`                    |
| `GET`    | `/api/expenses/summary?category=&from=&to=` | —            | `200` + `{"total": 1234.50}`     |

Усі параметри фільтра необов'язкові. Дати мають формат `YYYY-MM-DD`.

Приклад JSON витрати:
```json
{ "id": 1, "amount": 150.50, "category": "Їжа", "description": "Обід", "date": "2026-10-12" }
```

Формат помилки:
```json
{ "status": 400, "message": "Validation failed", "errors": { "amount": "must be greater than 0" } }
```

## Структура файлів (кінцева)

```
expense-traker/
├── CLAUDE.md, PLAN.md, README.md, .gitignore
├── docker-compose.yml
├── pom.xml
└── src/
    ├── main/java/com/example/expensetracker/
    │   ├── ExpenseTrackerApplication.java
    │   ├── model/Expense.java
    │   ├── repository/ExpenseRepository.java
    │   ├── service/ExpenseService.java
    │   ├── controller/ExpenseController.java
    │   └── exception/
    │       ├── ExpenseNotFoundException.java
    │       ├── ErrorResponse.java
    │       └── GlobalExceptionHandler.java
    ├── main/resources/
    │   ├── application.properties
    │   └── static/  index.html, style.css, app.js
    └── test/java/com/example/expensetracker/ ...
```

---

## Етап 1 — База даних у Docker ✅

- [x] 1.1 `docker-compose.yml`: образ `postgres:16`, база `expenses`, користувач `postgres`/`postgres`, том `pgdata`.
- [x] 1.2 Порт на Mac: **5433** (5432 зайнятий SSH-тунелем).
- [x] 1.3 Перевірка: `docker compose up -d` → `docker exec -it expense-tracker-db psql -U postgres -d expenses` → `SELECT version();`

**Чого навчились:** контейнери, порти `host:container`, томи для збереження даних.

## Етап 2 — Каркас Spring Boot ✅

- [x] 2.1 `pom.xml`: starter-web, starter-data-jpa, starter-validation, postgresql, starter-test.
- [x] 2.2 `application.properties`: підключення до `localhost:5433/expenses`, `ddl-auto=update`, `show-sql=true`, `open-in-view=false`.
- [x] 2.3 `ExpenseTrackerApplication` з виправленням часового поясу `Europe/Kyiv`.
- [x] 2.4 Перевірка: `mvn spring-boot:run` → у логах `Started ExpenseTrackerApplication`, `HikariPool-1 - Start completed`.

**Чого навчились:** Maven-залежності, автоконфігурація Spring Boot, читання логів і стектрейсів.

## Етап 3 — Git і порядок у репозиторії ✅

- [x] 3.1 `.gitignore`: `target/`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`, `*.log`.
- [x] 3.2 `git init`, перший коміт з етапами 1–2.
- [x] 3.3 Правила комітів — Conventional Commits (описано в `CLAUDE.md`); коміт після кожного завершеного етапу.

**Чого навчимось:** чому `target/` не комітять, історія змін як «точки збереження».

## Етап 4 — Сутність і репозиторій (шар даних)

- [ ] 4.1 `model/Expense.java`:
  - `@Entity`, `@Table(name = "expense")`;
  - `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`;
  - `@Column(precision = 12, scale = 2, nullable = false)` для `amount`;
  - `@Column(name = "expense_date", nullable = false)` для `date` (`date` — зарезервоване слово в SQL);
  - валідація: `@NotNull @Positive` для суми, `@NotBlank @Size(max = 50)` для категорії, `@Size(max = 255)` для опису, `@NotNull` для дати;
  - порожній конструктор (потрібен JPA), геттери й сеттери.
- [ ] 4.2 Запустити застосунок → у логах `create table expense ...` → `\d expense` у psql показує колонки.
- [ ] 4.3 `repository/ExpenseRepository.java` — `interface ExpenseRepository extends JpaRepository<Expense, Long>`:
  - `List<Expense> findByDateBetweenOrderByDateDescIdDesc(LocalDate from, LocalDate to)`;
  - `List<Expense> findByCategoryAndDateBetweenOrderByDateDescIdDesc(String category, LocalDate from, LocalDate to)`;
  - `@Query("select coalesce(sum(e.amount), 0) from Expense e where e.date between :from and :to and (:category is null or e.category = :category)") BigDecimal sumAmount(...)`.
- [ ] 4.4 Перевірка: кілька тестових рядків через psql (`INSERT INTO expense ...`) — вони знадобляться на наступних етапах.

**Чого навчимось:** ORM-відображення класу на таблицю, derived query methods (SQL з назви методу), JPQL.

## Етап 5 — Сервіс і обробка помилок (бізнес-логіка)

- [ ] 5.1 `exception/ExpenseNotFoundException.java` — `RuntimeException` з повідомленням `Expense with id=X not found`.
- [ ] 5.2 `service/ExpenseService.java` (`@Service`, репозиторій через конструктор):
  - `findAll(category, from, to)` — якщо дати не задані, підставляє широкий діапазон; порожню категорію вважає «всі»;
  - `findById(id)` — `orElseThrow(() -> new ExpenseNotFoundException(id))`;
  - `create(expense)` — обнуляє `id` (щоб клієнт не перезаписав чужий запис) і зберігає;
  - `update(id, expense)` — знаходить існуючий, копіює поля, зберігає;
  - `delete(id)` — перевіряє існування, потім видаляє;
  - `total(category, from, to)` — викликає `sumAmount`.
- [ ] 5.3 `@Transactional` на методах, що змінюють дані; `@Transactional(readOnly = true)` на читанні.

**Чого навчимось:** навіщо окремий шар сервісу, впровадження залежностей через конструктор, транзакції.

## Етап 6 — REST-контролер (API)

- [ ] 6.1 `controller/ExpenseController.java` — `@RestController @RequestMapping("/api/expenses")`, усі 6 ендпоінтів з таблиці вище:
  - `@RequestParam(required = false)` для фільтрів, `@DateTimeFormat(iso = DATE)` для дат;
  - `@Valid @RequestBody` для POST/PUT;
  - `ResponseEntity.status(201)` для створення, `ResponseEntity.noContent()` для видалення.
- [ ] 6.2 `exception/ErrorResponse.java` — `record ErrorResponse(int status, String message, Map<String, String> errors)`.
- [ ] 6.3 `exception/GlobalExceptionHandler.java` — `@RestControllerAdvice`:
  - `ExpenseNotFoundException` → `404`;
  - `MethodArgumentNotValidException` → `400` зі списком полів і помилок;
  - `HttpMessageNotReadableException` (зламаний JSON, неправильна дата) → `400`.
- [ ] 6.4 Перевірка curl-ом (кожен сценарій):
  ```bash
  curl -s localhost:8080/api/expenses
  curl -s -X POST localhost:8080/api/expenses -H 'Content-Type: application/json' \
       -d '{"amount":150.50,"category":"Їжа","description":"Обід","date":"2026-10-12"}'
  curl -s localhost:8080/api/expenses/1
  curl -s -X PUT localhost:8080/api/expenses/1 -H 'Content-Type: application/json' \
       -d '{"amount":200,"category":"Їжа","description":"Вечеря","date":"2026-10-12"}'
  curl -s "localhost:8080/api/expenses?category=Їжа&from=2026-10-01&to=2026-10-31"
  curl -s "localhost:8080/api/expenses/summary?from=2026-10-01&to=2026-10-31"
  curl -s -i -X DELETE localhost:8080/api/expenses/1          # 204
  curl -s -i localhost:8080/api/expenses/999                  # 404
  curl -s -i -X POST localhost:8080/api/expenses -H 'Content-Type: application/json' -d '{"amount":-5}'  # 400
  ```
- [ ] 6.5 Звірити дані в psql: `SELECT * FROM expense;`

**Чого навчимось:** HTTP-методи та статус-коди, серіалізація JSON (Jackson), валідація, централізована обробка помилок.

## Етап 7 — Фронтенд (HTML + CSS + JS)

- [ ] 7.1 `static/index.html` — розмітка:
  - заголовок; форма (сума `type=number step=0.01`, категорія `<select>`, опис, дата `type=date`), кнопки «Зберегти» / «Скасувати»;
  - панель фільтрів (категорія, від, до, «Застосувати», «Скинути»);
  - блок «Разом: … грн»;
  - таблиця витрат (дата, категорія, опис, сума, дії).
- [ ] 7.2 `static/style.css` — охайна верстка: центрована колонка, таблиця зі смугами, адаптивність для вузького екрана.
- [ ] 7.3 `static/app.js` — **читання:** `loadExpenses()` → `fetch('/api/expenses?...')` → `renderTable()`; `loadTotal()`. Відкрити `http://localhost:8080` — бачимо дані з етапу 6.
- [ ] 7.4 **Створення:** обробник `submit` форми → `POST` → перезавантажити список, очистити форму; дата за замовчуванням — сьогодні.
- [ ] 7.5 **Видалення:** кнопка «Видалити» → `confirm()` → `DELETE` → оновити список.
- [ ] 7.6 **Редагування:** кнопка «Редагувати» заповнює форму і запам'ятовує `editingId`; «Зберегти» робить `PUT` замість `POST`; «Скасувати» повертає форму в режим створення.
- [ ] 7.7 **Фільтри й підсумок:** параметри фільтра передаються і в список, і в `/summary`.
- [ ] 7.8 **Помилки:** якщо відповідь не `ok`, показати повідомлення з `ErrorResponse` біля форми.
- [ ] 7.9 Безпека: текст від користувача вставляти через `textContent`, не через `innerHTML` (захист від XSS).
- [ ] 7.10 Перевірка в браузері: додати, змінити, видалити, відфільтрувати; DevTools → Network — подивитись реальні запити та JSON.

**Чого навчимось:** DOM, події, `fetch` + `async/await`, як фронтенд і бекенд обмінюються JSON.

## Етап 8 — Тести

- [ ] 8.1 Додати залежності Testcontainers (`spring-boot-testcontainers`, `testcontainers:postgresql`, `testcontainers:junit-jupiter`) — тести запускають власний одноразовий PostgreSQL у Docker.
- [ ] 8.2 `ExpenseRepositoryTest` (`@DataJpaTest` + `@ServiceConnection`): збереження, фільтр за категорією і датами, сума.
- [ ] 8.3 `ExpenseControllerTest` (`@WebMvcTest` + `MockMvc`, сервіс замінено на мок): статуси 200/201/204/400/404.
- [ ] 8.4 Перевірка: `mvn test` — усе зелене.

**Чого навчимось:** модульні й інтеграційні тести, моки, навіщо тести, коли код змінюється.

## Етап 9 — Оформлення

- [ ] 9.1 `README.md`: що це, як запустити (3 команди), скріншот.
- [ ] 9.2 Фінальний прохід по коду: назви, зайве, коментарі там, де вони справді пояснюють.

## Ідеї для розвитку (після завершення)

- DTO (`ExpenseRequest` / `ExpenseResponse`) замість сутності в API.
- Flyway-міграції замість `ddl-auto=update`.
- Окрема таблиця `category` і зв'язок `@ManyToOne`.
- Пагінація (`Pageable`) для великої кількості записів.
- Графік витрат за місяцями (Chart.js).
- Експорт у CSV.
