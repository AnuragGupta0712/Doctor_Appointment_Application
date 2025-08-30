package com.codingshuttle.project.appointmentManagement.security;

import com.codingshuttle.project.appointmentManagement.dto.LoginRequestDto;
import com.codingshuttle.project.appointmentManagement.dto.LoginResponseDto;
import com.codingshuttle.project.appointmentManagement.dto.SignUpRequestDto;
import com.codingshuttle.project.appointmentManagement.dto.SignupResponseDto;
import com.codingshuttle.project.appointmentManagement.entity.Patient;
import com.codingshuttle.project.appointmentManagement.entity.User;
import com.codingshuttle.project.appointmentManagement.entity.type.AuthProviderType;
import com.codingshuttle.project.appointmentManagement.entity.type.RoleType;
import com.codingshuttle.project.appointmentManagement.repository.PatientRepository;
import com.codingshuttle.project.appointmentManagement.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUtil authUtil;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PatientRepository patientRepository;

    public LoginResponseDto login(LoginRequestDto loginRequestDto) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDto.getUsername(), loginRequestDto.getPassword())
        );

        User user = (User) authentication.getPrincipal();

        String token = authUtil.generateAccessToken(user);

        return new LoginResponseDto(token, user.getId());
    }

    public User signUpInternal(SignUpRequestDto signupRequestDto, AuthProviderType authProviderType, String providerId) {
        User user = userRepository.findByUsername(signupRequestDto.getUsername()).orElse(null);

        if(user != null) throw new IllegalArgumentException("User already exists");

        user = User.builder()
                .username(signupRequestDto.getUsername())
                .providerId(providerId)
                .providerType(authProviderType)
                .roles(signupRequestDto.getRoles()) // Role.PATIENT
                .build();

        if(authProviderType == AuthProviderType.EMAIL) {
            user.setPassword(passwordEncoder.encode(signupRequestDto.getPassword()));
        }

        user = userRepository.save(user);

        Patient patient = Patient.builder()
                .name(signupRequestDto.getName())
                .email(signupRequestDto.getUsername())
                .user(user)
                .build();
        patientRepository.save(patient);

       return user;
    }

    // login controller
    public SignupResponseDto signup(SignUpRequestDto signupRequestDto) {
        User user = signUpInternal(signupRequestDto, AuthProviderType.EMAIL, null);
        return new SignupResponseDto(user.getId(), user.getUsername());
    }

    @Transactional
    public ResponseEntity<LoginResponseDto> handleOAuth2LoginRequest(OAuth2User oAuth2User, String registrationId) {
        AuthProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);
        String providerId = authUtil.determineProviderIdFromOAuth2User(oAuth2User, registrationId);

        User user = userRepository.findByProviderIdAndProviderType(providerId, providerType).orElse(null);
        String email = oAuth2User.getAttribute("email"); // This field , we will get only at those places who provide us email, for example if we are doing with twitter this field will be null
        String name = oAuth2User.getAttribute("name");

        // Here we are considering email received from oAuth2User as our username
        User emailUser = userRepository.findByUsername(email).orElse(null); // If we have a valid email, then only we will be able to get an emailUser

        if(user == null && emailUser == null) {

            //If no user is found by either providerId or email, it means this is a brand new user.

            // signup flow:
            String username = authUtil.determineUsernameFromOAuth2User(oAuth2User, registrationId, providerId);
            user = signUpInternal(new SignUpRequestDto(username, null, name, Set.of(RoleType.PATIENT)), providerType, providerId);
        } else if(user != null) {
            if(email != null && !email.isBlank() && !email.equals(user.getUsername())) {
                user.setUsername(email);
                userRepository.save(user);
            }
        } else {
            throw new BadCredentialsException("This email is already registered with provider "+emailUser.getProviderType());
        }

        LoginResponseDto loginResponseDto = new LoginResponseDto(authUtil.generateAccessToken(user), user.getId());
        return ResponseEntity.ok(loginResponseDto);
    }
}

// Use this to understand the three possibilites of the above if else conditions
//The `handleOAuth2LoginRequest` method, part of an `AuthService`, is designed to manage the process of users logging in or signing up via an
// OAuth2 provider (like Google, GitHub, etc.). It's typically invoked by an `OAuth2SuccessHandler` after a successful authentication with the external provider.
// The method is annotated with `@Transactional`, ensuring that the entire operation runs within a single database transaction.
//
//Here's a step-by-step explanation of its flow:
//
//1.  **Determine Provider Type and ID**:
//
//      * `AuthProviderType providerType = authUtil.getProviderTypeFromRegistrationId(registrationId);`: It first uses `authUtil` to convert the OAuth2 provider's `registrationId` (e.g., "google", "github") into an internal `AuthProviderType` enum.
//      * `String providerId = authUtil.determineProviderIdFromOAuth2User(oAuth2User, registrationId);`: It then extracts a unique identifier for the user from the `OAuth2User` object (which contains data from the OAuth2 provider).
//      This `providerId` is specific to the user within that particular OAuth2 provider (e.g., Google's 'sub' claim, GitHub's 'id').
//
//2.  **Fetch User from Repository (by Provider ID and by Email)**:
//
//      * `User user = userRepository.findByProviderIdAndProviderType(providerId, providerType).orElse(null);`: It attempts to find an existing `User` in your application's database based on the `providerId` and `providerType`. This checks if the user has previously logged in with this specific OAuth2 account.
//      * `String email = oAuth2User.getAttribute("email");`: It extracts the user's email address from the OAuth2 user's attributes.
//      * `User emailUser = userRepository.findByUsername(email).orElse(null);`: It also attempts to find an existing `User` in your database based on their email address. This is crucial for handling cases where a user might try to link an existing email to a new OAuth2 login, or if they previously signed up with email.
//
//3.  **Authentication/Registration Logic (Conditional Flow)**:
//
//      * **Scenario A: New User (Signup Flow)**:
//
//        ```java
//        if(user == null && emailUser == null) {
//            // signup flow:
//            String username = authUtil.determineUsernameFromOAuth2User(oAuth2User, registrationId, providerId);
//            user = signUpInternal(new SignUpRequestDto(username, null, name, Set.of(RoleType.PATIENT)), providerType, providerId);
//        }
//        ```
//
//        If no user is found by either `providerId` or `email`, it means this is a brand new user.
//
//          * It determines a `username` for the new user (prioritizing email, then provider-specific identifiers like 'sub' or 'login').
//          * It calls `signUpInternal` (an internal method likely handling user creation in the database) to register this new user, setting their `AuthProviderType` and `providerId`. A default role (e.g., `RoleType.PATIENT`) is assigned.
//
//      * **Scenario B: Existing User, Potentially Updating Username/Email**:
//
//        ```java
//        else if(user != null) {
//            if(email != null && !email.isBlank() && !email.equals(user.getUsername())) {
//                user.setUsername(email);
//                userRepository.save(user);
//            }
//        }
//        ```
//
//        If a user *is* found by `providerId` (meaning they've logged in with this OAuth2 account before):
//
//          * It checks if an email is available from the OAuth2 provider and if that email is different from the `username` currently stored for that user in your database.
//          * If so, it updates the user's `username` (which likely serves as their email) in your database with the latest email from the OAuth2 provider. This handles cases where a user might change their email on the OAuth2 provider's side.
//
//      * **Scenario C: Email Already Registered with a Different Provider/Method (Conflict)**:
//
//        ```java
//        else { // This means user == null && emailUser != null
//            throw new BadCredentialsException("This email is already registered with provider "+emailUser.getProviderType());
//        }
//        ```
//
//        This `else` block is executed if `user` is `null` (no user found by `providerId`), but `emailUser` is **not** `null` (meaning an account with this email already exists, but it's not linked to the current `providerId`).
//
//          * This indicates a conflict: the user is trying to log in with an OAuth2 provider, but their email is already associated with an account in your system that was either created with a different OAuth2 provider or via traditional email/password signup.
//          * It throws a `BadCredentialsException` to prevent account hijacking or unintended merging, informing the user about the conflict.
//
//4.  **Generate and Return JWT**:
//
//      * `LoginResponseDto loginResponseDto = new LoginResponseDto(authUtil.generateAccessToken(user), user.getId());`: After the user is either found, updated, or newly signed up, `authUtil.generateAccessToken(user)` is called to create a JWT for them. The response includes this JWT and the user's ID.
//      * `return ResponseEntity.ok(loginResponseDto);`: The generated `LoginResponseDto` is wrapped in an `ResponseEntity` with an HTTP 200 OK status and returned. This response will be sent back to the client application (via the `OAuth2SuccessHandler`).

















