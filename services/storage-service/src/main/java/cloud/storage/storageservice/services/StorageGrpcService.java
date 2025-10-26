package cloud.storage.storageservice.services;

import cloud.storage.grpc.StorageServiceGrpc;
import cloud.storage.grpc.UploadFileRequest;
import cloud.storage.grpc.UploadFileResponse;
import cloud.storage.storageservice.configuration.S3Configuration;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.*;
import java.util.concurrent.*;

@GrpcService
@RequiredArgsConstructor
public class StorageGrpcService extends StorageServiceGrpc.StorageServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(StorageGrpcService.class);

    private final S3Configuration s3Configuration;
    private final S3Client s3Client;
    private final MultipartUploader multipartUploader = new MultipartUploader(8);

    @Override
    public StreamObserver<UploadFileRequest> uploadFile(StreamObserver<UploadFileResponse> responseObserver) {
        return new StreamObserver<>() {
            private UploadSession session;

            @Override
            public void onNext(UploadFileRequest chunk) {
                try {
                    if (session == null) {
                        session = multipartUploader.startSession(
                                s3Configuration.getBucket(),
                                chunk.getKey(),
                                chunk.getContentType() != null ? chunk.getContentType() : "application/octet-stream",
                                s3Client,
                                responseObserver
                        );
                    }

                    session.handleChunk(chunk);

                } catch (Exception e) {
                    log.error("Upload error for key={}: {}",
                            chunk.getKey(), e.getMessage(), e);
                    onError(e);
                }
            }

            @Override
            public void onError(Throwable t) {
                if (session != null) {
                    session.fail(t);
                } else {
                    log.error("Upload failed before initialization: {}", t.getMessage(), t);
                    responseObserver.onNext(UploadFileResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Upload failed: " + t.getMessage())
                            .build());
                    responseObserver.onCompleted();
                }
            }

            @Override
            public void onCompleted() {
                if (session != null) {
                    session.finishIfIncomplete();
                }
            }
        };
    }

    @PreDestroy
    public void shutdownExecutor() {
        multipartUploader.shutdown();
    }
}
