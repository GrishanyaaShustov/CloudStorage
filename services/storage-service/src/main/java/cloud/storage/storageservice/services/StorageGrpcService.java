package cloud.storage.storageservice.services;

import cloud.storage.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;
import com.google.protobuf.ByteString;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

@GrpcService
@RequiredArgsConstructor
public class StorageGrpcService extends StorageServiceGrpc.StorageServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(StorageGrpcService.class);
    private final S3ProviderService s3ProviderService;

    @Override
    public void uploadFile(UploadFileRequest request, StreamObserver<UploadFileResponse> responseObserver) {
        log.debug("Upload request received for key: {}", request.getKey());
        try {
            s3ProviderService.uploadFile(
                    request.getKey(),
                    new ByteArrayInputStream(request.getFileData().toByteArray()),
                    request.getFileData().size(),
                    request.getContentType()
            );
            log.info("File successfully uploaded: {}", request.getKey());
            responseObserver.onNext(UploadFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File uploaded successfully")
                    .build());
        } catch (Exception e) {
            log.error("Failed to upload file with key '{}': {}", request.getKey(), e.getMessage(), e);
            responseObserver.onNext(UploadFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error: " + e.getMessage())
                    .build());
        } finally {
            responseObserver.onCompleted();
        }
    }

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

    @Override
    public void downloadFile(DownloadFileRequest request, StreamObserver<DownloadFileResponse> responseObserver) {
        log.debug("Download request received for key: {}", request.getKey());
        try {
            InputStream inputStream = s3ProviderService.downloadFile(request.getKey());

            // Читаем InputStream в byte[]
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[4096];
            int nRead;
            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            byte[] fileBytes = buffer.toByteArray();

            log.info("File successfully downloaded: {}, size: {} bytes", request.getKey(), fileBytes.length);

            responseObserver.onNext(DownloadFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File downloaded successfully")
                    .setFileData(ByteString.copyFrom(fileBytes))
                    .build());

        } catch (Exception e) {
            log.error("Failed to download file with key '{}': {}", request.getKey(), e.getMessage(), e);
            responseObserver.onNext(DownloadFileResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Error downloading file: " + e.getMessage())
                    .build());
        } finally {
            responseObserver.onCompleted();
        }
    }
}
