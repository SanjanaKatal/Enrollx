package com.enrollx.service;

import com.enrollx.dto.CourseDto;
import com.enrollx.entity.Course;
import com.enrollx.entity.Department;
import com.enrollx.exception.ResourceNotFoundException;
import com.enrollx.repository.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final DepartmentService departmentService;

    public CourseService(CourseRepository courseRepository, DepartmentService departmentService) {
        this.courseRepository = courseRepository;
        this.departmentService = departmentService;
    }

    @Transactional(readOnly = true)
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Course> getActiveCourses() {
        return courseRepository.findByActiveTrueOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public Course getCourseById(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + id));
    }

    @Transactional
    public Course createCourse(CourseDto dto) {
        if (courseRepository.existsByCode(dto.getCode().trim().toUpperCase())) {
            throw new IllegalArgumentException("Course with code '" + dto.getCode() + "' already exists.");
        }
        Department dept = departmentService.getDepartmentById(dto.getDepartmentId());

        Course course = new Course();
        course.setCode(dto.getCode().trim().toUpperCase());
        course.setTitle(dto.getTitle().trim());
        course.setDescription(dto.getDescription());
        course.setCredits(dto.getCredits());
        course.setDepartment(dept);
        course.setActive(dto.isActive());

        return courseRepository.save(course);
    }

    @Transactional
    public Course updateCourse(Long id, CourseDto dto) {
        Course course = getCourseById(id);
        if (!course.getCode().equalsIgnoreCase(dto.getCode().trim()) &&
                courseRepository.existsByCode(dto.getCode().trim().toUpperCase())) {
            throw new IllegalArgumentException("Course with code '" + dto.getCode() + "' already exists.");
        }
        Department dept = departmentService.getDepartmentById(dto.getDepartmentId());

        course.setCode(dto.getCode().trim().toUpperCase());
        course.setTitle(dto.getTitle().trim());
        course.setDescription(dto.getDescription());
        course.setCredits(dto.getCredits());
        course.setDepartment(dept);
        course.setActive(dto.isActive());

        return courseRepository.save(course);
    }

    @Transactional
    public void toggleCourseStatus(Long id) {
        Course course = getCourseById(id);
        course.setActive(!course.isActive());
        courseRepository.save(course);
    }

    @Transactional(readOnly = true)
    public long countTotalCourses() {
        return courseRepository.count();
    }
}
