package com.enrollx.controller;

import com.enrollx.dto.StudentRegistrationDto;
import com.enrollx.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
            if (roles.contains("ROLE_ADMIN")) {
                return "redirect:/admin/dashboard";
            } else if (roles.contains("ROLE_STUDENT")) {
                return "redirect:/student/dashboard";
            }
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model, Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/";
        }
        if (!model.containsAttribute("registrationDto")) {
            model.addAttribute("registrationDto", new StudentRegistrationDto());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegistration(@Valid @ModelAttribute("registrationDto") StudentRegistrationDto dto,
                                      BindingResult bindingResult,
                                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.registerStudent(dto);
            redirectAttributes.addFlashAttribute("successMessage", "Registration successful! Please log in with your credentials.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("username", "error.user", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/default")
    public String defaultAfterLogin(Authentication authentication) {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        if (roles.contains("ROLE_ADMIN")) {
            return "redirect:/admin/dashboard";
        }
        return "redirect:/student/dashboard";
    }
}
