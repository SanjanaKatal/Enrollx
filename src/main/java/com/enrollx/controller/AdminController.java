package com.enrollx.controller;

import com.enrollx.dto.CourseDto;
import com.enrollx.dto.CourseOfferingDto;
import com.enrollx.dto.OfferingCapacityDto;
import com.enrollx.entity.Course;
import com.enrollx.entity.CourseOffering;
import com.enrollx.entity.Enrollment;
import com.enrollx.entity.SeatReservation;
import com.enrollx.entity.User;
import com.enrollx.service.*;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final CourseService courseService;
    private final CourseOfferingService courseOfferingService;
    private final DepartmentService departmentService;
    private final EnrollmentService enrollmentService;
    private final SeatReservationService seatReservationService;
    private final UserService userService;

    public AdminController(CourseService courseService,
                           CourseOfferingService courseOfferingService,
                           DepartmentService departmentService,
                           EnrollmentService enrollmentService,
                           SeatReservationService seatReservationService,
                           UserService userService) {
        this.courseService = courseService;
        this.courseOfferingService = courseOfferingService;
        this.departmentService = departmentService;
        this.enrollmentService = enrollmentService;
        this.seatReservationService = seatReservationService;
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        long totalStudents = userService.countTotalStudents();
        long totalCourses = courseService.countTotalCourses();
        long activeOfferings = courseOfferingService.countActiveOfferings();
        long confirmedEnrollments = enrollmentService.countConfirmedEnrollments();
        long activeReservations = seatReservationService.countActiveReservations();

        List<Object[]> popularCourses = enrollmentService.getEnrollmentStatistics();
        List<SeatReservation> recentReservations = seatReservationService.getAllActiveReservations();

        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalCourses", totalCourses);
        model.addAttribute("activeOfferings", activeOfferings);
        model.addAttribute("confirmedEnrollments", confirmedEnrollments);
        model.addAttribute("activeReservations", activeReservations);
        model.addAttribute("popularCourses", popularCourses);
        model.addAttribute("recentReservations", recentReservations);

        return "admin/dashboard";
    }

    @GetMapping("/courses")
    public String listCourses(Model model) {
        model.addAttribute("courses", courseService.getAllCourses());
        model.addAttribute("departments", departmentService.getAllDepartments());
        if (!model.containsAttribute("courseDto")) {
            model.addAttribute("courseDto", new CourseDto());
        }
        return "admin/courses";
    }

    @PostMapping("/courses")
    public String createCourse(@Valid @ModelAttribute("courseDto") CourseDto dto,
                               BindingResult bindingResult,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("courses", courseService.getAllCourses());
            model.addAttribute("departments", departmentService.getAllDepartments());
            return "admin/courses";
        }

        try {
            courseService.createCourse(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Course " + dto.getCode() + " created successfully!");
            return "redirect:/admin/courses";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/courses";
        }
    }

    @PostMapping("/courses/{id}/toggle")
    public String toggleCourse(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        courseService.toggleCourseStatus(id);
        redirectAttributes.addFlashAttribute("infoMessage", "Course status updated.");
        return "redirect:/admin/courses";
    }

    @GetMapping("/offerings")
    public String listOfferings(Model model) {
        List<OfferingCapacityDto> offerings = courseOfferingService.getAvailableOfferings(null, null, null, null);
        model.addAttribute("offerings", offerings);
        model.addAttribute("courses", courseService.getActiveCourses());
        if (!model.containsAttribute("offeringDto")) {
            model.addAttribute("offeringDto", new CourseOfferingDto());
        }
        return "admin/offerings";
    }

    @PostMapping("/offerings")
    public String createOffering(@Valid @ModelAttribute("offeringDto") CourseOfferingDto dto,
                                 BindingResult bindingResult,
                                 RedirectAttributes redirectAttributes,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("offerings", courseOfferingService.getAvailableOfferings(null, null, null, null));
            model.addAttribute("courses", courseService.getActiveCourses());
            return "admin/offerings";
        }

        try {
            courseOfferingService.createOffering(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Course Offering created successfully!");
            return "redirect:/admin/offerings";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/admin/offerings";
        }
    }

    @PostMapping("/offerings/{id}/capacity")
    public String updateCapacity(@PathVariable("id") Long id,
                                 @RequestParam("capacity") int capacity,
                                 RedirectAttributes redirectAttributes) {
        try {
            courseOfferingService.updateCapacity(id, capacity);
            redirectAttributes.addFlashAttribute("successMessage", "Capacity updated successfully to " + capacity + " seats.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/offerings";
    }

    @GetMapping("/enrollments")
    public String listEnrollments(Model model) {
        List<Enrollment> enrollments = enrollmentService.getAllEnrollments();
        model.addAttribute("enrollments", enrollments);
        return "admin/enrollments";
    }

    @PostMapping("/enrollments/{id}/drop")
    public String adminDropEnrollment(@PathVariable("id") Long id, RedirectAttributes redirectAttributes) {
        enrollmentService.dropCourse(id, null, true);
        redirectAttributes.addFlashAttribute("infoMessage", "Enrollment record dropped by admin.");
        return "redirect:/admin/enrollments";
    }

    @GetMapping("/students")
    public String listStudents(Model model) {
        List<User> students = userService.getAllStudents();
        model.addAttribute("students", students);
        return "admin/students";
    }
}
