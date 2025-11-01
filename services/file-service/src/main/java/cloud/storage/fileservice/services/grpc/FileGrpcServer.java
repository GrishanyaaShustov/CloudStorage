package cloud.storage.fileservice.services.grpc;

import cloud.storage.fileservice.configuration.grpc.GrpcServerInterceptor;
import cloud.storage.fileservice.customExceptions.grpcExceptions.GrpcAccessDeniedException;
import cloud.storage.fileservice.customExceptions.grpcExceptions.GrpcFileNotFoundException;
import cloud.storage.fileservice.models.File;
import cloud.storage.fileservice.repository.FileRepository;
import cloud.storage.fileservice.services.S3Services.S3Service;
import fileservice.File.*;
import fileservice.FileServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;

@RequiredArgsConstructor
@GrpcService(interceptors = GrpcServerInterceptor.class)
public class FileGrpcServer extends FileServiceGrpc.FileServiceImplBase {

    private final FileRepository fileRepository;
    private final S3Service s3Service;

    @Override
    @Transactional
    public void deleteFile(DeleteFileRequest request, StreamObserver<DeleteFileResponse> responseObserver) {
        try {
            Long fileId = request.getFileId();

            // Получаем userId из контекста (установленного серверным перехватчиком)
            Long userId = GrpcServerInterceptor.USER_ID_CTX_KEY.get();

            File file = fileRepository.findFileById(fileId).orElseThrow(() -> new GrpcFileNotFoundException("File not found"));
            if (!file.getUser().getId().equals(userId)) {
                responseObserver.onError(
                        Status.PERMISSION_DENIED.withDescription("Access denied").asRuntimeException()
                );
                return;
            }

            s3Service.deleteFile(file.getS3Key());
            fileRepository.delete(file);

            DeleteFileResponse response = DeleteFileResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("File with id: " + fileId + " - deleted by user with id: " + userId)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (GrpcFileNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException()
            );
        } catch (GrpcAccessDeniedException e) {
            responseObserver.onError(
                    Status.PERMISSION_DENIED.withDescription(e.getMessage()).asRuntimeException()
            );
        } catch (Exception e) {
            responseObserver.onError(
                    Status.INTERNAL.withDescription("Unexpected error: " + e.getMessage()).asRuntimeException()
            );
        }
    }
}
