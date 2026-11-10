package com.enrollx.controller;

import com.enrollx.dto.OfferingCapacityDto;
import com.enrollx.dto.ReservationResponseDto;
import com.enrollx.entity.CourseOffering;
import com.enrollx.entity.Enrollment;
import com.enrollx.entity.SeatReservation;
import com.enrollx.entity.User;
import com.enrollx.service.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentController {

    private final CourseOfferingService courseOfferingService;
    private final SeatReservationService seatReservationService;
    private final EnrollmentService enrollmentService;
    private final UserService userService;
    private final DepartmentService departmentService;

    public StudentController(CourseOfferingService courseOfferingService,
                             SeatReservationService seatReservationService,
                             EnrollmentService enrollmentService,
                             UserService userService,
                             DepartmentService departmentService) {
        this.courseOfferingService = courseOfferingService;
        this.seatReservationService = seatReservationService;
        this.enrollmentService = enrollmentService;
        this.userService = userService;
        this.departmentService = departmentService;
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        User student = userService.findByUsername(username);

        List<Enrollment> enrolledCourses = enrollmentService.getStudentActiveEnrollments(username);
        List<SeatReservation> activeReservations = seatReservationService.getActiveReservationsForUser(username);

        int totalCredits = enrolledCourses.stream()
                .mapToInt(e -> e.getCourseOffering().getCourse().getCredits())
                .sum();

        model.addAttribute("student", student);
        model.addAttribute("enrolledCourses", enrolledCourses);
        model.addAttribute("enrolledCount", enrolledCourses.size());
        model.addAttribute("activeReservations", activeReservations);
        model.addAttribute("activeReservationCount", activeReservations.size());
        model.addAttribute("totalCredits", totalCredits);

        return "student/dashboard";
    }

    @GetMapping("/courses")
    public String courseCatalog(@AuthenticationPrincipal UserDetails userDetails,
                                @RequestParam(value = "semester", required = false) String semester,
                                @RequestParam(value = "departmentId", required = false) Long departmentId,
                                @RequestParam(value = "search", required = false) String search,
                                Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        List<OfferingCapacityDto> offerings = courseOfferingService.getAvailableOfferings(semester, departmentId, search, currentUser);

        model.addAttribute("offerings", offerings);
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("semesters", courseOfferingService.getDistinctSemesters());
        model.addAttribute("selectedSemester", semester);
        model.addAttribute("selectedDepartmentId", departmentId);
        model.addAttribute("searchQuery", search);

        return "student/courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetails(@PathVariable("id") Long offeringId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        OfferingCapacityDto offeringDto = courseOfferingService.getOfferingCapacityDto(offeringId, currentUser);
        model.addAttribute("offering", offeringDto);
        return "student/course-detail";
    }

    /**
     * POST /student/reserve/{offeringId}
     * Real-time seat reservation with database row locking.
     */
    @PostMapping("/reserve/{offeringId}")
    public String reserveSeat(@PathVariable("offeringId") Long offeringId,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        String username = userDetails.getUsername();
        SeatReservation reservation = seatReservationService.reserveSeat(offeringId, username);
        redirectAttributes.addFlashAttribute("successMessage", 
                "Seat reserved! You have 5 minutes to confirm your registration.");
        return "redirect:/student/reservation/" + reservation.getId();
    }

    /**
     * GET /student/reservation/{reservationId}
     * Live reservation countdown screen.
     */
    @GetMapping("/reservation/{reservationId}")
    public String viewReservation(@PathVariable("reservationId") Long reservationId,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        String username = userDetails.getUsername();
        ReservationResponseDto reservation = seatReservationService.getReservationDetails(reservationId, username);

        if (reservation.isExpired()) {
            model.addAttribute("errorMessage", "This reservation has expired. The seat has been released.");
        }

        model.addAttribute("reservation", reservation);
        return "student/reservation";
    }

    /**
     * POST /student/confirm/{reservationId}
     * Atomic confirmation: reservation -> confirmed enrollment.
     */
    @PostMapping("/confirm/{reservationId}")
    public String confirmReservation(@PathVariable("reservationId") Long reservationId,
                                     @AuthenticationPrincipal UserDetails userDetails,
                                     RedirectAttributes redirectAttributes) {
        String username = userDetails.getUsername();
        Enrollment enrollment = enrollmentService.confirmReservation(reservationId, username);

        redirectAttributes.addFlashAttribute("successMessage", 
                "Congratulations! You are officially enrolled in " + 
                enrollment.getCourseOffering().getCourse().getCode() + " — " + 
                enrollment.getCourseOffering().getCourse().getTitle() + ".");

        return "redirect:/student/my-courses";
    }

    /**
     * POST /student/cancel-reservation/{reservationId}
     * Student cancels their temporary hold; seat is instantly returned to capacity.
     */
    @PostMapping("/cancel-reservation/{reservationId}")
    public String cancelReservation(@PathVariable("reservationId") Long reservationId,
                                    @AuthenticationPrincipal UserDetails userDetails,
                                    RedirectAttributes redirectAttributes) {
        String username = userDetails.getUsername();
        seatReservationService.cancelReservation(reservationId, username);
        redirectAttributes.addFlashAttribute("infoMessage", "Reservation cancelled. The seat has been released.");
        return "redirect:/student/courses";
    }

    @GetMapping("/my-courses")
    public String myCourses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        List<Enrollment> enrolledCourses = enrollmentService.getStudentActiveEnrollments(username);

        int totalCredits = enrolledCourses.stream()
                .mapToInt(e -> e.getCourseOffering().getCourse().getCredits())
                .sum();

        model.addAttribute("enrolledCourses", enrolledCourses);
        model.addAttribute("totalCredits", totalCredits);
        return "student/my-courses";
    }

    @PostMapping("/drop/{enrollmentId}")
    public String dropCourse(@PathVariable("enrollmentId") Long enrollmentId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        String username = userDetails.getUsername();
        enrollmentService.dropCourse(enrollmentId, username, false);
        redirectAttributes.addFlashAttribute("successMessage", "Course dropped successfully. The seat has been made available to other students.");
        return "redirect:/student/my-courses";
    }

    @GetMapping("/history")
    public String registrationHistory(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        List<Enrollment> enrollments = enrollmentService.getStudentEnrollmentHistory(username);
        List<SeatReservation> reservations = seatReservationService.getUserReservationHistory(username);

        model.addAttribute("enrollments", enrollments);
        model.addAttribute("reservations", reservations);
        return "student/history";
    }
}
