package cloud.storage.userservice.services.grpc;

import cloud.storage.userservice.customExceptions.FolderAccessDeniedException;
import cloud.storage.userservice.customExceptions.FolderNotFoundException;
import cloud.storage.userservice.models.Folder;
import cloud.storage.userservice.repository.FolderRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;
import userservice.FolderServiceGrpc;
import userservice.Folder.*;

@RequiredArgsConstructor
@GrpcService
public class FolderGrpcServer extends FolderServiceGrpc.FolderServiceImplBase {
    private final FolderRepository folderRepository;

    @Override
    @Transactional(readOnly = true)
    public void getFolderData(GetFolderDataRequest request, StreamObserver<GetFolderDataResponse> responseObserver) {
        try {
            Long folderId = request.getFolderId();
            Long userId = request.getUserId();

            Folder folder = folderRepository.findFolderById(folderId).orElseThrow(() -> new FolderNotFoundException("Folder not found"));
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
        } catch (FolderNotFoundException e) {
            responseObserver.onError(
                    Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException()
            );
        } catch (FolderAccessDeniedException e) {
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