package cloud.storage.userservice.services;

import cloud.storage.grpc.StorageServiceGrpc;
import cloud.storage.userservice.DTO.requests.fileRequests.*;
import cloud.storage.userservice.DTO.responses.fileResponses.*;
import cloud.storage.userservice.configuration.GrpcServerProperties;
import cloud.storage.userservice.configuration.JwtClientInterceptor;
import cloud.storage.userservice.models.File;
import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.models.User;
import cloud.storage.userservice.repository.FileRepository;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.Principal;

@Service
@RequiredArgsConstructor
public class FileServiceImplementation implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImplementation.class);

    private final FileRepository fileRepository;

    private final HelperService helperService;

    private final JwtClientInterceptor jwtClientInterceptor;
    private final GrpcServerProperties grpcServerProperties;

    @Override
    @Transactional
    public UploadFileResponse uploadFile(UploadFileRequest request, Principal principal) throws SecurityException {
        helperService.validateFileNotEmpty(request.getFile());
        User user = helperService.validateAndGetUser(principal);
        Folder folder = helperService.validateAndGetFolder(request.getFolderId(), user);
        helperService.validateFileNameUniqueness(request.getFile().getOriginalFilename(), folder, user);

        MultipartFile file = request.getFile();
        String key = helperService.buildS3Key(file.getOriginalFilename());

        UploadFileResponse grpcResponse = sendFileToStorageService(file, key);

        if(grpcResponse != null && grpcResponse.isSuccess()){
            fileRepository.save(File.builder()
                    .name(file.getOriginalFilename())
                    .contentType(file.getContentType())
                    .size(file.getSize())
                    .user(user)
                    .folder(folder)
                    .s3Key(key)
                    .build());
        }
        return grpcResponse != null ? grpcResponse : new UploadFileResponse("Something went wrong with file upload", false);
    }

    @Override
    public CopyFileResponse copyFile(CopyFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public ReplaceFileResponse replaceFile(ReplaceFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public DeleteFileResponse deleteFile(DeleteFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public GetFilesInDirectoryResponse getFilesInDirectory(GetFilesInDirectoryRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public DownloadFileResponse downloadFile(DownloadFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    @Override
    public RenameFileResponse renameFile(RenameFileRequest request, Principal principal) throws SecurityException {
        return null;
    }

    private UploadFileResponse sendFileToStorageService(MultipartFile file, String key) {
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(grpcServerProperties.getHost(), grpcServerProperties.getPort())
                .intercept(jwtClientInterceptor)
                .usePlaintext()
                .build();

        StorageServiceGrpc.StorageServiceStub asyncStub = StorageServiceGrpc.newStub(channel);
        final Object lock = new Object();
        final UploadFileResponse[] grpcResponse = new UploadFileResponse[1];

        StreamObserver<cloud.storage.grpc.UploadFileResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(cloud.storage.grpc.UploadFileResponse value) {
                grpcResponse[0] = new UploadFileResponse(value.getMessage(), value.getSuccess());
            }

            @Override
            public void onError(Throwable t) {
                grpcResponse[0] = new UploadFileResponse("Upload failed: " + t.getMessage(), false);
                synchronized (lock) {
                    lock.notify();
                }
            }

            @Override
            public void onCompleted() {
                synchronized (lock) {
                    lock.notify();
                }
            }
        };

        StreamObserver<cloud.storage.grpc.UploadFileRequest> requestObserver = asyncStub.uploadFile(responseObserver);

        try (InputStream inputStream = file.getInputStream()) {
            byte[] buffer = new byte[1024 * 1024 * 25]; // 25 MB
            int bytesRead;
            int chunkIndex = 0;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                boolean isLast = bytesRead < buffer.length && inputStream.available() == 0;

                cloud.storage.grpc.UploadFileRequest chunk = cloud.storage.grpc.UploadFileRequest.newBuilder()
                        .setKey(key)
                        .setContentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                        .setChunkIndex(chunkIndex++)
                        .setChunkData(com.google.protobuf.ByteString.copyFrom(buffer, 0, bytesRead))
                        .setIsLastChunk(isLast)
                        .build();

                requestObserver.onNext(chunk);

                if (isLast) break;
            }

            requestObserver.onCompleted();

            synchronized (lock) {
                lock.wait(); // ждем завершения
            }

        } catch (Exception e) {
            log.error("Error uploading file via gRPC: {}", e.getMessage(), e);
            requestObserver.onError(e);
            return new UploadFileResponse("Error: " + e.getMessage(), false);
        } finally {
            channel.shutdown();
        }

        return grpcResponse[0];
    }
}
