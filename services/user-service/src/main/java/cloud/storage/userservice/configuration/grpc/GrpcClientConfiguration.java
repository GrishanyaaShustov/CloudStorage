package cloud.storage.userservice.configuration.grpc;

import folderservice.FolderServiceGrpc;
import io.grpc.Channel;
import io.grpc.ClientInterceptors;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GrpcClientConfiguration {

    private final GrpcClientInterceptor grpcClientInterceptor;

    @Bean
    public Channel managedChannel() {

        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        return ClientInterceptors.intercept(channel, grpcClientInterceptor);
    }

    @Bean
    public FolderServiceGrpc.FolderServiceBlockingStub folderServiceStub(Channel channel) {
        return FolderServiceGrpc.newBlockingStub(channel);
    }
}
