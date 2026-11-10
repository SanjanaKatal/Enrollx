# EnrollX — Database Normalization Analysis (1NF to 3NF)

Normalization is the process of structuring a relational database in accordance with normal forms to reduce data redundancy and eliminate update, insertion, and deletion anomalies.

---

## 1. Unnormalized Form (UNF) Scenario
In an unnormalized course registration spreadsheet:
`StudentRegistration (StudentID, StudentName, StudentEmail, CourseCode, CourseTitle, DepartmentCode, DepartmentName, Credits, Semester, Instructor, Room, Capacity, ReservedAt, ExpiresAt, EnrolledAt)`

### Anomalies Present in UNF:
1. **Redundancy:** Department name, instructor, and course credits are duplicated for every student enrolled.
2. **Insertion Anomaly:** Cannot record a new course without enrolling at least one student.
3. **Deletion Anomaly:** If the only student in a course drops out, the course details and instructor information might be wiped out.
4. **Update Anomaly:** Changing the capacity or instructor of a course requires updating hundreds of student rows simultaneously; any missed row creates inconsistent data.

---

## 2. First Normal Form (1NF)
### Rule:
- Each table cell must contain a single, atomic (indivisible) value.
- There must be no repeating groups or multi-valued columns.
- Each row must have a unique identifier (Primary Key).

### Application in EnrollX:
- Separate multi-valued entries (e.g., student holding multiple courses) into individual rows.
- Atomic data types: Strings, Integers, Timestamps, and Enums.
- Synthetic surrogate primary keys (`BIGINT AUTO_INCREMENT`) defined on all tables.

---

## 3. Second Normal Form (2NF)
### Rule:
- Table must already be in **1NF**.
- No **partial functional dependency**: Every non-key attribute must depend on the **entire** candidate key, not a proper subset of a composite primary key.

### Decomposition into 2NF:
In a naive junction table with composite key `(StudentID, CourseCode, Semester)`:
- `Credits`, `CourseTitle`, and `DepartmentName` depend only on `CourseCode` (a subset of the primary key). This is a partial dependency.
- **Resolution:** Extract course details into a separate table `courses(id, code, title, credits, department_id)` and sections into `course_offerings(id, course_id, semester, ...)`.
- As a result, all non-key attributes now depend fully on the primary key of their respective tables.

---

## 4. Third Normal Form (3NF)
### Rule:
- Table must already be in **2NF**.
- No **transitive functional dependency**: Non-key attributes must not depend on other non-key attributes ($X \rightarrow Y \rightarrow Z$). Non-key columns must depend *only* on the primary key ("the key, the whole key, and nothing but the key").

### Decomposition into 3NF:
Notice that in the `courses` relation:
- `course_id -> department_code -> department_name`
- `department_name` depends on `department_code`, which is a non-key attribute in `courses`.
- **Resolution:** Decompose `departments` into its own entity:
  - `departments(id, code, name)`
  - `courses(id, code, title, description, credits, department_id)`
  where `courses.department_id` is a Foreign Key referencing `departments.id`.

### Final 3NF Relational Schema:
1. `departments (id [PK], code [UK], name)`
   - $FD: \text{id} \rightarrow \text{code, name}; \quad \text{code} \rightarrow \text{id, name}$
2. `users (id [PK], username [UK], email [UK], password, full_name, student_id [UK], role, enabled)`
   - $FD: \text{id} \rightarrow \text{all attributes}; \quad \text{username} \rightarrow \text{all attributes}$
3. `courses (id [PK], code [UK], title, description, credits, department_id [FK], active)`
   - $FD: \text{id} \rightarrow \text{code, title, description, credits, department_id, active}$
4. `course_offerings (id [PK], course_id [FK], semester, academic_year, capacity, instructor, room, schedule, active)`
   - Candidate Key: `id`, and composite `(course_id, semester, academic_year)`
   - $FD: \text{id} \rightarrow \text{all attributes}$
5. `seat_reservations (id [PK], offering_id [FK], user_id [FK], reserved_at, expires_at, status, version)`
   - $FD: \text{id} \rightarrow \text{all attributes}$
6. `enrollments (id [PK], offering_id [FK], user_id [FK], enrolled_at, status, dropped_at)`
   - Candidate Key: `id`, and composite `(offering_id, user_id)`
   - $FD: \text{id} \rightarrow \text{all attributes}$

---

## 5. Anomaly Analysis Summary

| Anomaly Type | In Unnormalized Schema | In EnrollX 3NF Normalized Schema |
| :--- | :--- | :--- |
| **Insertion** | Cannot add course without student enrollment. | Courses and Offerings exist independently in `courses` and `course_offerings`. |
| **Deletion** | Deleting a student drops course metadata. | Deleting an enrollment does not affect course catalog records (`ON DELETE RESTRICT`). |
| **Update** | Changing instructor requires updating all student rows. | Updated in one single row in `course_offerings`. |
