package cloud.storage.userservice.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "grpc.storage")
@Getter
@Setter
public class GrpcServerProperties {
    private String host;
    private String port;

    public String getAddress(){
        return this.host + ":" + this.port;
    }
}
