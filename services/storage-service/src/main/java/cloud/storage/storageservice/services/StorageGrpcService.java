package cloud.storage.storageservice.services;

import cloud.storage.grpc.*;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.grpc.server.service.GrpcService;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

@GrpcService
@RequiredArgsConstructor
public class StorageGrpcService extends StorageServiceGrpc.StorageServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(StorageGrpcService.class);

    private final S3ProviderService s3ProviderService;

    // ==================== Upload ====================
    @Override
    public StreamObserver<UploadFileRequest> uploadFile(StreamObserver<UploadFileResponse> responseObserver) {
        return new StreamObserver<>() {
            private static final long CHUNK_THRESHOLD = 100L * 1024 * 1024; // 100 MB
            private String key;
            private String contentType;
            private long totalBytes;
            private final List<byte[]> smallFileChunks = new ArrayList<>();
            private boolean isLargeFile = false;

            private Thread uploadThread; // поток для больших файлов

            @Override
            public void onNext(UploadFileRequest chunk) {
                if (key == null) key = chunk.getKey();
                if (contentType == null) contentType = chunk.getContentType();

                byte[] data = chunk.getChunkData().toByteArray();
                totalBytes += data.length;

                try {
                    if (!isLargeFile && totalBytes > CHUNK_THRESHOLD) {
                        // переключаемся на большой файл
                        isLargeFile = true;

                        // стартуем uploadThread для отправки чанков
                        List<byte[]> initialChunks = new ArrayList<>(smallFileChunks);
                        smallFileChunks.clear();

                        uploadThread = new Thread(() -> {
                            try {
                                s3ProviderService.uploadFileByChunks(key, contentType, initialChunks);
                            } catch (Exception e) {
                                log.error("Failed initial chunked upload: {}", e.getMessage(), e);
                            }
                        });
                        uploadThread.start();
                    }

                    if (isLargeFile) {
                        // для больших файлов: каждый чанк в отдельном потоке
                        byte[] chunkCopy = data;
                        new Thread(() -> {
                            try {
                                s3ProviderService.uploadFileByChunks(key, contentType, List.of(chunkCopy));
                            } catch (Exception e) {
                                log.error("Failed to upload chunk: {}", e.getMessage(), e);
                            }
                        }).start();
                    } else {
                        // малый файл — накапливаем
                        smallFileChunks.add(data);
                    }
                } catch (Exception e) {
                    log.error("Error processing chunk for key {}: {}", key, e.getMessage(), e);
                    responseObserver.onNext(UploadFileResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Error processing chunk: " + e.getMessage())
                            .build());
                    responseObserver.onCompleted();
                }
            }

            @Override
            public void onError(Throwable t) {
                log.error("Upload failed for key {}: {}", key, t.getMessage(), t);
                responseObserver.onNext(UploadFileResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Upload failed: " + t.getMessage())
                        .build());
                responseObserver.onCompleted();
            }

            @Override
            public void onCompleted() {
                try {
                    if (!isLargeFile) {
                        // малый файл — обычная загрузка
                        int size = smallFileChunks.stream().mapToInt(b -> b.length).sum();
                        byte[] fileBytes = new byte[size];
                        int offset = 0;
                        for (byte[] c : smallFileChunks) {
                            System.arraycopy(c, 0, fileBytes, offset, c.length);
                            offset += c.length;
                        }
                        s3ProviderService.uploadFile(key, new ByteArrayInputStream(fileBytes), size, contentType);
                    } else if (uploadThread != null) {
                        // ждём завершения потока с большими файлами
                        uploadThread.join();
                    }

                    responseObserver.onNext(UploadFileResponse.newBuilder()
                            .setSuccess(true)
                            .setMessage("File uploaded successfully")
                            .build());
                    log.info("File successfully uploaded: {}", key);
                } catch (Exception e) {
                    log.error("Failed to upload file '{}': {}", key, e.getMessage(), e);
                    responseObserver.onNext(UploadFileResponse.newBuilder()
                            .setSuccess(false)
                            .setMessage("Error uploading file: " + e.getMessage())
                            .build());
                } finally {
                    responseObserver.onCompleted();
                }
            }
        };
    }



    // ==================== Delete ====================
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

    // ==================== Copy ====================
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
