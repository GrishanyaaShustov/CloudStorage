package cloud.storage.fileservice.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtValidator extends OncePerRequestFilter {

    private final SecretKey secretKey;
    private final String jwtHeader;

    private static final Logger log = LoggerFactory.getLogger(JwtValidator.class);

    public JwtValidator(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getKey().getBytes(StandardCharsets.UTF_8));
        this.jwtHeader = jwtProperties.getHeader();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(jwtHeader);
        if (authHeader != null && authHeader.startsWith("Bearer")) {
            String jwt = authHeader.substring(7);

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(jwt)
                        .getBody();

                String email = claims.get("email", String.class);
                Long userId = claims.get("userId", Long.class);
                String authorities = claims.get("authorities", String.class);

                if (email == null || userId == null) {
                    sendUnauthorized(response, "Invalid token: missing user info");
                    return;
                }

                List<GrantedAuthority> auths = authorities != null
                        ? AuthorityUtils.commaSeparatedStringToAuthorityList(authorities)
                        : List.of();

                CustomUserPrincipal principal = new CustomUserPrincipal(userId, email);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(principal, jwt, auths);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                sendUnauthorized(response, "Invalid or expired token: " + e.getMessage());
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                String.format("{\"error\":\"Unauthorized\",\"message\":\"%s\"}", message)
        );
    }
}
