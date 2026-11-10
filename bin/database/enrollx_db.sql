-- ============================================================================
-- Project: EnrollX — Course Registration System with Real-Time Seat Locking
-- Database: MySQL 8.x
-- Storage Engine: InnoDB (Mandatory for ACID Transactions & Row-Level Locking)
-- File: database/enrollx_db.sql
-- ============================================================================

-- Step 1: Create Database
CREATE DATABASE IF NOT EXISTS enrollx_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE enrollx_db;

-- Step 2: Disable Foreign Key Checks temporarily for clean re-creation
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS enrollments;
DROP TABLE IF EXISTS seat_reservations;
DROP TABLE IF EXISTS course_offerings;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS departments;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================================
-- TABLE 1: departments
-- Purpose: Academic departments offering courses
-- ============================================================================
CREATE TABLE departments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(10) NOT NULL,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_department_code UNIQUE (code)
) ENGINE=InnoDB;

-- ============================================================================
-- TABLE 2: users
-- Purpose: System users (Students and Administrators) with BCrypt hashed credentials
-- ============================================================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    student_id VARCHAR(30) NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'ROLE_STUDENT',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_username UNIQUE (username),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT uq_user_student_id UNIQUE (student_id)
) ENGINE=InnoDB;

-- Index on role for fast administrative filtering
CREATE INDEX idx_users_role ON users(role);

-- ============================================================================
-- TABLE 3: courses
-- Purpose: Master catalog of academic courses
-- ============================================================================
CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    credits INT NOT NULL,
    department_id BIGINT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_code UNIQUE (code),
    CONSTRAINT chk_course_credits CHECK (credits > 0 AND credits <= 10),
    CONSTRAINT fk_course_department FOREIGN KEY (department_id) 
        REFERENCES departments(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_courses_dept ON courses(department_id);

-- ============================================================================
-- TABLE 4: course_offerings
-- Purpose: Section/instance of a course for a specific semester and academic year
-- Critical: Row-level pessimistic write lock (FOR UPDATE) is acquired on this table
-- ============================================================================
CREATE TABLE course_offerings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    semester VARCHAR(20) NOT NULL,
    academic_year VARCHAR(10) NOT NULL,
    capacity INT NOT NULL,
    instructor VARCHAR(100) NOT NULL,
    room VARCHAR(50),
    schedule VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_course_offering UNIQUE (course_id, semester, academic_year),
    CONSTRAINT chk_offering_capacity CHECK (capacity > 0),
    CONSTRAINT fk_offering_course FOREIGN KEY (course_id) 
        REFERENCES courses(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_offerings_sem_year ON course_offerings(semester, academic_year);

-- ============================================================================
-- TABLE 5: seat_reservations
-- Purpose: Temporary seat holds while a student completes registration (5-minute TTL)
-- Critical: Expiration timestamp and status prevent overbooking and deadlocks
-- ============================================================================
CREATE TABLE seat_reservations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offering_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    reserved_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_reservation_status CHECK (status IN ('PENDING', 'CONFIRMED', 'EXPIRED', 'CANCELLED')),
    CONSTRAINT fk_reservation_offering FOREIGN KEY (offering_id) 
        REFERENCES course_offerings(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reservation_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- Composite index optimized for the core seat availability query:
-- SELECT COUNT(*) FROM seat_reservations WHERE offering_id = ? AND status = 'PENDING' AND expires_at > NOW()
CREATE INDEX idx_res_offering_status_expires ON seat_reservations(offering_id, status, expires_at);
CREATE INDEX idx_res_user_status ON seat_reservations(user_id, status);

-- ============================================================================
-- TABLE 6: enrollments
-- Purpose: Confirmed, finalized course registrations
-- ============================================================================
CREATE TABLE enrollments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    offering_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) NOT NULL DEFAULT 'ENROLLED',
    dropped_at TIMESTAMP NULL,
    CONSTRAINT chk_enrollment_status CHECK (status IN ('ENROLLED', 'DROPPED')),
    CONSTRAINT uq_offering_user UNIQUE (offering_id, user_id),
    CONSTRAINT fk_enrollment_offering FOREIGN KEY (offering_id) 
        REFERENCES course_offerings(id) ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_enrollment_user FOREIGN KEY (user_id) 
        REFERENCES users(id) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

CREATE INDEX idx_enrollments_offering ON enrollments(offering_id);
CREATE INDEX idx_enrollments_user ON enrollments(user_id);

-- ============================================================================
-- SEED DATA FOR DEVELOPMENT AND VIVA DEMONSTRATION
-- ============================================================================

-- 1. Departments
INSERT INTO departments (id, code, name) VALUES
(1, 'CS', 'Computer Science and Engineering'),
(2, 'IT', 'Information Technology'),
(3, 'ECE', 'Electronics and Communication Engineering'),
(4, 'ME', 'Mechanical Engineering');

-- 2. Users (Admin and Students)
-- All passwords are encrypted with BCrypt:
-- Admin password is: Admin@123
-- Student passwords are: Student@123
INSERT INTO users (id, username, email, password, full_name, student_id, role, enabled) VALUES
(1, 'admin', 'admin@enrollx.edu', '$2a$10$wE47P85/KqQhU5vK1O9.t.P50a2oPZ9z2OsqQ8bF0M30a6pA4VwG2', 'System Administrator', NULL, 'ROLE_ADMIN', TRUE),
(2, 'student1', 'alice.johnson@enrollx.edu', '$2a$10$0zX7ePky8q0m2iE4LdM4beRz9d5aH2I7K9O6vA1b.2uD5k8g3Z8ye', 'Alice Johnson', 'STU2026001', 'ROLE_STUDENT', TRUE),
(3, 'student2', 'bob.smith@enrollx.edu', '$2a$10$0zX7ePky8q0m2iE4LdM4beRz9d5aH2I7K9O6vA1b.2uD5k8g3Z8ye', 'Bob Smith', 'STU2026002', 'ROLE_STUDENT', TRUE),
(4, 'student3', 'carol.davis@enrollx.edu', '$2a$10$0zX7ePky8q0m2iE4LdM4beRz9d5aH2I7K9O6vA1b.2uD5k8g3Z8ye', 'Carol Davis', 'STU2026003', 'ROLE_STUDENT', TRUE),
(5, 'student4', 'david.wilson@enrollx.edu', '$2a$10$0zX7ePky8q0m2iE4LdM4beRz9d5aH2I7K9O6vA1b.2uD5k8g3Z8ye', 'David Wilson', 'STU2026004', 'ROLE_STUDENT', TRUE);

-- 3. Courses
INSERT INTO courses (id, code, title, description, credits, department_id, active) VALUES
(1, 'CS101', 'Introduction to Computer Science', 'Foundations of computing, algorithmic thinking, and basic data structures.', 4, 1, TRUE),
(2, 'CS301', 'Database Management Systems', 'Relational database design, SQL, normalization, concurrency control, and transactions.', 4, 1, TRUE),
(3, 'CS305', 'Operating Systems', 'Processes, CPU scheduling, synchronization, deadlocks, memory virtualization, and file systems.', 4, 1, TRUE),
(4, 'IT202', 'Web Application Development', 'Full-stack web application development using Spring Boot, REST APIs, and modern HTML5.', 3, 2, TRUE),
(5, 'CS401', 'Distributed Systems & Cloud', 'Consensus protocols, CAP theorem, distributed transactions, and cloud architectures.', 4, 1, TRUE),
(6, 'EC201', 'Digital Electronics & Logic Design', 'Boolean algebra, logic gates, combinational and sequential circuit design.', 3, 3, TRUE);

-- 4. Course Offerings for Semester: "Fall 2026"
-- NOTICE: CS301 has capacity = 3. This enables easy testing of seat locking & full capacity in Viva!
INSERT INTO course_offerings (id, course_id, semester, academic_year, capacity, instructor, room, schedule, active) VALUES
(1, 1, 'Fall 2026', '2026-2027', 40, 'Dr. Alan Turing', 'Hall 101', 'Mon/Wed 09:00 - 10:30', TRUE),
(2, 2, 'Fall 2026', '2026-2027', 3, 'Dr. Edgar Codd', 'Lab 3', 'Tue/Thu 11:00 - 12:30', TRUE),
(3, 3, 'Fall 2026', '2026-2027', 35, 'Dr. Dennis Ritchie', 'Room 204', 'Mon/Wed 14:00 - 15:30', TRUE),
(4, 4, 'Fall 2026', '2026-2027', 25, 'Prof. Tim Berners-Lee', 'Lab 1', 'Fri 10:00 - 13:00', TRUE),
(5, 5, 'Fall 2026', '2026-2027', 20, 'Dr. Leslie Lamport', 'Room 305', 'Tue/Thu 14:00 - 15:30', TRUE),
(6, 6, 'Fall 2026', '2026-2027', 30, 'Dr. Claude Shannon', 'Hall 102', 'Mon/Wed 11:00 - 12:30', TRUE);

-- 5. Seed initial confirmed enrollment for Student 1 in CS301 (so 1 of 3 seats is already confirmed)
INSERT INTO enrollments (id, offering_id, user_id, enrolled_at, status) VALUES
(1, 2, 2, CURRENT_TIMESTAMP, 'ENROLLED');

-- Reset Auto-Increment pointers for safety
ALTER TABLE departments AUTO_INCREMENT = 10;
ALTER TABLE users AUTO_INCREMENT = 10;
ALTER TABLE courses AUTO_INCREMENT = 10;
ALTER TABLE course_offerings AUTO_INCREMENT = 10;
ALTER TABLE seat_reservations AUTO_INCREMENT = 10;
ALTER TABLE enrollments AUTO_INCREMENT = 10;

-- ============================================================================
-- VERIFICATION QUERIES (To run in MySQL Workbench to confirm setup)
-- ============================================================================
-- View all offerings with seat availability:
-- SELECT o.id, c.code, c.title, o.semester, o.capacity,
--        (SELECT COUNT(*) FROM enrollments e WHERE e.offering_id = o.id AND e.status = 'ENROLLED') AS enrolled_seats,
--        (SELECT COUNT(*) FROM seat_reservations r WHERE r.offering_id = o.id AND r.status = 'PENDING' AND r.expires_at > NOW()) AS reserved_seats,
--        (o.capacity - 
--          (SELECT COUNT(*) FROM enrollments e WHERE e.offering_id = o.id AND e.status = 'ENROLLED') - 
--          (SELECT COUNT(*) FROM seat_reservations r WHERE r.offering_id = o.id AND r.status = 'PENDING' AND r.expires_at > NOW())
--        ) AS available_seats
-- FROM course_offerings o
-- JOIN courses c ON o.course_id = c.id;
