package cloud.storage.authservice.controllers;

import cloud.storage.authservice.dto.requests.SignUpRequest;
import cloud.storage.authservice.dto.requests.SingInRequest;
import cloud.storage.authservice.dto.responses.SignInResponse;
import cloud.storage.authservice.dto.responses.SignUpResponse;
import cloud.storage.authservice.services.AuthService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signin")
    public ResponseEntity<SignInResponse> signIn(@RequestBody SingInRequest request){
        try{
            return ResponseEntity.ok(authService.signIn(request));
        } catch (Exception e){
            return ResponseEntity.status(Integer.parseInt(e.getMessage().split("\\.")[0])).body(new SignInResponse(null));
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<SignUpResponse> signUp(@RequestBody SignUpRequest request){
        try {
            return ResponseEntity.ok(authService.signUp(request));
        } catch (Exception e){
            return ResponseEntity.status(Integer.parseInt(e.getMessage().split("\\.")[0])).body(new SignUpResponse(e.getMessage().split("\\.")[1]));
        }
    }
}
