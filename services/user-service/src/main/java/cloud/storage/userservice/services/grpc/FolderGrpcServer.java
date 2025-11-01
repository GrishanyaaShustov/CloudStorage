package cloud.storage.userservice.services.grpc;

import cloud.storage.userservice.configuration.grpc.GrpcServerInterceptor;
import cloud.storage.userservice.customExceptions.grpcExceptions.GrpcAccessDeniedException;
import cloud.storage.userservice.customExceptions.grpcExceptions.FileNotFoundException;
import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.repository.FolderRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;
import folderservice.FolderServiceGrpc;
import folderservice.Folder.*;

@RequiredArgsConstructor
@GrpcService(interceptors = GrpcServerInterceptor.class)
public class FolderGrpcServer extends FolderServiceGrpc.FolderServiceImplBase {
    private final FolderRepository folderRepository;

    @Override
    @Transactional(readOnly = true)
    public void getFolderData(GetFolderDataRequest request, StreamObserver<GetFolderDataResponse> responseObserver) {
        try {
            Long folderId = request.getFolderId();

            // Получаем userId из контекста (установленного серверным перехватчиком)
            Long userId = GrpcServerInterceptor.USER_ID_CTX_KEY.get();

            Folder folder = folderRepository.findFolderById(folderId).orElseThrow(() -> new FileNotFoundException("Folder not found"));
            if (!folder.getUser().getId().equals(userId)) {
                responseObserver.onError(
                        Status.PERMISSION_DENIED.withDescription("Access denied").asRuntimeException()
                );
                return;
            }
            GetFolderDataResponse response = GetFolderDataResponse.newBuilder()
                    .setId(folder.getId())
                    .setName(folder.getName())
                    .setParentId(folder.getParent() != null ? folder.getParent().getId() : 0L)
                    .setUserId(folder.getUser().getId())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (FileNotFoundException e) {
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