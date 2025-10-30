package cloud.storage.fileservice.configuration.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import userservice.FolderServiceGrpc;

@Configuration
public class GrpcConfiguration {
    @Bean
    public ManagedChannel managedChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
    }

    @Bean
    public FolderServiceGrpc.FolderServiceBlockingStub folderServiceStub(ManagedChannel channel) {
        return FolderServiceGrpc.newBlockingStub(channel);
    }
}
