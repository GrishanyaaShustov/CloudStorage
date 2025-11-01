package cloud.storage.fileservice.configuration.grpc;

import cloud.storage.fileservice.configuration.jwt.JwtProvider;
import io.grpc.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Перехватчик для исходящих gRPC-вызовов.
 * Добавляет авторизационные заголовки т.е JWT в каждый запрос.
 */
@Component
@RequiredArgsConstructor
public class GrpcClientInterceptor implements ClientInterceptor {

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers){
                String jwt = JwtProvider.extractJwtToken();
                if(jwt != null){
                    Metadata.Key<String> authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);
                    headers.put(authKey, "Bearer " + jwt);
                }
                super.start(responseListener, headers);
            }
        };
    }
}
