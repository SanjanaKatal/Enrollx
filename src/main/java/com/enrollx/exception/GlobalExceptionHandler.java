package com.enrollx.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CourseFullException.class)
    public String handleCourseFull(CourseFullException ex, RedirectAttributes redirectAttributes) {
        log.warn("Course full exception: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/student/courses";
    }

    @ExceptionHandler(DuplicateEnrollmentException.class)
    public String handleDuplicateEnrollment(DuplicateEnrollmentException ex, RedirectAttributes redirectAttributes) {
        log.warn("Duplicate enrollment attempt: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/student/courses";
    }

    @ExceptionHandler(ReservationExpiredException.class)
    public String handleReservationExpired(ReservationExpiredException ex, RedirectAttributes redirectAttributes) {
        log.warn("Reservation expired: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/student/courses";
    }

    @ExceptionHandler(InvalidCapacityException.class)
    public String handleInvalidCapacity(InvalidCapacityException ex, RedirectAttributes redirectAttributes) {
        log.warn("Invalid capacity change: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        return "redirect:/admin/offerings";
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public String handleResourceNotFound(ResourceNotFoundException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute("errorTitle", "Item Not Found");
        model.addAttribute("errorMessage", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, Model model) {
        log.error("Unhandled application exception", ex);
        model.addAttribute("errorTitle", "An Unexpected Error Occurred");
        model.addAttribute("errorMessage", ex.getMessage() != null ? ex.getMessage() : "Please contact the administrator.");
        return "error";
    }
}
