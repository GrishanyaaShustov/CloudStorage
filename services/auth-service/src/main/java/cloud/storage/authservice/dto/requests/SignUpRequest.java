package cloud.storage.authservice.dto.requests;

import lombok.Data;

@Data
public class SignUpRequest {
    private String username;
    private String email;
    private String password;
    private String checkPassword;
}
