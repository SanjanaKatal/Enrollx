package com.enrollx.repository;

import com.enrollx.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCode(String code);
    boolean existsByCode(String code);
    List<Course> findByActiveTrueOrderByCodeAsc();
    List<Course> findByDepartmentId(Long departmentId);
    List<Course> findByDepartmentIdAndActiveTrue(Long departmentId);
}
