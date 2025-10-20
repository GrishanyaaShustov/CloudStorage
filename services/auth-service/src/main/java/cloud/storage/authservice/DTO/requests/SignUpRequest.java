package cloud.storage.authservice.DTO.requests;

import lombok.Data;

@Data
public class SignUpRequest {
    private String username;
    private String email;
    private String password;
    private String checkPassword;
}
