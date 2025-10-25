package cloud.storage.userservice.configuration;

import io.grpc.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class JwtClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers){
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if(authentication != null && authentication.getCredentials() instanceof String jwtToken){
                    Metadata.Key<String> AUTH_HEADER =
                            Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
                    headers.put(AUTH_HEADER, jwtToken);
                }

                super.start(responseListener, headers);
            }
        };
    }
}
