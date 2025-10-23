package cloud.storage.storageservice.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtValidator{
    private final SecretKey secretKey;
    private static final Logger log = LoggerFactory.getLogger(JwtValidator.class);

    public JwtValidator(JwtProperties jwtProperties){
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getKey().getBytes(StandardCharsets.UTF_8));
    }

    public Claims validateToken(String token) {
        String cleanToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(cleanToken)
                    .getBody();
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            throw new SecurityException("Invalid JWT token", e);
        }
    }
}
