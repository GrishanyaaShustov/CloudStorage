package cloud.storage.fileservice.services.grpc;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import userservice.Folder;
import userservice.FolderServiceGrpc;

import lombok.RequiredArgsConstructor;

@Service
@Slf4j
@RequiredArgsConstructor
public class FolderGrpcClient {

    private final FolderServiceGrpc.FolderServiceBlockingStub folderStub;

    public Folder.getFolderFolderDataResponse getFolderData(Long folderId, Long userId) {
        Folder.GetFolderDataRequest request = Folder.GetFolderDataRequest.newBuilder()
                .setFolderId(folderId)
                .setUserId(userId)
                .build();

        try {
            return folderStub.getFolderData(request);
        } catch (Exception e) {
            log.error("Error fetching folder from user-service: {}", e.getMessage());
            throw new RuntimeException("Cannot fetch folder", e);
        }
    }
}