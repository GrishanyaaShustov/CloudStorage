package cloud.storage.fileservice.configuration.jwt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CustomUserPrincipal {
    private final Long id;
    private final String email;
}
