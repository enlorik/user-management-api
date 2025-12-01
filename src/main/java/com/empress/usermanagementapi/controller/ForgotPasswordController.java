package com.empress.usermanagementapi.controller;

import com.empress.usermanagementapi.entity.PasswordResetToken;
import com.empress.usermanagementapi.entity.User;
import com.empress.usermanagementapi.repository.UserRepository;
import com.empress.usermanagementapi.service.EmailService;
import com.empress.usermanagementapi.service.PasswordResetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Optional;

@Controller
public class ForgotPasswordController {

    private final UserRepository userRepo;
    private final EmailService emailService;
    private final PasswordResetService passwordResetService;

    @Autowired
    public ForgotPasswordController(UserRepository userRepo,
                                    EmailService emailService,
                                    PasswordResetService passwordResetService) {
        this.userRepo = userRepo;
        this.emailService = emailService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/forgot-password")
    public String showForm(Model model) {
        // clear banners + debug data on fresh GET
        model.addAttribute("debugReceived", null);
        model.addAttribute("debugResetLink", null);
        model.addAttribute("message", null);
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForm(@RequestParam String username,
                             @RequestParam String email,
                             Model model) {

        // debug line so we see it in Railway logs
        System.out.println(">>> POST /forgot-password for username=" + username + ", email=" + email);

        // what we got from the browser (for your debug alerts)
        String received = "username=" + username + " email=" + email;
        model.addAttribute("debugReceived", received);

        Optional<User> opt = userRepo.findByUsernameAndEmail(username, email);

        // generic banner (no user enumeration)
        model.addAttribute(
            "message",
            "If an account matches those details, you’ll receive an email shortly."
        );

        if (opt.isPresent()) {
            // 1) create + store token in DB (24h expiry)
            PasswordResetToken tokenEntity =
                passwordResetService.createPasswordResetTokenForEmail(email);

            String token = tokenEntity.getToken();

            // 2) link for your deployed Railway app
            String baseUrl = "https://user-management-api-java.up.railway.app";
            String resetLink = baseUrl + "/reset-password?token=" + token;

            // 3) put link on the page as debug (so you can click it)
            model.addAttribute("debugResetLink", resetLink);

            // 4) real email send is disabled for now (no SMTP on Railway)
            //    later, when mail is configured, just uncomment this line.
            // emailService.sendPasswordResetEmail(email, resetLink);
        } else {
            // no user -> no link to show
            model.addAttribute("debugResetLink", null);
        }

        return "forgot-password";
    }
}
