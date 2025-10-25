package cloud.storage.storageservice.services;

import cloud.storage.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

@GrpcService
@RequiredArgsConstructor
public class StorageGrpcService extends StorageServiceGrpc.StorageServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(StorageGrpcService.class);
    private final S3ProviderService s3ProviderService;

    @Override
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {
        log.debug("Delete request received for key: {}", request.getKey());
        try {
            s3ProviderService.deleteFile(request.getKey());
            log.info("File successfully deleted: {}", request.getKey());
            responseObserver.onNext(DeleteFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File deleted successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to delete file with key '{}': {}", request.getKey(), e.getMessage(), e);
            responseObserver.onNext(DeleteFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
        } finally {
            responseObserver.onCompleted();
        }
    }

    @Override
    public void copyFile(CopyFileRequest request, StreamObserver<CopyFileResponse> responseObserver) {
        log.debug("Copy request received from '{}' to '{}'", request.getKey(), request.getDestinationKey());
        try {
            s3ProviderService.copyFile(request.getKey(), request.getDestinationKey());
            log.info("File successfully copied from '{}' to '{}'", request.getKey(), request.getDestinationKey());
            responseObserver.onNext(CopyFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File copied successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to copy file from '{}' to '{}': {}", request.getKey(), request.getDestinationKey(), e.getMessage(), e);
            responseObserver.onNext(CopyFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error copying file: " + e.getMessage())
                    .build());
        } finally {
            responseObserver.onCompleted();
        }
    }
}
