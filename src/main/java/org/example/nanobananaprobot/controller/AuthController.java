package org.example.nanobananaprobot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nanobananaprobot.domain.dto.SignInRequest;
import org.example.nanobananaprobot.domain.dto.SignUpRequest;
import org.example.nanobananaprobot.domain.model.User;
import org.example.nanobananaprobot.service.AuthenticationService;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;


@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationService authenticationService;

    @PostMapping("/sign-up")
    public Optional<User> signUp(@RequestBody @Valid SignUpRequest request) {
        return authenticationService.signUp(request);
    }

    @PostMapping("/sign-in")
    public Optional<User> signIn(@RequestBody @Valid SignInRequest request) {
        return authenticationService.signIn(request);
    }

}
