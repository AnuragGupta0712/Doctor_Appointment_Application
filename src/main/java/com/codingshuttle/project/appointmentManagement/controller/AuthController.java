package com.codingshuttle.project.appointmentManagement.controller;

import com.codingshuttle.project.appointmentManagement.security.AuthService;
import com.codingshuttle.project.appointmentManagement.dto.LoginRequestDto;
import com.codingshuttle.project.appointmentManagement.dto.LoginResponseDto;
import com.codingshuttle.project.appointmentManagement.dto.SignUpRequestDto;
import com.codingshuttle.project.appointmentManagement.dto.SignupResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        return ResponseEntity.ok(authService.login(loginRequestDto));
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponseDto> signup(@RequestBody SignUpRequestDto signupRequestDto) {
        return ResponseEntity.ok(authService.signup(signupRequestDto));
    }
}
