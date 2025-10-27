package cloud.storage.authservice.services;

import cloud.storage.authservice.dto.requests.SignUpRequest;
import cloud.storage.authservice.dto.requests.SingInRequest;
import cloud.storage.authservice.dto.responses.SignInResponse;
import cloud.storage.authservice.dto.responses.SignUpResponse;

public interface AuthService {
    SignInResponse signIn(SingInRequest request);
    SignUpResponse signUp(SignUpRequest request);
}
