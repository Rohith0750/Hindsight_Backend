package com.hindsight.backend.service;

import com.hindsight.backend.dto.AuthDto;
import com.hindsight.backend.exception.ApiException;
import com.hindsight.backend.model.User;
import com.hindsight.backend.repository.UserRepository;
import com.hindsight.backend.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    public AuthDto.AuthResponse signup(AuthDto.SignupRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ApiException("User already exists", HttpStatus.BAD_REQUEST);
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .email(email)
                .password(hashedPassword)
                .name(request.getName())
                .role(request.getRole() != null ? request.getRole() : "user")
                .xp(0)
                .level("Beginner")
                .streak(0)
                .solvedCount(0)
                .totalAttempts(0)
                .solvedProblems(new ArrayList<>())
                .languages(new ArrayList<>())
                .build();

        User savedUser = userRepository.save(user);

        String token = tokenProvider.generateToken(savedUser.getId(), savedUser.getRole());

        return AuthDto.AuthResponse.builder()
                .message("User created successfully")
                .user(mapToUserDto(savedUser))
                .token(token)
                .build();
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        String token = tokenProvider.generateToken(user.getId(), user.getRole());

        return AuthDto.AuthResponse.builder()
                .message("Login successful")
                .user(mapToUserDto(user))
                .token(token)
                .build();
    }

    public AuthDto.UserDto mapToUserDto(User user) {
        return AuthDto.UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .xp(user.getXp())
                .level(user.getLevel())
                .streak(user.getStreak())
                .solvedCount(user.getSolvedCount())
                .totalAttempts(user.getTotalAttempts())
                .build();
    }
}
