# EnrollX — Project Abstract & Scope Document

## 1. Project Title
**EnrollX — Course Registration System with Real-Time Seat Locking**

## 2. Project Abstract
University course registration is a classic high-concurrency database scenario. When thousands of students compete for a limited number of course seats at the start of an academic semester, traditional systems frequently suffer from race conditions, overbooking, database deadlocks, or unfair seat distribution. 

**EnrollX** is an academic database management system (DBMS) project designed to solve the overbooking and concurrency problem using **Pessimistic Row-Level Locking (`SELECT ... FOR UPDATE`)**, **ACID Transactions**, and **Time-To-Live (TTL) Seat Reservations**. When a student requests a seat, the application acquires an exclusive InnoDB row lock on the course offering, validates capacity against confirmed enrollments and active reservations, and grants a 5-minute temporary hold. If the student confirms within the 5-minute window, the reservation is atomically converted into a permanent enrollment; if the student cancels or the timer lapses, the seat is released back into available capacity immediately.

## 3. Problem Statement
In naive web registration systems:
1. Two students simultaneously see "1 seat available".
2. Both click "Register" at the same instant.
3. Both read statements return capacity available (`available = 1`).
4. Both write statements insert enrollment records (`enrolled = 2` on a capacity of 1).
5. **Result:** An illegal overbooking anomaly that violates database consistency and business constraints.

EnrollX resolves this by demonstrating strict serializability and row-level locking at the database engine level, ensuring that concurrent transactions are coordinated safely without data corruption or phantom allocations.

## 4. Key Objectives
- **Relational Database Design:** Build a fully normalized (3NF) relational schema with primary keys, foreign keys, check constraints, and composite unique indexes.
- **Real-Time Concurrency Control:** Enforce pessimistic write locking using MySQL InnoDB storage engine to guarantee zero overbooking.
- **ACID Transaction Management:** Wrap seat reservation, confirmation, and drop operations in atomic transactional boundaries (`@Transactional`).
- **Temporary Seat Reservation:** Provide students with a 5-minute countdown window to review and confirm their schedule.
- **Role-Based Access Control:** Differentiate between student operations (catalog browsing, locking, confirmation) and administrator operations (course cataloging, capacity adjustments, audit logs).
- **Comprehensive DBMS Documentation:** Provide complete viva preparation material including ER diagrams, DFDs, normalization analysis, and complex SQL queries.

## 5. Scope of the System
### In Scope:
- Student user registration and secure authentication with BCrypt password hashing.
- Course catalog with real-time capacity and search/filtering by department and semester.
- Concurrency-safe seat reservation with live JavaScript countdown timer.
- Atomic registration confirmation and course dropping.
- Administrative course and section creation, seat capacity modification with safety validation.
- Background asynchronous cleanup of stale reservations.
- Visual and programmatic testing of concurrency using multi-threaded test suites.

### Out of Scope:
- Online fee payment gateway integration.
- Automated prerequisite transcript OCR verification.

## 6. Hardware & Software Requirements
### Software Requirements:
- **Operating System:** Windows 10/11, macOS, or Linux
- **Java Development Kit (JDK):** JDK 17 or JDK 21 (LTS)
- **Database Server:** MySQL Server 8.0 or higher (InnoDB Engine)
- **Database Client:** MySQL Workbench or DBeaver
- **Build Tool:** Apache Maven 3.8+ (or included Maven Wrapper `mvnw`)
- **Framework:** Spring Boot 3.2.x, Spring Data JPA, Spring Security 6
- **Template Engine:** Thymeleaf
- **Web Browser:** Google Chrome, Mozilla Firefox, or Microsoft Edge

### Hardware Requirements:
- **Processor:** Intel Core i3 / AMD Ryzen 3 or higher
- **RAM:** Minimum 4 GB (8 GB recommended)
- **Disk Storage:** At least 500 MB free space for JDK, Maven repository, and database
