package cloud.storage.fileservice.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.async.SdkAsyncHttpClient;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;
import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "spring.storage.s3")
@Getter
@Setter
public class S3Configuration {
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String region;
    private String bucket;

    @Bean
    public S3Client s3Client() throws Exception {
        return S3Client.builder()
                .endpointOverride(new URI(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }

    @Bean
    public S3AsyncClient s3AsyncClient() throws Exception {
        SdkAsyncHttpClient httpClient = NettyNioAsyncHttpClient.builder()
                .writeTimeout(Duration.ofMinutes(5))   // важно для больших частей
                .readTimeout(Duration.ofMinutes(5))
                .maxConcurrency(100)                   // позволяет много одновременных частей
                .build();

        return S3AsyncClient.builder()
                .httpClient(httpClient)
                .endpointOverride(new URI(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
