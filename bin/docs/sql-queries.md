# EnrollX — 10 Required Complex SQL Queries & DBMS Concepts

Below are the 10 production-tested SQL queries written for MySQL 8.x InnoDB, annotated with the fundamental DBMS concepts they illustrate.

---

### Query 1: List All Courses with Real-Time Capacity & Available Seats
**DBMS Concept:** Correlated Scalar Subqueries, Arithmetic Expressions, and Multi-Table Inner Joins (`JOIN`).
```sql
SELECT 
    o.id AS offering_id,
    c.code AS course_code,
    c.title AS course_title,
    d.code AS department,
    o.semester,
    o.capacity AS total_capacity,
    (
        SELECT COUNT(*) 
        FROM enrollments e 
        WHERE e.offering_id = o.id AND e.status = 'ENROLLED'
    ) AS confirmed_enrolled,
    (
        SELECT COUNT(*) 
        FROM seat_reservations r 
        WHERE r.offering_id = o.id 
          AND r.status = 'PENDING' 
          AND r.expires_at > NOW()
    ) AS active_reservations,
    (
        o.capacity - 
        (SELECT COUNT(*) FROM enrollments e WHERE e.offering_id = o.id AND e.status = 'ENROLLED') - 
        (SELECT COUNT(*) FROM seat_reservations r WHERE r.offering_id = o.id AND r.status = 'PENDING' AND r.expires_at > NOW())
    ) AS available_seats
FROM course_offerings o
JOIN courses c ON o.course_id = c.id
JOIN departments d ON c.department_id = d.id
WHERE o.active = TRUE
ORDER BY c.code ASC;
```

---

### Query 2: Find All Courses Registered by a Specific Student
**DBMS Concept:** Multi-table relational navigation (`users` -> `enrollments` -> `course_offerings` -> `courses`) and Parameterized Filtering.
```sql
SELECT 
    u.student_id,
    u.full_name AS student_name,
    c.code AS course_code,
    c.title AS course_title,
    c.credits,
    o.semester,
    o.instructor,
    e.enrolled_at
FROM enrollments e
JOIN users u ON e.user_id = u.id
JOIN course_offerings o ON e.offering_id = o.id
JOIN courses c ON o.course_id = c.id
WHERE u.username = 'student1' 
  AND e.status = 'ENROLLED'
ORDER BY e.enrolled_at DESC;
```

---

### Query 3: Calculate Total Confirmed Enrollments per Course Offering
**DBMS Concept:** Aggregation with `COUNT()`, `GROUP BY`, and `LEFT OUTER JOIN` (ensuring offerings with 0 enrollments still appear).
```sql
SELECT 
    o.id AS offering_id,
    c.code AS course_code,
    c.title AS course_title,
    o.semester,
    o.capacity,
    COUNT(e.id) AS confirmed_enrollments
FROM course_offerings o
JOIN courses c ON o.course_id = c.id
LEFT JOIN enrollments e ON o.id = e.offering_id AND e.status = 'ENROLLED'
GROUP BY o.id, c.code, c.title, o.semester, o.capacity
ORDER BY confirmed_enrollments DESC;
```

---

### Query 4: Calculate Active Reservations per Offering
**DBMS Concept:** Conditional Aggregation, Temporal Comparison (`expires_at > NOW()`), and Index Range Scanning.
```sql
SELECT 
    o.id AS offering_id,
    c.code AS course_code,
    COUNT(r.id) AS active_holds
FROM course_offerings o
JOIN courses c ON o.course_id = c.id
LEFT JOIN seat_reservations r ON o.id = r.offering_id 
    AND r.status = 'PENDING' 
    AND r.expires_at > NOW()
GROUP BY o.id, c.code
ORDER BY active_holds DESC;
```

---

### Query 5: Find Courses with Zero Available Seats (Fully Booked)
**DBMS Concept:** Subquery filtering, Boolean Arithmetic, and `HAVING` clause equivalent via Derived Table.
```sql
SELECT * FROM (
    SELECT 
        o.id AS offering_id,
        c.code AS course_code,
        c.title AS course_title,
        o.capacity,
        (
            (SELECT COUNT(*) FROM enrollments e WHERE e.offering_id = o.id AND e.status = 'ENROLLED') +
            (SELECT COUNT(*) FROM seat_reservations r WHERE r.offering_id = o.id AND r.status = 'PENDING' AND r.expires_at > NOW())
        ) AS total_occupied_seats
    FROM course_offerings o
    JOIN courses c ON o.course_id = c.id
    WHERE o.active = TRUE
) AS seat_stats
WHERE seat_stats.total_occupied_seats >= seat_stats.capacity;
```

---

### Query 6: Display Students Enrolled in a Particular Course (e.g., CS301)
**DBMS Concept:** Relational Projection and Joins spanning the associative junction table.
```sql
SELECT 
    u.student_id,
    u.full_name,
    u.email,
    e.enrolled_at,
    e.status
FROM enrollments e
JOIN users u ON e.user_id = u.id
JOIN course_offerings o ON e.offering_id = o.id
JOIN courses c ON o.course_id = c.id
WHERE c.code = 'CS301' 
  AND o.semester = 'Fall 2026'
  AND e.status = 'ENROLLED'
ORDER BY u.full_name ASC;
```

---

### Query 7: Display Students Who Have Not Enrolled in Any Course
**DBMS Concept:** `LEFT OUTER JOIN` with `IS NULL` check (Set Difference / Antijoin), or `NOT IN` subquery.
```sql
-- Technique A: Left Outer Join (Antijoin pattern)
SELECT 
    u.id,
    u.student_id,
    u.full_name,
    u.email
FROM users u
LEFT JOIN enrollments e ON u.id = e.user_id AND e.status = 'ENROLLED'
WHERE u.role = 'ROLE_STUDENT' 
  AND e.id IS NULL;
```

---

### Query 8: Find Stale / Expired Pending Reservations
**DBMS Concept:** Temporal logic and State Machine auditing. Identifies seat holds that exceeded their 5-minute Time-To-Live.
```sql
SELECT 
    r.id AS reservation_id,
    u.username,
    u.full_name,
    c.code AS course_code,
    r.reserved_at,
    r.expires_at,
    TIMESTAMPDIFF(MINUTE, r.expires_at, NOW()) AS minutes_overdue
FROM seat_reservations r
JOIN users u ON r.user_id = u.id
JOIN course_offerings o ON r.offering_id = o.id
JOIN courses c ON o.course_id = c.id
WHERE r.status = 'PENDING' 
  AND r.expires_at <= NOW()
ORDER BY r.expires_at ASC;
```

---

### Query 9: Generate Enrollment Counts Grouped by Semester
**DBMS Concept:** Dimensional Aggregation and Multi-attribute Grouping.
```sql
SELECT 
    o.semester,
    o.academic_year,
    COUNT(DISTINCT e.user_id) AS total_unique_students,
    COUNT(e.id) AS total_course_registrations
FROM enrollments e
JOIN course_offerings o ON e.offering_id = o.id
WHERE e.status = 'ENROLLED'
GROUP BY o.semester, o.academic_year
ORDER BY o.academic_year DESC, o.semester ASC;
```

---

### Query 10: Generate Course Popularity Statistics
**DBMS Concept:** Percentage calculation, Sorting by aggregate ranking, and Mathematical Expressions.
```sql
SELECT 
    c.code AS course_code,
    c.title AS course_title,
    o.semester,
    o.capacity,
    COUNT(e.id) AS enrolled_students,
    ROUND((COUNT(e.id) * 100.0 / o.capacity), 2) AS utilization_percentage
FROM course_offerings o
JOIN courses c ON o.course_id = c.id
LEFT JOIN enrollments e ON o.id = e.offering_id AND e.status = 'ENROLLED'
GROUP BY c.code, c.title, o.semester, o.capacity
ORDER BY utilization_percentage DESC;
```
