package cloud.storage.authservice.dto.requests;

import lombok.Data;

@Data
public class SingInRequest {
    private String email;
    private String password;
}
