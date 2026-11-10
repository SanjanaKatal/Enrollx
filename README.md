# EnrollX — Course Registration System with Real-Time Seat Locking

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%20InnoDB-blue.svg)](https://www.mysql.com/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1-green.svg)](https://www.thymeleaf.org/)
[![Bootstrap](https://img.shields.io/badge/Bootstrap-5.3-purple.svg)](https://getbootstrap.com/)

**EnrollX** is a university course registration platform built specifically as a comprehensive DBMS academic project. It demonstrates how to solve high-concurrency race conditions, overbooking, and lost updates in relational databases using **Pessimistic Row-Level Locking (`SELECT ... FOR UPDATE`)**, **ACID Transactions**, and **Time-To-Live (TTL) Temporary Seat Reservations**.

---

## 1. Key Features
- **Real-Time Seat Locking:** Exclusive row locks in MySQL InnoDB prevent simultaneous students from overbooking the final seat.
- **5-Minute Countdown Timer:** Visual and server-enforced countdown timer with auto-release upon expiration or cancellation.
- **Normalized Relational Schema (3NF):** Built with strict foreign keys, check constraints, and composite B+ tree indexes.
- **Two Role-Based Portals:**
  - **Student Portal:** Browse catalog, inspect live capacity gauges, lock seats, confirm enrollments, drop courses, and view audit history.
  - **Admin Portal:** Manage courses and offerings, adjust seat caps with safety validations, monitor active locks, and audit enrollments.
- **Production-Grade Spring Security 6:** BCrypt password hashing, session management, and CSRF protection.

---

## 2. Default Demo Credentials

| Role | Username | Password | Notes |
| :--- | :--- | :--- | :--- |
| **System Admin** | `admin` | `Admin@123` | Full administrative control |
| **Sample Student 1** | `student1` | `Student@123` | Already enrolled in CS301 (Demo seed) |
| **Sample Student 2** | `student2` | `Student@123` | Ready to reserve seats |
| **Sample Student 3** | `student3` | `Student@123` | Ready to test concurrent locking |

*New students can also register immediately via the "Register as Student" link.*

---

## 3. Step-by-Step Setup Guide for Windows

### Prerequisites
1. **Java JDK 17 or 21:** Download from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [Eclipse Temurin](https://adoptium.net/).
   - Open Command Prompt and verify: `java -version`
2. **MySQL Server 8.0+:** Download and install [MySQL Community Server](https://dev.mysql.com/downloads/mysql/).
3. **MySQL Workbench:** Included in the MySQL installer.
4. **IDE:** Spring Tool Suite (STS) 4 or IntelliJ IDEA Community/Ultimate.

---

### Step 1: Initialize Database in MySQL Workbench
1. Open **MySQL Workbench** and connect to your local MySQL instance (usually `localhost:3306`).
2. Click **File -> Open SQL Script...**
3. Browse to `EnrollX/database/enrollx_db.sql` and open it.
4. Click the yellow lightning bolt icon (**Execute**) to run the entire script.
5. In the left Navigator pane, right-click and click **Refresh All**. You will see `enrollx_db` created with 6 normalized tables and seed data.

---

### Step 2: Configure Database Credentials in Spring Boot
Open `src/main/resources/application.properties` and verify your MySQL password:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/enrollx_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8
spring.datasource.username=root
spring.datasource.password=root    # <-- Change to your MySQL root password if different
```

---

### Step 3: Import Project into Spring Tool Suite (STS) or IntelliJ

#### Option A: In Spring Tool Suite (STS)
1. Open STS and select **File -> Import...**
2. Choose **Maven -> Existing Maven Projects** and click **Next**.
3. Click **Browse...** and select the `EnrollX` folder.
4. Ensure `pom.xml` is checked and click **Finish**.
5. Wait 1–2 minutes for STS to download dependencies.
6. Right-click the project -> **Run As -> Spring Boot App**.

#### Option B: In IntelliJ IDEA
1. Open IntelliJ and select **File -> Open...**
2. Select the `EnrollX` folder and click **Open**.
3. Choose **Open as Project**. IntelliJ will recognize the `pom.xml` and sync Maven dependencies.
4. Navigate to `src/main/java/com/enrollx/EnrollXApplication.java`.
5. Right-click and choose **Run 'EnrollXApplication'**.

#### Option C: Running via Command Line
```powershell
cd EnrollX
mvn clean install
mvn spring-boot:run
```

---

### Step 4: Open the Application
Open your web browser and navigate to:
```
http://localhost:8080
```
Log in using either the Admin or Student credentials provided above.

---

## 4. How to Test Real-Time Seat Locking in Viva / College Demo

1. Open **Browser 1 (Chrome)**: Log in as `student2` / `Student@123`.
2. Open **Browser 2 (Chrome Incognito or Edge)**: Log in as `student3` / `Student@123`.
3. Notice that course **CS301 (Database Management Systems)** in `Fall 2026` has:
   - **Total Capacity:** 3
   - **Confirmed Enrolled:** 1 (Student 1)
   - **Available:** 2
4. In **Browser 1**, click **"Reserve Seat"** on CS301.
   - Student 2 enters the reservation screen with the **5:00 live countdown timer**.
5. In **Browser 2**, refresh the catalog.
   - The available seats dynamically decreases to **1**, with **1 Temporary Hold** indicated.
6. In **Browser 2**, click **"Reserve Seat"** on CS301.
   - Student 3 claims the final seat (0 seats remaining!).
7. Open a third window as `student4`:
   - CS301 shows **Course Full** and the Reserve button is automatically disabled.
8. In Browser 1, click **"Release Seat"** (Cancel Reservation).
   - In Student 4's window, refresh the page: 1 seat is instantly unlocked and available again!
9. In Browser 2, click **"Confirm Enrollment Now"**.
   - Student 3's enrollment is permanently written to disk. Verify in MySQL Workbench:
     ```sql
     SELECT * FROM enrollments WHERE offering_id = 2;
     ```

---

## 5. Troubleshooting Common Issues

### Issue 1: `Access denied for user 'root'@'localhost'`
- **Fix:** Update `spring.datasource.password` in `src/main/resources/application.properties` to match your local MySQL root password.

### Issue 2: `Port 8080 was already in use`
- **Fix:** Either terminate the existing process or change the port in `application.properties`:
  ```properties
  server.port=8081
  ```
  Then access `http://localhost:8081`.

### Issue 3: `Table 'enrollx_db.courses' doesn't exist`
- **Fix:** Ensure you ran `database/enrollx_db.sql` in MySQL Workbench, or allow Hibernate's auto-update by ensuring `spring.jpa.hibernate.ddl-auto=update` is present in `application.properties`.

---

## 6. DBMS Viva & Academic Documentation Index

All DBMS evaluation documents are available in the [`docs/`](docs/) directory:
- [Abstract & Scope](docs/abstract.md)
- [Requirements Specification (SRS)](docs/requirements.md)
- [Mermaid ER Diagram & Cardinality](docs/er-diagram.md)
- [Data Flow Diagrams (Level 0 and Level 1)](docs/dfd.md)
- [Normalization Proofs (1NF to 3NF)](docs/normalization.md)
- [10 Required Complex SQL Queries](docs/sql-queries.md)
- [Test Cases & Concurrency Test Plan](docs/test-cases.md)
- [Top 25 DBMS Viva Questions & Detailed Answers](docs/viva-questions.md)
