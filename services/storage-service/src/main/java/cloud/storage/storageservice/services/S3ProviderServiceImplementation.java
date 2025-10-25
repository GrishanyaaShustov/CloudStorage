package cloud.storage.storageservice.services;

import cloud.storage.storageservice.configuration.S3Configuration;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.copyOf;

@Service
@AllArgsConstructor
public class S3ProviderServiceImplementation implements S3ProviderService {

    private final S3Configuration s3Configuration;
    private final S3Client s3Client;


    // ---------- Обычная загрузка ----------
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

    // ---------- загрузка по частям ----------
    @Override
    public void uploadFileInParts(String key, InputStream inputStream, String contentType) throws IOException {
        final long partSize = 50L * 1024 * 1024; // 50 MB
        String bucket = s3Configuration.getBucket();

        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse createResponse = s3Client.createMultipartUpload(createRequest);
        String uploadId = createResponse.uploadId();

        List<CompletedPart> completedParts = new ArrayList<>();
        byte[] buffer = new byte[(int) partSize];
        int bytesRead;
        int partNumber = 1;

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();

                UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest,
                        RequestBody.fromBytes(copyOf(buffer, bytesRead)));

                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build());

                partNumber++;
            }
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build();

            s3Client.completeMultipartUpload(completeRequest);
        } catch (Exception e) {
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .uploadId(uploadId)
                    .build());
            throw new IOException("Multipart upload failed: " + e.getMessage(), e);
        }

    }

}
