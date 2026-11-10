# EnrollX — Top 25 DBMS Viva Questions & In-Depth Answers

This document provides comprehensive answers to viva examination questions based specifically on the **EnrollX** course registration and seat locking architecture.

---

### Q1: What is the primary DBMS problem that EnrollX solves?
**Answer:** EnrollX solves the **Lost Update** and **Overbooking Anomaly** caused by concurrent, uncoordinated read-check-write operations. In a naive system, two concurrent transactions can simultaneously read that 1 seat is available, and both commit registrations, resulting in 2 enrolled students on a capacity of 1. EnrollX eliminates this race condition using **Pessimistic Row-Level Write Locking (`SELECT ... FOR UPDATE`)** in MySQL InnoDB.

---

### Q2: What is the difference between Pessimistic Locking and Optimistic Locking? Which is used in EnrollX and why?
**Answer:**
- **Optimistic Locking:** Assumes conflict is rare. Records include a `@Version` number. When committing, the system checks if the version has changed; if so, an `OptimisticLockException` is thrown, and the transaction is aborted. This causes high abort rates and poor user experience when thousands of students compete for the same final seat.
- **Pessimistic Locking:** Assumes conflict is frequent and immediate. It requests an exclusive lock on the database row at the beginning of the transaction. Other transactions attempting to reserve the same offering must wait until the first transaction commits or rolls back.
- **Choice in EnrollX:** EnrollX uses **Pessimistic Write Locking (`PESSIMISTIC_WRITE`)** on the `course_offerings` row to serialize reservations fairly and avoid mass rollbacks.

---

### Q3: What SQL statement does `@Lock(LockModeType.PESSIMISTIC_WRITE)` generate in MySQL?
**Answer:** In MySQL InnoDB, Hibernate translates `@Lock(LockModeType.PESSIMISTIC_WRITE)` into:
```sql
SELECT * FROM course_offerings WHERE id = ? FOR UPDATE;
```
This instructs InnoDB to place an **Exclusive Row Lock (X-Lock)** on that specific row. Any subsequent query attempting `SELECT ... FOR UPDATE` or `UPDATE` on the same row blocks until the holding transaction issues a `COMMIT` or `ROLLBACK`.

---

### Q4: Why can't we just use Java's `synchronized` block instead of database row locking?
**Answer:** A Java `synchronized` block only coordinates threads running within the **same Java Virtual Machine (JVM)** process. In real-world enterprise deployments, applications run across multiple load-balanced server instances or containers (e.g., Docker, Kubernetes). Two separate servers have independent memory spaces, so Java `synchronized` cannot coordinate them. The database is the single shared source of truth; therefore, locking must be enforced at the **database engine level**.

---

### Q5: Explain the ACID properties and how each is enforced in EnrollX.
**Answer:**
1. **Atomicity:** When a student confirms a reservation, updating `seat_reservations` to `CONFIRMED` and inserting an `enrollments` row occurs within a single `@Transactional` boundary. If inserting fails, the reservation update rolls back. All succeed or none succeed.
2. **Consistency:** Database integrity constraints (`NOT NULL`, foreign keys, check constraints like `capacity > 0`, and unique constraints like `UNIQUE(offering_id, user_id)`) ensure the database never enters an invalid state.
3. **Isolation:** MySQL InnoDB uses `READ COMMITTED` or `REPEATABLE READ` transaction isolation with row locks, preventing transactions from seeing uncommitted dirty data.
4. **Durability:** Once a transaction commits, MySQL writes the changes to the Write-Ahead Log (InnoDB Redo Log) and flushes them to disk, ensuring data survives server power outages.

---

### Q6: Why did you choose MySQL's InnoDB storage engine instead of MyISAM?
**Answer:**
- **MyISAM** only supports table-level locking (locking the entire table during any insert/update, causing massive concurrency bottlenecks) and does **not** support ACID transactions or foreign keys.
- **InnoDB** supports true **row-level locking**, foreign key referential integrity constraints, and ACID-compliant transaction rollback, which are mandatory for EnrollX.

---

### Q7: How does EnrollX handle reservation expiration? Does it solely rely on a background scheduled cleanup?
**Answer:** **No, it does not rely solely on the background scheduler.** The scheduler runs periodically (e.g., every 30 seconds) as a housekeeping routine to keep historical records tidy. However, every seat availability calculation and confirmation check dynamically evaluates:
```sql
status = 'PENDING' AND expires_at > NOW()
```
Because expiration is evaluated using server-side timestamps on **every single request**, a seat is released the exact millisecond its 5-minute timer expires, even if the background scheduler has not yet executed.

---

### Q8: What normal form is the EnrollX database in? Explain why.
**Answer:** The database is in **Third Normal Form (3NF)**:
- **1NF:** All attributes are atomic and indivisible; no repeating groups.
- **2NF:** It is in 1NF and contains no partial dependencies (every non-key attribute depends on the full primary key).
- **3NF:** It is in 2NF and contains no transitive dependencies (no non-key attribute depends on another non-key attribute). For example, department attributes (`code`, `name`) were separated from `courses` into a dedicated `departments` table.

---

### Q9: What is the purpose of the composite index `idx_res_offering_status_expires (offering_id, status, expires_at)`?
**Answer:** In MySQL, this creates a B+ Tree index covering all three columns. The core query to calculate seats executes:
```sql
SELECT COUNT(*) FROM seat_reservations WHERE offering_id = ? AND status = 'PENDING' AND expires_at > NOW();
```
With this composite index, the database engine performs an efficient **Index Range Scan** directly on the B+ Tree without reading whole data pages from disk, executing in sub-millisecond time.

---

### Q10: How does EnrollX prevent a student from registering twice for the same course?
**Answer:** At two levels:
1. **Application Level:** `EnrollmentRepository.existsByCourseOfferingIdAndUserIdAndStatus(...)` checks for prior enrollment before granting a hold.
2. **Database Engine Level:** The unique constraint `CONSTRAINT uq_offering_user UNIQUE (offering_id, user_id)` guarantees that even if a concurrent race condition bypassed application logic, MySQL would reject the second `INSERT` with error code `1062 (Duplicate entry)`.

---

### Q11: What is a Deadlock in MySQL, and how does EnrollX prevent them?
**Answer:** A deadlock occurs when Transaction A holds a lock on Row 1 and requests Row 2, while Transaction B holds a lock on Row 2 and requests Row 1. Neither can proceed.
- **Prevention in EnrollX:** All reservation transactions acquire locks in a consistent, uniform order: first acquire the row lock on `course_offerings`, then insert into `seat_reservations`. By never acquiring locks out-of-order, circular wait dependencies are eliminated.

---

### Q12: What is the difference between `ON DELETE CASCADE` and `ON DELETE RESTRICT`?
**Answer:**
- `ON DELETE CASCADE`: If a parent row is deleted, MySQL automatically deletes all child rows referencing it.
- `ON DELETE RESTRICT`: MySQL refuses deletion of a parent row if any child rows reference it.
- In EnrollX, `courses` and `course_offerings` use `ON DELETE RESTRICT` to protect academic history from accidental deletion.

---

### Q13: Explain the difference between `READ COMMITTED` and `REPEATABLE READ` isolation levels.
**Answer:**
- **READ COMMITTED:** A transaction only reads data committed before each query starts. Phantom reads and non-repeatable reads can occur if another transaction commits between queries.
- **REPEATABLE READ (MySQL Default):** Guarantees that any row read by a transaction will look the same throughout the entire transaction by utilizing Multi-Version Concurrency Control (MVCC) snapshots.

---

### Q14: What is a Phantom Read?
**Answer:** A phantom read occurs when Transaction A queries a range of rows (e.g., `COUNT(*) WHERE offering_id = 2`), and before Transaction A finishes, Transaction B inserts a new row matching that condition and commits. When Transaction A runs the count again, a "phantom" row has appeared. InnoDB prevents phantom reads in `REPEATABLE READ` using **Next-Key Locks**.

---

### Q15: What is the purpose of the `version` column in `seat_reservations`?
**Answer:** The `version` column is annotated with `@Version` in JPA. While seat reservation utilizes pessimistic row locking on `course_offerings`, the `version` field serves as a secondary optimistic lock safeguard during status updates to prevent concurrent stale writes on the reservation entity itself.

---

### Q16: Why are passwords hashed with BCrypt instead of MD5 or SHA-256?
**Answer:** MD5 and SHA-256 are fast mathematical hashing functions designed for checksums. Because GPUs can compute billions of SHA-256 hashes per second, they are vulnerable to dictionary and rainbow table attacks. **BCrypt** includes an adaptive work factor (salt rounds = 10) and an internal salt that slows down computation intentionally, rendering brute-force attacks computationally infeasible.

---

### Q17: What is the role of HikariCP in this project?
**Answer:** HikariCP is a high-performance JDBC connection pool. Instead of opening a new TCP socket connection to MySQL for every HTTP request (which takes 50–100ms of handshake overhead), HikariCP maintains a pool of pre-established physical connections. Spring Boot borrows a connection from the pool, runs the transaction, and returns it immediately.

---

### Q18: What is the difference between DDL, DML, and DCL in the context of EnrollX?
**Answer:**
- **DDL (Data Definition Language):** Statements that define the schema structure, such as `CREATE TABLE`, `ALTER TABLE`, and `DROP TABLE` in `enrollx_db.sql`.
- **DML (Data Manipulation Language):** Statements that manipulate row data, such as `INSERT`, `UPDATE`, `DELETE`, and `SELECT`.
- **DCL (Data Control Language):** Statements that manage user permissions, such as `GRANT` and `REVOKE`.

---

### Q19: Explain the difference between `COUNT(*)`, `COUNT(column)`, and `COUNT(DISTINCT column)`.
**Answer:**
- `COUNT(*)` counts all rows returned by the query, including rows containing `NULL` values.
- `COUNT(column)` counts only rows where the specified column is **not NULL**.
- `COUNT(DISTINCT column)` counts unique, non-NULL values of the specified column (used in Query 9 to count distinct enrolled students).

---

### Q20: How does the admin dashboard prevent an administrator from entering an invalid capacity?
**Answer:** In `CourseOfferingService.java`, before updating `capacity`, the system queries `countByCourseOfferingIdAndStatus(offeringId, EnrollmentStatus.ENROLLED)`. If `newCapacity < confirmedEnrollments`, the application throws an `InvalidCapacityException`, preventing an administrator from compromising confirmed student seats.

---

### Q21: What is a Foreign Key constraint and why is it important in relational DBMS?
**Answer:** A Foreign Key is a field in one table that links to the Primary Key of another table. It enforces **Referential Integrity**, guaranteeing that a child record cannot reference a non-existent parent record (e.g., an enrollment cannot be assigned to a non-existent course offering).

---

### Q22: What happens if a server crashes while a student is in the middle of a 5-minute reservation?
**Answer:** The reservation row is already persisted in the database with `status = 'PENDING'` and `expires_at = timestamp`. When the server reboots, the client or background cleanup checks the server time against `expires_at`. If 5 minutes have elapsed, the seat is recognized as expired. If time remains, the student can log back in and confirm their enrollment.

---

### Q23: Why do we use Surrogate Primary Keys (`id BIGINT AUTO_INCREMENT`) instead of Natural Keys (like `course_code` or `student_id`)?
**Answer:**
1. **Performance:** Integer and BigInt primary keys are compact (8 bytes) and sequence-ordered, which minimizes B+ Tree page splits during inserts and ensures smaller secondary index sizes.
2. **Immutability:** Natural keys (like student IDs or course codes) can change due to university administrative policy renumbering. If natural keys were foreign keys across 5 tables, an update would require cascading modifications across thousands of child records.

---

### Q24: What is the difference between `WHERE` and `HAVING` in SQL?
**Answer:**
- `WHERE` filters rows **before** any aggregation or grouping takes place.
- `HAVING` filters aggregated summary rows **after** the `GROUP BY` clause has been computed.

---

### Q25: How would you scale EnrollX to handle 100,000 students registering simultaneously at 8:00 AM?
**Answer:**
1. **Database Read/Write Splitting:** Route read queries (course browsing and catalog searches) to MySQL Read Replicas, reserving the Primary master instance exclusively for transactional seat locking.
2. **In-Memory Caching / Redis Lock:** Offload hot seat counter checks and distributed locking to Redis with Lua scripts or Redlock before touching the relational database.
3. **Message Queue Buffer:** Place reservation attempts on a RabbitMQ or Kafka queue to smoothly rate-limit traffic hitting the relational database.
