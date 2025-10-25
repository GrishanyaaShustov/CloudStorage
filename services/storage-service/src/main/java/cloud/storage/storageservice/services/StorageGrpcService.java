package cloud.storage.storageservice.services;

import cloud.storage.grpc.StorageServiceGrpc;
import cloud.storage.grpc.UploadFileRequest;
import cloud.storage.grpc.UploadFileResponse;
import cloud.storage.storageservice.configuration.S3Configuration;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class StorageGrpcService extends StorageServiceGrpc.StorageServiceImplBase{

    private static final Logger log = LoggerFactory.getLogger(StorageGrpcService.class);

    private final S3Configuration s3Configuration;
    private final S3Client s3Client;

    @Override
    public StreamObserver<UploadFileRequest> uploadFile(StreamObserver<UploadFileResponse> responseObserver) {
        return new StreamObserver<>() {
            private String key;
            private String contentType;
            private String uploadId;
            private final List<CompletedPart> completedParts = new ArrayList<>();
            private int partNumber = 1;

            @Override
            public void onNext(UploadFileRequest chunk) {
                try {
                    if (uploadId == null) {
                        key = chunk.getKey();
                        contentType = chunk.getContentType() != null
                                ? chunk.getContentType()
                                : "application/octet-stream";

                        // 1️⃣ Инициализация multipart upload
                        CreateMultipartUploadRequest createRequest = CreateMultipartUploadRequest.builder()
                                .bucket(s3Configuration.getBucket())
                                .key(key)
                                .contentType(contentType)
                                .build();
                        uploadId = s3Client.createMultipartUpload(createRequest).uploadId();
                        log.info("Started multipart upload: key={}, uploadId={}", key, uploadId);
                    }

                    if (!chunk.getChunkData().isEmpty()) {
                        byte[] data = chunk.getChunkData().toByteArray();

                        // 2️⃣ Загружаем часть в S3
                        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                                .bucket(s3Configuration.getBucket())
                                .key(key)
                                .uploadId(uploadId)
                                .partNumber(partNumber)
                                .build();

                        UploadPartResponse uploadPartResponse = s3Client.uploadPart(uploadPartRequest,
                                RequestBody.fromBytes(data));

                        completedParts.add(CompletedPart.builder()
                                .partNumber(partNumber)
                                .eTag(uploadPartResponse.eTag())
                                .build());

                        log.debug("Uploaded part {} ({} bytes)", partNumber, data.length);
                        partNumber++;
                    }

                    if (chunk.getIsLastChunk()) {
                        // 3️⃣ Завершить multipart upload
                        CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                                .bucket(s3Configuration.getBucket())
                                .key(key)
                                .uploadId(uploadId)
                                .multipartUpload(CompletedMultipartUpload.builder()
                                        .parts(completedParts)
                                        .build())
                                .build();

                        s3Client.completeMultipartUpload(completeRequest);
                        log.info("Completed upload for key={}", key);
                    }

                } catch (Exception e) {
                    log.error("Upload failed for key={}: {}", key, e.getMessage());
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("gRPC upload error: {}", t.getMessage(), t);
                try {
                    if (uploadId != null) {
                        s3Client.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                                .bucket(s3Configuration.getBucket())
                                .key(key)
                                .uploadId(uploadId)
                                .build());
                        log.warn("Aborted multipart upload: key={}", key);
                    }
                } catch (Exception abortEx) {
                    log.error("Failed to abort upload: {}", abortEx.getMessage());
                }

                responseObserver.onNext(UploadFileResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Upload failed: " + t.getMessage())
                        .build());
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                responseObserver.onNext(UploadFileResponse.newBuilder()
                        .setSuccess(true)
                        .setMessage("File uploaded successfully: " + key)
                        .build());
                responseObserver.onCompleted();
            }
        };
    }
}
