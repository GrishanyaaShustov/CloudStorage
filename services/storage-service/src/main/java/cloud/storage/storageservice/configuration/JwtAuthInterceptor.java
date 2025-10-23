package cloud.storage.storageservice.configuration;

import io.grpc.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtAuthInterceptor implements ServerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthInterceptor.class);
    private final JwtValidator jwtValidator;

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        String authHeader = headers.get(Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER));
        if(authHeader == null || authHeader.isEmpty()) {
            log.warn("gRPC call rejected: missing Authorization header. Method: {}", call.getMethodDescriptor().getFullMethodName());
            call.close(Status.UNAUTHENTICATED.withDescription("Authorization header misssing"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        try{
            jwtValidator.validateToken(authHeader);
            log.debug("JWT validation successful for method: {}", call.getMethodDescriptor().getFullMethodName());
        } catch (SecurityException e){
            log.warn("JWT validation failed for method: {} - {}", call.getMethodDescriptor().getFullMethodName(), e.getMessage());
            call.close(Status.UNAUTHENTICATED.withDescription("Invalid JWT"), new Metadata());
            return new ServerCall.Listener<>() {};
        }

        return next.startCall(call, headers);
    }

}
