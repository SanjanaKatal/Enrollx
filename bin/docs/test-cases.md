# EnrollX — Test Plan & Test Cases Specification

## 1. Test Summary Matrix

| Test ID | Module | Test Scenario | Execution Mode | Expected Result | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **TC-01** | Auth | Student Registration with valid unique details | Manual / Web | Account created; redirected to login with success alert | **PASS** |
| **TC-02** | Auth | Registration with existing username/email | Automated / Web | Rejection with "Username/Email already taken" | **PASS** |
| **TC-03** | Auth | BCrypt password encryption verification | SQL / Unit | Plaintext password not found in `users` table; starts with `$2a$10$` | **PASS** |
| **TC-04** | Catalog | Real-time seat calculation | Automated / SQL | `available = capacity - (enrolled + active_holds)` | **PASS** |
| **TC-05** | Seat Lock | Pessimistic seat reservation for available seat | Automated / Web | Reservation created with status `PENDING` and 5-min TTL | **PASS** |
| **TC-06** | Seat Lock | Concurrent attempts for final available seat | Multi-threaded | Exactly 1 student succeeds; others receive `CourseFullException` | **PASS** |
| **TC-07** | Seat Lock | Duplicate hold attempt by same student | Automated | Rejected with "You already hold an active reservation" | **PASS** |
| **TC-08** | Expiry | Confirmation attempt after TTL expiration | Automated / Web | Rejected with `ReservationExpiredException`; status updated to `EXPIRED` | **PASS** |
| **TC-09** | Confirm | Atomic confirmation before expiry | Automated / Web | Reservation marked `CONFIRMED`; new row in `enrollments` table | **PASS** |
| **TC-10** | Release | Student cancels temporary hold | Manual / Web | Status marked `CANCELLED`; seat immediately becomes available | **PASS** |
| **TC-11** | Drop | Student drops confirmed course | Manual / Web | Enrollment status set to `DROPPED`; capacity incremented | **PASS** |
| **TC-12** | Admin | Reduce capacity below current confirmed count | Manual / Web | Form rejected with `InvalidCapacityException` | **PASS** |

---

## 2. Step-by-Step Two-Browser Concurrency Demonstration (For Viva / Demo)

This manual test demonstrates **Pessimistic Row-Level Locking** in real time using two different browser sessions and MySQL Workbench.

### Setup:
1. Ensure course **CS301 (DBMS)** in `Fall 2026` has:
   - Capacity: **3**
   - Enrolled: **1** (Student 1)
   - Available Seats: **2**
2. Have **Alice (`student1`)**, **Bob (`student2`)**, and **Carol (`student3`)** ready.

### Test Execution Steps:
1. **Window 1 (Chrome):** Log in as **Bob (`student2` / `Student@123`)**.
2. **Window 2 (Chrome Incognito or MS Edge):** Log in as **Carol (`student3` / `Student@123`)**.
3. In **Window 1**, navigate to **Course Catalog** and click **"Reserve Seat"** on CS301.
   - *Result:* Bob enters the Reservation screen with a live 5:00 countdown timer. Available seats in database drops from 2 to 1!
4. In **Window 2**, refresh the catalog.
   - *Result:* Carol sees available seats now equals **1**, with **1 Temporary Hold** indicated.
5. In **Window 2**, Carol clicks **"Reserve Seat"** on CS301.
   - *Result:* Carol secures the final remaining seat and gets her 5:00 countdown timer!
6. Open **Window 3** (or log in as **David `student4`**).
   - In David's view, CS301 now shows **"0 Available"**, and the Reserve button is automatically disabled displaying **"Course Full"**!
7. **Demonstrating Cancellation & Dynamic Release:**
   - In Window 1, Bob decides not to take the course and clicks **"Release Seat"** (Cancel Reservation).
   - In Window 3, refresh David's page: Available seats immediately updates to **1**, and the **"Reserve Seat"** button turns active again!
8. **Demonstrating Confirmation:**
   - In Window 2, Carol clicks **"Confirm Enrollment Now"**.
   - Her enrollment is finalized and appears under **"My Courses"**.
   - Check in MySQL Workbench:
     ```sql
     SELECT * FROM enrollments WHERE user_id = 4 AND offering_id = 2;
     ```
     The record is permanently committed to disk.
