package cloud.storage.authservice.services;

import cloud.storage.authservice.customExceptions.*;
import cloud.storage.authservice.dto.requests.SignUpRequest;
import cloud.storage.authservice.dto.requests.SingInRequest;
import cloud.storage.authservice.dto.responses.SignInResponse;
import cloud.storage.authservice.dto.responses.SignUpResponse;
import cloud.storage.authservice.configuration.JwtProvider;
import cloud.storage.authservice.repository.UserRepository;
import cloud.storage.authservice.models.User;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AuthServiceImplementation implements AuthService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    private final JwtProvider jwtProvider;

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImplementation.class);

    @Transactional
    @Override
    public SignUpResponse signUp(SignUpRequest request) {

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException("User with this username already exists");
        }
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }
        if(!request.getPassword().equals(request.getCheckPassword())) {
            throw new PasswordMismatchException("Passwords must be the same");
        }

        String hashedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .passwordHash(hashedPassword)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: username={}, email={}", request.getUsername(), request.getEmail());
        return new SignUpResponse("User created successfully");
    }

    @Override
    public SignInResponse signIn(SingInRequest request) {
        Authentication auth;
        try{
            auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
            SecurityContextHolder.getContext().setAuthentication(auth);
            User user = userRepository.findUserByEmail(request.getEmail())
                    .orElseThrow(() -> new UserNotFoundException("User not found with email: " + request.getEmail()));
            String jwt = jwtProvider.generateToken(auth, user.getId());
            log.info("User authenticated successfully: email={}", request.getEmail());
            return new SignInResponse(jwt);
        } catch (BadCredentialsException e) {
            throw new InvalidCredentialsException("Invalid email or password");
        } catch (UserNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new TokenGenerationException("Failed to generate JWT token", e);
        }
    }
}
