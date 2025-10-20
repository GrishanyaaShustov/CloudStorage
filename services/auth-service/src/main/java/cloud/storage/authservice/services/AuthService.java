package cloud.storage.authservice.services;

import cloud.storage.authservice.DTO.requests.SignUpRequest;
import cloud.storage.authservice.DTO.requests.SingInRequest;
import cloud.storage.authservice.DTO.responses.SignInResponse;
import cloud.storage.authservice.DTO.responses.SignUpResponse;

public interface AuthService {
    SignInResponse signIn(SingInRequest request);
    SignUpResponse signUp(SignUpRequest request);
}
