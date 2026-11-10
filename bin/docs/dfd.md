# EnrollX — Data Flow Diagrams (DFD)

## 1. DFD Level 0 (Context Diagram)

The Level 0 Context Diagram depicts the entire EnrollX system as a single central process interacting with external entities (Student, Administrator, and MySQL Database Engine).

```mermaid
flowchart TD
    Student(["Student"])
    Admin(["Administrator"])
    EnrollXSystem[["EnrollX Course Registration & Seat Locking System"]]
    DB[("MySQL Database (InnoDB)")]

    Student -->|"1. Credentials / Registration"| EnrollXSystem
    Student -->|"2. Reserve Seat Request"| EnrollXSystem
    Student -->|"3. Confirm / Cancel Hold"| EnrollXSystem
    Student -->|"4. Drop Course Request"| EnrollXSystem

    EnrollXSystem -->|"Live Seat Availability & Countdown"| Student
    EnrollXSystem -->|"Enrollment Confirmation Receipt"| Student

    Admin -->|"5. Manage Courses & Offerings"| EnrollXSystem
    Admin -->|"6. Adjust Seat Capacity"| EnrollXSystem
    Admin -->|"7. View Analytics & Audit Logs"| EnrollXSystem

    EnrollXSystem -->|"Dashboard Reports & Statistics"| Admin

    EnrollXSystem <-->|"ACID Transactions, Row Locks & Queries"| DB
```

---

## 2. DFD Level 1 (Detailed Subsystem Decomposition)

The Level 1 DFD decomposes the system into six core functional sub-processes and data stores.

```mermaid
flowchart TD
    Student(["Student"])
    Admin(["Administrator"])

    subgraph Data Stores
        D1[("D1: Users")]
        D2[("D2: Departments & Courses")]
        D3[("D3: Course Offerings")]
        D4[("D4: Seat Reservations")]
        D5[("D5: Enrollments")]
    end

    subgraph Processes
        P1["1.0 Authentication & User Profile Management"]
        P2["2.0 Course Catalog Browsing & Live Seat Query"]
        P3["3.0 Pessimistic Seat Locking & Reservation Engine"]
        P4["4.0 Atomic Confirmation & Permanent Enrollment"]
        P5["5.0 Administrative Course & Section Management"]
        P6["6.0 Stale Reservation Cleanup Service"]
    end

    %% Process 1
    Student -->|"Signup / Login"| P1
    Admin -->|"Admin Login"| P1
    P1 <-->|"Validate Credentials"| D1

    %% Process 2
    Student -->|"Filter by Semester/Dept"| P2
    D2 -->|"Course Info"| P2
    D3 -->|"Capacity"| P2
    D4 -->|"Active Holds"| P2
    D5 -->|"Confirmed Counts"| P2
    P2 -->|"Live Seat Availability"| Student

    %% Process 3
    Student -->|"Reserve Seat Request"| P3
    P3 -->|"Pessimistic Row Lock (SELECT ... FOR UPDATE)"| D3
    P3 -->|"Check Capacity & Duplicate"| D4
    P3 -->|"Create 5-min Hold"| D4
    P3 -->|"Reservation ID & Countdown Timer"| Student

    %% Process 4
    Student -->|"Confirm Reservation"| P4
    P4 -->|"Verify TTL (NOW < expires_at)"| D4
    P4 -->|"Mark Status CONFIRMED"| D4
    P4 -->|"Insert Confirmed Enrollment"| D5
    P4 -->|"Enrollment Success"| Student

    %% Process 5
    Admin -->|"Add Course / Offering / Capacity"| P5
    P5 -->|"Save Master Course"| D2
    P5 -->|"Update Offering / Capacity"| D3
    D5 -->|"Aggregate Statistics"| P5
    P5 -->|"Analytics Report"| Admin

    %% Process 6
    P6 -->|"Scan Pending (NOW >= expires_at)"| D4
    P6 -->|"Update Status to EXPIRED"| D4
```
