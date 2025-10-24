package cloud.storage.userservice.DTO.requests.folderRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CopyFolderRequest {
    private Long copiedFolderId;
    private Long copyFolderId;
}
