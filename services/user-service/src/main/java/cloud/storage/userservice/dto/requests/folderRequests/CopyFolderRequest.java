package cloud.storage.userservice.dto.requests.folderRequests;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CopyFolderRequest {
    private Long copiedFolderId;
    private Long copyFolderId;
}
