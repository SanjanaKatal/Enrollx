package com.enrollx.repository;

import com.enrollx.entity.CourseOffering;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseOfferingRepository extends JpaRepository<CourseOffering, Long> {

    /**
     * Pessimistic row-level write lock (FOR UPDATE in MySQL InnoDB).
     * Serializes concurrent attempts to reserve seats for the same course offering.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM CourseOffering o WHERE o.id = :id")
    Optional<CourseOffering> findWithLockById(@Param("id") Long id);

    List<CourseOffering> findByActiveTrue();

    List<CourseOffering> findBySemesterAndAcademicYearAndActiveTrue(String semester, String academicYear);

    @Query("SELECT DISTINCT o.semester FROM CourseOffering o ORDER BY o.semester ASC")
    List<String> findDistinctSemesters();

    @Query("SELECT o FROM CourseOffering o " +
           "JOIN FETCH o.course c " +
           "JOIN FETCH c.department d " +
           "WHERE o.active = true " +
           "AND (:semester IS NULL OR o.semester = :semester) " +
           "AND (:departmentId IS NULL OR d.id = :departmentId) " +
           "AND (:searchQuery IS NULL OR LOWER(c.code) LIKE LOWER(CONCAT('%', :searchQuery, '%')) OR LOWER(c.title) LIKE LOWER(CONCAT('%', :searchQuery, '%'))) " +
           "ORDER BY c.code ASC")
    List<CourseOffering> searchOfferings(
            @Param("semester") String semester,
            @Param("departmentId") Long departmentId,
            @Param("searchQuery") String searchQuery);

    boolean existsByCourseIdAndSemesterAndAcademicYear(Long courseId, String semester, String academicYear);
}
