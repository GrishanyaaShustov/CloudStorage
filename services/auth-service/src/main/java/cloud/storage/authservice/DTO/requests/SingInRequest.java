package cloud.storage.authservice.DTO.requests;

import lombok.Data;

@Data
public class SingInRequest {
    private String email;
    private String password;
}
