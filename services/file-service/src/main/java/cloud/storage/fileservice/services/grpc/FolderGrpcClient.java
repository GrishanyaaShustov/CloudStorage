package cloud.storage.fileservice.services.grpc;

import cloud.storage.fileservice.customExceptions.AccessDeniedException;
import cloud.storage.fileservice.customExceptions.FolderNotFoundException;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import folderservice.Folder.*;
import folderservice.FolderServiceGrpc;

@Slf4j
@Service
@RequiredArgsConstructor
public class FolderGrpcClient {

    private final FolderServiceGrpc.FolderServiceBlockingStub folderStub;

    public GetFolderDataResponse getFolderData(Long folderId, Long userId) {
        GetFolderDataRequest request = GetFolderDataRequest.newBuilder()
                .setFolderId(folderId)
                .setUserId(userId)
                .build();

        try {
            return folderStub.getFolderData(request);

        } catch (StatusRuntimeException e) {
            Status.Code code = e.getStatus().getCode();

            switch (code) {
                case NOT_FOUND -> {
                    log.warn("Folder not found (id={}): {}", folderId, e.getMessage());
                    throw new FolderNotFoundException("Folder not found");
                }
                case PERMISSION_DENIED -> {
                    log.warn("Access denied to folder (id={}, userId={}): {}", folderId, userId, e.getMessage());
                    throw new AccessDeniedException("Access denied to this folder");
                }
                default -> {
                    log.error("Unexpected gRPC error: {}", e.getMessage(), e);
                    throw new RuntimeException("Unexpected gRPC error: " + e.getMessage(), e);
                }
            }

        } catch (Exception e) {
            log.error("Unexpected error fetching folder: {}", e.getMessage(), e);
            throw new RuntimeException("Cannot fetch folder", e);
        }
    }
}