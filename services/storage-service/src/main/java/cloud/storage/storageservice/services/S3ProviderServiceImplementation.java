package cloud.storage.storageservice.services;

import cloud.storage.storageservice.configuration.S3Configuration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;

@Service
@AllArgsConstructor
public class S3ProviderServiceImplementation implements S3ProviderService {

    private final S3Configuration s3Configuration;
    private final S3Client s3Client;

    @Override
    public void uploadFile(String key, InputStream stream, long size, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(s3Configuration.getBucket())
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(stream, size));
    }

    @Override
    public void deleteFile(String key) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(s3Configuration.getBucket())
                .key(key)
                .build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    @Override
    public void copyFile(String key, String destinationKey) {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(s3Configuration.getBucket())
                .destinationBucket(s3Configuration.getBucket())
                .sourceKey(key)
                .destinationKey(destinationKey)
                .build();
        s3Client.copyObject(copyObjectRequest);
    }

    @Override
    public InputStream downloadFile(String key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Configuration.getBucket())
                .key(key)
                .build();
        return s3Client.getObject(getObjectRequest);
    }

}
