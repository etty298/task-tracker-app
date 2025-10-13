package ru.home.authentication.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import ru.home.authentication.api.dto.SignInDto;
import ru.home.authentication.api.dto.SignUpDto;
import ru.home.authentication.api.jwt.JwtToken;
import ru.home.authentication.kafka.producer.UserEventProducer;
import ru.home.authentication.store.entities.UserEntity;
import ru.home.authentication.store.repositories.UserRepository;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Transactional
public class SecurityController {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JwtToken jwtToken;

    private final UserEventProducer userEventProducer;


    @PostMapping("/signup")
    public ResponseEntity<?> signUp(@RequestBody SignUpDto signUpDto) {
        if (userRepository.existsByUsername(signUpDto.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Choose different username.");
        }
        if (userRepository.existsByEmail(signUpDto.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Choose different email.");
        }

        UserEntity user = new UserEntity();
        user.setUsername(signUpDto.getUsername());
        user.setEmail(signUpDto.getEmail());
        user.setPassword(passwordEncoder.encode(signUpDto.getPassword()));

        userRepository.save(user);

        userEventProducer.sendUserRegisteredEvent(signUpDto.getEmail(), signUpDto.getUsername());

        return ResponseEntity.status(HttpStatus.OK).body(user);
    }

    @PostMapping("/signin")
    public ResponseEntity<?> signIn(@RequestBody SignInDto signInDto) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(signInDto.getUsername(), signInDto.getPassword())
            );
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtToken.generateToken(authentication);
        return ResponseEntity.ok(token);
    }
}
