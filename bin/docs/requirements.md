# EnrollX — Software Requirements Specification (SRS)

## 1. Functional Requirements (FR)

### FR-1: User Management & Authentication
- **FR-1.1:** System shall permit new students to register with Full Name, Student ID, Email, Username, and Password.
- **FR-1.2:** System shall enforce unique constraints on Username, University Email, and Student ID.
- **FR-1.3:** System shall encrypt all passwords using the BCrypt hashing algorithm prior to persistence.
- **FR-1.4:** System shall support authentication and role-based authorization for two distinct roles: `ROLE_STUDENT` and `ROLE_ADMIN`.

### FR-2: Course & Section Management (Admin)
- **FR-2.1:** Administrator shall create, update, and toggle active status of master courses.
- **FR-2.2:** Administrator shall configure course offerings with semester, academic year, instructor, room, schedule, and maximum seat capacity.
- **FR-2.3:** System shall reject offering capacity reductions below the number of currently confirmed students (`new_capacity >= confirmed_enrollments`).
- **FR-2.4:** System shall prevent duplicate sections of the same course in the same semester and academic year.

### FR-3: Course Catalog & Real-Time Capacity (Student)
- **FR-3.1:** Students shall view all active course offerings for the current term.
- **FR-3.2:** System shall display real-time calculated seat counts: `Capacity`, `Enrolled`, `Reserved (Holds)`, and `Available Seats`.
- **FR-3.3:** Students shall filter offerings by department, semester, and search keyword.

### FR-4: Real-Time Seat Locking & Reservation
- **FR-4.1:** Student shall initiate a temporary seat reservation by clicking "Reserve Seat".
- **FR-4.2:** System shall acquire an exclusive row-level lock (`PESSIMISTIC_WRITE`) on the target course offering in MySQL InnoDB.
- **FR-4.3:** System shall verify student is not already enrolled and does not hold an active hold for the course.
- **FR-4.4:** If `availableSeats > 0`, system shall create a reservation record with an expiry timestamp set to `NOW() + 5 minutes`.
- **FR-4.5:** If `availableSeats <= 0`, system shall reject the reservation with a `CourseFullException` message.
- **FR-4.6:** Student shall see a live countdown timer showing remaining seconds on the reservation confirmation screen.

### FR-5: Enrollment Confirmation & Drop
- **FR-5.1:** Student shall confirm enrollment before timer expiration, atomically converting the hold into a confirmed enrollment.
- **FR-5.2:** If the student clicks confirm after the expiration timestamp, system shall reject the request and mark the reservation as `EXPIRED`.
- **FR-5.3:** Student shall cancel an active hold at any time, immediately returning the seat to available capacity.
- **FR-5.4:** Student shall drop an enrolled course from their dashboard, instantly freeing capacity for other students.

---

## 2. Non-Functional Requirements (NFR)

### NFR-1: Data Integrity & ACID Compliance
- All seat reservation, confirmation, and drop operations must execute within transactional boundaries (`@Transactional`) guaranteeing **Atomicity**, **Consistency**, **Isolation**, and **Durability**.

### NFR-2: Concurrency & Serializability
- System must prevent race conditions and overbooking when multiple concurrent users attempt to book the final available seat. The database row lock must serialize access to the seat count evaluation.

### NFR-3: Performance & Response Time
- Seat reservation response time under typical college load (< 50 concurrent requests/sec) shall be under 200 milliseconds.
- Database queries must leverage composite B+ tree indexes on foreign keys and status/expiry timestamps.

### NFR-4: Security
- Prevention of CSRF attacks on all state-altering POST requests.
- Role-based authorization restricting administrative endpoints (`/admin/**`) exclusively to administrators.
- Password hashes must employ work factor (salt rounds = 10).

---

## 3. Use Case Descriptions

```
+-------------------------------------------------------------------------+
| Use Case UC-1: Reserve Course Seat                                      |
+-------------------------------------------------------------------------+
| Primary Actor: Student                                                  |
| Preconditions: Student is authenticated and course has available seats. |
| Main Flow:                                                              |
|   1. Student navigates to Course Catalog.                               |
|   2. Student clicks "Reserve Seat" on Course Offering.                  |
|   3. System starts transaction with isolation READ_COMMITTED.           |
|   4. System executes SELECT ... FOR UPDATE on course offering row.     |
|   5. System calculates confirmed enrollments + active reservations.     |
|   6. System verifies available seats > 0.                               |
|   7. System creates SeatReservation with 5-minute TTL.                  |
|   8. Transaction commits and releases row lock.                         |
|   9. Student is redirected to reservation countdown page.               |
| Alternate Flow (Seat Full):                                             |
|   6a. If available seats == 0, system throws CourseFullException.      |
|   6b. Transaction rolls back. User is alerted with "Course is Full".    |
+-------------------------------------------------------------------------+
```
