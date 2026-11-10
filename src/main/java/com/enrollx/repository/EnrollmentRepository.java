package com.enrollx.repository;

import com.enrollx.entity.Enrollment;
import com.enrollx.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    long countByCourseOfferingIdAndStatus(Long offeringId, EnrollmentStatus status);

    boolean existsByCourseOfferingIdAndUserIdAndStatus(Long offeringId, Long userId, EnrollmentStatus status);

    Optional<Enrollment> findByCourseOfferingIdAndUserIdAndStatus(Long offeringId, Long userId, EnrollmentStatus status);

    List<Enrollment> findByUserIdAndStatusOrderByEnrolledAtDesc(Long userId, EnrollmentStatus status);

    List<Enrollment> findByUserIdOrderByEnrolledAtDesc(Long userId);

    List<Enrollment> findByCourseOfferingIdOrderByEnrolledAtDesc(Long offeringId);

    List<Enrollment> findAllByOrderByEnrolledAtDesc();

    long countByStatus(EnrollmentStatus status);

    @Query("SELECT e.courseOffering.id, e.courseOffering.course.code, e.courseOffering.course.title, e.courseOffering.semester, COUNT(e) " +
           "FROM Enrollment e " +
           "WHERE e.status = com.enrollx.enums.EnrollmentStatus.ENROLLED " +
           "GROUP BY e.courseOffering.id, e.courseOffering.course.code, e.courseOffering.course.title, e.courseOffering.semester " +
           "ORDER BY COUNT(e) DESC")
    List<Object[]> findEnrollmentStatistics();

    @Query("SELECT DISTINCT e.user.id FROM Enrollment e WHERE e.status = com.enrollx.enums.EnrollmentStatus.ENROLLED")
    List<Long> findDistinctEnrolledUserIds();
}
