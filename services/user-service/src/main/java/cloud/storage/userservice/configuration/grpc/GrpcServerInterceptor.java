package cloud.storage.userservice.configuration.grpc;

import cloud.storage.userservice.configuration.jwt.JwtProperties;
import io.grpc.*;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;


/**
 * Перехватчик для входящих gRPC-вызовов.
 * Проверяет авторизационные заголовки т.е JWT на каждом запросе.
 */
@Component
public class GrpcServerInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(GrpcServerInterceptor.class);

    private final SecretKey secretKey;

    // Контекстный ключ для передачи userId в метод сервиса
    public static final Context.Key<Long> USER_ID_CTX_KEY = Context.key("userId");
    private static final Metadata.Key<String> AUTHORIZATION_HEADER = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    public GrpcServerInterceptor(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getKey().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String authHeader = headers.get(AUTHORIZATION_HEADER);
        if(authHeader == null || !authHeader.startsWith("Bearer ")){
            log.warn("gRPC request missing JWT");
            call.close(Status.UNAUTHENTICATED.withDescription("Missing or invalid JWT"), headers);
            return new ServerCall.Listener<>() {};
        }
        String jwt = authHeader.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            Long userId = claims.get("userId", Long.class);
            if(userId == null){
                call.close(Status.UNAUTHENTICATED.withDescription("JWT does not contain userId"), headers);
                return new ServerCall.Listener<>() {};
            }
            // Создаём контекст с userId и передаём дальше
            Context ctx = Context.current().withValue(USER_ID_CTX_KEY, userId);
            return Contexts.interceptCall(ctx, call, headers, next);
        }
        catch (Exception e){
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT"), headers);
            return new ServerCall.Listener<>() {};
        }
    }
}
