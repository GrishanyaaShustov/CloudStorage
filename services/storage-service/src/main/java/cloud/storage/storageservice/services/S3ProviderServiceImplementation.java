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

    @Override
    public void uploadFileByChunks(String key, String contentType, List<byte[]> chunks) throws IOException {
        // 1. Инициализация multipart upload
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                .bucket(s3Configuration.getBucket())
                .key(key)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
        String uploadId = response.uploadId();

        List<CompletedPart> completedParts = new ArrayList<>();

        try {
            // 2. Загружаем чанки один за другим
            int partNumber = 1;
            for(byte[] chunk: chunks){
                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(s3Configuration.getBucket())
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .build();
                UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest, RequestBody.fromBytes(chunk));

                completedParts.add(CompletedPart.builder()
                        .partNumber(partNumber)
                        .eTag(uploadPartResponse.eTag())
                        .build());

                partNumber++;
            }
            // 3. Завершаем multipart upload
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(s3Configuration.getBucket())
                    .key(key)
                    .uploadId(uploadId)
                    .multipartUpload(CompletedMultipartUpload.builder()
                            .parts(completedParts)
                            .build())
                    .build();
        } catch (Exception e){
            // При ошибке — отменяем multipart upload
            s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(s3Configuration.getBucket())
                    .key(key)
                    .uploadId(uploadId)
                    .build());
            throw new IOException("Error during multipart upload: " + e.getMessage(), e);
        }


    }
}
